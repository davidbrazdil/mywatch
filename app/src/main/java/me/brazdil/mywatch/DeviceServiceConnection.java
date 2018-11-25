package me.brazdil.mywatch;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;

public class DeviceServiceConnection implements ServiceConnection {
    private Context mContext;
    private DeviceService mService;

    public DeviceServiceConnection(Context context) {
        mContext = context;
    }

    public void startBind() {
        Intent intent = new Intent(mContext, DeviceService.class);
        mContext.bindService(intent, this, Context.BIND_AUTO_CREATE);
    }

    public void stopBind() {
        mContext.unbindService(this);
    }

    public DeviceService getService() {
        return mService;
    }

    @Override
    public void onServiceConnected(ComponentName className, IBinder service) {
        mService = ((DeviceService.DeviceConnectionServiceBinder) service).getService();
    }

    @Override
    public void onServiceDisconnected(ComponentName arg0) {
        mService = null;
    }
}
