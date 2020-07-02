package org.gmu.control;

import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.location.Location;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import org.gmu.config.Constants;
import org.gmu.config.IConfig;
import org.gmu.context.GmuContext;
import org.gmu.dao.IPlaceElementDAO;

import org.gmu.dao.OrderDefinition;
import org.gmu.dao.impl.sqlite.DBPlaceElementDAO;
import org.gmu.pojo.NavigationItem;
import org.gmu.pojo.OfflineMapDefinition;
import org.gmu.pojo.PlaceElement;

import org.gmu.sync.UpdateStateChecker;
import org.gmu.track.Tracker;
import org.gmu.utils.Utils;

import java.io.File;
import java.util.*;

/**
 * User: ttg
 * Date: 12/11/12
 * Time: 13:01
 * To change this template use File | Settings | File Templates.
 */
public class Controller implements GmuEventListener {

    private static final String TAG = Controller.class.getName();
    private GmuContext gmuContext =new GmuContext();
    private static Controller ourInstance = null;
    private GmuEventListener delegate;
    private IPlaceElementDAO dao = null;

    private Context context = null;
    private MediaPlayer mediaPlayer;

    private IConfig config;


    public static Controller getInstance() {
        if (ourInstance == null) {
            ourInstance = new Controller();

        }

        return ourInstance;

    }

    private Map<String, Typeface> fonts = new HashMap();


    private Controller() {
        //initialize stack at first level
        NavigationItem item = new NavigationItem(null, null, VIEW_MICROSITE,null);
        gmuContext.navigationStack.push(item);


    }



    public IConfig getConfig() {
        return config;
    }

    public void setConfig(IConfig config)
    {
        if(this.config==null)
        {
            //initialize map mode from config
            this.getGmuContext().offlineMode = config.isDefaultMapModeOffline();
            //initialize priority to defaults in config
            //init to defaults;
            this.getGmuContext().order.setPriorByString(config.getAttribute(IConfig.ORDER_DEFINITION));

        }
        this.config = config;
    }

