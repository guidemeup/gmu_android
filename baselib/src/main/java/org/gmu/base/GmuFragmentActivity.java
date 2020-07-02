package org.gmu.base;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Bundle;

import android.util.Log;
import android.util.Pair;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.Surface;
import android.view.View;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;


import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;



import org.gmu.config.Constants;
import org.gmu.context.GmuContext;
import org.gmu.control.Controller;
import org.gmu.dao.IPlaceElementDAO;
import org.gmu.fragments.GuideListFragment;
import org.gmu.fragments.MicroSiteFragment;
import org.gmu.fragments.PlacesListFragment;
import org.gmu.fragments.maps.GoogleMapFragment;
import org.gmu.fragments.maps.MapsForgeMapFragment;
import org.gmu.fragments.placedetails.AbstractPlaceDetailFragment;
import org.gmu.fragments.placedetails.GenericPlaceDetailFragment;
import org.gmu.fragments.placedetails.MultiplePlaceDetailFragment;
import org.gmu.fragments.placedetails.MultiplePlaceSliderDetailFragment;
import org.gmu.fragments.placedetails.ParentPlaceDetailFragment;
import org.gmu.pojo.PlaceElement;
import org.gmu.utils.NetUtils;
import org.gmu.utils.Utils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import me.piruin.quickaction.ActionItem;
import me.piruin.quickaction.QuickAction;

/**
 * User: ttg
 * Date: 18/01/13
 * Time: 11:54
 * implements fragment management
 */
public class GmuFragmentActivity extends AppCompatActivity implements ActionBar.OnNavigationListener {
    protected Controller controller = Controller.getInstance();

    protected static enum STRATEGIES {REMOVE, DETTACH}

    protected static STRATEGIES CURRENTSTRATEGY = STRATEGIES.REMOVE;

    protected Menu menu;

    public static enum FRAGMENTS {MAPFRAGMENT, MICROSITE_FRAGMENT, LIST_FRAGMENT, GUIDE_LIST_FRAGMENT, DETAIL_FRAGMENT}

