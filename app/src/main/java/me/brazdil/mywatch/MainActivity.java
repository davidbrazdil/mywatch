package me.brazdil.mywatch;

import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import me.brazdil.libmywatch.DeviceScanner;
import me.brazdil.libmywatch.DeviceScannerCallback;

public class MainActivity extends AppCompatActivity implements DeviceScannerCallback {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mDeviceScanner = new DeviceScanner(this, this);

        mScanningProgressBar = (ProgressBar) findViewById(R.id.progScanning);
        mScanningProgressBar.setVisibility(View.INVISIBLE);

        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.listDevices);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(mDeviceAdapter);
    }

    private void showLocationPermissionRequestDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.location_request_dialog_msg)
               .setTitle(R.string.location_request_dialog_title);
        AlertDialog dialog = builder.create();
        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                mDeviceScanner.requestLocationPermission(MainActivity.this,
                        ID_REQUEST_LOCATION_PERMISSION);
            }
        });
        dialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        dialog.show();
    }

    private void startDeviceScan() {
        if (!mDeviceScanner.hasLocationPermission()) {
            showLocationPermissionRequestDialog();
            return;
        }

        mDeviceScanner.startScan();
    }

    @Override
    protected void onStart() {
        super.onStart();
        startDeviceScan();
    }

    @Override
    public void onScanStarted() {
        mScanningProgressBar.setVisibility(View.VISIBLE);
    }

    @Override
    public void onScanStopped() {
        mScanningProgressBar.setVisibility(View.INVISIBLE);
    }

    @Override
    public void onDeviceFound(BluetoothDevice device) {
        mDeviceAdapter.addDevice(device);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case ID_REQUEST_LOCATION_PERMISSION:
                startDeviceScan();
                break;
        }
    }

    private class DeviceViewHolder extends RecyclerView.ViewHolder {
        private ConstraintLayout mTopLayout;
        private TextView mName;
        private TextView mAddress;

        public DeviceViewHolder(View v) {
            super(v);
            mTopLayout = (ConstraintLayout) v;
            mName = (TextView) v.findViewById(R.id.textName);
            mAddress = (TextView) v.findViewById(R.id.textAddress);
        }

        public void setFrom(final BluetoothDevice device) {
            mName.setText(device.getName());
            mAddress.setText(device.getAddress());

            mTopLayout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Toast.makeText(MainActivity.this, "Selected device: " + device.getAddress(), Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private class DeviceListAdapter extends RecyclerView.Adapter<DeviceViewHolder> {
        private List<BluetoothDevice> mDevices = new ArrayList<>();

        public void addDevice(BluetoothDevice device) {
            if (device.getName() == null) {
                return;
            } else if (mDevices.contains(device)) {
                return;
            }

            mDevices.add(device);
            notifyItemInserted(mDevices.size() - 1);
        }

        @Override
        public DeviceViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new DeviceViewHolder(LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.device_scan_list_item_layout, parent, false));
        }

        @Override
        public void onBindViewHolder(DeviceViewHolder holder, int position) {
            holder.setFrom(mDevices.get(position));
        }

        @Override
        public int getItemCount() {
            return mDevices.size();
        }
    }

    private static final int ID_REQUEST_LOCATION_PERMISSION = 1;

    private DeviceScanner mDeviceScanner;
    private DeviceListAdapter mDeviceAdapter = new DeviceListAdapter();

    private ProgressBar mScanningProgressBar;
}
