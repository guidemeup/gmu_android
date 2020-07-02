package org.gmu.track;

import org.gmu.control.Controller;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * User: ttg
 * Date: 16/07/13
 * Time: 18:26
 * To change this template use File | Settings | File Templates.
 */
public abstract class Tracker {
    private static Tracker ourInstance = new MultipleTracker(Arrays.asList(new Tracker[]{new GAnalyticsTracker()}));
    protected boolean active=true;

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public static Tracker getInstance() {
        return ourInstance;
    }

    protected Tracker() {
    }

    public abstract void activityStart(android.app.Activity activity);
    public abstract void activityStop(android.app.Activity activity);
    public boolean mustLog(String guideId)
    {
        try
        {   boolean isPr=Controller.getInstance().getDao().isPreProductionGuide(guideId);
           return  !isPr;

        } catch (Exception ign)
        {
            ign.printStackTrace();
        }

        return true;


    }
    public abstract void sendEvent(String guideId, String category, String action, LinkedHashMap<String,String> params) ;

}
