package me.brazdil.libmywatch;

import android.bluetooth.BluetoothDevice;

public interface DeviceScannerCallback {
    public void onScanStarted();
    public void onScanStopped();

    public void onDeviceFound(BluetoothDevice device);
}
