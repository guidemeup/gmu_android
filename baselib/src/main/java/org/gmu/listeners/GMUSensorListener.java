package org.gmu.listeners;

import android.app.Activity;
import android.content.Context;
import android.hardware.*;
import android.location.Location;
import android.location.LocationManager;
import android.util.Log;
import org.gmu.control.Controller;
import org.gmu.utils.LowPassFilter;
import org.gmu.utils.Utils;

import java.util.List;

/**
 * User: ttg
 * Date: 23/04/13
 * Time: 10:31
 * To change this template use File | Settings | File Templates.
 */
public class GMUSensorListener implements SensorEventListener
{   private Location lastLocation=null;
    private static GeomagneticField gmf = null;
    private static final float grav[] = new float[3]; // Gravity (a.k.a
    // accelerometer data)
    private static final float mag[] = new float[3]; // Magnetic
    private static final float rotation[] = new float[9]; // Rotation matrix in
    // Android format
    private static final float orientation[] = new float[3]; // azimuth, pitch,
    // roll
    private static float smoothed[] = new float[3];




    private static String lastFacing="";
    private float lastAngle=0;
    private SensorManager mSensorManager;

    public GMUSensorListener()
    {

    }

    public void register(Activity main) {    //unregister

        unRegister(main);
        //start position
        // Get the location manager

        mSensorManager = (SensorManager) main.getSystemService(Context.SENSOR_SERVICE);
         List<Sensor> sensors = null;
        Sensor sensorGrav = null;
         Sensor sensorMag = null;
        sensors = mSensorManager.getSensorList(Sensor.TYPE_ACCELEROMETER);
        if (sensors.size() > 0) sensorGrav = sensors.get(0);

        sensors = mSensorManager.getSensorList(Sensor.TYPE_MAGNETIC_FIELD);
        if (sensors.size() > 0) sensorMag = sensors.get(0);

        mSensorManager.registerListener(this, sensorGrav, SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(this, sensorMag, SensorManager.SENSOR_DELAY_NORMAL);
    }


    public void unRegister(Activity main)
    {
        if (mSensorManager != null) {
            mSensorManager.unregisterListener(this);
        }
        mSensorManager = null;
    }


    public void onAccuracyChanged(Sensor arg0, int accuracy) {
    }


    public void onSensorChanged(SensorEvent event)
    {

        if(lastLocation==null||!lastLocation.equals(Controller.getInstance().getGmuContext().lastUserLocation))
        {
            lastLocation=Controller.getInstance().getGmuContext().lastUserLocation;
            float alt=0;
            try{alt=(float) lastLocation.getAltitude();}catch(Exception ign){}
            if(lastLocation!=null)
            {
                gmf = new GeomagneticField((float) lastLocation.getLatitude(), (float) lastLocation.getLongitude(), alt,
                        System.currentTimeMillis());
            }


         }
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            smoothed = LowPassFilter.filter(event.values, grav);
            grav[0] = smoothed[0];
            grav[1] = smoothed[1];
            grav[2] = smoothed[2];
        } else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            smoothed = LowPassFilter.filter(event.values, mag);
            mag[0] = smoothed[0];
            mag[1] = smoothed[1];
            mag[2] = smoothed[2];
        }

        // Get rotation matrix given the gravity and geomagnetic matrices
        SensorManager.getRotationMatrix(rotation, null, grav, mag);
        SensorManager.getOrientation(rotation, orientation);
        double floatBearing = orientation[0];

        // Convert from radians to degrees
        floatBearing = Math.toDegrees(floatBearing); // degrees east of true
        // north (180 to -180)

        // Compensate for the difference between true north and magnetic north
        if (gmf != null) floatBearing += gmf.getDeclination();

        // adjust to 0-360
        if (floatBearing < 0) floatBearing += 360;



        float x=  (float) floatBearing;

        //interpolate


        float dif=Math.abs(lastAngle-x);
        if (dif>15.0)
        {   lastAngle=x;
            //refresh location
            Controller.getInstance().getGmuContext().lastUserViewAngle=x;
            Controller.getInstance().onLocationChanged(Controller.getInstance().getGmuContext().lastUserLocation ) ;
        }
    }





}
