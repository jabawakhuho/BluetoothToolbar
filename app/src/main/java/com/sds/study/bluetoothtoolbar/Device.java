package com.sds.study.bluetoothtoolbar;

import android.bluetooth.BluetoothDevice;
import android.os.Parcelable;

/**
 * Created by student on 2016-11-22.
 */

public class Device{
    private BluetoothDevice bluetoothDevice;
    private String name;
    private String address;

    public BluetoothDevice getBluetoothDevice() {
        return bluetoothDevice;
    }

    public void setBluetoothDevice(BluetoothDevice bluetoothDevice) {
        this.bluetoothDevice = bluetoothDevice;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }
}
