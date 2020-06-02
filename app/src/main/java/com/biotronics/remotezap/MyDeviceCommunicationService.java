package com.biotronics.remotezap;

import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import com.biotronics.remotezap.DeviceStatus.DeviceStatusE;
import com.felhr.usbserial.UsbSerialDevice;
import com.felhr.usbserial.UsbSerialInterface;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static com.biotronics.remotezap.Settings.USB_ARDUINO_VID;

//TODO don't allow to enter other activities while loading start
//TODO cant always sent intent console when status change. Probably service don turn off because after OnDestroy intent is sent
public class MyDeviceCommunicationService extends Service {

    // Binder given to clients
    private final IBinder binder = new LocalBinder();

    private static final String ACTION_USB_PERMISSION = "com.biotronics.remotezap.USB_PERMISSION";
    //private static int ARDUINO_NANO_VID = 0x1A86; //TODO and load from external file

    public static final String CB_ACTION_CODE =
            "com.biotronics.remotezap.cbActionCode";
    public static final String CB_ACTION = "CB_ACTION";
    public static final int NO_ACTION = 0;
    public static final int UPDATE_DEVICE_STATUS = 1;
    public static final int LOAD_START = 2;
    public static final int LOAD_END = 3;
    public static final int THP_UPLOAD_START = 4;
    public static final int THP_UPLOAD_END = 5;

    private DeviceStatus mDevStatMain;
    protected volatile String lastDataSent, lastDataReceived;

    private UsbDevice device;
    private UsbDeviceConnection usbConnection;
    private UsbManager usbManager;
    private UsbSerialDevice serialPort;

    public MyDeviceCommunicationService()
    {
    }

    @Override
    public void onCreate(){
        usbManager = (UsbManager) getSystemService(this.USB_SERVICE);
        mDevStatMain = DeviceStatus.getInstance();
        IntentFilter filter;
        filter = new IntentFilter();
        filter.addAction(ACTION_USB_PERMISSION);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        registerReceiver(broadcastReceiver, filter);
        lastDataSent = "null";
        lastDataReceived = "null";
        Log.d("DeviceService", "onCreate start");
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d("DeviceService", "onBind start");
        return binder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.d("DeviceService", "in onUnbind");
        return true;//allowRebind
    }

    @Override
    public void onRebind(Intent intent) {
        Log.d("DeviceService", "in onRebind");
    }

    @Override
    public void onDestroy() {
        Log.d("DeviceService", "in onDestroy");
        super.onDestroy();
        stopPortCommunication();
        unregisterReceiver(broadcastReceiver);
        Log.d("DeviceService", "in onDestroy2");
    }

    private int getSettingsVid(){
        SharedPreferences sharedPreferences = getSharedPreferences("SettingsShdPf", MODE_PRIVATE);
        return sharedPreferences.getInt(USB_ARDUINO_VID, 0x1A86);
    }

    /* method for clients */
    /*Not used since singleton solution
    public DeviceStatus gGetDeviceStatus()
    {
        return mDevStatMain;
    }
     */

    /** method for initiate Device Port Open*/
    public void connectToDevice(){
        if(mDevStatMain.getDeviceStatusCode() == DeviceStatusE.ATTACHED)
        {
            mDevStatMain.setVid("Device list is empty! \n");
            HashMap<String, UsbDevice> usbDevices = usbManager.getDeviceList();
            if (!usbDevices.isEmpty()) {
                boolean keep = true;
                for (Map.Entry<String, UsbDevice> entry : usbDevices.entrySet()) {
                    device = entry.getValue();
                    int deviceVID = device.getVendorId();
                    mDevStatMain.setVid(Integer.toString(deviceVID));
                    if (deviceVID == /*ARDUINO_NANO_VID*/getSettingsVid())//Arduino Vendor ID
                    {
                        PendingIntent pi = PendingIntent.getBroadcast(this, 0,
                                new Intent(ACTION_USB_PERMISSION), 0);
                        usbManager.requestPermission(device, pi);
                        keep = false;
                    } else {
                        usbConnection = null;
                        device = null;
                    }
                    if (!keep)
                        break;
                }
            }
        }
        else
        {
            Log.d("SERIAL", "Device status code "+mDevStatMain.getDeviceStatusCode());
        }
    }

    /** method for clients */
    public void sendConsoleCmd(String cmd)
    {
        if(mDevStatMain.getIsSerialPortOpen())
        {
            lastDataSent = cmd;
            serialPort.write(cmd.getBytes());
        }
    }

    /** method for clients */
    public void sendTherapyToDev(String[] thp)
    {
        int[] startingEv = {MyDeviceCommunicationService.THP_UPLOAD_START};
        int[] endingEv = {MyDeviceCommunicationService.THP_UPLOAD_END};
        new DeviceDataSendAsync(this, thp, 0,
                startingEv, endingEv).execute();
    }

    /** method for clients */
    public void stopPortCommunication()
    {
        if(mDevStatMain.getIsSerialPortOpen())
        {
            serialPort.close();
            mDevStatMain.setIsSerialPortOpen(false);

            //If user press STOP in console, change status
            if(mDevStatMain.getDeviceStatusCode() == DeviceStatusE.CONNECTED) {
                mDevStatMain.setDeviceStatusCode(DeviceStatusE.ATTACHED);
            }
            sendCbCode(UPDATE_DEVICE_STATUS);
        }
    }

