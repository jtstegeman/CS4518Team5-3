package com.jtstegeman.cs4518team5_3;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.icu.util.TimeUnit;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.location.DetectedActivity;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;

public class MainActivity extends AppCompatActivity {



    private BackgroundService mService;
    private boolean mBound=false;
    ScheduledFuture<?> updateActions=null;
    TextView stepCount;
    TextView curAct;
    TextView fullerCount;
    TextView libraryCount;
    ImageView activityDisplay;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        stepCount = (TextView) findViewById(R.id.stepCount);
        curAct = (TextView) findViewById(R.id.curActiv);
        fullerCount = (TextView) findViewById(R.id.fullerCount);
        libraryCount = (TextView) findViewById(R.id.libraryCount);
        activityDisplay = (ImageView) findViewById(R.id.activityDisplay);
        updateUI();
    }

    @Override
    protected void onPause() {
        super.onPause();
        unbindService(mConnection);
        mBound=false;
        mService=null;
        if (updateActions!=null){
            updateActions.cancel(true);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Intent intent = new Intent(this, BackgroundService.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
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
            fullerCount.setText(mService.getEntryCount(BackgroundService.FULLER));
            libraryCount.setText(mService.getEntryCount(BackgroundService.GORDON));
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
