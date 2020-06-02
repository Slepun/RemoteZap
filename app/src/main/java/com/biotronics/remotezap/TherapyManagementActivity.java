package com.biotronics.remotezap;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SimpleItemAnimator;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.Spinner;

import java.util.ArrayList;
import java.util.List;
//TODO UPLOAD THERAPY Button on/off update
//TODO check what happen if while update therapy list, go back pressed
public class TherapyManagementActivity extends AppCompatActivity {

    private static final String LOG_TRP = "ThpMgm";
    private static final int DUMMY = 0xBEEF;
    DeviceStatus mDevStatGet;

    protected RecyclerView m_recyclerView;

    /* Device connection*/
    private boolean mBound = false;
    MyDeviceCommunicationService mService;

    /* Manipulated by loadTherapies() & ThpWebDonwload async task*/
    protected ProgressBar progressBar;//while loading from web
    protected List<Therapy> therapies;
    Spinner spinner;
    ThpMgmRecycleAdapter recycle_adapter;

    /*File operations*/
    FileOperationsClass fio;
    protected static final String THP_DB = "therapies.json";
    protected static final String THP_DB_NEW = "therapies_update.db";
    protected static final String USER_THP_FILE = "userTherapies.json";
    protected static final String USER_ROOT_DIR = Environment.getExternalStorageDirectory().toString();
    protected static final String USER_DIR = "/remoteZap/therapies";
    protected static final String JSON_ARRAY = "therapy";

    protected int ulPositionClicked = DUMMY;

    private IntentFilter mfilter;

