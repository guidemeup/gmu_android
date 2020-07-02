package org.gmu.utils.gis.wkt;

import android.location.Location;
import org.gmu.map.MapPosition;
import org.gmu.utils.MapUtils;

import org.mapsforge.core.model.LatLong;

import java.util.List;

/**
 * User: ttg
 * Date: 15/02/13
 * Time: 11:49
 * always returns last point
 */
public class SimpleGISBuffer {
    private static final double CONVERSION_FACTOR = 1000000d;
    private Location min = null, max = null;

    public void addPoint(LatLong geoPoint) {


        if (min == null) {
            min = MapUtils.mapsForge2Location(geoPoint);

        }
        if (max == null) {
            max = MapUtils.mapsForge2Location(geoPoint);
        }
        if (geoPoint.getLatitude() < min.getLatitude()) min.setLatitude(geoPoint.getLatitude());
        if (geoPoint.getLongitude() < min.getLongitude()) min.setLongitude(geoPoint.getLongitude());
        if (geoPoint.getLatitude() > max.getLatitude()) max.setLatitude(geoPoint.getLatitude());
        if (geoPoint.getLongitude() > max.getLongitude()) max.setLongitude(geoPoint.getLongitude());


    }

    public Location getMin() {
        return min;
    }


    public Location getMax() {
        return max;
    }

    private double toE6(double d) {
        return new Double (d );
    }

    public MapPosition getCenter(int screenW) {
        if (min == null) return null;
        double latitudeOffset = (toE6(max.getLatitude()) - toE6(min.getLatitude())) / 2;
        double longitudeOffset = (toE6(max.getLongitude()) - toE6(min.getLongitude())) / 2;


        LatLong gp = new LatLong(toE6(min.getLatitude()) + latitudeOffset, toE6(min.getLongitude()) + longitudeOffset);
        MapPosition r = new MapPosition();
        r.location = MapUtils.location2WKT(MapUtils.mapsForge2Location(gp));
        r.zoom = getZoomLevel(screenW);
        return r;
    }

    private Byte getZoomLevel(int screenW) {
        float distanceM = (float) (max.distanceTo(min) + 1);


        int a = (int) Math.floor(-logB2(distanceM / (156412 * screenW)));


        return new Byte("" + a);

    }

    private static double logB2(float x) {
        return Math.log(x) / Math.log(2);
    }


}
