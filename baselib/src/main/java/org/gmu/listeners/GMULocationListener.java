package org.gmu.listeners;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;

import androidx.core.app.ActivityCompat;

import org.gmu.control.Controller;

import java.util.Arrays;
import java.util.List;

/**
 * User: ttg
 * Date: 14/01/13
 * Time: 10:56
 * To change this template use File | Settings | File Templates.
 */
public class GMULocationListener implements android.location.LocationListener {
    protected String TAG = "GMULocationListener";


    private boolean locationEnabled = false;
    protected long distanceMin = 5;

    private long lastGpsTms = 0;
    private long gpsInactivityThresholdMs = 30 * 1000;  //30 secs
    private static final long DEFAULT_ALTITUDE = 0;
    private Location lastLocation;
    private LocationManager locationManager = null;

    private static final long LOCATION_EXPIRATION_MS = 20 * 60 * 1000; //20min

    private List<String> PROVIDERSALLOWED = Arrays.asList(new String[]{LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER});


    public void setLocationEnabled(boolean enabled) {

        locationEnabled = enabled;
    }

    public void register(Activity main) {    //unregister
        unRegister(main);
        //start position
        // Get the location manager
        locationManager = (LocationManager) main.getSystemService(Context.LOCATION_SERVICE);
        for (int i = 0; i < PROVIDERSALLOWED.size(); i++) {
            String provider = PROVIDERSALLOWED.get(i);
            if (locationEnabled) listenToProvider(provider);
        }


    }

    public void unRegister(Activity main) {
        if (locationManager != null) {
            locationManager.removeUpdates(this);
        }
        locationManager = null;
    }

    public void setProvidersAllowed(List<String> allowed) {
        PROVIDERSALLOWED = allowed;
    }

    public void onExtendedLocationChanged(Location location) {
        Controller.getInstance().onLocationChanged(location);
    }

    public Location getLastLocation() {
        return lastLocation;
    }

    public void setLastLocation(Location loc) {
        lastLocation = loc;
    }

    public void setGpsInactivityThresholdMs(long gpsInactivityThresholdMs) {
        this.gpsInactivityThresholdMs = gpsInactivityThresholdMs;
    }

    public void setDistanceMin(long distanceMin) {
        this.distanceMin = distanceMin;
    }

    public void onLocationChanged(Location location) {


        try {

            if (location.getLatitude() == 0.0 || location.getLongitude() == 0.0) {   //ignore
                return;

            }
            long timeFrom = System.currentTimeMillis() - location.getTime();


            Log.d(TAG, "Location received from: " + location.getProvider() + "|accuracy: " + location.getAccuracy() + " timeFrom=" + timeFrom / 1000 + " s");
            if (timeFrom > LOCATION_EXPIRATION_MS) {
                //expired locations (allow)
                Log.w(TAG, "Expired location! current time= " + System.currentTimeMillis() + "- location time=" + location.getTime());

            }

            //ignore non-gps locations if gps is active
            if (!location.getProvider().equals(LocationManager.GPS_PROVIDER) &&
                    (System.currentTimeMillis() - lastGpsTms < gpsInactivityThresholdMs)
            ) { //ignore
                Log.d(TAG, "Location no GPS in threshold: ignored ");
                return;
            }
            if (location.getProvider().equals(LocationManager.GPS_PROVIDER)) {
                lastGpsTms = System.currentTimeMillis();
            }

            location.setAltitude(DEFAULT_ALTITUDE);


            float dist = 9999999;
            if (getLastLocation() != null) {
                dist = location.distanceTo(getLastLocation());
            }
            Log.d(TAG, "Location Recv: " + location.getProvider() + " lat: " + location.getLatitude() + " lon: "
                    + location.getLongitude() + " alt: " + location.getAltitude() + " acc: " + location.getAccuracy() + " dis:" + dist);
            if (dist > distanceMin) {
                Log.d(TAG, "Location Sended: " + location.getProvider());
                onExtendedLocationChanged(location);
                setLastLocation(location);

            } else {
                Log.d(TAG, "Location ignored by distance: " + location.getProvider());

            }


        } catch (Exception ex) {


            ex.printStackTrace();
        }
    }

    public void onStatusChanged(String s, int i, Bundle bundle) {

    }

    public void onProviderEnabled(String s) {

    }

    public void onProviderDisabled(String s) {

    }


    private synchronized void listenToProvider(String provider) {


        if (ActivityCompat.checkSelfPermission( Controller.getInstance().getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission( Controller.getInstance().getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        Location location = locationManager.getLastKnownLocation(provider);

        // Initialize the location fields
        if (location != null)
        {
            Log.d(TAG,"Provider " + provider + " has been selected.");
            onLocationChanged(location);
        }
        try{locationManager.requestLocationUpdates(provider, 1000, 10, this);}catch(Exception ign)
        {
           Log.e(TAG,"Error getting updates from "+provider,ign);
            return;
        }


    }
}