package com.biotronics.remotezap;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.os.Bundle;
import android.os.IBinder;
import android.os.SystemClock;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.biotronics.remotezap.MyDeviceCommunicationService.LocalBinder;

//TODO Loading circle doesn't turn off when to quick go to console. Don't allow to go to console before loading ends
public class MainMenu extends AppCompatActivity {

    private static final String LOG_ID = "MAIN_MENU";
    protected static final int CLICK_TIME_BREAK = 1000;

    Button menuButtonConsole, menuButtonTherapy, menuButtonSettings;
    ProgressBar progressBar;
    TextView txtVwConnectionStatusVal, txtVwPortStatusVal, txtVwTherapyVal, txtVwDetectedVidVal;
    TextView txtVwConnectionStatus, txtVwPortStatus, txtVwTherapy, txtVwDetectedVid;
    private long mLastClickTime = 0;
    MyDeviceCommunicationService mService;
    private boolean mBound = false;
    DeviceStatus mDevStatGet;
    IntentFilter mFilter;

    @SuppressWarnings("RedundantCast")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_menu);

        Toolbar toolbar = (Toolbar) findViewById(R.id.mainMenuToolbar);
        progressBar = (ProgressBar) findViewById((R.id.progressBarMainMenu)) ;
        menuButtonConsole = (Button) findViewById(R.id.menuButtonConsole);
        menuButtonTherapy = (Button) findViewById(R.id.menuButtonTherapy);
        menuButtonSettings = (Button) findViewById(R.id.menuButtonSettings);

        /*Status text*/
        txtVwConnectionStatusVal = (TextView) findViewById(R.id.mainMenuTextViewConnectionStatusVal);
        txtVwPortStatusVal = (TextView) findViewById(R.id.mainMenuTextViewPortStatusVal);
        txtVwTherapyVal = (TextView) findViewById(R.id.mainMenuTextViewTherapyVal);
        txtVwConnectionStatus = (TextView) findViewById(R.id.mainMenuTextViewConnectionStatus);
        txtVwPortStatus = (TextView) findViewById(R.id.mainMenuTextViewPortStatus);
        txtVwTherapy = (TextView) findViewById(R.id.mainMenuTextViewTherapy);
        txtVwDetectedVidVal = (TextView) findViewById(R.id.mainMenuTextViewVidVal);
        txtVwDetectedVid = (TextView) findViewById(R.id.mainMenuTextViewVid);

        setSupportActionBar(toolbar);
        mDevStatGet = DeviceStatus.getInstance();
        //loadVID();
        mFilter = new IntentFilter();
        mFilter.addAction(MyDeviceCommunicationService.CB_ACTION_CODE);
        registerReceiver(deviceConnectionCallback, mFilter);
        updateUiDeviceStatus(mDevStatGet.getDeviceAction());
        Log.d(LOG_ID, "onCreate start");
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Bind to LocalService
        Intent intent = new Intent(this, MyDeviceCommunicationService.class);
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
        Log.d(LOG_ID, "onStart start");
    }


    @Override
    public void onResume() {
        super.onResume();
        registerReceiver(deviceConnectionCallback, mFilter);
        updateUiDeviceStatus(MyDeviceCommunicationService.NO_ACTION);
        Log.d(LOG_ID, "onResume start");
    }

    @Override
    public void onPause() {
        unregisterReceiver(deviceConnectionCallback);
        Log.d(LOG_ID, "onPause start");
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(LOG_ID, "onStop start");
    }

    @SuppressLint("ApplySharedPref")
    @Override
    protected void onDestroy() {
        super.onDestroy();
        /*It's ok to unbind in onDestroy.
         *See https://developer.android.com/guide/components/bound-services.html#Additional_Notes
        */
        unbindService(serviceConnection);
        mBound = false;
        getApplicationContext().getSharedPreferences("consoleShdPf", MODE_PRIVATE).edit().clear().commit();
        Log.d(LOG_ID, "onDestroy start");
    }

    public void onClickMenuButtonConsole(View view) {
        if (SystemClock.elapsedRealtime() - mLastClickTime > CLICK_TIME_BREAK){
            mLastClickTime = SystemClock.elapsedRealtime();
            Intent intentConsole = new Intent(this, ConsoleActivity.class);
            startActivity(intentConsole);
        }
    }

    public void onClickMenuButtonTherapy(View view)
    {
        if (SystemClock.elapsedRealtime() - mLastClickTime > CLICK_TIME_BREAK){
            Intent intentThp = new Intent(this, TherapyManagementActivity.class);
            startActivity(intentThp);
        }
    }

    public void onClickMenuButtonSettings(View view) {
        if (SystemClock.elapsedRealtime() - mLastClickTime > CLICK_TIME_BREAK){
            Intent intentSett = new Intent(this, Settings.class);
            startActivity(intentSett);
        }
    }

    public void onClickMenuButtonAbout(View view) {
        if (SystemClock.elapsedRealtime() - mLastClickTime > CLICK_TIME_BREAK){
            Intent intentAbout = new Intent(this, About.class);
            startActivity(intentAbout);
        }
    }

    private final BroadcastReceiver deviceConnectionCallback = new BroadcastReceiver() { //Broadcast Receiver to automatically start and stop the Serial usbConnection.
        @Override
        public void onReceive(Context context, Intent intent) {
            int callbackCode = intent.getIntExtra(MyDeviceCommunicationService.CB_ACTION,0);
            switch (callbackCode) {
                case MyDeviceCommunicationService.UPDATE_DEVICE_STATUS:
                    updateUiDeviceStatus(callbackCode);
                    Log.d(LOG_ID, "Receiver case. Data:"+ callbackCode);
                    break;

                case MyDeviceCommunicationService.LOAD_START:
                case MyDeviceCommunicationService.LOAD_END:
                    updateUiDeviceStatus(callbackCode);
                    break;
                default:
                    Log.d(LOG_ID, "Receiver default"+ callbackCode);
                    break;
            }
        }
    };

    private void setUiEnabled(boolean enabled)
    {
        menuButtonConsole.setEnabled(enabled);
        menuButtonTherapy.setEnabled(enabled);
        menuButtonSettings.setEnabled(enabled);
        //menuButtonConsole.setEnabled(true);//TODO remove when TEST finished
    }

    private void setThpButtonEnabled(@SuppressWarnings("SameParameterValue") boolean enabled)
    {
        menuButtonTherapy.setEnabled(enabled);
    }
    private void setCmdButtonEnabled(@SuppressWarnings("SameParameterValue") boolean enabled)
    {
        menuButtonConsole.setEnabled(enabled);
    }
    private void setSettButtonEnabled(@SuppressWarnings("SameParameterValue")boolean enabled)
    {
        menuButtonSettings.setEnabled(enabled);
    }

    /**  After service callback actions */
    private void updateUiDeviceStatus(int action) {
        if (mBound) {
            int color = Color.RED;
            switch (mDevStatGet.getDeviceStatusCode()) {
                case DISCONNECTED:
                    color = Color.RED;
                    setThpButtonEnabled(true);
                    setCmdButtonEnabled(false);//TODO change in final
                    setSettButtonEnabled(true);
                    break;
                case ISSUE:
                case ATTACHED:
                    color = 0xFFE08C1C;//orange
                    setThpButtonEnabled(true);
                    setCmdButtonEnabled(false);
                    setSettButtonEnabled(true);
                    break;
                case CONNECTED:
                    setUiEnabled(true);
                    color = Color.GREEN;
                    break;

                default:
                    break;
            }
            if(action == MyDeviceCommunicationService.LOAD_START)
            {
                progressBar.setVisibility(View.VISIBLE);
                setUiEnabled(false);
            }
            if(action == MyDeviceCommunicationService.LOAD_END)
            {
                progressBar.setVisibility(View.INVISIBLE);
                setUiEnabled(true);
            }

            txtVwConnectionStatusVal.setTextColor(color);
            txtVwPortStatusVal.setTextColor(color);
            txtVwTherapyVal.setTextColor(color);
            txtVwConnectionStatus.setTextColor(color);
            txtVwPortStatus.setTextColor(color);
            txtVwTherapy.setTextColor(color);
            txtVwDetectedVidVal.setTextColor(color);
            txtVwDetectedVid.setTextColor(color);

            txtVwConnectionStatusVal.setText(mDevStatGet.getDeviceStatusText());
            String stat = (mDevStatGet.getIsSerialPortOpen() ?
                    getResources().getString(R.string.port_open) : getResources().getString(R.string.port_closed));
            txtVwDetectedVidVal.setText(mDevStatGet.getVid());
            txtVwPortStatusVal.setText(stat);
            txtVwTherapyVal.setText(mDevStatGet.getDeviceTherapy());
        }
        else
        {
            Log.w(LOG_ID, "updateDeviCeStatus mBound FALSE");
        }
    }

    /** Defines callbacks for service binding, passed to bindService() */
    private ServiceConnection serviceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            Log.d(LOG_ID, "onService connected call");
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            LocalBinder binder = (LocalBinder) service;
            mService = binder.getService();
            mBound = true;
            updateUiDeviceStatus(mDevStatGet.getDeviceAction());
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
        }
    };
}
