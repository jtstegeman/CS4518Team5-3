package com.jtstegeman.cs4518team5_3;

import android.app.IntentService;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.location.DetectedActivity;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p>
 * TODO: Customize class - update intent actions and extra parameters.
 */
public class GeofenceTransitionsIntentService extends IntentService {

    public static final String GEO_NAME = "geof_intent";
    private final Set<String> in = new HashSet<>();

    public GeofenceTransitionsIntentService() {
        super("GeofenceTransitionsIntentService");
    }

    protected void onHandleIntent(Intent intent) {
        GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);
        if (geofencingEvent.hasError()) {
            return;
        }
        Log.i("GEO", "ALERT");
        int geofenceTransition = geofencingEvent.getGeofenceTransition();



        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER) {
            List<Geofence> triggeringGeofences = geofencingEvent.getTriggeringGeofences();
            for (Geofence f : triggeringGeofences) {
                in.add(f.getRequestId());
            }
            sendNotification();
        } else if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT) {

            List<Geofence> triggeringGeofences = geofencingEvent.getTriggeringGeofences();
            for (Geofence f : triggeringGeofences){
                in.remove(f.getRequestId());
            }
            sendNotification();
        }
    }

    private void sendNotification() {
        Intent intent = new Intent(GEO_NAME);
        intent.putStringArrayListExtra("in", new ArrayList<String>(in));
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }
}
