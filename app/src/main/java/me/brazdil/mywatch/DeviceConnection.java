package me.brazdil.mywatch;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;

public class DeviceConnection {
    private final BluetoothAdapter mBluetoothAdapter;
    private final Context mContext;
    private final BluetoothDevice mDevice;
    private long mLastSynced;

    /* package */ DeviceConnection(Context context, String deviceAddress) {
        mContext = context;
        mBluetoothAdapter = ((BluetoothManager)
                mContext.getSystemService(Context.BLUETOOTH_SERVICE)).getAdapter();
        mDevice = mBluetoothAdapter.getRemoteDevice(deviceAddress);
        mLastSynced = 0;
    }

    public String getName() {
        return mDevice.getName();
    }

    public String getAddress() {
        return mDevice.getAddress();
    }

    public long getLastSynced() {
        return mLastSynced;
    }

    public void readAllData() {
        mLastSynced = System.currentTimeMillis();
    }
}
