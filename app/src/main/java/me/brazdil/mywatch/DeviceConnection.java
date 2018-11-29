package me.brazdil.mywatch;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.nio.ByteBuffer;
import java.util.Calendar;
import java.util.Collection;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;
import java.util.UUID;

public class DeviceConnection extends BluetoothGattCallback {
    private static final String TAG = DeviceConnection.class.getSimpleName();

    public static final String ACTION_DEVICE_UPDATE = "me.brazdil.mywatch.DeviceConnection.UPDATE";

    public static final String EXTRA_NAME = "device_name";
    public static final String EXTRA_ADDRESS = "device_addr";
    public static final String EXTRA_STATE = "device_state";
    public static final String EXTRA_BATTERY_LEVEL = "device_battery_level";
    public static final String EXTRA_SERIAL_NUMBER = "device_serial_number";
    public static final String EXTRA_SOFTWARE_REVISION = "device_software_revision";
    public static final String EXTRA_WATCH_CLOCK = "device_watch_clock";
    public static final String EXTRA_PHONE_CLOCK = "device_phone_clock";

    private static final String UUID_BATTERY_SERVICE =
            "0000180f-0000-1000-8000-00805f9b34fb";
    private static final String UUID_BATTERY_LEVEL_CHARACTERISTIC =
            "00002a19-0000-1000-8000-00805f9b34fb";
    private static final String UUID_DEVICE_INFO_SERVICE =
            "0000180a-0000-1000-8000-00805f9b34fb";
    private static final String UUID_DEVICE_INFO_SERIAL_NUMBER_CHARACTERISTIC =
            "00002a25-0000-1000-8000-00805f9b34fb";
    private static final String UUID_DEVICE_INFO_SOFTWARE_REVISION_CHARACTERISTIC =
            "00002a28-0000-1000-8000-00805f9b34fb";

    private static final String UUID_WATCH_SERVICE =
            "c99a3001-7f3c-4e85-bde2-92f2037bfd42";
    private static final String UUID_WATCH_CLOCK_CHARACTERISTIC =
            "c99a3109-7f3c-4e85-bde2-92f2037bfd42";

    private final Context mContext;
    private final BluetoothAdapter mBluetoothAdapter;
    private final BluetoothDevice mDevice;
    private final BluetoothGatt mDeviceGatt;
    private boolean mServicesDiscovered = false;
    private long mLastSynced = -1;
    private int mState = BluetoothProfile.STATE_DISCONNECTED;
    private int mBatteryLevel = -1;
    private String mSerialNumber = null;
    private String mSoftwareRevision = null;
    private long mWatchClock = -1;
    private long mPhoneClock = -1;

    private Queue<CharacteristicOperation> mCharacteristicOperationsQueue = new LinkedList<>();

    private static class CharacteristicOperation {
        public BluetoothGattCharacteristic characteristic;
        boolean isWrite;

        CharacteristicOperation(BluetoothGattCharacteristic c, boolean write) {
            characteristic = c;
            isWrite = write;
        }

        @Override
        public boolean equals(Object o) {
            return (o instanceof CharacteristicOperation) &&
                    ((CharacteristicOperation) o).characteristic.equals(this.characteristic) &&
                    ((CharacteristicOperation) o).isWrite == this.isWrite;
        }
    }

    /* package */ DeviceConnection(Context context, String deviceAddress) {
        mContext = context;
        mBluetoothAdapter = ((BluetoothManager)
                mContext.getSystemService(Context.BLUETOOTH_SERVICE)).getAdapter();

        mState = BluetoothProfile.STATE_CONNECTING;
        mDevice = mBluetoothAdapter.getRemoteDevice(deviceAddress);
        mDeviceGatt = mDevice.connectGatt(mContext, true, this);
        Log.d(TAG, "Connecting to device: " + mDevice.getName() + ", " + mDevice.getAddress());

        broadcastUpdate();
    }

    private void updateCharacteristicIfExists(String uuidService,
                                              String uuidCharacteristic) {
        BluetoothGattService service = mDeviceGatt.getService(UUID.fromString(uuidService));
        if (service == null) {
            return;
        }
        BluetoothGattCharacteristic characteristic =
                service.getCharacteristic(UUID.fromString(uuidCharacteristic));
        if (characteristic == null) {
            return;
        }

        if (!mDeviceGatt.readCharacteristic(characteristic)) {
            CharacteristicOperation op = new CharacteristicOperation(characteristic, false);
            if (!mCharacteristicOperationsQueue.contains(op)) {
                mCharacteristicOperationsQueue.add(op);
            }
        }
    }

    public void updateCharacteristics() {
        updateCharacteristicIfExists(UUID_BATTERY_SERVICE, UUID_BATTERY_LEVEL_CHARACTERISTIC);
        updateCharacteristicIfExists(UUID_DEVICE_INFO_SERVICE,
                UUID_DEVICE_INFO_SERIAL_NUMBER_CHARACTERISTIC);
        updateCharacteristicIfExists(UUID_DEVICE_INFO_SERVICE,
                UUID_DEVICE_INFO_SOFTWARE_REVISION_CHARACTERISTIC);
        updateCharacteristicIfExists(UUID_WATCH_SERVICE, UUID_WATCH_CLOCK_CHARACTERISTIC);
    }

