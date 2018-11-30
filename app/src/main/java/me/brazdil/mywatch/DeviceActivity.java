package me.brazdil.mywatch;

import android.app.Dialog;
import android.app.TimePickerDialog;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.preference.PreferenceManager;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.TimePicker;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.Random;

public class DeviceActivity extends AppCompatActivity {
    private static final String TAG = DeviceActivity.class.getSimpleName();

    private BroadcastReceiver mDeviceUpdateReceiver;
    private IntentFilter mDeviceUpdateIntentFilter;

    private TextView mTextDeviceName;
    private TextView mTextDeviceAddress;
    private TextView mTextDeviceStatus;
    private TextView mTextDeviceLastSynced;
    private TextView mTextDeviceBatteryLevel;
    private TextView mTextDeviceSerialNumber;
    private TextView mTextDeviceSoftwareRevision;
    private TextView mTextWatchTime;
    private TextView mTextPhoneTime;
    private Button mButtonSyncTime;
    private Button mButtonSyncTimeRandom;
    private Button mButtonSyncTimePicker;

    private Random mRandom = new Random();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device);

        mTextDeviceName = (TextView) findViewById(R.id.textName);
        mTextDeviceAddress = (TextView) findViewById(R.id.textAddress);
        mTextDeviceStatus = (TextView) findViewById(R.id.textStatus);
        mTextDeviceLastSynced = (TextView) findViewById(R.id.textLastSynced);
        mTextDeviceBatteryLevel = (TextView) findViewById(R.id.textBatteryLevel);
        mTextDeviceSerialNumber = (TextView) findViewById(R.id.textSerialNumber);
        mTextDeviceSoftwareRevision = (TextView) findViewById(R.id.textSoftwareRevision);
        mTextWatchTime = (TextView) findViewById(R.id.textWatchTime);
        mTextPhoneTime = (TextView) findViewById(R.id.textPhoneTime);

        mDeviceUpdateReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (DeviceConnection.ACTION_DEVICE_UPDATE.equals(intent.getAction())) {
                    Log.d(TAG, "Received a device update");
                    onDeviceUpdate(intent);
                }
            }
        };

        mDeviceUpdateIntentFilter = new IntentFilter(DeviceConnection.ACTION_DEVICE_UPDATE);

        mButtonSyncTime = (Button) findViewById(R.id.btnSyncTime);
        mButtonSyncTime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                syncDeviceTime(-1L);
            }
        });

        mButtonSyncTimeRandom = (Button) findViewById(R.id.btnSyncTimeRandom);
        mButtonSyncTimeRandom.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int offset = mRandom.nextInt(24 * 3600 * 1000);
                syncDeviceTime(Calendar.getInstance().getTimeInMillis() + offset);
            }
        });

        mButtonSyncTimePicker = (Button) findViewById(R.id.btnSyncTimePicker);
        mButtonSyncTimePicker.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TimePickerFragment fragment = new TimePickerFragment();
                Bundle args = new Bundle();
                args.putString(TimePickerFragment.ARG_DEVICE_ADDR,
                        mTextDeviceAddress.getText().toString());
                fragment.setArguments(args);
                fragment.show(getSupportFragmentManager(), "timePicker");
            }
        });

    }

    @Override
    public void onStart() {
        super.onStart();

        boolean devicesConfigured = this.getIntent().hasExtra(Constants.EXTRA_NEW_DEVICE_ADDR) ||
                !PreferenceManager.getDefaultSharedPreferences(this)
                        .getStringSet(Constants.PREF_CONNECTED_DEVICE_ADDRS,
                                Collections.<String>emptySet())
                        .isEmpty();
        if (devicesConfigured) {
            Intent intent = new Intent(this, DeviceService.class);
            startService(intent);
        } else {
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
        }

        registerReceiver(mDeviceUpdateReceiver, mDeviceUpdateIntentFilter);
    }

    @Override
    public void onStop() {
        super.onStop();
        stopService(new Intent(this, DeviceService.class));
        unregisterReceiver(mDeviceUpdateReceiver);
    }

    private void updateStringText(TextView tv, Intent intent, String extraName) {
        String val = intent.getStringExtra(extraName);
        if (val == null) {
            tv.setText("unknown");
        } else {
            tv.setText(val);
        }
    }

    private void syncDeviceTime(long millis) {
        syncDeviceTime(this, millis, mTextDeviceAddress.getText().toString());
    }

    private static void syncDeviceTime(Context context, long millis, String address) {
        final Intent intent = new Intent(DeviceConnection.ACTION_DEVICE_SYNC_TIME);
        intent.putExtra(DeviceConnection.EXTRA_ADDRESS, address);
        intent.putExtra(DeviceConnection.EXTRA_WATCH_CLOCK, millis);
        context.sendBroadcast(intent);
    }

    private void onDeviceUpdate(Intent intent) {
        updateStringText(mTextDeviceName, intent, DeviceConnection.EXTRA_NAME);
        updateStringText(mTextDeviceAddress, intent, DeviceConnection.EXTRA_ADDRESS);
        updateStringText(mTextDeviceSerialNumber, intent, DeviceConnection.EXTRA_SERIAL_NUMBER);
        updateStringText(mTextDeviceSoftwareRevision, intent,
                DeviceConnection.EXTRA_SOFTWARE_REVISION);

        int batteryLevel = intent.getIntExtra(DeviceConnection.EXTRA_BATTERY_LEVEL, -1);
        if (batteryLevel < 0 || batteryLevel > 100) {
            mTextDeviceBatteryLevel.setText("unknown");
        } else {
            mTextDeviceBatteryLevel.setText(String.valueOf(batteryLevel) + "%");
        }

        int deviceState = intent.getIntExtra(DeviceConnection.EXTRA_STATE,
                BluetoothProfile.STATE_DISCONNECTED);
        switch (deviceState) {
            case BluetoothProfile.STATE_CONNECTED:
                mTextDeviceStatus.setText("connected");
                break;
            case BluetoothProfile.STATE_CONNECTING:
                mTextDeviceStatus.setText("connecting");
                break;
            case BluetoothProfile.STATE_DISCONNECTED:
                mTextDeviceStatus.setText("disconnected");
                break;
            case BluetoothProfile.STATE_DISCONNECTING:
                mTextDeviceStatus.setText("disconnecting");
                break;
        }

        final Calendar cal = Calendar.getInstance();
        final SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss dd/MM/yyyy");

        long watchClock = intent.getLongExtra(DeviceConnection.EXTRA_WATCH_CLOCK, -1);
        if (watchClock < 0) {
            mTextWatchTime.setText("unknown");
        } else {
            cal.setTimeInMillis(watchClock);
            mTextWatchTime.setText(dateFormat.format(cal.getTime()));
        }

        long phoneClock = intent.getLongExtra(DeviceConnection.EXTRA_PHONE_CLOCK, -1);
        if (phoneClock < 0) {
            mTextPhoneTime.setText("unknown");
        } else {
            cal.setTimeInMillis(phoneClock);
            mTextPhoneTime.setText(dateFormat.format(cal.getTime()));
        }
    }

    public static class TimePickerFragment extends DialogFragment
            implements TimePickerDialog.OnTimeSetListener {

        public static final String ARG_DEVICE_ADDR = "device_address";

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            // Use the current time as the default values for the picker
            final Calendar c = Calendar.getInstance();
            int hour = c.get(Calendar.HOUR_OF_DAY);
            int minute = c.get(Calendar.MINUTE);

            // Create a new instance of TimePickerDialog and return it
            return new TimePickerDialog(getActivity(), this, hour, minute,
                    DateFormat.is24HourFormat(getActivity()));
        }

        public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.HOUR_OF_DAY, hourOfDay);
            cal.set(Calendar.MINUTE, minute);
            cal.clear(Calendar.SECOND);
            cal.clear(Calendar.MILLISECOND);
            DeviceActivity.syncDeviceTime(getActivity(), cal.getTimeInMillis(),
                    getArguments().getString(ARG_DEVICE_ADDR));
        }
    }
}