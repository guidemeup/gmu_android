package org.gmu.sync;

import android.util.Log;
import org.gmu.config.Constants;
import org.gmu.control.Controller;
import org.gmu.pojo.PlaceElement;
import org.gmu.utils.FileUtils;
import org.gmu.utils.NetUtils;
import org.gmu.utils.Utils;

import java.io.File;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * User: ttg
 * Date: 9/04/13
 * Time: 13:00
 * Silent check update timestamps in current guides.
 */
public class UpdateStateChecker
{
   public static enum UPDATESTATE {
        UPDATED, NOTUPDATED, NOEXISTENT,NOTCHECKED
    }
    private static final String TAG = UpdateStateChecker.class.getName();
    private static Map<String,UPDATESTATE> updateStateMap =new HashMap();


    public static void checkForUpdates()
    {
        try{ UpdateThread t=new UpdateThread();
            t.start();} catch (Throwable ign){}


    }

    private static void checkAllGuides() throws Exception
    {
         //1: get guide list
           List<PlaceElement> guides=Controller.getInstance().getDao().listAllGuides();
           File tmpDir = new File(Utils.getFilePath("/cache/gmutmp"));
            tmpDir.mkdirs();
            for (int i = 0; i < guides.size(); i++) {
                PlaceElement placeElement = guides.get(i);
                try{ checkGuide(placeElement.getUid());
                }catch (Exception ign)
                 {   //ignore individual errors
                 }
            }




    }

    public static UPDATESTATE checkGuide(String guideId) throws Exception
    {       String baseServerGuides=Controller.getInstance().getConfig().getBaseServerGuides();



          //2: get old update ts
            File oldFile=new File(Utils.getFilePath("/" + guideId + "/" + Constants.UPDATEFILENAME));
            if(!oldFile.exists())
            {
                setGuideState(guideId, UPDATESTATE.NOEXISTENT);
            }else
            {
                long oldVersionTs =readTs(oldFile);

                //3: check remote update file
                    String newUpdate =new String(NetUtils.getURL(baseServerGuides + "/" + guideId + "/" + Constants.UPDATEFILENAME+"?t="+System.currentTimeMillis()));

                    long newVersionTs=new Long(newUpdate);

                    //update guide state
                    if (newVersionTs > oldVersionTs)
                    {
                        setGuideState(guideId, UPDATESTATE.NOTUPDATED);
                    }else
                    {
                        setGuideState(guideId, UPDATESTATE.UPDATED);
                    }


            }//else

        return  getGuideState(guideId);
    }


    private static UPDATESTATE getGuideState(String guideUID)
    {   if(!updateStateMap.containsKey(guideUID))
        {      return UPDATESTATE.NOTCHECKED;
        }
        else
        {return   updateStateMap.get(guideUID);}
    }
    public static void setGuideState(String guideUID,UPDATESTATE state)
    {
        updateStateMap.put(guideUID, state);
    }


    private static Long readTs(File f) throws Exception
    {   if(!f.exists()){return (long)0;}

        return new Long(FileUtils.readFile(f.getAbsolutePath()));

    }


    private static final class UpdateThread extends Thread
    {


        public void run() {

            try {

                checkAllGuides();


            }  catch (Throwable ign)
            {
                Log.e(TAG, "Error checking updates", ign);

            }

        }
    }




}
