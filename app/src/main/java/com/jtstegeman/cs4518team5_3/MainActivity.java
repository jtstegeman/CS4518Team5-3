package com.jtstegeman.cs4518team5_3;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.icu.util.TimeUnit;
import android.os.IBinder;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.DetectedActivity;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;

public class MainActivity extends AppCompatActivity {

    private static final String BUNDLE_STEPS = "steps";
    private static final String BUNDLE_FULLER_COUNT = "fuller_count";
    private static final String BUNDLE_LIBRARY_COUNT = "library_count";
    private BackgroundService mService;
    private boolean mBound=false;
    private float steps = 0.0f;
    private int fuller = 0;
    private int gordon = 0;
    ScheduledFuture<?> updateActions=null;
    TextView stepCount;
    TextView curAct;
    TextView fullerCount;
    TextView libraryCount;
    ImageView activityDisplay;

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        steps = savedInstanceState.getFloat(BUNDLE_STEPS, 0.0f);
        fuller = savedInstanceState.getInt(BUNDLE_FULLER_COUNT, 0);
        gordon = savedInstanceState.getInt(BUNDLE_LIBRARY_COUNT, 0);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        stepCount = (TextView) findViewById(R.id.stepCount);
        curAct = (TextView) findViewById(R.id.curActiv);
        fullerCount = (TextView) findViewById(R.id.fullerCount);
        libraryCount = (TextView) findViewById(R.id.libraryCount);
        activityDisplay = (ImageView) findViewById(R.id.activityDisplay);

        requestPermission();

        updateUI();
    }

    private void requestPermission(){
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        1);
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 1: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    runBackgroundService();
                } else {
                   finish();
                }
            }
        }
    }

    private void runBackgroundService(){
        Intent intent = BackgroundService.makeIntent(this, steps, fuller, gordon);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(mBound) {
            steps = mService.getSteps();
            fuller = mService.getEntryCount(BackgroundService.FULLER);
            gordon = mService.getEntryCount(BackgroundService.GORDON);

            unbindService(mConnection);
            mBound = false;
                mService = null;
            if (updateActions != null) {
                updateActions.cancel(true);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED){
            runBackgroundService();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        if (mService != null) {
            savedInstanceState.putFloat(BUNDLE_STEPS, steps);
            savedInstanceState.putInt(BUNDLE_FULLER_COUNT, fuller);
            savedInstanceState.putInt(BUNDLE_LIBRARY_COUNT, gordon);
        }
    }

    void updateUI() {
        if (mService != null) {
            stepCount.setText("Steps: " + mService.getSteps());
            curAct.setText(mService.getActivityName());
            switch (mService.getCurrentActivity()) {
                case DetectedActivity.STILL:
                    activityDisplay.setImageDrawable(getDrawable(R.drawable.still));
                    break;
                case DetectedActivity.WALKING:
                    activityDisplay.setImageDrawable(getDrawable(R.drawable.walking));
                    break;
                case DetectedActivity.RUNNING:
                    activityDisplay.setImageDrawable(getDrawable(R.drawable.running));
                    break;
            }
            fullerCount.setText(String.valueOf(mService.getEntryCount(BackgroundService.FULLER)));
            libraryCount.setText(String.valueOf(mService.getEntryCount(BackgroundService.GORDON)));
        }
        else {
            stepCount.setText("Waiting");
            curAct.setText("Waiting");
        }
    }

    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            BackgroundService.BackgroundBinder binder = (BackgroundService.BackgroundBinder) service;
            mService = binder.getService();
            updateActions = mService.ex.scheduleWithFixedDelay(new Runnable() {
                @Override
                public void run() {
                    runOnUiThread(new Runnable() {@Override
                    public void run() {updateUI();}});
                }
            }, 1000, 1000, java.util.concurrent.TimeUnit.MILLISECONDS);
            mBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
        }
    };
}
