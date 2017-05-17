package com.example.tania.activityrectesi;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.RemoteException;
import android.os.SystemClock;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.microsoft.band.BandClient;
import com.microsoft.band.BandClientManager;
import com.microsoft.band.BandException;
import com.microsoft.band.BandInfo;
import com.microsoft.band.ConnectionState;
import com.microsoft.band.UserConsent;
import com.microsoft.band.sensors.BandAccelerometerEvent;
import com.microsoft.band.sensors.BandAccelerometerEventListener;
import com.microsoft.band.sensors.BandGsrEvent;
import com.microsoft.band.sensors.BandGsrEventListener;
import com.microsoft.band.sensors.BandGyroscopeEvent;
import com.microsoft.band.sensors.BandGyroscopeEventListener;
import com.microsoft.band.sensors.BandHeartRateEvent;
import com.microsoft.band.sensors.BandHeartRateEventListener;
import com.microsoft.band.sensors.BandRRIntervalEvent;
import com.microsoft.band.sensors.BandRRIntervalEventListener;
import com.microsoft.band.sensors.BandSkinTemperatureEvent;
import com.microsoft.band.sensors.BandSkinTemperatureEventListener;
import com.microsoft.band.sensors.GsrSampleRate;
import com.microsoft.band.sensors.HeartRateConsentListener;
import com.microsoft.band.sensors.SampleRate;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collection;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements SensorEventListener,BeaconConsumer {

    public FileOutputStream HRos=null;
    public FileOutputStream GSRos=null;
    public FileOutputStream RRos=null;
    public FileOutputStream STos=null;
    public FileOutputStream Accos=null;
    public FileOutputStream Gyros=null;
    public FileOutputStream Taccos=null;

    private SensorManager SM;
    private Sensor accSens;
    private SensorManager SM1;
    private Sensor accGyro;

    public File HRlog;
    public File GSRlog;
    public File RRlog;
    public File STlog;
    public File Acclog;
    public File Gyrolog;

    public static Context mContext;

    //robba per beacons
    protected final String TAG = "RangingActivity";
    private String actualBeacon="";
    private BeaconManager beaconManager;
    private String beaconsDesc;

    //folder e path generali
    public String appFolder="ActivityRecon_Data";
    public String folder="";
    public String path= Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath();
    public final String folderBand="Band";
    public final String folderBeacons="Beacons";
    public final String folderPhoneData="Phone";

    //define the time for checking devices
    private long lastCheck;
    private boolean phoneStatus=false;
    private boolean bandStatus=false;
    private boolean beaconStatus=false;
    private ImageView ivphoneStatYES;
    private ImageView ivphoneStatNO;
    private ImageView ivbandStatYES;
    private ImageView ivbandStatNO;
    private ImageView ivbeaconStatYES;
    private ImageView ivbeaconStatNO;
    private int m_interval = 5000; // 5 seconds by default, can be changed later
    private Handler m_handler;
    private BandClient client = null;

    //define the starting folder
    public static EditText eTdirectory;

    //intro
    private ImageButton btnFolder, btnStartSession, btnStopSession;
    private boolean isFolderSelected=false;
    private boolean isSessionStarted=false;
    private Chronometer chrono;
    private long time=0;

    //status checker
    private boolean stateHros,stateGSRos,stateRRos,stateSTos,stateAccos,stateGyros,stateTaccos;
    private boolean checker;




    private boolean initNewSession() {

        path= Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath();
        File file = new File(path, appFolder);
        if (!file.exists()) {
            try {
                file.mkdir();
            } catch (SecurityException e) {
            }
        }
        path+= "/"+appFolder;

        file = new File(path, folder);
        if (!file.exists()) {
            try {
                file.mkdir();
            } catch (SecurityException e) {
            }
        }

        file = new File(path+"/"+folder,folderBand);

        if (!file.exists()) {
            try {
                file.mkdir();
            } catch (SecurityException e) {
            }
        }

        HRlog = new File(file,"HRlog.txt");
        GSRlog = new File(file,"GSRlog.txt");
        RRlog = new File(file,"RRlog.txt");
        STlog = new File(file,"STlog.txt");
        Acclog = new File(file,"Acclog.txt");
        Gyrolog = new File(file,"Gyrolog.txt");



        try {
            HRlog.createNewFile();
            GSRlog.createNewFile();
            RRlog.createNewFile();
            STlog.createNewFile();
            Acclog.createNewFile();
            Gyrolog.createNewFile();
        } catch (IOException e) {
            Log.d("App",String.format("Errore: "+e));
        }
        try {
            HRos = new FileOutputStream(HRlog);
            GSRos = new FileOutputStream(GSRlog);
            RRos = new FileOutputStream(RRlog);
            STos = new FileOutputStream(STlog);
            Accos = new FileOutputStream(Acclog);
            Gyros = new FileOutputStream(Gyrolog);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        //---------------------------------------------DEFINING THE TREAD FOR CHECKING DEVICE STATUS
        m_handler = new Handler();


        return true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        mContext=this.getApplicationContext();




        //variables for checking animation
        ivphoneStatYES= (ImageView) findViewById(R.id.iv_phoneStatOK);
        ivphoneStatNO= (ImageView) findViewById(R.id.iv_phoneStatNope);
        ivphoneStatNO.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Toast.makeText(getBaseContext(),"Fatal Error with phone sensor!",Toast.LENGTH_SHORT).show();

            }
        });

        ivbandStatYES= (ImageView) findViewById(R.id.iv_bandStatOK);
        ivbandStatNO= (ImageView) findViewById(R.id.iv_bandStatNope);
        ivbandStatNO.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Toast.makeText(getBaseContext(),"Band not altready attached, or problems with some sensors!",Toast.LENGTH_SHORT).show();
            }
        });

        ivbeaconStatYES= (ImageView) findViewById(R.id.iv_beaconStatOK);
        ivbeaconStatNO= (ImageView) findViewById(R.id.iv_BeaconsStatNope);
        ivbeaconStatNO.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Toast.makeText(getBaseContext(),"Bluetooth not connected!",Toast.LENGTH_SHORT).show();
            }
        });




        //---------------------------------------------------------------------------SENSOR SETTINGS
        SM= (SensorManager)getSystemService(SENSOR_SERVICE);
        accSens = SM.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        SM1= (SensorManager)getSystemService(SENSOR_SERVICE);
        accGyro = SM.getDefaultSensor(Sensor.TYPE_GYROSCOPE);

        SM1.registerListener(this,accGyro, SensorManager.SENSOR_DELAY_UI);
        SM.registerListener(this,accSens, SensorManager.SENSOR_DELAY_UI);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openFolder();
            }

        });

        //--------------------------------------------------------------------------BEACONS SETTINGS
        beaconManager = BeaconManager.getInstanceForApplication(this);
        beaconManager.getBeaconParsers().add(new BeaconParser().setBeaconLayout("m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24"));
        /*
            m - matching byte sequence for this beacon type to parse (exactly one required)

            s - ServiceUuid for this beacon type to parse (optional, only for Gatt-based beacons)

            i - identifier (at least one required, multiple allowed)

            p - power calibration field (exactly one required)

            d - data field (optional, multiple allowed)

            x - extra layout. Signifies that the layout is secondary to a primary layout with the same matching byte sequence (or ServiceUuid). Extra layouts do not require power or identifier fields and create Beacon objects without identifiers.

            Example of a parser string for AltBeacon:

            "m:2-3=beac,i:4-19,i:20-21,i:22-23,p:24-24,d:25-25"
         */
        beaconManager.bind(this);

        //sessions command procedure
        final TextView tvSessionStatus= (TextView) findViewById(R.id.tv_sessionStatus);
        tvSessionStatus.setTextColor(Color.RED);
        chrono = (Chronometer) findViewById(R.id.chronoSession);

        btnStopSession= (ImageButton) findViewById(R.id.ib_stop);
        btnStartSession = (ImageButton) findViewById(R.id.ib_play);
        btnFolder = (ImageButton) findViewById(R.id.ib_folder);

        btnFolder.setEnabled(true);
        btnStartSession.setEnabled(true);
        btnStopSession.setEnabled(false);


        btnStartSession.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(isFolderSelected) {
                    if (initNewSession()) {
                        new HeartRateSubscriptionTask().execute();
                        new GsrSubscriptionTask().execute();
                        new RRIntervalSubscriptionTask().execute();
                        new SkinTemperatureSubscriptionTask().execute();
                        new AccelerometerSubscriptionTask().execute();
                        new GyroscopeSubscriptionTask().execute();
                        tvSessionStatus.setText("Running...");
                        tvSessionStatus.setTextColor(Color.GREEN);
                        chrono.setTextColor(Color.GREEN);
                        chrono.setBase(SystemClock.elapsedRealtime());
                        chrono.start();
                        isSessionStarted=true;
                        btnFolder.setEnabled(false);
                        btnStopSession.setVisibility(View.VISIBLE);
                        btnStartSession.setVisibility(View.INVISIBLE);
                        btnStopSession.setEnabled(true);
                        btnStartSession.setEnabled(false);
                        btnFolder.setEnabled(false);
                        startRepeatingTask();
                        Toast.makeText(getBaseContext(),"Session Started Successfully!",Toast.LENGTH_SHORT).show();
                    }
                }else{
                    Toast.makeText(getBaseContext(),"pls Select the folder",Toast.LENGTH_SHORT).show();
                }
            }
        });


        final WeakReference<Activity> reference = new WeakReference<Activity>(this);

        btnFolder.setOnClickListener(new View.OnClickListener() {
            @SuppressWarnings("unchecked")
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setMessage("Choose the name of the directory")
                        .setTitle("Choose Directory");
                eTdirectory = new EditText(getContext());
                eTdirectory.setTextColor(Color.BLACK);
                builder.setView(eTdirectory);
                builder.setPositiveButton("Submit", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        folder=eTdirectory.getText().toString();
                        isFolderSelected=true;
                    }
                });
                builder.show();
                new HeartRateConsentTask().execute(reference);
            }
        });

        btnStopSession.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isSessionStarted=false;
                isFolderSelected=false;
                btnStartSession.setEnabled(true);
                btnFolder.setEnabled(true);
                btnStopSession.setEnabled(false);
                btnStartSession.setVisibility(View.VISIBLE);
                btnStopSession.setVisibility(View.INVISIBLE);
                tvSessionStatus.setText("notRunning");
                tvSessionStatus.setTextColor(Color.RED);
                if (client != null) {
                    try {
                        client.disconnect().await();
                        HRos.close();
                        GSRos.close();
                        RRos.close();
                        STos.close();
                        Accos.close();
                        Gyros.close();
                        Taccos.close();
                    } catch (InterruptedException e) {
                        // Do nothing as this is happening during destroy
                    } catch (BandException e) {
                        // Do nothing as this is happening during destroy
                    } catch (IOException e) {

                    }
                }
                //time=chrono.getBase()+SystemClock.elapsedRealtime();
                chrono.setTextColor(Color.RED);
                chrono.stop();
                stopRepeatingTask();
                Toast.makeText(getBaseContext(),"Session Closed...pls select the new one",Toast.LENGTH_SHORT).show();
            }
        });

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void appendToUI(final String string) {
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.i("LOG",string);
            }
        });
    }


    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {

        if(isSessionStarted) {
            String tipo = null;
            String sensType = null;
            String dati = null;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT_WATCH) {
                tipo = sensorEvent.sensor.getStringType();
            }
            if (tipo.equals("android.sensor.accelerometer")) {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ITALY);
                Calendar c = Calendar.getInstance();
                dati = String.valueOf(sensorEvent.values[0]) +
                        " " + String.valueOf(sensorEvent.values[1]) +
                        " " + String.valueOf(sensorEvent.values[2]) +
                        " " + sdf.format(c.getTime()) + ";\n";
                sensType = "Accelerometer";
            } else if (tipo.equals("android.sensor.gyroscope")) {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ITALY);
                Calendar c = Calendar.getInstance();
                dati = String.valueOf(sensorEvent.values[0]) +
                        " " + String.valueOf(sensorEvent.values[1]) +
                        " " + String.valueOf(sensorEvent.values[2]) +
                        " " + sdf.format(c.getTime()) + ";\n";
                sensType = "Gyroscope";
            }
            File root = new File(path+ "/" + folder, folderPhoneData);
            if (!root.exists()) {
                root.mkdirs(); // this will create folder.
            }


            OutputStreamWriter outStreamWriter = null;
            File out = new File(root, sensType + ".txt");  // file path to save

            if (out.exists() == false) {
                try {
                    out.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            try {
                Taccos = new FileOutputStream(out, true);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            outStreamWriter = new OutputStreamWriter(Taccos);

            try {
                outStreamWriter.append(dati);
                phoneStatus = true;
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                outStreamWriter.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }


    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    @Override
    protected void onResume() {
        super.onResume();
        //startRepeatingTask();
    }

    @Override
    protected void onPause() {
        super.onPause();
        SM.unregisterListener(this);
        SM1.unregisterListener(this);
        //stopRepeatingTask();
    }

    @Override
    protected void onDestroy() {
        beaconManager.unbind(this);
        if (client != null) {
            try {
                client.disconnect().await();
                HRos.close();
                GSRos.close();
                RRos.close();
                STos.close();
                Accos.close();
                Gyros.close();
                Taccos.close();
            } catch (InterruptedException e) {
                // Do nothing as this is happening during destroy
            } catch (BandException e) {
                // Do nothing as this is happening during destroy
            } catch (IOException e) {

            }
        }
        super.onDestroy();
    }

    @Override
    public void onBeaconServiceConnect() {

        beaconManager.setRangeNotifier(new RangeNotifier() {
            @Override
            public void didRangeBeaconsInRegion(Collection<Beacon> beacons, Region region) {
                if ((beacons.size() > 0)&&isSessionStarted) {
                    //stamp for the number of beacons in region
                    beaconStatus = true;
                    for (Beacon beacon : beacons) {
                        beaconsDesc = beacon.getId2() +
                                " " + beacon.getId3() +
                                " " + beacon.getRssi() + " ";
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ITALY);
                        Calendar c = Calendar.getInstance();
                        beaconsDesc += sdf.format(c.getTime()) + ";\n";
                        File root = new File(path+ "/" + folder, folderBeacons);
                        if (!root.exists()) {
                            root.mkdirs(); // this will create folder.
                        }
                        OutputStreamWriter outStreamWriter = null;
                        File out = new File(root, "Beacons.txt");  // file path to save

                        if (out.exists() == false) {
                            try {
                                out.createNewFile();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                        try {
                            Taccos = new FileOutputStream(out, true);
                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                        }
                        outStreamWriter = new OutputStreamWriter(Taccos);

                        try {
                            outStreamWriter.append(beaconsDesc);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        try {
                            outStreamWriter.flush();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    }
                }
            });

            try {
                beaconManager.startRangingBeaconsInRegion(new Region("myRangingUniqueId", null, null, null));
            } catch (RemoteException e) {
            }

    }

    public static Context getContext() {
        return mContext;
    }

    public static Activity getActivity(){return getActivity();}




    public void refreshListenerStatus(){
        bandStatus= checkAllStatus();
        //Log.i(TAG, phoneStatus+" "+bandStatus+" "+beaconStatus);
        printListenerStatus();
        phoneStatus=false;
        bandStatus=false;
        beaconStatus=false;
        resetAllStatus();
    }

    private void printListenerStatus(){
        if (phoneStatus)
        {
            ivphoneStatYES.setVisibility(View.VISIBLE);
            ivphoneStatNO.setVisibility(View.INVISIBLE);
        }else{
            ivphoneStatYES.setVisibility(View.INVISIBLE);
            ivphoneStatNO.setVisibility(View.VISIBLE);
            //Toast.makeText(getBaseContext(),"Fatal Error with phone sensor!",Toast.LENGTH_SHORT).show();
        }

        if (bandStatus)
        {
            ivbandStatYES.setVisibility(View.VISIBLE);
            ivbandStatNO.setVisibility(View.INVISIBLE);
        }else{
            ivbandStatYES.setVisibility(View.INVISIBLE);
            ivbandStatNO.setVisibility(View.VISIBLE);
            //Toast.makeText(getBaseContext(),"Band not altready attached, or problems with some sensors!",Toast.LENGTH_SHORT).show();
        }

        if (beaconStatus)
        {
            ivbeaconStatYES.setVisibility(View.VISIBLE);
            ivbeaconStatNO.setVisibility(View.INVISIBLE);
        }else{
            ivbeaconStatYES.setVisibility(View.INVISIBLE);
            ivbeaconStatNO.setVisibility(View.VISIBLE);
            //Toast.makeText(getBaseContext(),"Bluetooth not connected!",Toast.LENGTH_SHORT).show();
        }
    }

    Runnable m_statusChecker = new Runnable()
    {
        @Override
        public void run() {
            refreshListenerStatus(); //this function can change value of m_interval.
            m_handler.postDelayed(m_statusChecker, m_interval);
        }
    };

    public void startRepeatingTask()
    {
        m_statusChecker.run();
    }

    public void stopRepeatingTask()
    {
        m_handler.removeCallbacks(m_statusChecker);
    }

    public void openFolder()
    {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        Uri uri = Uri.parse(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath()+"/"+appFolder);
        intent.setDataAndType(uri, "text/csv");
        startActivity(Intent.createChooser(intent, "Open folder"));
    }

    //---------------------------------------------------------------------------------------------// band procedure
    private BandGsrEventListener mGsrEventListener = new BandGsrEventListener() {
        @Override
        public void onBandGsrChanged(BandGsrEvent event) {
            if ((event != null)&&isSessionStarted) {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ITALY);
                Calendar c = Calendar.getInstance();
                String string = String.format(event.getResistance()+" "+sdf.format(c.getTime())+";\n\r");
                try {
                    stateGSRos=true;
                    GSRos.write(string.getBytes());
                } catch (IOException e) {
                    appendToUI("Attenzione:scrittura fallita (GSR)");
                }
            }
        }
    };

    private BandRRIntervalEventListener mRRIntervalEventListener = new BandRRIntervalEventListener() {
        @Override
        public void onBandRRIntervalChanged(final BandRRIntervalEvent event) {
            if ((event != null)&&isSessionStarted) {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ITALY);
                Calendar c = Calendar.getInstance();
                String string = String.format(event.getInterval()+" "+sdf.format(c.getTime())+";\n\r");
                try {
                    stateRRos=true;
                    RRos.write(string.getBytes());
                } catch (IOException e) {
                    appendToUI("Attenzione:scrittura fallita (RR)");
                }
            }
        }
    };

    private BandSkinTemperatureEventListener mSkinTemperatureEventListener = new BandSkinTemperatureEventListener() {
        @Override
        public void onBandSkinTemperatureChanged(BandSkinTemperatureEvent bandSkinTemperatureEvent) {
            if ((bandSkinTemperatureEvent != null)&&isSessionStarted) {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ITALY);
                Calendar c = Calendar.getInstance();
                String string = String.format(bandSkinTemperatureEvent.getTemperature()+" "+sdf.format(c.getTime())+";\n\r");
                try {
                    stateSTos=true;
                    STos.write(string.getBytes());
                } catch (IOException e) {
                    appendToUI("Attenzione:scrittura fallita (ST)");
                }
            }
        }
    };
    private BandAccelerometerEventListener accelerometerEventListener = new BandAccelerometerEventListener() {
        @Override
        public void onBandAccelerometerChanged(BandAccelerometerEvent bandAccelerometerEvent) {
            if((bandAccelerometerEvent != null)&&isSessionStarted){
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ITALY);
                Calendar c = Calendar.getInstance();
                String string = String.valueOf(bandAccelerometerEvent.getAccelerationX()*9.8)+" "+
                        String.valueOf(bandAccelerometerEvent.getAccelerationY()*9.8)+" "+
                        String.valueOf(bandAccelerometerEvent.getAccelerationZ()*9.8)+ " "+
                        sdf.format(c.getTime())+";\n\r";
                try {
                    stateAccos=true;
                    Accos.write(string.getBytes());
                } catch (IOException e) {
                    appendToUI("Attenzione:scrittura fallita (Acc)");
                }
            }
        }

    };

    private BandGyroscopeEventListener gyroscopeEventListener = new BandGyroscopeEventListener() {
        @Override
        public void onBandGyroscopeChanged(BandGyroscopeEvent bandGyroscopeEvent) {

            if((gyroscopeEventListener!= null)&&isSessionStarted){
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ITALY);
                Calendar c = Calendar.getInstance();
                String string = String.valueOf(bandGyroscopeEvent.getAngularVelocityX())+" "+
                        String.valueOf(bandGyroscopeEvent.getAngularVelocityY())+" "+
                        String.valueOf(bandGyroscopeEvent.getAngularVelocityZ())+ " "+
                        sdf.format(c.getTime())+";\n\r";
                try {
                    stateGyros=true;
                    Gyros.write(string.getBytes());
                } catch (IOException e) {
                    appendToUI("Attenzione:scrittura fallita (Acc)");
                }
            }
        }
    };

    private BandHeartRateEventListener mHeartRateEventListener = new BandHeartRateEventListener() {
        @Override
        public void onBandHeartRateChanged(final BandHeartRateEvent event) {
            if ((event != null)&&isSessionStarted) {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ITALY);
                Calendar c = Calendar.getInstance();
                String string = String.format(event.getHeartRate()+" "+sdf.format(c.getTime())+";\n\r");
                try {
                    stateHros=true;
                    HRos.write(string.getBytes());
                } catch (IOException e) {
                    appendToUI("Attenzione:scrittura fallita (HR)");
                }
            }
        }
    };

    private class GsrSubscriptionTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... params) {
            try {
                if (getConnectedBandClient()) {
                    appendToUI("Band is connected.\n");
                    client.getSensorManager().registerGsrEventListener(mGsrEventListener, GsrSampleRate.MS200);
                } else {
                    appendToUI("Band isn't connected. Please make sure bluetooth is on and the band is in range.\n");
                }
            } catch (BandException e) {
                String exceptionMessage="";
                switch (e.getErrorType()) {
                    case UNSUPPORTED_SDK_VERSION_ERROR:
                        exceptionMessage = "Microsoft Health BandService doesn't support your SDK Version. Please update to latest SDK.\n";
                        break;
                    case SERVICE_ERROR:
                        exceptionMessage = "Microsoft Health BandService is not available. Please make sure Microsoft Health is installed and that you have the correct permissions.\n";
                        break;
                    default:
                        exceptionMessage = "Unknown error occured: " + e.getMessage() + "\n";
                        break;
                }
                appendToUI(exceptionMessage);

            } catch (Exception e) {
                appendToUI(e.getMessage());
            }
            return null;
        }
    }

    private class SkinTemperatureSubscriptionTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... params) {
            try {
                if (getConnectedBandClient()) {
                    appendToUI("Band is connected.\n");
                    client.getSensorManager().registerSkinTemperatureEventListener(mSkinTemperatureEventListener);
                } else {
                    appendToUI("Band isn't connected. Please make sure bluetooth is on and the band is in range.\n");
                }
            } catch (BandException e) {
                String exceptionMessage="";
                switch (e.getErrorType()) {
                    case UNSUPPORTED_SDK_VERSION_ERROR:
                        exceptionMessage = "Microsoft Health BandService doesn't support your SDK Version. Please update to latest SDK.\n";
                        break;
                    case SERVICE_ERROR:
                        exceptionMessage = "Microsoft Health BandService is not available. Please make sure Microsoft Health is installed and that you have the correct permissions.\n";
                        break;
                    default:
                        exceptionMessage = "Unknown error occured: " + e.getMessage() + "\n";
                        break;
                }
                appendToUI(exceptionMessage);

            } catch (Exception e) {
                appendToUI(e.getMessage());
            }
            return null;
        }
    }

    private class RRIntervalSubscriptionTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... params) {
            try {
                if (getConnectedBandClient()) {
                    int hardwareVersion = Integer.parseInt(client.getHardwareVersion().await());
                    if (hardwareVersion >= 20) {
                        if (client.getSensorManager().getCurrentHeartRateConsent() == UserConsent.GRANTED) {
                            client.getSensorManager().registerRRIntervalEventListener(mRRIntervalEventListener);
                        } else {
                            appendToUI("You have not given this application consent to access heart rate data yet."
                                    + " Please press the Heart Rate Consent button.\n");
                        }
                    } else {
                        appendToUI("The RR Interval sensor is not supported with your Band version. Microsoft Band 2 is required.\n");
                    }
                } else {
                    appendToUI("Band isn't connected. Please make sure bluetooth is on and the band is in range.\n");
                }
            } catch (BandException e) {
                String exceptionMessage="";
                switch (e.getErrorType()) {
                    case UNSUPPORTED_SDK_VERSION_ERROR:
                        exceptionMessage = "Microsoft Health BandService doesn't support your SDK Version. Please update to latest SDK.\n";
                        break;
                    case SERVICE_ERROR:
                        exceptionMessage = "Microsoft Health BandService is not available. Please make sure Microsoft Health is installed and that you have the correct permissions.\n";
                        break;
                    default:
                        exceptionMessage = "Unknown error occured: " + e.getMessage() + "\n";
                        break;
                }
                appendToUI(exceptionMessage);

            } catch (Exception e) {
                appendToUI(e.getMessage());
            }
            return null;
        }
    }

    private class HeartRateSubscriptionTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... params) {
            try {
                if (getConnectedBandClient()) {
                    if (client.getSensorManager().getCurrentHeartRateConsent() == UserConsent.GRANTED) {
                        client.getSensorManager().registerHeartRateEventListener(mHeartRateEventListener);
                    } else {
                        appendToUI("You have not given this application consent to access heart rate data yet."
                                + " Please press the Heart Rate Consent button.\n");
                    }
                } else {
                    appendToUI("Band isn't connected. Please make sure bluetooth is on and the band is in range.\n");
                }
            } catch (BandException e) {
                String exceptionMessage="";
                switch (e.getErrorType()) {
                    case UNSUPPORTED_SDK_VERSION_ERROR:
                        exceptionMessage = "Microsoft Health BandService doesn't support your SDK Version. Please update to latest SDK.\n";
                        break;
                    case SERVICE_ERROR:
                        exceptionMessage = "Microsoft Health BandService is not available. Please make sure Microsoft Health is installed and that you have the correct permissions.\n";
                        break;
                    default:
                        exceptionMessage = "Unknown error occured: " + e.getMessage() + "\n";
                        break;
                }
                appendToUI(exceptionMessage);

            } catch (Exception e) {
                appendToUI(e.getMessage());
            }
            return null;
        }
    }
    private class AccelerometerSubscriptionTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... params) {
            try {
                if (getConnectedBandClient()) {
                    appendToUI("Band is connected.\n");
                    client.getSensorManager().registerAccelerometerEventListener(accelerometerEventListener, SampleRate.MS32);
                } else {
                    appendToUI("Band isn't connected. Please make sure bluetooth is on and the band is in range.\n");
                }
            } catch (BandException e) {
                String exceptionMessage="";
                switch (e.getErrorType()) {
                    case UNSUPPORTED_SDK_VERSION_ERROR:
                        exceptionMessage = "Microsoft Health BandService doesn't support your SDK Version. Please update to latest SDK.\n";
                        break;
                    case SERVICE_ERROR:
                        exceptionMessage = "Microsoft Health BandService is not available. Please make sure Microsoft Health is installed and that you have the correct permissions.\n";
                        break;
                    default:
                        exceptionMessage = "Unknown error occured: " + e.getMessage() + "\n";
                        break;
                }
                appendToUI(exceptionMessage);

            } catch (Exception e) {
                appendToUI(e.getMessage());
            }
            return null;
        }
    }

    private class GyroscopeSubscriptionTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... params) {
            try {
                if (getConnectedBandClient()) {
                    appendToUI("Band is connected.\n");
                    client.getSensorManager().registerGyroscopeEventListener(gyroscopeEventListener, SampleRate.MS32);
                } else {
                    appendToUI("Band isn't connected. Please make sure bluetooth is on and the band is in range.\n");
                }
            } catch (BandException e) {
                String exceptionMessage="";
                switch (e.getErrorType()) {
                    case UNSUPPORTED_SDK_VERSION_ERROR:
                        exceptionMessage = "Microsoft Health BandService doesn't support your SDK Version. Please update to latest SDK.\n";
                        break;
                    case SERVICE_ERROR:
                        exceptionMessage = "Microsoft Health BandService is not available. Please make sure Microsoft Health is installed and that you have the correct permissions.\n";
                        break;
                    default:
                        exceptionMessage = "Unknown error occured: " + e.getMessage() + "\n";
                        break;
                }
                appendToUI(exceptionMessage);

            } catch (Exception e) {
                appendToUI(e.getMessage());
            }
            return null;
        }
    }

    private class HeartRateConsentTask extends AsyncTask<WeakReference<Activity>, Void, Void> {
        @Override
        protected Void doInBackground(WeakReference<Activity>... params) {
            Log.d("Lucia",String.format("Sto chiedendo il consenso"));
            try {
                if (getConnectedBandClient()) {

                    if (params[0].get() != null) {
                        client.getSensorManager().requestHeartRateConsent(params[0].get(), new HeartRateConsentListener() {
                            @Override
                            public void userAccepted(boolean consentGiven) {
                            }
                        });
                    }
                } else {
                    appendToUI("Band isn't connected. Please make sure bluetooth is on and the band is in range.\n");
                }
            } catch (BandException e) {
                String exceptionMessage="";
                switch (e.getErrorType()) {
                    case UNSUPPORTED_SDK_VERSION_ERROR:
                        exceptionMessage = "Microsoft Health BandService doesn't support your SDK Version. Please update to latest SDK.\n";
                        break;
                    case SERVICE_ERROR:
                        exceptionMessage = "Microsoft Health BandService is not available. Please make sure Microsoft Health is installed and that you have the correct permissions.\n";
                        break;
                    default:
                        exceptionMessage = "Unknown error occured: " + e.getMessage() + "\n";
                        break;
                }
                appendToUI(exceptionMessage);

            } catch (Exception e) {
                appendToUI(e.getMessage());
            }
            return null;
        }
    }


    private boolean getConnectedBandClient() throws InterruptedException, BandException {
        if (client == null) {
            BandInfo[] devices = BandClientManager.getInstance().getPairedBands();
            if (devices.length == 0) {
                appendToUI("Band isn't paired with your phone.\n");
                return false;
            }
            client = BandClientManager.getInstance().create(getBaseContext(), devices[0]);
        } else if (ConnectionState.CONNECTED == client.getConnectionState()) {
            return true;
        }

        appendToUI("Band is connecting...\n");
        return ConnectionState.CONNECTED == client.connect().await();
    }

    public boolean checkAllStatus()
    {
        Log.i("CHECK",stateHros+"+"+stateRRos+"+"+stateSTos+"+"+stateAccos+"+"+stateGSRos+"+"+stateGyros);
        return stateHros&&stateRRos&&stateSTos&&stateAccos&&stateGSRos&&stateGyros;
    }

    public void resetAllStatus(){
        stateGSRos=false;
        stateAccos=false;
        stateRRos=false;
        stateGyros=false;
        stateHros=false;
    }


}