    private void broadcastUpdate() {
        final Intent intent = new Intent(ACTION_DEVICE_UPDATE);
        intent.putExtra(EXTRA_NAME, mDevice.getName());
        intent.putExtra(EXTRA_ADDRESS, mDevice.getAddress());
        intent.putExtra(EXTRA_STATE, mState);
        intent.putExtra(EXTRA_BATTERY_LEVEL, mBatteryLevel);
        intent.putExtra(EXTRA_SERIAL_NUMBER, mSerialNumber);
        intent.putExtra(EXTRA_SOFTWARE_REVISION, mSoftwareRevision);
        intent.putExtra(EXTRA_WATCH_CLOCK, mWatchClock);
        intent.putExtra(EXTRA_PHONE_CLOCK, mPhoneClock);
        mContext.sendBroadcast(intent);
    }

    @Override
    public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
        super.onConnectionStateChange(gatt, status, newState);
        mState = newState;
        Log.d(TAG, "Status change to device: " + mDevice.getName() + ", " + mDevice.getAddress());
        broadcastUpdate();

        if (mState == BluetoothProfile.STATE_CONNECTED) {
            if (!mServicesDiscovered) {
                gatt.discoverServices();
            }
        }
    }

    public String getAddress() { return mDevice.getAddress(); }

    @Override
    public void onServicesDiscovered(BluetoothGatt gatt, int status) {
        super.onServicesDiscovered(gatt, status);
        if (status == BluetoothGatt.GATT_SUCCESS) {
            for (BluetoothGattService service : gatt.getServices()) {
                Log.d(TAG, "Service: " + service.getUuid());

                for (BluetoothGattCharacteristic chara : service.getCharacteristics()) {
                    Log.d(TAG, "        Characteristic: " + chara.getUuid());
                }
            }
        }

        updateCharacteristics();
    }

    private void executeNextCharacteristicOperation() {
        if (!mCharacteristicOperationsQueue.isEmpty()) {
            CharacteristicOperation op = mCharacteristicOperationsQueue.remove();
            if (op.isWrite) {
                if (!mDeviceGatt.writeCharacteristic(op.characteristic)) {
                    Log.e(TAG, "Failed to init write characteristic operation for: "
                            + op.characteristic.getUuid());
                }
            } else {
                if (!mDeviceGatt.readCharacteristic(op.characteristic)) {
                    Log.e(TAG, "Failed to init read characteristic operation for: "
                            + op.characteristic.getUuid());
                }
            }
        }
    }

    public static long toUnsignedInt(byte[] bytes) {
        ByteBuffer buffer = ByteBuffer.allocate(8).put(
                new byte[] { 0, 0, 0, 0, bytes[3], bytes[2], bytes[1], bytes[0] });
        buffer.position(0);
        return buffer.getLong();
    }

    @Override
    public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic,
                                     int status) {
        super.onCharacteristicRead(gatt, characteristic, status);

        if (status == BluetoothGatt.GATT_SUCCESS) {
            switch (characteristic.getUuid().toString()) {
                case UUID_BATTERY_LEVEL_CHARACTERISTIC: {
                    Integer val = characteristic.getIntValue(
                            BluetoothGattCharacteristic.FORMAT_UINT8, 0);
                    if (val == null) {
                        Log.e(TAG, "Invalid offset of battery level characteristic");
                        mBatteryLevel = -1;
                    } else {
                        mBatteryLevel = val.intValue();
                    }
                    break;
                }
                case UUID_DEVICE_INFO_SERIAL_NUMBER_CHARACTERISTIC: {
                    mSerialNumber = characteristic.getStringValue(0);
                    if (mSerialNumber == null) {
                        Log.e(TAG, "Invalid offset of serial number characteristic");
                    }
                    break;
                }
                case UUID_DEVICE_INFO_SOFTWARE_REVISION_CHARACTERISTIC: {
                    mSoftwareRevision = characteristic.getStringValue(0);
                    if (mSoftwareRevision == null) {
                        Log.e(TAG, "Invalid offset of software revision characteristic");
                    }
                    break;
                }
                case UUID_WATCH_CLOCK_CHARACTERISTIC: {
                    byte[] val = characteristic.getValue();
                    if (val == null) {
                        Log.e(TAG, "Could not read watch clock");
                    } else {
                        long watchClockSeconds = toUnsignedInt(val);
                        Calendar cal = new GregorianCalendar(2000, 2, 1);
                        cal.setLenient(true);
                        while (watchClockSeconds > Integer.MAX_VALUE) {
                            cal.add(Calendar.SECOND, Integer.MAX_VALUE);
                            watchClockSeconds -= Integer.MAX_VALUE;
                        }
                        cal.add(Calendar.SECOND, (int) watchClockSeconds);
                        mWatchClock = cal.getTimeInMillis();
                        mPhoneClock = Calendar.getInstance().getTimeInMillis();
                    }
                    break;
                }
            }

            broadcastUpdate();
        } else {
            Log.e(TAG, "Failed to read characteristic: " + characteristic.getUuid() +
                    ". Queuing again.");
            mCharacteristicOperationsQueue.add(
                    new CharacteristicOperation(characteristic, false));
        }

        executeNextCharacteristicOperation();
    }
}