package me.brazdil.mywatch;

import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;

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
        final DateFormat dateFormat = new SimpleDateFormat("HH:mm:ss dd/MM/yyyy");

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
}