package me.brazdil.mywatch;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.ArraySet;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class DeviceService extends Service {
    public class DeviceConnectionServiceBinder extends Binder {
        public DeviceService getService() { return DeviceService.this; }
    }

    private DeviceConnectionServiceBinder mBinder = new DeviceConnectionServiceBinder();

    @Override
    public IBinder onBind(Intent intent) { return mBinder; }

    private List<DeviceConnection> mDeviceConnections;

    @Override
    public void onCreate() {
        super.onCreate();

        mDeviceConnections = new ArrayList<>();
        for (String addr : getDeviceAddressesSetFromPrefs()) {
            mDeviceConnections.add(new DeviceConnection(this, addr));
        }
    }

    private Set<String> getDeviceAddressesSetFromPrefs() {
        return PreferenceManager.getDefaultSharedPreferences(this).getStringSet(
                Constants.PREF_CONNECTED_DEVICE_ADDRS, Collections.<String>emptySet());
    }

    private void updateDeviceAddressesSetInPrefs() {
        Set<String> deviceAddrs = new ArraySet<>();
        for (DeviceConnection conn : mDeviceConnections) {
            deviceAddrs.add(conn.getAddress());
        }
        PreferenceManager.getDefaultSharedPreferences(this)
                .edit()
                .putStringSet(Constants.PREF_CONNECTED_DEVICE_ADDRS, deviceAddrs)
                .apply();
    }

    public DeviceConnection connect(String address) {
        DeviceConnection newConn = new DeviceConnection(this, address);
        mDeviceConnections.add(newConn);
        updateDeviceAddressesSetInPrefs();
        return newConn;
    }

    public List<DeviceConnection> getDeviceConnections() {
        return Collections.unmodifiableList(mDeviceConnections);
    }
}
