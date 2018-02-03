package com.jtstegeman.cs4518team5_3;

import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Binder;
import android.os.IBinder;
import android.provider.SyncStateContract;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.location.ActivityRecognitionClient;
import com.google.android.gms.location.DetectedActivity;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import static com.jtstegeman.cs4518team5_3.DetectedActivitiesIntentService.INTENT_STR;
import static com.jtstegeman.cs4518team5_3.GeofenceTransitionsIntentService.GEO_NAME;

public class BackgroundService extends Service implements SensorEventListener{
    public static final String FULLER = "Fuller";
    public static final String GORDON = "Gordon Library";

    public ScheduledExecutorService ex = Executors.newSingleThreadScheduledExecutor();
    private final IBinder mBinder = new BackgroundBinder();
    private float markedSteps=0;
    private float lastSteps=0;
    private SensorManager sensorManager;
    private ActivityRecognitionClient mActivityRecognitionClient;
    private PendingIntent mPendingIntent;
    private Intent mIntentService;
    private BroadcastReceiver broadcastReceiver;

    private long startOfCurrentActivity = System.currentTimeMillis();
    private int lastActivity = -1;
    private GeofencingClient mGeofencingClient;
    private PendingIntent mGeofencePendingIntent;
    private Set<String> geoIn = new HashSet<>();
    private Map<String, Integer> entryCounts = new HashMap<>();

    public BackgroundService() {
    }

    public class BackgroundBinder extends Binder {
        BackgroundService getService() {
            return BackgroundService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        Sensor countSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
        if (countSensor!=null)
            sensorManager.registerListener(this, countSensor, SensorManager.SENSOR_DELAY_NORMAL);

        mActivityRecognitionClient = new ActivityRecognitionClient(this);
        mIntentService = new Intent(this, DetectedActivitiesIntentService.class);
        mPendingIntent = PendingIntent.getService(this, 1, mIntentService, PendingIntent.FLAG_UPDATE_CURRENT);
        mActivityRecognitionClient.requestActivityUpdates(3000, mPendingIntent);
        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals(INTENT_STR)) {
                    int type = intent.getIntExtra("type", -1);
                    int confidence = intent.getIntExtra("confidence", 0);
                    handleUserActivity(type, confidence);
                }
                if (intent.getAction().equals(GEO_NAME)){
                    ArrayList<String> in = intent.getStringArrayListExtra("in");
                    for (String s : in){
                        if (!geoIn.contains(s)){
                            markSteps();
                            Toast.makeText(BackgroundService.this, "Entered: "+s, Toast.LENGTH_LONG).show();
                        }
                    }
                    in.clear();
                    in.addAll(in);
                }
            }
        };
        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver, new IntentFilter(INTENT_STR));
        mGeofencingClient = LocationServices.getGeofencingClient(this);
        try {
            mGeofencingClient.addGeofences(getGeofencingRequest(), getGeofencePendingIntent());
        } catch (SecurityException e){
            Toast.makeText(this,"Geofencing not allowed",Toast.LENGTH_LONG).show();
        }
    }

    private GeofencingRequest getGeofencingRequest() {
        GeofencingRequest.Builder builder = new GeofencingRequest.Builder();
        builder.setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER);
        builder.addGeofence(new Geofence.Builder()
                .setRequestId(GORDON)
                .setCircularRegion(42.274249, -71.806660,5)
                .setExpirationDuration(1000).setNotificationResponsiveness(0)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER |
                        Geofence.GEOFENCE_TRANSITION_EXIT)
                .build());
        builder.addGeofence(new Geofence.Builder()
                .setRequestId(FULLER)
                .setCircularRegion(42.274879, -71.806681,5)
                .setExpirationDuration(1000).setNotificationResponsiveness(0)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER |
                        Geofence.GEOFENCE_TRANSITION_EXIT)
                .build());
        return builder.build();
    }
    private PendingIntent getGeofencePendingIntent() {
        if (mGeofencePendingIntent != null) {
            return mGeofencePendingIntent;
        }
        Intent intent = new Intent(this, GeofenceTransitionsIntentService.class);
        mGeofencePendingIntent = PendingIntent.getService(this, 0, intent, PendingIntent.
                FLAG_UPDATE_CURRENT);
        return mGeofencePendingIntent;
    }

    private void handleUserActivity(int type, int confidence) {
        if (lastActivity==-1){
            lastActivity=type;
            startOfCurrentActivity = System.currentTimeMillis();
            return;
        }
        if (type!=lastActivity){
            String name = "doing something";
            switch (lastActivity){
                case DetectedActivity.STILL:
                    name = "standing still";
                    break;
                case DetectedActivity.RUNNING:
                    name = "running";
                    break;
                case DetectedActivity.WALKING:
                    name = "walking";
                    break;
                default:
                    break;
            }
            long duration = System.currentTimeMillis() - startOfCurrentActivity;
            int min = (int)duration/60000;
            int sec = ((int)duration%60000)/1000;
            Toast.makeText(this, String.format("You just finished %s for %d minutes, %d seconds.", name, min, sec),Toast.LENGTH_LONG).show();
            lastActivity=type;
            startOfCurrentActivity = System.currentTimeMillis();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        sensorManager.unregisterListener(this);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver);
        mGeofencingClient.removeGeofences(getGeofencePendingIntent());
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent,flags,startId);
        return START_STICKY;
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        if (sensorEvent!=null){
            Log.i("SEV","Sensor Event");
            switch (sensorEvent.sensor.getType()){
                case Sensor.TYPE_STEP_COUNTER:
                    if (markedSteps==0){
                        markedSteps = sensorEvent.values[0];
                    }
                    lastSteps = sensorEvent.values[0];
                    checkZone();
                    break;
                default:
                    break;
            }
        }
    }

    private void checkZone() {
        if (this.getSteps()>=6){
            if (!this.geoIn.isEmpty()){
                for (String s : this.geoIn){
                    int i =  this.getEntryCount(s);
                    this.entryCounts.put(s, i+1);
                    Toast.makeText(this, "6 Steps into zone: '"+s+"', incrementing count",Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    public float getSteps(){
        return lastSteps - markedSteps;
    }

    public void markSteps(){
        markedSteps = lastSteps;
    }

    public String getActivityName(){
        String name = "doing something";
        switch (this.lastActivity){
            case DetectedActivity.STILL:
                name = "standing still";
                break;
            case DetectedActivity.RUNNING:
                name = "running";
                break;
            case DetectedActivity.WALKING:
                name = "walking";
                break;
            default:
                break;
        }
        return name;
    }

    public int getCurrentActivity(){
        return this.lastActivity;
    }

    public int getEntryCount(String zoneId){
        Integer i = entryCounts.get(zoneId);
        if (i==null)
            return 0;
        return i;
    }
}