    @SuppressWarnings("RedundantCast")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_therapy_management);

        mDevStatGet = DeviceStatus.getInstance();

        mfilter = new IntentFilter();
        mfilter.addAction(MyDeviceCommunicationService.CB_ACTION_CODE);
        registerReceiver(deviceConnectionCallback, mfilter);

        progressBar = (ProgressBar) findViewById(R.id.progressBarLoadTherapies);
        progressBar.setVisibility(View.INVISIBLE);

        spinner = (Spinner) findViewById(R.id.devFilter_spinner);
        setupSort();

        //noinspection Convert2Diamond
        therapies = new ArrayList<Therapy>(50);
        fio = new FileOperationsClass();

        loadTherapies();
        populateRecycleAdapter();

        m_recyclerView = findViewById(R.id.thpMgmRecview);
        ((SimpleItemAnimator) m_recyclerView.getItemAnimator()).setSupportsChangeAnimations(false);
        m_recyclerView.setLayoutManager(new LinearLayoutManager(this));
        m_recyclerView.setAdapter(recycle_adapter);
        m_recyclerView.setItemViewCacheSize(20);
        //m_recyclerView.setHasFixedSize(true);

        Log.d("LOG_TRP", "onCreate start");
    }

    @Override
    protected void onStart() {
        super.onStart();
        Intent intent = new Intent(this, MyDeviceCommunicationService.class);
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
        Log.d("LOG_TRP", "onStart start");
    }

    @Override
    public void onResume() {
        super.onResume();
        registerReceiver(deviceConnectionCallback, mfilter);
        loadTherapies();
        recycle_adapter.notifyDataSetChanged();
        Log.d("LOG_TRP", "onResume start");
    }

    @Override
    protected void onPause() {
        unregisterReceiver(deviceConnectionCallback);
        Log.d("LOG_TRP", "onPause start");
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        unbindService(serviceConnection);
        mBound = false;
        Log.d("LOG_TRP", "onStop start");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d("LOG_TRP", "onDestroy start");
    }

    public void thp_fab_onClick(View view) {
        Intent intentNewThp = new Intent(this, CreateNewThpActivity.class);
        startActivityForResult(intentNewThp, 1);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 1) {

            if (resultCode == RESULT_OK) {
                spinner.setSelection(0);
                //Update List
                addCreatedTherapyToList();
                //populateRecycleAdapter();
                //recycle_adapter.notifyDataSetChanged();
                recycle_adapter.updateData(therapies);

            }
            //noinspection StatementWithEmptyBody
            if (resultCode == RESULT_CANCELED) {
                //Do nothing
            }
        }
    }//onActivityResult

    private boolean loadTherapies() {
        boolean fRetVal = true;
        Context context = this.getApplicationContext();
        fio.populateInitTherapyData(context, THP_DB);
        String json = fio.loadFileToString(context.getFilesDir().toString(), THP_DB);
        therapies = fio.loadJsonObjects(json);
        //TODO while no permision userThp won't be added in this run
        if (fio.fileExist(USER_ROOT_DIR, USER_DIR, USER_THP_FILE, true)) {
            json = fio.loadFileToString(USER_ROOT_DIR + USER_DIR, USER_THP_FILE);
            List<Therapy> usrTherapies = fio.loadJsonObjects(json);
            therapies.addAll(usrTherapies);
        }
        enumerateTherapies();
/*
 TODO Propose new therapy format on website / database?
 Currently it's very hard to parse therapy data, there is a lot of corner cases while parsing
 else
 {//Download therapies
 //TODO What if no network?
 new TherapyWebDownload(this).execute();
 fRetVal = false;
 }
 */
        return fRetVal;
    }

    private boolean addCreatedTherapyToList() {
        if (fio.fileExist(USER_ROOT_DIR, USER_DIR, USER_THP_FILE, true)) {
            String json = fio.loadFileToString(USER_ROOT_DIR + USER_DIR, USER_THP_FILE);
            List<Therapy> usrTherapies = fio.loadJsonObjects(json);
            therapies.add(usrTherapies.get(usrTherapies.size()-1));
        }
        enumerateTherapies();
        return true;
    }

    //Therapies view after filter, was not inline with therapies list.
    //This caused an issue while sending therapy(wrong therapy was sent)
    private void enumerateTherapies()
    {
        for(int i=0; i<therapies.size(); i++){
            therapies.get(i).setThpId(i);
        }
    }

    private void populateRecycleAdapter() {
        recycle_adapter = new ThpMgmRecycleAdapter(this.getApplicationContext(), therapies, new ClickListener() {
            @Override
            public void onPositionClicked(int thpId, int viewPosition) {
                // callback performed on click
                if (mBound) {
                    mService.sendTherapyToDev(therapies.get(thpId).getScript());
                    ulPositionClicked = viewPosition;
                }
            }
        });
    }

    private void setupSort(){
        ArrayAdapter<CharSequence> spinner_adapter = ArrayAdapter.createFromResource(this,
                R.array.device_array, android.R.layout.simple_spinner_item);
        spinner_adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(spinner_adapter);

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long l) {
                // 0 - all, 1 - freePEMF, 2 - multiZAP, 3 - zapper-plus
                recycle_adapter.filterOutDevices(position);
                spinner_adapter.notifyDataSetChanged();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
    }

    private final BroadcastReceiver deviceConnectionCallback = new BroadcastReceiver() { //Broadcast Receiver to automatically start and stop the Serial usbConnection.
        @Override
        public void onReceive(Context context, Intent intent) {
            int callbackCode = intent.getIntExtra(MyDeviceCommunicationService.CB_ACTION,0);//"DEVICE_STAT"
            switch (callbackCode) {
                case MyDeviceCommunicationService.UPDATE_DEVICE_STATUS:
                    recycle_adapter.notifyDataSetChanged();//TODO for current therapy mark
                    break;

                case MyDeviceCommunicationService.THP_UPLOAD_START:
                    changeThpProgressUl(MyDeviceCommunicationService.THP_UPLOAD_START);
//                    getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
//                            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
                    break;
                case MyDeviceCommunicationService.THP_UPLOAD_END:
                    changeThpProgressUl(MyDeviceCommunicationService.THP_UPLOAD_END);
//                    getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
                    
                    break;
                default:
                    break;
            }
            Log.d(LOG_TRP, "Receiver case. Data:"+ callbackCode);
        }
    };



    @SuppressWarnings("unused")
    //Was used when downloading data from Web
    private void changeProgressState(int code)
    {
        if (code ==  MyDeviceCommunicationService.LOAD_START)
        {
            progressBar.setVisibility(View.VISIBLE);
        }
        if (code == MyDeviceCommunicationService.LOAD_END)
        {
            progressBar.setVisibility(View.GONE);
        }
    }

    private void changeThpProgressUl(int code)
    {//TODO possible null pointer exception while view recycled right after findViewolderForAdapterPosition
        if(ulPositionClicked != DUMMY)
        {
            ThpMgmRecycleAdapter.RecViewHolder viewHolder = (ThpMgmRecycleAdapter.RecViewHolder)
                    m_recyclerView.findViewHolderForAdapterPosition(ulPositionClicked);
            if (code ==  MyDeviceCommunicationService.THP_UPLOAD_START)
            {
                if(viewHolder != null) {
                    viewHolder.progresUpload.setVisibility(View.VISIBLE);
                }
            }
            if (code == MyDeviceCommunicationService.THP_UPLOAD_END)
            {
                if(viewHolder != null) {
                    viewHolder.progresUpload.setVisibility(View.INVISIBLE);
                } else
                {
                    recycle_adapter.filterOutDevices(0);
                }
            }
        }
    }


    /** Defines callbacks for service binding, passed to bindService() */
    private ServiceConnection serviceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            Log.d("Main", "onService connected call");
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            MyDeviceCommunicationService.LocalBinder binder = (MyDeviceCommunicationService.LocalBinder) service;
            mService = binder.getService();
            mBound = true;
            //updateDeviceStatus();
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
        }
    };

}
