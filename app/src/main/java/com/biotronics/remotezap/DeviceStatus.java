package com.biotronics.remotezap;

import android.util.Log;

import java.io.ObjectStreamException;

public class DeviceStatus {

    private static volatile DeviceStatus devStatInstance;

    private String deviceStatusText;
    private DeviceStatusE deviceStatusCode;
    private String deviceVid;
    private boolean isSerialPortOpen;
    private volatile String deviceTherapy;
    private int deviceAction;

    public enum DeviceStatusE {
        DISCONNECTED,
        ATTACHED,
        CONNECTED,//port open
        ISSUE
    }


    private DeviceStatus()
    {
        if (devStatInstance != null)
        {//prevent reflection
            throw new IllegalStateException("Cannot create new instance, please use getInstance method instead.");
        }
        deviceStatusText = "";
        deviceVid = "";
        deviceStatusCode = DeviceStatusE.DISCONNECTED;
        isSerialPortOpen = false;
        deviceTherapy = "";
        deviceAction = MyDeviceCommunicationService.NO_ACTION;
    }

    static DeviceStatus getInstance()
    {
        if (devStatInstance == null) {
            synchronized (DeviceStatus.class) {
                if (devStatInstance == null) {
                    devStatInstance = new DeviceStatus();
                }
            }
        }
        return devStatInstance;
    }

    //Signleton part
    @SuppressWarnings({"unused", "RedundantThrows"})
    private Object readResolve() throws ObjectStreamException {
        return getInstance();
    }

    //Signleton part
    @SuppressWarnings("RedundantThrows")
    @Override
    public DeviceStatus clone() throws CloneNotSupportedException {
        return getInstance();
    }

    void setDeviceStatus(DeviceStatusE deviceStatusCode, String deviceStatusText)
    {
        this.deviceStatusCode = deviceStatusCode;
        this.deviceStatusText = deviceStatusText;
        Log.d("DeviceStatus", "Code"+deviceStatusCode);
    }


    String getDeviceStatusText() {
        return deviceStatusText;
    }

    @SuppressWarnings("unused")
    public void setDeviceStatusText(String deviceStatusText) {
        this.deviceStatusText = deviceStatusText;
    }

    DeviceStatusE getDeviceStatusCode() {
        return deviceStatusCode;
    }

    void setDeviceStatusCode(@SuppressWarnings("SameParameterValue") DeviceStatusE deviceStatusCode) {
        this.deviceStatusCode = deviceStatusCode;
    }

    String getVid() {
        return deviceVid;
    }

    void setVid(String vid) {
        this.deviceVid = vid;
    }

    boolean getIsSerialPortOpen() { return isSerialPortOpen; }

    void setIsSerialPortOpen(boolean serialPortOpen) { isSerialPortOpen = serialPortOpen; }

    String getDeviceTherapy() {
        return deviceTherapy;
    }

    void setDeviceTherapy(String lastDeviceData) {
        this.deviceTherapy = lastDeviceData;
    }

    int getDeviceAction() {
        return deviceAction;
    }

    void setDeviceAction(int deviceAction) {
        this.deviceAction = deviceAction;
    }

}
