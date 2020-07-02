package org.gmu.track;

import android.app.Activity;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * User: ttg
 * Date: 16/07/13
 * Time: 18:55
 * To change this template use File | Settings | File Templates.
 */
public class MultipleTracker extends Tracker{

    List<Tracker> trackers;

    public MultipleTracker(List<Tracker> trackers)
    {
        this.trackers=trackers;
    }
    @Override
    public void activityStart(Activity activity)
    {
        for (int i = 0; i < trackers.size(); i++) {
            try{ if(trackers.get(i).isActive()) trackers.get(i).activityStart(activity);}catch (Exception ign){}

        }
    }

    @Override
    public void activityStop(Activity activity) {
        for (int i = 0; i < trackers.size(); i++) {
            try{if(trackers.get(i).isActive()) trackers.get(i).activityStop(activity);}catch (Exception ign){}

        }
    }

    @Override
    public void sendEvent(String guideId,String category, String action, LinkedHashMap<String,String> params) {
        for (int i = 0; i < trackers.size(); i++) {
            try{ if(trackers.get(i).isActive())trackers.get(i).sendEvent(guideId,category,action,params);}catch (Exception ign){}

        }
    }
}
