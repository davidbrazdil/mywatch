package me.brazdil.mywatch;

import android.content.Intent;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

import java.util.Collections;

public class DeviceActivity extends AppCompatActivity {
    private static final String TAG = DeviceActivity.class.getSimpleName();

    private DeviceServiceConnection mDeviceServiceConnection;

    TextView mTextDeviceName;
    TextView mTextLastSynced;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device);

        mDeviceServiceConnection = new DeviceServiceConnection(this);

        mTextDeviceName = (TextView) findViewById(R.id.textName);
        mTextLastSynced = (TextView) findViewById(R.id.textLastSynced);

        boolean noDevicesConfigured = PreferenceManager.getDefaultSharedPreferences(this)
                .getStringSet(Constants.PREF_CONNECTED_DEVICE_ADDRS, Collections.<String>emptySet())
                .isEmpty();
        if (noDevicesConfigured) {
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
            return;
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        mDeviceServiceConnection.startBind();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mDeviceServiceConnection.stopBind();
    }

//
//
//    private void syncAllDevices() {
//        Integer[] conns = new Integer[mDeviceConnections.size()];
//        for (int i = 0; i < mDeviceConnections.size(); ++i) {
//            conns[i] = i;
//        }
//        new DeviceSyncTask().execute(conns);
//    }
//
//    private class DeviceSyncTask extends AsyncTask<Integer, Void, Integer[]> {
//        @Override
//        protected Integer[] doInBackground(Integer... connIndices) {
//            for (Integer connIndex : connIndices) {
//                mDeviceConnections.get(connIndex).readAllData();
//            }
//            return connIndices;
//        }
//
//        @Override
//        protected void onPostExecute(Integer[] connIndices) {
//            super.onPostExecute(connIndices);
//            for (Integer connIndex : connIndices) {
//                DeviceConnection conn = mDeviceConnections.get(connIndex);
//                mTextDeviceName.setText(conn.getName());
//                mTextLastSynced.setText("now");
//            }
//        }
//    }
}
