package org.gmu.inappbilling;

import android.location.Location;

import org.gmu.control.Controller;
import org.gmu.map.MapPosition;
import org.gmu.pojo.PlaceElement;
import org.gmu.track.Tracker;
import org.gmu.utils.MapUtils;

import org.gmu.utils.Utils;
import org.json.JSONArray;

import java.util.LinkedHashMap;
import java.util.regex.Pattern;


/**
 * Created by ttg on 11/03/2015.
 */
public class PromotionChecker
{

    public static void applyPromotion(PurchaseItem item)
    {
        try
        {
            //promotion based on keys on description
            String description=item.description;
            if(Utils.isEmpty(description)) return;
            int start=description.indexOf("[");
            int end=description.lastIndexOf("]");
            if(start!=-1&&end!=-1)
            {  String jss=description.substring(start,end+1);

                JSONArray js=new JSONArray (jss);
                for(int i=0;i<js.length();i++)
                {
                   if(checkPattern(js.getString(i)))
                   {    //set purchased
                       item.price = "none";
                       item.purchased = true;
                       return;
                   }
                }
            }

        }catch (Exception ign){ return;}




    }

    private static boolean checkPattern(String pattern)
    {
      double kmRad=Double.MAX_VALUE;

       if(Utils.isEmpty(pattern)) return true;

       String[] p= pattern.split( Pattern.quote("|"));
        if(p[0].equals("a"))
        {   //all allowed
            return true;
        }


        Location baseCenter=new Location("dummy");
        baseCenter.setLatitude(new Double(p[1]));
        baseCenter.setLongitude(new Double(p[2]));


        if(p.length>3)
       {
           kmRad=Double.parseDouble(p[3]);
           kmRad=kmRad*1000.0;
       }

       if(p[0].contains("g"))
       {    //check last gps position
           Location lastGPS=Controller.getInstance().getGmuContext().lastUserLocation;
            if(lastGPS!=null&&lastGPS.distanceTo(baseCenter)<kmRad)
            {
                return true;
            }

       }
        if(p[0].contains("p"))
        {    //check last map center position
            MapPosition lastCenter=Controller.getInstance().getGmuContext().lastMapCenterLocation;
            if(lastCenter!=null&&!Utils.isEmpty(lastCenter.location))
            {
                Location lastGPS= MapUtils.WKT2Location(lastCenter.location);
                if(lastGPS!=null&&lastGPS.distanceTo(baseCenter)<kmRad)
                {
                    return true;
                }
            }

        }

       return false;

    }

    public static void main(String args[]) throws Exception
    {

        PurchaseItem it=new PurchaseItem();
        it.description="pepepe";
        applyPromotion(it);
        it.description="lalslsla{[a]}lalsls";
        applyPromotion(it);
        it.description="lalslsla[g|18.36868|-72.10705]lalsls";
        applyPromotion(it);
        it.description="lalslsla[g|18.36868|-72.10705|4.5]lalsls";
        applyPromotion(it);
        it.description="lalslsla[gp|18.36868|-72.10705|4.5]lalsls";
        applyPromotion(it);

    }



}
