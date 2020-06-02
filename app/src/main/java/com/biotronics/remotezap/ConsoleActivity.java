package com.biotronics.remotezap;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.biotronics.remotezap.MyDeviceCommunicationService.LocalBinder;

public class ConsoleActivity extends AppCompatActivity {

    private Button qCmdLsButton, sendButton, qCmdBatButton, qCmdRmButton, qCmdOffButton;
    private TextView textView;
    private EditText editText;

    private MyDeviceCommunicationService mService;
    private boolean mBound;
    private boolean uiEnabled;
    private IntentFilter mFilter;
    private DeviceStatus mDevStatGet;

    private static final String CONSOLE_TXT = "CONSOLE_TXT";

    @SuppressWarnings("RedundantCast")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_console);

        mDevStatGet = DeviceStatus.getInstance();

        mBound = false;
        mFilter = new IntentFilter();
        mFilter.addAction(MyDeviceCommunicationService.CB_ACTION_CODE);
        registerReceiver(deviceConnectionCallback, mFilter);

        sendButton = (Button) findViewById(R.id.buttonSend);
        qCmdLsButton = (Button) findViewById(R.id.buttonQuickLs);
        qCmdBatButton = (Button) findViewById(R.id.buttonQuickBat);
        qCmdRmButton = (Button) findViewById(R.id.buttonQuickRm);
        qCmdOffButton = (Button) findViewById(R.id.buttonQuickOff);
        editText = (EditText) findViewById(R.id.editText);
        textView = (TextView) findViewById(R.id.textView);
        textView.setMovementMethod(new ScrollingMovementMethod());

        uiEnabled = false;
        //noinspection ConstantConditions
        setUiEnabled(uiEnabled);

    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d("Console", "onStart start");
        // Bind to LocalService
        Intent intent = new Intent(this, MyDeviceCommunicationService.class);

        /*Binding service. onServiceConnected needs some time to start.
         OnStart is to early to call service cb methods. EnableGui in onServiceConnected
         */
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
        LoadPreferences();
    }

    @Override
    protected void onStop() {
        super.onStop();
        unbindService(serviceConnection);
        mBound = false;
        SavePreferences();
        Log.d("Console", "onStop start");
    }

    @Override
    public void onPause() {
        unregisterReceiver(deviceConnectionCallback);
        Log.d("Console", "onPause start");
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        registerReceiver(deviceConnectionCallback, mFilter);
        updateDeviceStatus();
        setUiEnabled(uiEnabled);
        Log.d("Console", "onResume start");
    }

    public void setUiEnabled(boolean bool) {
        qCmdLsButton.setEnabled(bool);
        sendButton.setEnabled(bool);
        qCmdBatButton.setEnabled(bool);
        qCmdRmButton.setEnabled(bool);
        qCmdOffButton.setEnabled(bool);
    }

    public void onClickSend(View view) {
        String string = "Send error";
        if (mBound) {
            string = editText.getText().toString();
            string = string + "\n";
            mService.sendConsoleCmd(string.toLowerCase());
        }
        tvAppend(textView, "\nData Sent : " + string + "\n");

    }

    public void onClickQuickLs(View view) {
        if (mBound) {
            mService.sendConsoleCmd("ls\n");
        }
    }

    public void onClickQuickBat(View view) {
        if (mBound) {
            mService.sendConsoleCmd("bat\n");
        }
    }

    public void onClickQuickRm(View view) {
        if (mBound) {
            mService.sendConsoleCmd("rm\n");
        }
    }

    public void onClickQuickOff(View view) {
        if (mBound) {
            mService.sendConsoleCmd("off\n");
        }
    }

    public void onClickClear(View view) {
        textView.setText(" ");
    }

    private void tvAppend(TextView tv, CharSequence text) {
        final TextView ftv = tv;
        final CharSequence ftext = text;

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ftv.append(ftext);
            }
        });
    }

    /** Defines callbacks for service binding, passed to bindService() */
    private ServiceConnection serviceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            Log.d("Console", "onService connected call");
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            LocalBinder binder = (LocalBinder) service;
            mService = binder.getService();
            mBound = true;
            updateDeviceStatus();
            setUiEnabled(uiEnabled);
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
            Log.d("Console", "onService DISconnected call");
        }
    };


    private final BroadcastReceiver deviceConnectionCallback = new BroadcastReceiver() { //Broadcast Receiver to automatically start and stop the Serial usbConnection.
        @Override
        public void onReceive(Context context, Intent intent) {
            String consoleStatus = intent.getStringExtra("CONSOLE_STAT");
            if (consoleStatus != null && !consoleStatus.isEmpty()) {
                tvAppend(textView, "\nData received : " + consoleStatus + "\n");
            }
            else {
                int callbackCode = intent.getIntExtra(MyDeviceCommunicationService.CB_ACTION, 0);
                //noinspection SwitchStatementWithTooFewBranches
                switch (callbackCode) {
                    case MyDeviceCommunicationService.UPDATE_DEVICE_STATUS:
                        updateDeviceStatus();
                        setUiEnabled(uiEnabled);
                        Log.d("Console", "Receiver case. Data:" + callbackCode);
                        break;
                    default:
                        Log.d("Console", "Receiver default" + callbackCode);
                        break;
                }
            }
        }
    };

    /**  After service callback actions */
    private void updateDeviceStatus() {
        if (mBound) {
            uiEnabled = true;
            //mDevStatGet = mService.gGetDeviceStatus();
            //noinspection PointlessBooleanExpression
            if (mDevStatGet.getDeviceStatusCode() == DeviceStatus.DeviceStatusE.DISCONNECTED ||
                mDevStatGet.getIsSerialPortOpen() == false){
                uiEnabled = false;
                tvAppend(textView, "Device DISCONNECTED / PORT CLOSED\n");
            }
        }
        else
        {
            Log.w("Console", "updateDeviCeStatus mBound FALSE");
        }
    }

    private void SavePreferences(){
        SharedPreferences sharedPreferences = getSharedPreferences("consoleShdPf", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(CONSOLE_TXT, textView.getText().toString());
        editor.apply();   // I missed to save the data to preference here,.
    }

    private void LoadPreferences(){
        SharedPreferences sharedPreferences = getSharedPreferences("consoleShdPf", MODE_PRIVATE);
        String consoleText = sharedPreferences.getString(CONSOLE_TXT, "");
        textView.setText(consoleText);
    }

}
