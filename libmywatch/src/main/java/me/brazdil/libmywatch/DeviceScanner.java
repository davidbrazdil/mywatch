package me.brazdil.libmywatch;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;

import java.util.List;

public class DeviceScanner {
    private final BluetoothAdapter mBluetoothAdapter;
    private final Context mContext;
    private final Handler mHandler;
    private final long mScanPeriod;
    private final DeviceScannerCallback mOwnerCallback;

    private static final long DEFAULT_SCAN_PERIOD = 60 * 1000;

    public DeviceScanner(Context context, DeviceScannerCallback callback) {
        this(context, callback, DEFAULT_SCAN_PERIOD);
    }

    public DeviceScanner(Context context, DeviceScannerCallback callback, long scanPeriod) {
        mContext = context;
        mScanPeriod = scanPeriod;
        mOwnerCallback = callback;
        mHandler = new Handler();
        mBluetoothAdapter = ((BluetoothManager)
                mContext.getSystemService(Context.BLUETOOTH_SERVICE)).getAdapter();
    }

    public void startScan() {
        if (!hasLocationPermission()) {
            throw new IllegalStateException("Request location permission first");
        }

        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                DeviceScanner.this.stopScan();
            }
        }, mScanPeriod);

        mBluetoothAdapter.getBluetoothLeScanner().startScan(mFrameworkCallback);
        mOwnerCallback.onScanStarted();
    }

    public void stopScan() {
        mBluetoothAdapter.getBluetoothLeScanner().stopScan(mFrameworkCallback);
        mOwnerCallback.onScanStopped();
    }

    private final ScanCallback mFrameworkCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            mOwnerCallback.onDeviceFound(result.getDevice());
        }
    };

    public boolean hasLocationPermission() {
        return hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION);
    }

    public boolean requestLocationPermission(Activity activity, int requestId) {
        if (hasLocationPermission()) {
            return false;
        }

        String[] requestArgs = new String[] { Manifest.permission.ACCESS_COARSE_LOCATION };
        ActivityCompat.requestPermissions(activity, requestArgs, requestId);
        return true;
    }

    private boolean hasPermission(String permission) {
        return ContextCompat.checkSelfPermission(mContext, permission)
                == PackageManager.PERMISSION_GRANTED;
    }
}
