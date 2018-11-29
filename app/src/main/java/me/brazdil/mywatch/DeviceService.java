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
    public static final String EXTRA_ADDRESS = "device_addr";

    private List<DeviceConnection> mDeviceConnections = new ArrayList<>();

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        if (intent.hasExtra(EXTRA_ADDRESS)) {
            connect(intent.getStringExtra(EXTRA_ADDRESS));
        } else {
            for (String addr : getDeviceAddressesSetFromPrefs()) {
                connect(addr);
            }
        }
        return Service.START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
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
