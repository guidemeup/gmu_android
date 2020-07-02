package org.gmu.base;


import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import android.os.StrictMode;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.Toast;


import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.DialogFragment;

import org.gmu.config.Constants;
import org.gmu.config.IConfig;
import org.gmu.control.Controller;
import org.gmu.control.GmuEventListener;
import org.gmu.dao.impl.AbstractPlaceElementDAO;
import org.gmu.fragments.AbstractMapFragment;
import org.gmu.fragments.CloseDialogFragment;
import org.gmu.fragments.GuideListFragment;
import org.gmu.fragments.MicroSiteFragment;
import org.gmu.listeners.GMULocationListener;
import org.gmu.listeners.GMUSensorListener;
import org.gmu.listeners.PausePlayPhoneStateListener;
import org.gmu.pojo.PlaceElement;

import org.gmu.sync.GuideSynchronizer;
import org.gmu.track.Tracker;
import org.gmu.ui.GuideDownloadProgressDialog;
import org.gmu.ui.PlaceSearchView;
import org.gmu.utils.MapUtils;
import org.gmu.utils.Utils;

import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Locale;

public class GmuMainActivity extends GmuFragmentActivity
        implements SearchView.OnQueryTextListener, PlaceSearchView.OnCollapseListener, GmuEventListener {


    private static final String TAG =GmuMainActivity.class.getName();
    private static int theme=-1;



    private MenuItem searchItem;
    private PlaceSearchView searchView;


    private PhoneStateListener phoneStateListener = new PausePlayPhoneStateListener();

    private GMULocationListener locationListener = new GMULocationListener();
    private GMUSensorListener sensorListener=new GMUSensorListener();

    private boolean locationEnabled=false;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {


        //set initial context
        initController();
        if(theme==-1) theme= Controller.getInstance().getConfig().getTheme();


        setTheme(theme); //Used for theme switching in samples

        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.main);

        controller.setDelegate(this);

        if(Constants.AUDIO_GUIDE_ENABLED)
        {   //listener to stop play on phone calls
            TelephonyManager mgr = (TelephonyManager) this.getSystemService(Activity.TELEPHONY_SERVICE);
            if (mgr != null) {
                mgr.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
            }


        }



        //set custom logo
//        ImageView logo = (ImageView) findViewById(android.R.id.home);
//        logo.setImageDrawable( getResources().getDrawable( R.drawable.logo));





    }


    @Override
    public void onStart()
    {
        super.onStart();
        checkPermission();
        initController();
        Tracker.getInstance().activityStart(this);

        if(Controller.getInstance().getGmuContext().isUpdateInProgress())
        {   //wait to update
            return;


        }




        if (controller.getGmuContext().getCurrentGuide()!=null&& AbstractPlaceElementDAO.isGuideDownloaded(controller.getGmuContext().getCurrentGuide()))
        {
            //context restored or guide downloaded

           // Log.d("EOO","View="+controller.getSelectedUID());
            //restored instance --> refresh
            controller.refresh();
        } else
        {
            controller.getGmuContext().setCurrentGuide(controller.getMainGuideId());
            updateMainGuide();
        }
    }





    @Override
    public void onStop() {
        super.onStop();

        Tracker.getInstance().activityStop(this);
    }

    @Override
    public void onBackPressed() {

        if (currentFragment != null && currentFragment instanceof MicroSiteFragment) {
            //check if microsite can go back
            if (((MicroSiteFragment) currentFragment).goBack()) {   //consume event
                return;
            }
        }

        boolean firstLevel = controller.popDetail();

        if (firstLevel) {
            DialogFragment newFragment = CloseDialogFragment.newInstance(
                    R.string.close_app);
            newFragment.show(this.getSupportFragmentManager(), "dialog");


            //super.onBackPressed();
        } else {
            //force switchview event
            controller.switchView(controller.getCurrentView());


        }


        return;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu _menu)
    {
        menu = _menu;
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        searchItem = menu.findItem(R.id.menu_search);
        searchView = (PlaceSearchView) searchItem.getActionView();
        searchView.setOnQueryTextListener(this);
        searchView.setOnCollapseListener(this);

        //hide guide player
        this.findViewById(R.id.mediaplayerguide).setVisibility(View.GONE);
        //hide theme selector (TODO remove theme)
        menu.findItem(R.id.menuitem_theme).setVisible(false);
         Log.d(TAG,"Menu created");



//        //test
//        Bundle savedInstanceState=new Bundle();
//        savedInstanceState.putString("gmucontext",Constants.RESTORE);
//        GmuContext con= GmuContext.deserializeFromBundle(savedInstanceState);
//        //set context in controller
//        if(con!=null){ Controller.getInstance().setGmuContext(con);}


        return true;
    }

    @Override
    public boolean onOptionsItemSelected ( MenuItem item) {
        //back button in action bar
        int itemId = item.getItemId();

        if (itemId == android.R.id.home) {
            this.onBackPressed();
        } else if (itemId == R.id.menu_search) {
            //do nothing (filter will update view)

            //controller.setCurrentView(Controller.VIEW_LIST);
            //render();

        } else if (itemId == R.id.menu_directions) {
            //show selected place on gmaps
            PlaceElement selected = controller.getDao().load(controller.getSelectedUID());
            PlaceElement selectedChild = controller.getDao().load(controller.getSelectedChild());
            if (selectedChild != null && !Utils.isEmpty(selectedChild.getPointWKT())) {
                selected = selectedChild;
            }
            String title = selected.getTitle().replace("(", "-").replace(")", "-");
            Location l = MapUtils.WKT2Location(selected.getPointWKT());
            String uri = String.format(Locale.ENGLISH,
                    "geo:%f,%f?z=%d&q=%f,%f (%s)",
                    l.getLatitude(), l.getLongitude(), 2,
                    l.getLatitude(), l.getLongitude(),
                    title);

            if (!Utils.isIntentAvailable(this, uri))
            {
                Toast.makeText(this.getApplicationContext(), "Google Maps is not available in your device.", Toast.LENGTH_SHORT).show();
            } else {
                Intent intentM = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
                this.startActivity(intentM);
            }


        }else if (itemId == R.id.menu_refresh)
        {

            runOnUiThread(new Runnable() {
                public void run() {



                    updatePlace( controller.getDetailedElement());
                }
            });
        }
        else if (itemId == R.id.menu_refresh_map)
        {

            runOnUiThread(new Runnable() {
                public void run() {
                    //update all elements in group
                    PlaceElement selected = controller.getDao().load(controller.getGroupFilter());
                    updatePlace(selected);
                }
            });
        }

        else if (itemId == R.id.menu_share)
        {
            //send place url to share
            PlaceElement selected = controller.getDao().load(controller.getSelectedUID());
            PlaceElement selectedChild = controller.getDao().load(controller.getSelectedChild());
            if (selectedChild != null && !Utils.isEmpty(selectedChild.getPointWKT())) {
                selected = selectedChild;
            }

            Intent sendIntent = new Intent();
            sendIntent.setAction(Intent.ACTION_SEND);
            String url=""+selected.getAttributes().get("url");
            if(url.startsWith("/"))
            {   //relative url
                url= controller.getConfig().getBaseServerGuides()+url;
                if(controller.getDao().isPreProductionGuide(selected.getUid()))
                {
                    url=url.replace("guides","testguides");
                }


            }else if(!url.toUpperCase().startsWith("HTTP"))
            {
                url="http://"+url;
            }

            if(selected.getType().equals(PlaceElement.TYPE_GUIDE)&&
                    Controller.getInstance().getConfig().getPurchaseManager()!=null
                    )
            {
                //if store allows purchase share app url
                url="https://play.google.com/store/apps/details?id="+ Controller.getInstance().getConfig().getPackageName();



            }

            String text=selected.getTitle()+" - "+url;
            sendIntent.putExtra(Intent.EXTRA_TEXT, text);
            sendIntent.setType("text/plain");
            startActivity(sendIntent);

//            startActivity(Intent.createChooser(new Intent(Intent.ACTION_SEND,
//                    Uri.parse(selected.getAttributes().get("url"))),"Share URL"));

        }

        else if (itemId == R.id.menu_switchtomap) {
            //push and go to map
            controller.pushDetail(controller.getSelectedUID(), Controller.VIEW_MAP);

            controller.refresh();

        } else if (itemId == R.id.menu_centeronmap) {
            //push and go to map
            controller.pushDetail(controller.getSelectedUID(), Controller.VIEW_MAP);

            controller.refresh();

        } else if (itemId == R.id.menu_favorite) {
            if (controller.getFilter().getAttributes().containsKey("favorite"))
            {
                controller.getFilter().getAttributes().remove("favorite");
                item.setIcon(R.drawable.ic_action_favorite_off);
                item.setTitle(R.string.favorite_off);
                Toast.makeText(this.getApplicationContext(), getString(R.string.favorite_off), Toast.LENGTH_SHORT).show();
            } else {
                controller.getFilter().getAttributes().put("favorite", "TRUE");
                item.setTitle(R.string.favorite_on);
                item.setIcon(R.drawable.ic_action_favorite_on);
                Toast.makeText(this.getApplicationContext(), getString(R.string.favorite_on), Toast.LENGTH_SHORT).show();
            }
            controller.refresh();


        } else if (itemId == R.id.menu_gps)
        {

            if(!locationEnabled)  Toast.makeText(this, getString(R.string.nogpsperm), Toast.LENGTH_LONG).show();

            setAutoCenter(true);



        }
        return true;
    }


    @Override
    protected void onDestroy() {
        try {
            super.onDestroy();
            //destroy purchase manager

            if(Controller.getInstance().getConfig().getPurchaseManager()!=null)
            {
                Controller.getInstance().getConfig().getPurchaseManager().dispose();
            }


            //unregister listeners
            locationListener.unRegister(this);
            sensorListener.unRegister(this);
            TelephonyManager mgr = (TelephonyManager) this.getSystemService(Activity.TELEPHONY_SERVICE);
            if (mgr != null) {
                mgr.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE);
            }
            Controller.getInstance().setPlayingAudio(null, null);


        } catch (Exception ign) {
            ign.printStackTrace();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        locationListener.unRegister(this);
        sensorListener.unRegister(this);
    }

    @Override
    protected void onResume() {
        initController();
        super.onResume();
        locationListener.setProvidersAllowed(controller.getConfig().getLocationProviders());
        locationListener.setLocationEnabled(locationEnabled);
        locationListener.register(this);
        sensorListener.register(this);
    }

    private void refreshAudioView() {
        if (controller.getPlayingAudio() == null) {
            this.findViewById(R.id.mediaplayerguide).setVisibility(View.GONE);
        } else {
            this.findViewById(R.id.mediaplayerguide).setVisibility(View.VISIBLE);
        }
    }

    public void refresh() {

        Log.d("EOOO", "Render de " + controller.getCurrentView());

        refreshAudioView();
        refreshNavigationBar();

        if (controller.getCurrentView() == Controller.VIEW_GUIDE_LIST)
        {

           String  directAccessUID=getDirectAccessUID();
            //direct access to param guide
            if(directAccessUID!=null)
            {   Toast.makeText(this.getApplicationContext(), getString(R.string.private_access)+"...", Toast.LENGTH_LONG).show();

                String targetGuideUID=directAccessUID;

                GuideDownloadProgressDialog dialog = new GuideDownloadProgressDialog(this, targetGuideUID,true);
                dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    public void onDismiss(DialogInterface dialog) {
                        if (((GuideDownloadProgressDialog) dialog).showSyncMessages(true))
                        {   String downloadedGuideId=((GuideDownloadProgressDialog) dialog).getGuideUID();

                            //push detail to guide
                            Controller.getInstance().pushDetail(downloadedGuideId);
                            //switch dao
                            Controller.getInstance().switchGuide(downloadedGuideId);
                            //start with microsite
                            Controller.getInstance().switchView(GmuEventListener.VIEW_MICROSITE);

                            //remove direct load from preferences

                            SharedPreferences settings = getSharedPreferences("UserInfo", Context.MODE_PRIVATE);
                            SharedPreferences.Editor editor = settings.edit();
                            editor.remove("initialtoken");
                            editor.commit();


                        }
                    }
                });



            }
            switchFragment(FRAGMENTS.GUIDE_LIST_FRAGMENT);



        } else if (controller.getCurrentView() == Controller.VIEW_LIST) {   //unselect
            controller.setSelectedUID(null);


            switchFragment(FRAGMENTS.LIST_FRAGMENT);

        } else if (controller.getCurrentView() == Controller.VIEW_MAP)
        {
            closeSearch();

            AbstractMapFragment m = (AbstractMapFragment) switchFragment(FRAGMENTS.MAPFRAGMENT);


        } else if (controller.getCurrentView() == Controller.VIEW_DETAIL) {
           closeSearch();


            switchFragment(FRAGMENTS.DETAIL_FRAGMENT);

            LinkedHashMap<String,String> params=new LinkedHashMap<String,String>();
            String title= Controller.getInstance().getDao().load(controller.getSelectedUID()).getTitle();
            String text="guideuid:"+ Controller.getInstance().getDao().getBaseGuideId()+"#uid:"+controller.getSelectedUID()+"#title:"+title;
            params.put("uid",text);

            Tracker.getInstance().sendEvent(Controller.getInstance().getDao().getBaseGuideId(),"user_action", "detail_open", params);


        } else if (controller.getCurrentView() == Controller.VIEW_MICROSITE) {
            closeSearch();


            switchFragment(FRAGMENTS.MICROSITE_FRAGMENT);


        }
        getSupportActionBar().setDisplayHomeAsUpEnabled(!controller.isTopLevel());
        getSupportActionBar().setHomeButtonEnabled(!controller.isTopLevel());

        checkTheme();



    }


    public void updateMainGuide()
    {   boolean update=true;
        boolean interactive=false;

        if(System.currentTimeMillis() - controller.getGmuContext().lastUpdated < Constants.MINIMUM_UPDATE_PERIOD)
        {    //ignore download and refresh
            update=false;


        }
        if(update)
        {
            if(controller.getConfig().getAppType()== IConfig.APP_TYPE.GUIDE)
            {
                interactive=true;
            }else
            {   //store, check if there are downloaded guides
                if(controller.getGmuContext().onlyGuidesDownloadedFilter&&
                        GuideListFragment.getDownloadedGuides().size()>0)
                {  //user can see downloaded list, no update required
                   update=false;
                }

            }
        }



        if(Utils.needPackageUpdate(this))
        {
            update=true;interactive=false;
        }
        if(!update)
        {  controller.refresh();
            return;

        }else
        {


            //reload root definition
            GuideDownloadProgressDialog dialog = new GuideDownloadProgressDialog(GmuMainActivity.this, Controller.getMainGuideId(), interactive);
            dialog.setCancelable(false);
            dialog.setOnDismissListener(new DialogInterface.OnDismissListener()
            {
                public void onDismiss(DialogInterface dialog) {
                    if (((GuideDownloadProgressDialog) dialog).showSyncMessages(false))
                    {
                            if(checkEndStatus(((GuideDownloadProgressDialog) dialog).getEndStatus()))
                            {
                                Log.d(TAG,"End status="+((GuideDownloadProgressDialog) dialog).getEndStatus());

                                if( AbstractPlaceElementDAO.isGuideDownloaded(controller.getGmuContext().getCurrentGuide()))
                                {
                                    //set last update time to reduce sync server calls
                                    controller.getGmuContext().lastUpdated = System.currentTimeMillis();
                                    controller.refresh();
                                }

                            }

                     }else
                    {   checkEndStatus(((GuideDownloadProgressDialog) dialog).getEndStatus());

                    }

                }
            });

        }



    }

    /**
     * Check guide update end status
     * @param endStatus
     * @return  true if not error
     */
    private boolean checkEndStatus(GuideSynchronizer.SYNCSTATE endStatus)
    {
        if( endStatus == GuideSynchronizer.SYNCSTATE.UPDATE_ERROR || endStatus == GuideSynchronizer.SYNCSTATE.NO_VERSION_FOUND)
        {    //error downloading main guide--> show error dialog
            runOnUiThread(new Runnable() {
                public void run()
                {
                    //show user confirmation
                    DialogInterface.OnClickListener dialogClickListener =
                            new DialogInterface.OnClickListener()
                            {
                                public void onClick(DialogInterface dialog, int which) {
                                    switch (which){
                                        case DialogInterface.BUTTON_POSITIVE:
                                            //startup package don't contains mainguide  try to download update from server
                                            runOnUiThread(new Runnable() {
                                                public void run() {
                                                    //kill activity
                                                    updateMainGuide();
                                                }
                                            });
                                            break;

                                        case DialogInterface.BUTTON_NEGATIVE:
                                            Controller.getInstance().close();
                                            break;
                                    }
                                }
                            };

                    AlertDialog.Builder builder = new AlertDialog.Builder(GmuMainActivity.this);
                    builder.setCancelable(false);
                    builder.setMessage(GmuMainActivity.this.getString(R.string.mainupdateprompt))

                            .setPositiveButton(GmuMainActivity.this.getString(R.string.retry), dialogClickListener)
                            .setNegativeButton(GmuMainActivity.this.getString(R.string.exit), dialogClickListener).show();
                }
            });
            return false;

        }
        return true;

    }


    /**
     * BEGIN : GMUEventListener implementation*
     */

    public void onLocationChanged(Location l)
    {    //draw on map
        AbstractMapFragment m = (AbstractMapFragment) getFragmentByTag(FRAGMENTS.MAPFRAGMENT);
        if (m != null)
        {
            m.showGPSPosition();
        }

    }

    public void setFavorite(String uid, boolean remove) {
        //do nothing
    }

    public void goToDetail(final String uid) {
        //toggle and select list


        runOnUiThread(new Runnable() {
            public void run() {


                //1 push in navigation
                controller.pushDetail(uid);

                //render
                controller.refresh();


            }
        });


    }

    public void playRelatedAudio(String[] playList, Integer current) {
        Controller.getInstance().setPlayingAudio(playList, current);

        refreshAudioView();


        //render();


    }

    public void close() {
        runOnUiThread(new Runnable() {
            public void run() {
                //kill activity
                finish();
            }
        });
    }

    public void  switchView(int viewId)
    {

            controller.setCurrentView(viewId);
            controller.refresh();


    }

    public void switchGuide(String guideId)
    {
       // controller.getGmuContext().offlineMode = controller.allowOffline();
        //always start with predefined mode
        controller.getGmuContext().offlineMode = Controller.getInstance().getConfig().isDefaultMapModeOffline();

        LinkedHashMap<String,String> params=new LinkedHashMap<String,String>();
        params.put("uid",guideId);
        String name="none";
        try{name= Controller.getInstance().getDao().load(guideId).getTitle();}catch (Exception ign){}
        params.put("name",name);
        Tracker.getInstance().sendEvent(Controller.getInstance().getDao().getBaseGuideId(),"user_action", "open_guide", params);







    }

    /**END : GMUEventListener implementation**/


    /**
     * BEGIN: SearchView.OnQueryTextListener,OnCloseListener implementation*
     */
    private void closeSearch() {
        //close search if open
        if (searchView != null && searchView.isShown()) {
            searchItem.collapseActionView();

            searchView.setQuery("", false);       //clears your query without submit
            controller.getFilter().setTitle(null);

        }

    }

    public boolean onQueryTextChange(String newText)
    {   //filter  by name
        if (newText != null)
        {
            newText = newText.trim().toUpperCase();
            if(newText.length()>2||!Utils.isEmpty(controller.getFilter().getTitle()))
            {
                controller.getFilter().setTitle(newText);
                controller.refresh();
            }

        }


        return true;
    }

    public boolean onQueryTextSubmit(String query) {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }


    public void onCollapse() {

        if (controller.getFilter() != null && !Utils.isEmpty(controller.getFilter().getTitle())) {   //clear  filter query
            controller.getFilter().setTitle(null);
            controller.refresh();
        }

    }


    private void initController()
    {   Controller.getInstance().setContext(this.getBaseContext());
        Controller.getInstance().setDelegate(this);
        controller= Controller.getInstance();



    }


    /**END: SearchView.OnQueryTextListener implementation**/
    private String getInitialToken()
    {

            //try to get initial token parameter

            SharedPreferences settings = getSharedPreferences("UserInfo", Context.MODE_PRIVATE);
            return settings.getString("initialtoken",null);



    }

    private String getDirectAccessUID()
    {
       Intent intent = getIntent();
       String link = intent.getDataString();
       String directAccessUID=null;
        if(link!=null)
        {
            Log.d(TAG,"Deal direct access link="+link);
            //1:check if gmu token link:  /*guides/access?token=gmut_{guideuid}
            int i=link.indexOf("token=");
            if(i!=-1)
            {
                directAccessUID=link.substring(i+6).trim();
                if(link.contains("/testguides/"))
                {
                    //set preproduction  indicator
                    directAccessUID=directAccessUID+ Constants.PREPRODUCTION_GUIDE_SUFFIX;
                }
            }


        }else
        {
            directAccessUID= getInitialToken();
        }
        if(Utils.equals(controller.getGmuContext().lastPrivateGuideAccessed,directAccessUID))
        {   //ignore private access for the same guide
            return  null;
        }
        //disable direct access in this session
        controller.getGmuContext().lastPrivateGuideAccessed=directAccessUID ;
        Log.d(TAG,"Direct token="+directAccessUID);
        return directAccessUID;
    }


    public void setAutoCenter(boolean center)
    {   //TODO ignore bug when menu is not available
        if(this.menu==null) return;
        //set autocenter off
        MenuItem menu=this.menu.findItem(R.id.menu_gps);
        if(center)
        {
            menu.setIcon(R.drawable.ic_action_location_center);
            AbstractMapFragment m1 = ((AbstractMapFragment) getFragmentByTag(FRAGMENTS.MAPFRAGMENT));

            if (m1 != null)
            {   m1.enableAutoCenterMode();

            }

        } else
        {
            menu.setIcon(R.drawable.ic_action_location_found);
        }




    }

    private void checkTheme()
    {
        int targetTheme= Controller.getInstance().getConfig().getTheme();
        PlaceElement baseGuide= Controller.getInstance().getDao().load(Controller.getInstance().getDao().getBaseGuideId());
        if(baseGuide!=null&&!Utils.isEmpty(baseGuide.getAttributes().get("android_theme")))
        {
            targetTheme= Utils.getStyleResource(baseGuide.getAttributes().get("android_theme"));
        }

        if(     (controller.getCurrentView()!= GmuEventListener.VIEW_MICROSITE
               )        &&theme!=targetTheme)
        {
            theme=targetTheme;
            setTheme(targetTheme);


            finish();
            Intent intent = getIntent();

            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        }

    }
    private final int REQUEST_CODE_ASK_PERMISSIONS_ST = 123;

    private void checkPermission() {

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {

        //    Toast.makeText(this, "This version is not Android 6 or later " + Build.VERSION.SDK_INT, Toast.LENGTH_LONG).show();

        } else {

            int hasLocationPermission = checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION);

            int hasStoragePermission = checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE);

            if (hasLocationPermission != PackageManager.PERMISSION_GRANTED)
            {





                locationEnabled=false;


            }else if (hasLocationPermission == PackageManager.PERMISSION_GRANTED){

               // Toast.makeText(this, "The permissions are already granted ", Toast.LENGTH_LONG).show();

                locationEnabled=true;
            }

            if (hasStoragePermission != PackageManager.PERMISSION_GRANTED)
            {



                //Toast.makeText(this, "No storage permissions", Toast.LENGTH_LONG).show();


                requestPermissions(new String[] {android.Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        REQUEST_CODE_ASK_PERMISSIONS_ST);

                locationEnabled=false;


            }else if (hasStoragePermission == PackageManager.PERMISSION_GRANTED){

              //  Toast.makeText(this, "The permissions are already granted ", Toast.LENGTH_LONG).show();


            }



        }

        return;
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if(REQUEST_CODE_ASK_PERMISSIONS_ST == requestCode) {
            if (grantResults.length>0&&grantResults[0] == PackageManager.PERMISSION_GRANTED) {
               // Toast.makeText(this, "OK Permissions granted ! " + Build.VERSION.SDK_INT, Toast.LENGTH_LONG).show();

            } else {
               // Toast.makeText(this, "Permissions are not granted ! " + Build.VERSION.SDK_INT, Toast.LENGTH_LONG).show();
                Controller.getInstance().close();

            }
        }else{
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }


    protected void disableFileExposureCheck()
    {
        if(Build.VERSION.SDK_INT>=24){
            try{
                Method m = StrictMode.class.getMethod("disableDeathOnFileUriExposure");
                m.invoke(null);
            }catch(Exception e){
                e.printStackTrace();
            }
        }

    }


}