    protected Fragment currentFragment;
    private static final String TAG = GmuFragmentActivity.class.getName();


    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);



        createItemQuickAction();
    }


    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event)
    {
        if (keyCode == KeyEvent.KEYCODE_MENU&&controller.getCurrentView() == Controller.VIEW_MICROSITE)
        {
            //ignore menu click
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    public void onFragmentResumed(Fragment f) {
        Log.d(TAG," Fragment resumed="+controller.getCurrentView());
        refreshMenu();
    }
    public boolean onPrepareOptionsMenu (Menu menu)
    {       Log.d(TAG,"prepare options invoked");
           refreshMenu();
           return true;
    }

    protected void  refreshMenu()
    {    //workaround to refresh menu on resume

        try {
            if(menu==null) return;



            Log.d(TAG,"Refresh menu view="+controller.getCurrentView());

            //switch properly menu options
            if (controller.getCurrentView() == Controller.VIEW_MAP)
            {

                menu.setGroupVisible(R.id.listmenu, false);
                menu.setGroupVisible(R.id.mapmenu, true);
                menu.setGroupVisible(R.id.detail_menu, false);
                menu.setGroupVisible(R.id.filters, !controller.getDao().getBaseGuideId().equals(Controller.getInstance().getConfig().getRootId()));
                menu.findItem(R.id.menuitem_theme).setVisible(false);

                //only show refresh if elements are rt
                menu.findItem(R.id.menu_refresh_map).setVisible(getFilterRTTs(Controller.getInstance().getGroupFilter())!=null);



                ActionBar actionBar = this.getSupportActionBar();
                if (!actionBar.isShowing()) {
                    actionBar.show();
                }

            } else if (controller.getCurrentView() == Controller.VIEW_GUIDE_LIST)
            {
                menu.setGroupVisible(R.id.listmenu, true);
                menu.setGroupVisible(R.id.mapmenu, false);
                menu.setGroupVisible(R.id.detail_menu, false);
                menu.setGroupVisible(R.id.filters, false);
                menu.findItem(R.id.menuitem_theme).setVisible(false);

                //hide search if downloaded mode
                menu.findItem(R.id.menu_search).setVisible(!Controller.getInstance().isOnlyDownloadedGuidesView());


                ActionBar actionBar = this.getSupportActionBar();
                if (!actionBar.isShowing()) {
                    actionBar.show();
                }


                // hideMenus();

            } else if (controller.getCurrentView() == Controller.VIEW_LIST) {
                menu.setGroupVisible(R.id.listmenu, true);
                menu.setGroupVisible(R.id.mapmenu, false);
                menu.setGroupVisible(R.id.detail_menu, false);
                menu.setGroupVisible(R.id.filters, true);
                menu.findItem(R.id.menuitem_theme).setVisible(false);


                ActionBar actionBar = this.getSupportActionBar();
                if (!actionBar.isShowing()) {
                    actionBar.show();
                }

            } else if (controller.getCurrentView() == Controller.VIEW_DETAIL)
            {
                dealDetailPageRefresh();


            } else if (controller.getCurrentView() == Controller.VIEW_MICROSITE) {


                 this.getSupportActionBar().hide();


            }

            if(!Controller.getInstance().getConfig().showMap())
            {   //app without map (disable)
                menu.findItem(R.id.menu_switchtomap).setVisible(false);

            }


        } catch (Exception ign) {
            //ignore exceptions (menu not rendered)
            Log.w("Error dealing menu", ign);
        }
    }



    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        //save context
        Controller.getInstance().getGmuContext().serializeToBundle(savedInstanceState);
        //Log.d("EOO","Context saved="+ Controller.getInstance().getGmuContext());
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState)
    {

        GmuContext con = GmuContext.deserializeFromBundle(savedInstanceState);
        //set context in controller
        if (con != null)
        {
            Controller.getInstance().setGmuContext(con);
            //Log.d("EOO","Context restored:"+ con);

        } 
        super.onRestoreInstanceState(savedInstanceState);



    }

    @Override
    protected void onDestroy() {
        closeLoadDialog();
        super.onDestroy();
    }

    @Override
    public void finish() {
        closeLoadDialog();
        super.finish();
    }

    public void dealDetailPageRefresh() {
        Log.d(TAG,"Detail page refresh, menu="+menu);
        if (menu != null)
        {

            menu.setGroupVisible(R.id.listmenu, false);
            menu.setGroupVisible(R.id.mapmenu, false);


            //show center actions over selected child
            PlaceElement elem = null;
            if (controller.getSelectedChild() != null) {
                elem = controller.getDao().load(controller.getSelectedChild());
            } else {
                elem = controller.getDao().load(controller.getSelectedUID());
            }


            menu.setGroupVisible(R.id.detail_menu, elem != null && !Utils.isEmpty(elem.getPointWKT()));
            //only allow directions to not indoor locations
            menu.findItem(R.id.menu_directions).setVisible(elem != null && (!Utils.isEmpty(elem.getPointWKT()))
                    && (Utils.isEmpty(elem.getAttributes().get("indoor_map")))
            );

            menu.findItem(R.id.menu_share).setVisible(elem != null &&
                     (!Utils.isEmpty(elem.getAttributes().get("url"))
                     )
            );
            refreshNavigationBar();

            menu.setGroupVisible(R.id.filters, false);
            menu.findItem(R.id.menuitem_theme).setVisible(false);

            //only show refresh is element is rt
            menu.findItem(R.id.menu_refresh).setVisible(!controller.getDetailedElement().getType().equals(PlaceElement.TYPE_GUIDE)&&getFilterRTTs(controller.getDetailedElement().getUid())!=null);

            ActionBar actionBar = this.getSupportActionBar();
            if (!actionBar.isShowing()) {
                actionBar.show();
            }

        }


    }

    protected Fragment getFragmentByTag(FRAGMENTS f) {

        return getSupportFragmentManager().findFragmentByTag(f.name());


    }

    protected Fragment switchFragment(final FRAGMENTS targetFrament) {
        Log.d("EOOO", "Switch to " + targetFrament);
        this.getWindow().getDecorView().setEnabled(false);
        this.setSupportProgressBarIndeterminateVisibility(true);

        openLoadDialog();

        Fragment fr = switchFragment2(targetFrament);

        closeLoadDialog();
        this.setSupportProgressBarIndeterminateVisibility(false);
        this.getWindow().getDecorView().setEnabled(true);
        Log.d("EOOO", "Switch ends " + targetFrament);
        return fr;

    }


    protected synchronized Fragment switchFragment2(FRAGMENTS targetFrament) {

        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction tr = fragmentManager.beginTransaction();

        Fragment g = null;


        //1: remove others
        FRAGMENTS[] fr = FRAGMENTS.values();
        for (int i = 0; i < fr.length; i++) {
            FRAGMENTS toRemoveFragment = fr[i];
            if (targetFrament == null || !targetFrament.equals(toRemoveFragment)) {
                Fragment t = getFragmentByTag(toRemoveFragment);
                if (t != null) {
                    if (CURRENTSTRATEGY.equals(STRATEGIES.REMOVE)) {
                        Log.d("EO", "Remove fragment=" + toRemoveFragment);
                        tr.remove(t);
                    } else if (CURRENTSTRATEGY.equals(STRATEGIES.DETTACH)) {
                        tr.detach(t);
                    }
                }
            }

        }
        if (targetFrament != null) {
            g = getFragmentByTag(targetFrament);
            Log.d("EO", "Found fragment?=" + g);
            //1.2 check validity
            if (!validFragment(g, targetFrament)) {
                tr.remove(g);
                g = null;
            }
            //2:add fragment
            if (g == null) {   //create fragment
                g = getFragmentInstance(targetFrament);
                Log.d("EOO", "Created fragment=" + targetFrament);
                tr.add(R.id.basecontent, g, targetFrament.name());
            }
            refreshData(g);

            if (g.isDetached())
            {
                if (CURRENTSTRATEGY.equals(STRATEGIES.REMOVE))
                {

                    tr.add(R.id.basecontent, g, targetFrament.name());
                } else if (CURRENTSTRATEGY.equals(STRATEGIES.DETTACH)) {
                    tr.attach(g);
                }
            } else
            {   //resume to force refresh
                if (g.isVisible()) {
                    Log.d("EOO", "Doing resume=" + targetFrament);
                    g.onResume();
                }

            }
        }
         //tr.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
       // tr.setCustomAnimations(R.anim.enter, R.anim.exit, R.anim.popenter, R.anim.popexit);

      /*tr.setCustomAnimations(
                R.animator.card_flip_right_in, R.animator.card_flip_right_out,
                R.animator.card_flip_left_in, R.animator.card_flip_left_out);*/

        tr.commitAllowingStateLoss();
        //force immediate execution
        fragmentManager.executePendingTransactions();
        currentFragment = g;

        Log.d("EO", "Found fragment AFETER?=" + getFragmentByTag(targetFrament));

        return g;

    }

    private void refreshData(Fragment fr) {
        if (AbstractPlaceDetailFragment.class.isAssignableFrom(fr.getClass())) {
            ((AbstractPlaceDetailFragment) fr).setUID(controller.getSelectedUID());
        }


        //            //TODO: hacer mejor
//            if(g instanceof MultiplePlaceDetailFragment )
//            {
//                //set uids to show
//                setUIDS((MultiplePlaceDetailFragment)g);
//            }
    }


    protected void hideMenus() {
        if(menu==null) return;



        menu.setGroupVisible(R.id.listmenu, false);
        menu.setGroupVisible(R.id.mapmenu, false);
        menu.setGroupVisible(R.id.detail_menu, false);
        menu.setGroupVisible(R.id.filters, false);
        menu.findItem(R.id.menuitem_theme).setVisible(false);
        //hide all items
//        for(int i=0;i<menu.size();i++)
//        {
//            menu.getItem(i).setVisible(false);
//        }


    }

    public Fragment getFragmentInstance(FRAGMENTS fragment) {
        Fragment ret = null;


        if (fragment.equals(FRAGMENTS.MAPFRAGMENT)) {
            Class f = getValidMapClass();

            try {
                ret = (Fragment) f.newInstance();
            } catch (Exception ign) {
            }


        } else if (fragment.equals(FRAGMENTS.LIST_FRAGMENT)) {

            ret = new PlacesListFragment();


        } else if (fragment.equals(FRAGMENTS.MICROSITE_FRAGMENT)) {

            ret = new MicroSiteFragment();


        } else if (fragment.equals(FRAGMENTS.GUIDE_LIST_FRAGMENT)) {

            ret = new GuideListFragment();


        } else if (fragment.equals(FRAGMENTS.DETAIL_FRAGMENT))
        {
            PlaceElement selected=controller.getDetailedElement();

            if (controller.getDao().getBaseGuideId().equals(Controller.getInstance().getConfig().getRootId()))
            {  //guide main page: show without slider
                ret = new ParentPlaceDetailFragment();
            } else
            {
              String guideLayout= Controller.getInstance().getDao().load(Controller.getInstance().getDao().getBaseGuideId()).getAttributes().get("layout");
               // guideLayout=Constants.LAYOUT_PLACE_SLIDER;
               if(
                       (!Utils.isEmpty(guideLayout)&&guideLayout.equals(Constants.LAYOUT_PLACE_SLIDER))
                        &&(!selected.getType().equals(PlaceElement.TYPE_ROUTE))
                        &&(!selected.getType().equals(PlaceElement.TYPE_MULTIMEDIA))
                       )
               {   //slide between places in the same group filter
                   ret = new MultiplePlaceSliderDetailFragment();
               } else
               {   //slide between place children and related places and multimedia
                   ret = new MultiplePlaceDetailFragment();
               }
               ((MultiplePlaceDetailFragment) ret).setType(GenericPlaceDetailFragment.class);
            }

        }


        return ret;
    }

//    private void setUIDS(MultiplePlaceDetailFragment ret)
//    { String UID=controller.getSelectedUID();
//
//        List<PlaceElement> relatedObjects= Controller.getInstance().getDao().getRelatedPlaceElements(UID, IPlaceElementDAO.PARENT_AND_LINKS_RELATIONS,IPlaceElementDAO.PLACE_RELATED_ITEMS);
//        relatedObjects.add(0,Controller.getInstance().getDao().load(UID));
//        ((MultiplePlaceDetailFragment)ret).setUIDs(relatedObjects);
//
//    }

    private boolean validFragment(Fragment g, FRAGMENTS fragment) {
        if(g==null) return true;
        if( MultiplePlaceDetailFragment.class.isAssignableFrom(g.getClass()))
        {   //TODO: lo damos por no valido ya que el gesto se queda al rotar de lugar a otro y desplazaba el elemento inicial
            return false;
        }

        if (fragment.equals(FRAGMENTS.MAPFRAGMENT)) {
            Class f = getValidMapClass();
            if (!(g.getClass().equals(f))) {

                return false;
            }


        }
        return true;
    }

    /**
     * Rules to chose map engine
     *
     * @return
     */
    private Class getValidMapClass() {

            return MapsForgeMapFragment.class;



    }

    /**
     * BEGIN:  navigation action bar logic*
     */
    private String[][] mLocations = null;

    private boolean displayTitle() {
        return (!controller.getDao().getBaseGuideId().equals(Controller.getInstance().getConfig().getRootId()) ||
                controller.getCurrentView() == Controller.VIEW_DETAIL
                ||
                (Controller.getInstance().isOnlyDownloadedGuidesView()));


    }


    public void refreshNavigationBar() {
        PlaceElement group = null;
        if (!Utils.isEmpty(controller.getGroupFilter())) {
            group = controller.getDao().load(controller.getGroupFilter());
        }
        List<PlaceElement> elems = new ArrayList<PlaceElement>();
        //get current group list
        String filter = controller.getPreviousSelectedItem();
        String parentPlaceName = "Todos";
        if (!displayTitle()) {    //check if groups are defined
            elems = Controller.getInstance().getDao().list(null,
                    filter,
                    IPlaceElementDAO.VISIBLEONLIST_ITEMS, 1, Controller.getInstance().getDistanceOrderDefinition());


            //filter elements
            for (int i = elems.size() - 1; i != -1; i--) {
                PlaceElement item = elems.get(i);

                if ((controller.getDao().getBaseGuideId().equals(Controller.getInstance().getConfig().getRootId())) &&
                        (item.getType().equals(PlaceElement.TYPE_GUIDE))) {     //in the store guides are not groups

                    elems.remove(i);

                } else if (!Controller.getInstance().getDao().isParentPlaceElement(item)) {//remove non parent elements
                    elems.remove(i);
                } else if (Utils.equals(filter, item.getUid())) {   //remove current selection (result list always starts with filter uid)
                    parentPlaceName = (item.getTitle() + " (todo)");
                    elems.remove(i);
                }
            }
        }


        if (displayTitle() || elems.size() == 0) {
            getSupportActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
            getSupportActionBar().setDisplayShowTitleEnabled(true);
            if (group == null) {
                getSupportActionBar().setTitle(Controller.getInstance().getConfig().getRootName());
            } else {

                getSupportActionBar().setTitle(group.getTitle());
            }

            PlaceElement eleme = controller.getDao().load(controller.getSelectedUID());
            if (eleme != null && !Utils.equals(eleme, group)) {
                //show detail element as subtitle
                getSupportActionBar().setSubtitle(eleme.getTitle());
            } else {
                getSupportActionBar().setSubtitle(null);
            }
            //set title font
            String[] ids_toCostumize = new String[]{"action_bar_subtitle", "action_bar_title"};

            for (int i = 0; i < ids_toCostumize.length; i++) {
                String s = ids_toCostumize[i];
                final int titleId =
                        Resources.getSystem().getIdentifier(s, "id", "android");
                TextView title = (TextView) getWindow().findViewById(titleId);
                controller.setFont(title, null, null);
            }


        } else {


            getSupportActionBar().setDisplayShowTitleEnabled(false);

            mLocations = new String[2][elems.size() + 1];
            mLocations[0][0] = parentPlaceName;
            mLocations[1][0] = filter;
            int selected = 0;
            for (int i = 0; i < elems.size(); i++) {
                PlaceElement placeElement = elems.get(i);
                mLocations[0][i + 1] = placeElement.getTitle();
                mLocations[1][i + 1] = placeElement.getUid();
                if (Utils.equals(controller.getGroupFilter(), placeElement.getUid())) {
                    selected = i + 1;
                }

            }

            //set selected

            //TODO: ver que pasa
             Context context = getSupportActionBar().getThemedContext();
            ArrayAdapter<CharSequence> list = new ArrayAdapter<CharSequence>(context, R.layout.support_simple_spinner_dropdown_item, mLocations[0]);

            list.setDropDownViewResource(R.layout.support_simple_spinner_dropdown_item);

            getSupportActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
            getSupportActionBar().setListNavigationCallbacks(list, this);
            getSupportActionBar().setSelectedNavigationItem(selected);

        }   //if VIEW!=DETAIL
    }


    public boolean onNavigationItemSelected(int itemPosition, long itemId) {
        String uid = mLocations[1][itemPosition];
        //Toast.makeText(this.getApplicationContext(),"Selected "+mLocations[0][itemPosition]+"- uid="+uid, Toast.LENGTH_SHORT).show();
        if (Utils.equals(uid, controller.getGroupFilter())) {
            return true;

        }
        if (Utils.equals(controller.getDao().getBaseGuideId(), Controller.getInstance().getConfig().getRootId())) {  //store:   only one hierarchy level, switch group filter
            controller.setGroupFilter(uid);

        } else {
            if (itemPosition == 0) {    //if All item is selected --> pop navigation maintaining view
                int currentView = controller.getCurrentView();
                controller.popDetail();
                controller.setCurrentView(currentView);


            } else {
                if (Utils.equals(mLocations[1][0], controller.getGroupFilter())) {   //push detail on child  maintaining view
                    int currentView = controller.getCurrentView();
                    controller.pushDetail(uid, currentView);


                } else {   //switch group filter
                    controller.setGroupFilter(uid);

                }

            }
        }

        controller.refresh();


        return true;
    }
    /**END: navigation action bar logic**/

    /**
     * BEGIN: item quickaction options*
     */
    private QuickAction mQuickAction = null;
    private String quickActionUID;

    public void openItemActions(String uid, View v) {
        quickActionUID = uid;
        mQuickAction.show(v);
        mQuickAction.setAnimStyle(QuickAction.Animation.GROW_FROM_CENTER);
    }

    protected void createItemQuickAction() {
        ActionItem addItem = new ActionItem(1, "Remove", R.drawable.remove);


        //use setSticky(true) to disable QuickAction dialog being dismissed after an item is clicked
        //uploadItem.setSticky(true);

        mQuickAction = new QuickAction(this);

        mQuickAction.addActionItem(addItem);


        //setup the action item click listener
        mQuickAction.setOnActionItemClickListener(new QuickAction.OnActionItemClickListener() {

            @Override
            public void onItemClick(ActionItem actionItem) {

                if (actionItem.getActionId() == 1) {
                    openDeleteDialog();


                } else {
                    Toast.makeText(getApplicationContext(), actionItem.getTitle() + " selected", Toast.LENGTH_SHORT).show();
                }
            }


        });

        mQuickAction.setOnDismissListener(new QuickAction.OnDismissListener() {

            public void onDismiss() {

            }
        });
    }

    private void openDeleteDialog() {
        final Activity context = GmuFragmentActivity.this;
        //delete guide
        new AlertDialog.Builder(context)
                .setIcon(R.drawable.alert)
                .setTitle(R.string.delete_guide)
                .setPositiveButton(R.string.alert_dialog_ok,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {    //load dao and delete

                                controller.deleteGuide(quickActionUID);

                            }
                        }
                )
                .setNegativeButton(R.string.alert_dialog_cancel,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {

                            }
                        }
                )
                .create().show();
    }

    /**
     * END: item quickaction options*
     */
    private ProgressDialog loadDialog = null;
    private Thread tr = null;

    private synchronized void openLoadDialog() {
        closeLoadDialog();
        loadDialog = ProgressDialog.show(this,
                null,
                "Loading...",
                true, false);


        tr = new Thread(new Runnable() {
            public void run() {
                try {
                    Thread.sleep(10000);
                } catch (Exception e) {
                   // e.printStackTrace();
                }

                GmuFragmentActivity.this.closeLoadDialog();
            }
        });

        tr.start();

    }

    private synchronized void closeLoadDialog() {
        try {

            if (loadDialog != null) {
                loadDialog.dismiss();
            }
            if (tr != null) {
                tr.interrupt();
            }
            tr = null;
        } catch (Exception ign) {
            ign.printStackTrace();
        }
        loadDialog = null;

    }


    public int getDeviceDefaultOrientation() {

        WindowManager lWindowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

        Configuration cfg = getResources().getConfiguration();
        int lRotation = lWindowManager.getDefaultDisplay().getRotation();

        if ((((lRotation == Surface.ROTATION_0) || (lRotation == Surface.ROTATION_180)) &&
                (cfg.orientation == Configuration.ORIENTATION_LANDSCAPE)) ||
                (((lRotation == Surface.ROTATION_90) || (lRotation == Surface.ROTATION_270)) &&
                        (cfg.orientation == Configuration.ORIENTATION_PORTRAIT))) {

            return Configuration.ORIENTATION_LANDSCAPE;
        }

        return Configuration.ORIENTATION_PORTRAIT;
    }



    private static void setActivityOrientation(Activity activity, int preferenceOrientation) {
        if (preferenceOrientation == Configuration.ORIENTATION_LANDSCAPE) {
            if (activity.getRequestedOrientation() != ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
                // You need to check if your desired orientation isn't already set because setting orientation restarts your Activity which takes long
                activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            }
        } else if (preferenceOrientation == Configuration.ORIENTATION_PORTRAIT) {
            if (activity.getRequestedOrientation() != ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) {
                activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            }
        } else {
            if (activity.getRequestedOrientation() != ActivityInfo.SCREEN_ORIENTATION_SENSOR) {
                activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
            }
        }
    }

    /**
     * RealTime updater
     */
    private ProgressDialog updaterDialog;
    protected void updatePlace(PlaceElement place)
    {   updaterDialog =  ProgressDialog.show(this,
            null,
            getString(R.string.updating_rt)+"...",
            true, false);




        new PlaceUpdaterTask().execute(place);

    }

    private class PlaceUpdaterTask   extends AsyncTask<PlaceElement, Integer,Integer> {
        int totalUpdated=0;
        int total=0;
        protected void onPreExecute() {
            updaterDialog.setProgress(0);
            updaterDialog.setMax(100);
            updaterDialog.show(); //Mostramos el diálogo antes de comenzar
        }

        protected Integer doInBackground(PlaceElement... places)
        {   List<PlaceElement> toSave=new LinkedList<PlaceElement>();
            try
            {
                long min_rt_ts=-1;
                for (int i2 = 0; i2 < places.length; i2++)
                {
                    PlaceElement input = places[i2];
                    long parent_rt_ts=-1;
                    try{parent_rt_ts=Long.parseLong(input.getAttributes().get("rt_ts"));}catch (Exception ign){}

                    String url= Controller.getInstance().getConfig().getBaseServerGuides()+ Constants.SOLR_QUERY;



                    List<Pair<String,String>> params=new LinkedList<Pair<String,String>>();

                    String slrFilter= Constants.SOLR_FILTER;
                    slrFilter=slrFilter.replace("{rt_ts}",""+(parent_rt_ts+1));

                    if(!input.getType().equals(PlaceElement.TYPE_PLACE))
                    { StringBuilder st=new StringBuilder();
                        List<PlaceElement> el= Controller.getInstance().getDao().list(Controller.getInstance().getFilter(), Controller.getInstance().getGroupFilter(), Controller.getInstance().getDao().visibleOnMapItems(), null);
                        if(el.isEmpty()) return 0;
                        //create or query

                        for (int i = 0; i < el.size(); i++) {
                            PlaceElement placeElement = el.get(i);
                            st.append(" "+placeElement.getUid());
                            total++;
                        }

                        slrFilter= slrFilter.replace("{uids}",st.toString());

                    }else
                    {
                        //filter by id
                        slrFilter= slrFilter.replace("{uids}",input.getUid());
                        total++;
                    }

                    params.add(new Pair<String,String>("q",
                            slrFilter.toString()));


                    JSONObject js=new JSONObject(new String(NetUtils.postUrl(url, params)));
                    JSONArray elements=js.getJSONObject("response").getJSONArray("docs");

                    for(int a=0;a<elements.length();a++)
                    {
                        JSONObject place=elements.optJSONObject(a);
                        //load place
                        PlaceElement p2Update=controller.getDao().load(""+place.get("id"));
                        if(p2Update==null) continue;

                        publishProgress((int) ((a / (float) elements.length()) * 100));
                        int i=0;
                        //check ts before write
                        long rt_ts=place.getLong("rt_ts");
                        long place_rt_ts=-1;
                        try{place_rt_ts=Long.parseLong(p2Update.getAttributes().get("rt_ts"));}catch (Exception ign){}
                        if(min_rt_ts==-1||min_rt_ts>rt_ts) {min_rt_ts=rt_ts;}
                        if(rt_ts<=place_rt_ts)
                        {   //no changes
                            continue;}
                        boolean mustStore=false;
                        for (i = 0; i < Constants.RT_VALUES.length; i++)
                        {
                            String rtValue = Constants.RT_VALUES[i];
                            String value=""+place.get(rtValue);
                            if((!Utils.equals(p2Update.getAttributes().get(rtValue),value))&&i!=0)
                            {   //something has changed (ignoring ts)--> update BDD
                                mustStore=true;
                            }
                            p2Update.getAttributes().put(rtValue, value);

                        }

                        //save

                        if(mustStore)
                        {   toSave.add(p2Update);


                            totalUpdated++;
                        }

                    }

                    //finally update ts
                    if(min_rt_ts!=-1)
                    {   input.getAttributes().put("rt_ts",""+min_rt_ts);
                        toSave.add(input);
                    }

                    controller.getDao().saveOrUpdate(toSave);


                }


            }catch(Exception ign)
            {
                ign.printStackTrace();
            }
            return total;
        }

        protected void onProgressUpdate (Integer... progress) {
            int p = Math.round(progress[0]);
            //updaterDialog.setProgress(p);
        }

        protected void onPostExecute(Integer totalPlaces) {
            updaterDialog.dismiss();
            if(totalUpdated>0)
            {    Toast.makeText(GmuFragmentActivity.this.getApplicationContext(), totalUpdated+"/"+total+" "+getString(R.string.updated_elements), Toast.LENGTH_LONG).show();
                //refresh!!
                controller.refresh();
            }else
            {

                Toast.makeText(GmuFragmentActivity.this.getApplicationContext(), totalUpdated+"/"+total+" "+getString(R.string.updated_elements), Toast.LENGTH_LONG).show();

            }

        }
    }

    protected Long getFilterRTTs(String uid)
    {
        PlaceElement pl=controller.getDao().load( uid);
        long place_rt_ts=-1;
        try{place_rt_ts=Long.parseLong(pl.getAttributes().get("rt_ts"));}catch (Exception ign){}
        if(place_rt_ts==-1) return null;
        return place_rt_ts;
    }



}
