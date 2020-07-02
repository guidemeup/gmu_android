package org.gmu.track;

import android.app.Activity;


import com.google.firebase.analytics.FirebaseAnalytics;



import java.util.Iterator;
import java.util.LinkedHashMap;


/**
 * User: ttg
 * Date: 16/07/13
 * Time: 18:27
 * TODO: desactivado: implementar!!
 */
public class GAnalyticsTracker extends Tracker
{

    private boolean ENABLED=false;

    private FirebaseAnalytics mFirebaseAnalytics;
     public GAnalyticsTracker()
     {
         super();

     }
    @Override
    public void activityStart(Activity activity) {

        // Obtain the FirebaseAnalytics instance.
       if(ENABLED) mFirebaseAnalytics = FirebaseAnalytics.getInstance(activity);


    }

    @Override
    public void activityStop(Activity activity) {

        //EasyTracker.getInstance().activityStop(activity);
    }

    @Override
    public void sendEvent(String guideId,String category, String action, LinkedHashMap<String, String> params)
    {
        if(!ENABLED||!mustLog(guideId)) return;
        Iterator<String> keys=params.keySet().iterator();
        //only log first param
        if (keys.hasNext()) {
            String next = keys.next();

            //TODO implementar!!
           /* Bundle bundle = new Bundle();
            bundle.putString(FirebaseAnalytics.Param.ITEM_ID, id);
            bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, name);
            bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, "image");
            mFirebaseAnalytics.logEvent(category, bundle);*/


         //   EasyTracker.getInstance().getTracker().sendEvent(category, action,params.get(next) , null);
        }


    }
}