    public void setFont(View myV, String name, String style) {

        if (myV instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) myV;
            int count = group.getChildCount();
            for (int i = 0; i < count; i++) {

                setFont(group.getChildAt(i), name, style);

            }
        } else {
            updateFont(myV, name, style);
        }
    }


    public   OrderDefinition getDistanceOrderDefinition() {
        getGmuContext().order.updateLocation(getGmuContext().lastUserLocation);
        return   getGmuContext().order;
    }

    public GmuContext getGmuContext() {
        return gmuContext;
    }

    public void setGmuContext(GmuContext gmuContext) {
        if (Utils.isEmpty(gmuContext.getCurrentGuide())) {   //invalid context
            return;
        }
        //reload!
        this.switchGuide(gmuContext.getCurrentGuide());
        this.gmuContext = gmuContext;

        this.switchView(((NavigationItem) gmuContext.navigationStack.peek()).viewAreaid);

    }

    public void setContext(Context context) {
        this.context = context;

        //create fonts
        fonts = new HashMap();
        fonts.put("Omnes-Bold", Typeface.createFromAsset(context.getAssets(), "fonts/Omnes-Bold.otf"));
        fonts.put("Omnes-Regular", Typeface.createFromAsset(context.getAssets(), "fonts/Omnes-Regular.otf"));
        fonts.put("Omnes-Medium", Typeface.createFromAsset(context.getAssets(), "fonts/Omnes-Medium.otf"));
        fonts.put("Omnes-Thin", Typeface.createFromAsset(context.getAssets(), "fonts/Omnes-Thin.otf"));
        fonts.put("Omnes-LightItalic", Typeface.createFromAsset(context.getAssets(), "fonts/Omnes-LightItalic.otf"));
        fonts.put("default", fonts.get("Omnes-Regular"));

    }

    public boolean offlineMode() {
        return gmuContext.offlineMode;
    }

    /**
     * BEGIN GmuEventListener implementation*
     */

    public void goToDetail(String uid)
    {
        this.delegate.goToDetail (uid);
    }

    public void playRelatedAudio(String[] playList, Integer current) {
        if (!Constants.AUDIO_GUIDE_ENABLED) return;
        this.delegate.playRelatedAudio(playList, current);
    }

    public void close()
    {   try
        {
            if(this.delegate!=null)
            {
                this.delegate.close();

            }
            if (dao != null) {
                dao.destroy();
            }
            dao = null;
            ourInstance = null;
        }catch (Exception wng)
         {
             Log.w(TAG,"Error on close context",wng);
         }
    }

    public void switchView(int viewId) {


        if (viewId == GmuEventListener.VIEW_GUIDE_LIST)
        {   //switch dao to root
            switchGuide(Controller.getInstance().getConfig().getRootId());

            //initialize filter to downloaded guides
            Controller.getInstance().getGmuContext().setOnlyGuidesDownloadedFilter(Controller.getInstance().getGmuContext().onlyGuidesDownloadedFilter);


        }
        if (viewId == GmuEventListener.VIEW_GUIDE_LIST || viewId == GmuEventListener.VIEW_LIST) {   //reset selections

            Controller.getInstance().setSelectedChild(null);
            Controller.getInstance().setSelectedUID(null);



        }

        if (delegate != null) {
            delegate.switchView(viewId);
        }
    }



    public void onLocationChanged(Location l) {
        gmuContext.lastUserLocation = l;
        if(this.delegate!=null)
        {
            this.delegate.onLocationChanged(l);
        }

    }

    public void setFavorite(String uid, boolean remove) {

        if (remove)
        {    LinkedHashMap<String,String> params=new LinkedHashMap<String,String>();
            params.put("uid",uid);
            params.put("name",Controller.getInstance().getDao().load(uid).getTitle());
            Tracker.getInstance().sendEvent(Controller.getInstance().getDao().getBaseGuideId(),"user_action", "favorite_del", params);

            Controller.getInstance().getDao().delFromFavs(uid);
        } else {
            LinkedHashMap<String,String> params=new LinkedHashMap<String,String>();
            params.put("uid",uid);
            params.put("name",Controller.getInstance().getDao().load(uid).getTitle());
            Tracker.getInstance().sendEvent(Controller.getInstance().getDao().getBaseGuideId(),"user_action", "favorite_add", params);

            Controller.getInstance().getDao().addToFavs(uid);
        }
    }

    public void reloadDAO() {
        String guideId = this.getDao().getBaseGuideId();
        this.getDao().destroy();

        dao = new DBPlaceElementDAO(this.context, guideId);
    }

    public void switchGuide(String guideId)
    {

        if(Utils.isEmpty(guideId)) return;
        if (!this.getDao().getBaseGuideId().equals(guideId))
        {
            //switch dao
            this.getDao().destroy();
            Log.d("EEO", "Dao set to " + guideId);
            dao = new DBPlaceElementDAO(this.context, guideId);
            resetContext();
            delegate.switchGuide(guideId);


        }
        gmuContext.setCurrentGuide(guideId);




    }
    public void resetContext()
    {
        //reset variables
        gmuContext.filter = new PlaceElement();
        this.setSelectedUID(null);
        this.setSelectedChild(null);
        gmuContext.lastMapCenterLocation=null;
        gmuContext.offlineMode=  getConfig().isDefaultMapModeOffline();
        Controller.getInstance().setPlayingAudio(null, null);
    }

    public void refresh() {


        if (delegate != null) {
            delegate.refresh();
        }


    }

    public void setAutoCenter(boolean center)
    {
        if (delegate != null) {
            delegate.setAutoCenter(center);
        }

    }


    /**
     * END GmuEventListener implementation*
     */

    public boolean allowOffline()
    {

        boolean ret = false;
        try {


            OfflineMapDefinition m = getDao().getOfflineMapData();
            File f = new File(m.mapPath);
            return f.exists();

        } catch (Exception ign) {
        }
        return ret;

    }


    public GmuEventListener getDelegate() {
        return delegate;
    }

    public void setDelegate(GmuEventListener delegate) {
        this.delegate = delegate;
    }

    public IPlaceElementDAO getDao() {
        if (dao == null) {
            dao = new DBPlaceElementDAO(this.context, getMainGuideId());

        }
        return dao;
    }

    public void setDao(IPlaceElementDAO dao) {
        this.dao = dao;
    }

    public void pushDetail(String uid)
    {


        pushDetail(uid, null);
    }

    public void pushDetail(String uid, Integer targetView) {
        int view = this.getCurrentView();

        String groupFilter = this.getGroupFilter();
        if (targetView != null)
        {   //view is fixed
            if (view == VIEW_DETAIL&&targetView==VIEW_MAP)
            {   //1: VIEW-->MAP: pop detail (switch)
                gmuContext.navigationStack.pop();
                if (gmuContext.navigationStack.elementAt(gmuContext.navigationStack.size() - 1).viewAreaid == VIEW_MAP)
                {
                    //map in back stack: pop  previous map and goto new map
                    gmuContext.navigationStack.pop();
                }
            }
            view = targetView;
        } else {   //infer view by user action
            if (uid == null) {
                //start Navigation
                NavigationItem item = new NavigationItem(null, null, VIEW_LIST,gmuContext.getCurrentGuide());
                gmuContext.navigationStack.push(item);
                return;
            }
            PlaceElement pl = getDao().load(uid);
            PlaceElement child = getDao().load(this.getSelectedChild());

            //0: map-->detail
            if (view == VIEW_MAP)
            {
                if (gmuContext.navigationStack.elementAt(gmuContext.navigationStack.size() - 2).viewAreaid == VIEW_DETAIL) {
                    //detail in back stack: pop  previous detail and goto new detail (switch)
                    gmuContext.navigationStack.pop();
                }
                //map  are always not stackable
               //gmuContext.navigationStack.pop();
                view = VIEW_DETAIL;
            }
            else if (!Utils.isEmpty(groupFilter) && Utils.equals(uid, groupFilter))
            {   //1: uid==groupfilter --> user wants to see detail
                view = Controller.VIEW_DETAIL;
            } else if (pl != null &&
                    Controller.getInstance().getDao().getRelatedPlaceElements(pl.getUid(),
                            IPlaceElementDAO.PARENT_AND_LINKS_RELATIONS, IPlaceElementDAO.VISIBLEONLIST_ITEMS).size() > 0) {
                //uid is a parent--> list children in list_view
                groupFilter = uid;
                uid = null;
                view = Controller.VIEW_LIST;
            } else
            {   //detail page
                view = Controller.VIEW_DETAIL;
                if (pl != null && Utils.contains(IPlaceElementDAO.DETAIL_ROOT_ITEMS, pl.getType())) {    //click on not children element


                    if (child != null && !Utils.equals(child.getAttributes().get("parent"), uid)) {
                        this.setSelectedChild(null);

                    }


                } else {  //related item --> set has child
                    this.setSelectedChild(uid);

                }

            }
        }
        if (this.getCurrentView() == Controller.VIEW_DETAIL && view == Controller.VIEW_DETAIL)
        {   //detail to detail view switch are not stackable
            gmuContext.navigationStack.pop();

        }
        NavigationItem item = new NavigationItem(uid, groupFilter, view,gmuContext.getCurrentGuide());
        gmuContext.navigationStack.push(item);


    }

    public boolean popDetail() {
        if (isTopLevel()) {
            return true;
        } else {
            gmuContext.navigationStack.pop();
            this.switchGuide(((NavigationItem) gmuContext.navigationStack.peek()).guideuid);
            return false;
        }


    }

    public boolean isTopLevel() {
        return (gmuContext.navigationStack.size() < 2);
    }

    public PlaceElement getFilter() {
        return gmuContext.filter;
    }

    public void setFilter(PlaceElement filter) {
        this.gmuContext.filter = filter;
    }

    public String getGroupFilter() {


        return ((NavigationItem) gmuContext.navigationStack.peek()).groupFilter;
    }

    public void setGroupFilter(String groupFilter) {
        ((NavigationItem) gmuContext.navigationStack.peek()).groupFilter = groupFilter;
    }

    public String getSelectedUID() {
        return ((NavigationItem) gmuContext.navigationStack.peek()).uid;
    }

    public void setSelectedUID(String selectedUID)
    {

        ((NavigationItem) gmuContext.navigationStack.peek()).uid = selectedUID;
    }

    public int getCurrentView() {
        return ((NavigationItem) gmuContext.navigationStack.peek()).viewAreaid;
    }

    public void setCurrentView(int currentView) {
        ((NavigationItem) gmuContext.navigationStack.peek()).viewAreaid = currentView;
    }

    public String getPreviousSelectedItem() {
        String ret = null;
        try {
            synchronized (gmuContext.navigationStack) {
                if (gmuContext.navigationStack.size() > 1) {
                    ret = ((NavigationItem) gmuContext.navigationStack.get(gmuContext.navigationStack.size() - 2)).groupFilter;
                }

            }
        } catch (Exception ign) {//empty stack
        }
        return ret;

    }

    public String getSelectedChild() {
        return gmuContext.selectedChild;
    }

    public void setSelectedChild(String selectedChild) {
        this.gmuContext.selectedChild = selectedChild;
    }

    public String[] getPlayList() {
        return gmuContext.playList;
    }

    public void setPlayList(String[] playList) {
        this.gmuContext.playList = playList;
    }

    /**
     * BEGIN Mediaplayer stuff*
     */


    public Integer getPlayingAudio() {
        return gmuContext.playingAudio;
    }

    public void setPlayingAudio(String[] playList, Integer current) {
        synchronized (this) {

            PlaceElement playingPlace = null;
            String path = null;
            if (current != null) {
                if (current < 0 || current >= playList.length) {   //invalid position: ignore
                    return;
                }
                playingPlace = this.getDao().load(playList[current]);

                if (playingPlace != null && playingPlace.getAttributes().get("related_file") != null) {
                    path = getDao().getRelatedFileDescriptor(playingPlace.getAttributes().get("related_file"));
                    if (Utils.equals(gmuContext.playingUID, playingPlace.getUid())) {   //ignore
                        return;
                    }
                }

            }
            gmuContext.playingUID = null;
            this.gmuContext.playingAudio = current;
            this.gmuContext.playList = playList;
            //release previous
            if (mediaPlayer != null) {
                //release media player

                mediaPlayer.release();
                mediaPlayer = null;


            }
            if (path != null) {   //start media player
                try {
                    mediaPlayer = new MediaPlayer();
                    mediaPlayer.setDataSource(path);
                    //mediaPlayer.setDataSource(file2play.getFileDescriptor());

                    mediaPlayer.prepare();
                    mediaPlayer.seekTo(0);
                    mediaPlayer.start();
                    gmuContext.playingUID = playingPlace.getUid();
                } catch (Exception e) {
                    mediaPlayer = null;
                    return;
                }


            }
        }
    }


    public void seekToNext() {

    }

    public void seekToPrevious() {

    }

    public MediaPlayer getMediaPlayer() {
        return mediaPlayer;
    }

    /**
     * END Mediaplayer stuff*
     */

    public boolean isOnlyDownloadedGuidesView() {
        return this.getGmuContext().onlyGuidesDownloadedFilter;

    }

    public void updateFont(View v, String name, String type) {
        if (v == null) return;
        Typeface font = fonts.get("default");
        if (!Utils.isEmpty(name)) {
            font = fonts.get(name);
        }
        int style = Typeface.NORMAL;
        if (!Utils.isEmpty(type) && type.equalsIgnoreCase("BOLD")) style = Typeface.BOLD;
        if (TextView.class.isAssignableFrom(v.getClass())) {
            ((TextView) v).setTypeface(font, style);

        } else if (Button.class.isAssignableFrom(v.getClass())) {

            ((Button) v).setTypeface(font, style);
        }


    }

    public static String getMainGuideId()
    {
        if (Controller.getInstance().getConfig().getMainGuideId() == null) {
            return Controller.getInstance().getConfig().getRootId();

        } else {
            return Controller.getInstance().getConfig().getMainGuideId();
        }
    }


    public void deleteGuide(String guideUID) {
        IPlaceElementDAO dao = new DBPlaceElementDAO(context, guideUID);
        dao.deleteGuide();
        dao.destroy();
        UpdateStateChecker.setGuideState(guideUID, UpdateStateChecker.UPDATESTATE.NOEXISTENT);
        Toast.makeText(context, "Guide has been removed!", Toast.LENGTH_SHORT).show();
        //mark guide as not downloaded
        PlaceElement item = getDao().load(guideUID);
        item.getAttributes().put("guidedownloaded", "FALSE");
        //remove draft indicator
        getDao().setPreProductionGuide(guideUID,false);
        //remove declaration file
        File destination = new File(dao.getRelatedFileDescriptor("base/kml/private-"+guideUID+".kml"));
        destination.delete();
        //reload dao
        reloadDAO();

        Controller.getInstance().refresh();

    }

    public Integer getScrollIndex(String UID)
    {
        return ((NavigationItem) gmuContext.navigationStack.peek()).scrollIndexes.get(UID);

    }

    public Integer setScrollIndex(String UID,Integer index)
    {
        return ((NavigationItem) gmuContext.navigationStack.peek()).scrollIndexes.put(UID,index);

    }

    public List<PlaceElement> getFilteredItems()
    {

        int searchDeep = 1;
        Set<String> filterType = IPlaceElementDAO.VISIBLEONLIST_ITEMS;
        if (!Utils.isEmpty(this.getFilter().getTitle())) {
            //deep search on title search filter and show only places and routes
            searchDeep = 999;
            filterType = IPlaceElementDAO.DETAIL_ROOT_ITEMS;
        }
        List<PlaceElement> result = this.getDao().list(this.getFilter(),
                this.getGroupFilter(),
                filterType, searchDeep, Controller.getInstance().getDistanceOrderDefinition());


        return result;

    }


    public PlaceElement getDetailedElement()
    {
        PlaceElement selected = getDao().load(getSelectedUID());
        PlaceElement selectedChild = getDao().load(getSelectedChild());
        if (selectedChild != null && !Utils.isEmpty(selectedChild.getPointWKT())) {
            selected = selectedChild;
        }
        return selected;
    }

    public Context getContext() {
        return context;
    }


}