    //TODO simplify / split below
    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() { //Broadcast Receiver to automatically start and stop the Serial usbConnection.
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case ACTION_USB_PERMISSION:
                    boolean granted = intent.getExtras().getBoolean(UsbManager.EXTRA_PERMISSION_GRANTED);
                    if (granted) {
                        usbConnection = usbManager.openDevice(device);
                        if (usbConnection != null) {
                            mDevStatMain.setDeviceStatus(DeviceStatusE.CONNECTED, getResources().getString(R.string.dev_attached));
                            sendCbCode(UPDATE_DEVICE_STATUS);
                            serialPort = UsbSerialDevice.createUsbSerialDevice(device, usbConnection);
                            if (serialPort != null) {
                                if (serialPort.open()) { //Set Serial Connection Parameters.
                                    serialPort.setBaudRate(9600);
                                    serialPort.setDataBits(UsbSerialInterface.DATA_BITS_8);
                                    serialPort.setStopBits(UsbSerialInterface.STOP_BITS_1);
                                    serialPort.setParity(UsbSerialInterface.PARITY_NONE);
                                    serialPort.setFlowControl(UsbSerialInterface.FLOW_CONTROL_OFF);
                                    serialPort.read(mCallback);
                                    mDevStatMain.setDeviceStatus(DeviceStatusE.CONNECTED, getResources().getString(R.string.dev_connected));
                                    mDevStatMain.setIsSerialPortOpen(true);

                                    String[] cmd = {"ls"};
                                    int[] startingEv = {MyDeviceCommunicationService.LOAD_START};
                                    int[] endingEv = {MyDeviceCommunicationService.LOAD_END,
                                            MyDeviceCommunicationService.UPDATE_DEVICE_STATUS};
                                    new DeviceDataSendAsync(MyDeviceCommunicationService.this,
                                            cmd, 1, startingEv, endingEv).execute();
                                } else {
                                    Log.d("SERIAL", "PORT NOT OPEN");
                                    mDevStatMain.setDeviceStatus(DeviceStatusE.ISSUE, getResources().getString(R.string.dev_attached_port));
                                    sendCbCode(UPDATE_DEVICE_STATUS);
                                }
                            } else {
                                Log.d("SERIAL", "PORT IS NULL");
                                mDevStatMain.setDeviceStatus(DeviceStatusE.ISSUE, getResources().getString(R.string.port_null));
                                stopPortCommunication();
                                sendCbCode(UPDATE_DEVICE_STATUS);
                            }
                        }
                    } else {
                        mDevStatMain.setDeviceStatus(DeviceStatusE.ISSUE, getResources().getString(R.string.usb_no_grant));
                        sendCbCode(UPDATE_DEVICE_STATUS);
                    }
                    break;
                case UsbManager.ACTION_USB_DEVICE_ATTACHED:
                    mDevStatMain.setDeviceStatus(DeviceStatusE.ATTACHED, getResources().getString(R.string.dev_attached_port));
                    connectToDevice();
                    sendCbCode(UPDATE_DEVICE_STATUS);
                    break;
                case UsbManager.ACTION_USB_DEVICE_DETACHED:
                    mDevStatMain.setDeviceStatus(DeviceStatusE.DISCONNECTED, getResources().getString(R.string.dev_disconnected));
                    stopPortCommunication();
                    sendCbCode(UPDATE_DEVICE_STATUS);
                    break;
            }
        }
    };

    UsbSerialInterface.UsbReadCallback mCallback = new UsbSerialInterface.UsbReadCallback() { //Defining a Callback which triggers whenever data is read.
        @Override
        public void onReceivedData(byte[] arg0) {
            String data;
            data = new String(arg0, StandardCharsets.UTF_8);
            //noinspection ResultOfMethodCallIgnored
            data.concat("\n");
            lastDataReceived = data;
            updateConsoleText(data);
        }
    };

    /**
     * Class used for the client Binder.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    class LocalBinder extends Binder {
        MyDeviceCommunicationService getService() {
            // Return this instance of LocalService so clients can call public methods
            return MyDeviceCommunicationService.this;
        }
    }


    protected void checkCurrentTherapy()
    {
        String[] lines = lastDataReceived.split("\n", 5);
        for (String line : lines)
        {
            if (line.matches(".*#.*"))
            {
                mDevStatMain.setDeviceTherapy(line);
                break;
            }
        }

    }

    /****************** Callback methods********************
     *  CommunicationService change -> inform Interested Class -> Interested Class ask for status by service
     * */
    protected void sendCbCode(int actionCode)
    {
        mDevStatMain.setDeviceAction(actionCode);
        Intent intent = new Intent();
        intent.setAction(CB_ACTION_CODE);
        intent.putExtra(CB_ACTION, actionCode);
        sendBroadcast(intent);
        Log.d("DeviceService", "Intent sent1");
    }

    private void updateConsoleText(String cStatus)
    {
        Intent intent = new Intent();
        intent.setAction(CB_ACTION_CODE);
        intent.putExtra("CONSOLE_STAT", cStatus);
        sendBroadcast(intent);
        Log.d("DeviceService", "Intent console sent");
    }
}
