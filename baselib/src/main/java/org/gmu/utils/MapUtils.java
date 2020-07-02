package org.gmu.utils;

import android.location.Location;

import org.mapsforge.core.model.LatLong;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

/**
 * User: ttg
 * Date: 15/12/12
 * Time: 20:06
 * To change this template use File | Settings | File Templates.
 */
public class MapUtils {

    private static final String[] INVALID_TILESOURCES=new String[]{"opencyclemap.org","mapbox"};

    public static final String[] OSM_BASE_TILES=new String[]{ "a.tile.openstreetmap.org", "b.tile.openstreetmap.org", "c.tile.openstreetmap.org"};

    public static boolean isValidMapTileSource(String hostname)
    {
        //invalid tile sources
        if(!Utils.isEmpty(hostname))
        {
            for (String invalidTilesource : INVALID_TILESOURCES) {
                if(hostname.toLowerCase().contains(invalidTilesource))
                {
                    return false;
                }
            }
        }
        return true;

    }

    public static com.google.android.gms.maps.model.LatLng location2GMaps(Location l) {
        return new com.google.android.gms.maps.model.LatLng(l.getLatitude(), l.getLongitude());
    }

    public static LatLong location2MapsForge(Location l)
    {    return new org.mapsforge.core.model.LatLong(l.getLatitude(),l.getLongitude());

    }



    public static Location mapsForge2Location(org.mapsforge.core.model.LatLong g) {
        Location l = new Location("dummy");
        l.setLatitude((double) (g.getLatitude()));
        l.setLongitude((double) (g.getLongitude()));
        return l;
    }



    public static Location WKT2Location(String WKT) {

        List<Location> g = toLocation(WKT);
        if (g != null && g.size() > 0) {

            return g.get(0);
        }
        return null;
    }

    public static String location2WKT(Location loc) {
        if(loc==null) return "";
        return "POINT(" + loc.getLongitude() + " " + loc.getLatitude() + ")";
    }
    public static boolean isLine(String WKT)
    {

        return (!Utils.isEmpty(WKT))&&(WKT.contains("LINE"));

    }

    /**OSMDROID**/

    public static List<Location> toLocation(String WKT) {

        List<Location> ret = new ArrayList<Location>();
        if (!Utils.isEmpty(WKT)) {
            WKT = WKT.replace("POINT(", "").replace(")", "").replace("MULTILINESTRING((", "").replace(",", " ").replace("LINESTRING(", "");
            StringTokenizer token = new StringTokenizer(WKT, " ");

            double lon, lat;
            while (token.hasMoreTokens())
            {
                lon = new Double(token.nextToken());
                lat = new Double(token.nextToken());
                Location l=new Location("dummy");
                l.setLatitude(lat);
                l.setLongitude(lon);
                ret.add(l);
            }
        }

        return ret;
    }



}
