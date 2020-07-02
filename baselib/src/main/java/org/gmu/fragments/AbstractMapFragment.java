package org.gmu.fragments;


import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;

import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.*;

import org.gmu.config.Constants;
import org.gmu.base.R;
import org.gmu.adapters.place.PlaceAdapter;

import org.gmu.control.Controller;
import org.gmu.dao.IPlaceElementDAO;
import org.gmu.dao.impl.AbstractPlaceElementDAO;
import org.gmu.map.MapPosition;
import org.gmu.pojo.DirectAccess;
import org.gmu.pojo.PlaceElement;
import org.gmu.utils.MapUtils;
import org.gmu.utils.Utils;
import org.gmu.utils.gis.wkt.SimpleGISBuffer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * User: ttg
 * Date: 13/12/12
 * Time: 16:16
 * To change this template use File | Settings | File Templates.
 */
public abstract class AbstractMapFragment extends GMUBaseFragment
{

    private static final boolean LOAD_ELEMENTS_AFTER_SHOW=true;
    private static final int MINIDETAILVIEWFIXEDVISIBILITY = View.INVISIBLE;
    protected static final byte DEFAULT_ZOOM = 17;
    private static final String TAG = AbstractMapFragment.class.getName();
    private boolean connected = true;
    private boolean centerOnChild = true;
    private boolean autoCenterMode=false;


    protected static final String POINTERUID = "GMUPointerUID";

    protected static String fixedMap = null;

    protected String currentSelectedChild = null;
    protected String currentSelectedElement = null;


    protected View miniDetailView;
    protected String previousSelectedKey = null;
    protected String currentBaseMap = null;
    protected long lastTouchMs=0;

    private HashMap childrenParents=new HashMap();
    protected boolean placesLoaded=false;

    //layer list ordered by priority
    protected static enum LAYERS {
        GPS, SELECTEDPLACES, MARKERS, CHILDREN, LINES,MARKERSGEOMETRY

    }

    private ZoomButton switchonline = null;



    protected abstract void doInitialCenter();

    protected abstract void show();

    protected abstract void switchMap(String relativeMapPath);

    protected abstract void zoomIn();

    protected abstract void zoomOut();

    protected abstract void drawElement(PlaceElement elem, boolean drawGeometry, LAYERS targetLayer, boolean center);

    protected abstract void clearLayer(LAYERS layer);

    protected abstract void refreshView();

    protected void updateCenterIncontext() {
    }

    protected SimpleGISBuffer gisBuffer = new SimpleGISBuffer();


    public interface MapEventListener
    {
        public void onDraw();

        public boolean onMapTap();

        public void onZoomLevelChanged();

        public void onTouchEvent();

    }


    public static void setFixedMap(String fixedMap) {
        AbstractMapFragment.fixedMap = fixedMap;
    }

    public void showGPSPosition()
    {   Location loc=Controller.getInstance().getGmuContext().lastUserLocation ;
        if(loc!=null)
        {

            if((!autoCenterMode)&& Controller.getInstance().getConfig().getInactiveCenterThresholdSecs()!=null)
            {
                //center if threshold was exceeded
                long inactiveTimesecs=(System.currentTimeMillis()-lastTouchMs)/1000;
                if(inactiveTimesecs > Controller.getInstance().getConfig().getInactiveCenterThresholdSecs())
                {
                   //callback
                    Controller.getInstance().setAutoCenter(true);
                   return;
                }

            }

            PlaceElement pointerplace = new PlaceElement();
            pointerplace.setPointWKT(MapUtils.location2WKT(loc));
            pointerplace.setUid("GMUGPS");
            pointerplace.setCategory("GPS");
            this.clearLayer(LAYERS.GPS);
            this.drawElement(pointerplace, false, LAYERS.GPS, autoCenterMode);
        }





    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        currentSelectedChild = null;
        currentSelectedElement = null;
        //disable autocenter on creation
        this.disableAutoCenterMode();

    }

    public void onResume() {
        super.onResume();

        final ConnectivityManager cm = (ConnectivityManager) this.getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
        final NetworkInfo networkInfo = cm.getActiveNetworkInfo();
        connected = (networkInfo != null && networkInfo.isConnected());
        if (!connected && !Controller.getInstance().getGmuContext().offlineMode) {   //No connection: auto switch to offline
            if (Controller.getInstance().allowOffline()) {
                Controller.getInstance().getGmuContext().offlineMode = true;
                showMapModeMsg();

            }

        }

        //crear buffer
        gisBuffer = new SimpleGISBuffer();
        Log.i(TAG, "Before Show");
        //start on Thread to show map  while places are loading
        //doInitialCenter();

        if(LOAD_ELEMENTS_AFTER_SHOW)
        {

            showLoadingDialog();


            Thread thread = new Thread(){
                @Override
                public void run() {
                    try {
                        synchronized (this) {
                            wait(500);
                        }
                            AbstractMapFragment.this.getActivity().runOnUiThread(new Runnable()
                            {
                                public void run() {
                                    try {
                                        long start=System.currentTimeMillis();
                                        show();
                                        refreshOnlineButtonMode();
                                        Log.i(TAG,"Show done  in="+(System.currentTimeMillis()-start)+"ms");
                                        placesLoaded=true;
                                    }catch (Exception ign)
                                    {
                                        Log.e(TAG,"Error on show",ign);
                                    }

                                }
                            });




                    } catch (Exception e)
                    {
                        //ignore (in fragments without activity)
                        Log.w(TAG,"Error on load after show ",e);
                       // e.printStackTrace();
                    }
                    dismissLoadingDialog();
                };
            };

            thread.start();
        }else
        {

            show();
            refreshOnlineButtonMode();

        }





        Log.i(TAG,"After Show");


    }

    protected OnZoomChangeListener onZoomChangeListener = null;


    protected void selectElement(PlaceElement elem, boolean center, LAYERS layer) {

        DirectAccess pointerplace = new DirectAccess();
        pointerplace.setDelegate(elem);

        pointerplace.setUid(POINTERUID);
        pointerplace.setCategory("infowindow");

        pointerplace.getAttributes().put("indoor_map", currentBaseMap);

        pointerplace.setPointWKT(elem.getPointWKT());
        this.clearLayer(LAYERS.SELECTEDPLACES);
        this.drawElement(pointerplace, false, LAYERS.SELECTEDPLACES, center);


    }


    protected void setBaseMap(String baseMap) {
        String mapPath = baseMap;








        if (!Utils.equals(baseMap, currentBaseMap)) {

            currentBaseMap = baseMap;

            if (!Utils.isEmpty(fixedMap)) {  //ignore and always fix map

                mapPath = fixedMap;

            }

            switchMap(mapPath);

            this.show();
        }

    }


    protected void addCommonView(LinearLayout main) {
        Controller instance = Controller.getInstance();
        miniDetailView = main.findViewById(R.id.minidetailarea);
        miniDetailView.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Controller.getInstance().goToDetail(Controller.getInstance().getSelectedUID());
            }
        });

        final ZoomButton zoommIn = (ZoomButton) main.findViewById(R.id.zoomIn);
        zoommIn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                zoomIn();
            }
        });

        final ZoomButton zoommOut = (ZoomButton) main.findViewById(R.id.zoomOut);
        zoommOut.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                zoomOut();
            }
        });

        switchonline = (ZoomButton) main.findViewById(R.id.map_online);
        switchonline.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Controller.getInstance().getGmuContext().offlineMode = !Controller.getInstance().getGmuContext().offlineMode;
                showMapModeMsg();
                //store center
                updateCenterIncontext();
                Controller.getInstance().refresh();
            }
        });
        //create listener
        onZoomChangeListener = new OnZoomChangeListener() {
            public void onZoom(boolean canZoomIn, boolean canZoomOut) {
                zoommOut.setEnabled(canZoomOut);
                zoommIn.setEnabled(canZoomIn);

            }
        };
        currentBaseMap = null;
        IPlaceElementDAO dao = Controller.getInstance().getDao();
        //set fixed map path from guide attribs
        fixedMap = dao.getFixedMap();


    }


    protected void showSelectionOnMap(PlaceElement selected, PlaceElement selectedChild, boolean centerSelection) {


        if (!Utils.equals(previousSelectedKey, getSelectedKey())) {


            if(!Controller.getInstance().getConfig().allwaysShowSelectedRouteMode())
            {
                //clear selected and load selected children on selected layer
                clearLayer(LAYERS.LINES);
                clearLayer(LAYERS.CHILDREN);

            }



            boolean isRoute = selected.getType().equals(PlaceElement.TYPE_ROUTE);

            List<PlaceElement> relatedObjects = Controller.getInstance().getDao().getRelatedPlaceElements(selected.getUid(), IPlaceElementDAO.PARENT_AND_LINKS_RELATIONS, IPlaceElementDAO.PLACE_RELATED_ITEMS);

            int routeCounter=1;
            for (int i = 0; i < relatedObjects.size(); i++) {
                PlaceElement elem = relatedObjects.get(i);
                String initialCategory = elem.getCategory();
                if (isRoute)
                {   //temporally change category to number
                   if(!Utils.isEmpty(elem.getPointWKT()))
                   {   String o="00"+routeCounter;
                       o=o.substring(o.length()-2);
                       elem.setCategory(o);
                       routeCounter++;
                   };
                }
                childrenParents.put(elem.getUid(),selected.getUid());
                drawElement(elem, true, LAYERS.CHILDREN, false);
                elem.setCategory(initialCategory);

            }

            //draw parent line
            drawElement(selected, true, LAYERS.LINES, false);
        }

        if (selectedChild != null) {   //center on selected child
            selectElement(selectedChild, centerSelection && centerOnChild, LAYERS.CHILDREN);
        } else {
            selectElement(selected, centerSelection && centerOnChild, LAYERS.MARKERS);
        }
        //set to false to avoid center on child  on resume (ex: on base map switch)
        centerOnChild = false;
        previousSelectedKey = selected.getUid();
        previousSelectedKey = getSelectedKey();

    }


    protected void showSelection(boolean center)
    {

        //locate place to show in minidetail  and to select in map

        PlaceElement selected = Controller.getInstance().getDao().load(currentSelectedElement);
        PlaceElement selectedChild = Controller.getInstance().getDao().load(currentSelectedChild);
        //show element if is visible on map
        if (selected != null)

        {
            //show mini detail view
            PlaceElement showInMiniDetail = selected;
            if (selectedChild != null)
            {
                if (!selectedChild.getAttributes().get("mime").equals(PlaceElement.MIME_AUDIO))
                {
                    showInMiniDetail = selectedChild;
                }

                //set base Map
                setBaseMap(selectedChild.getAttributes().get("indoor_map"));

            } else
            {
                //set base Map
                setBaseMap(selected.getAttributes().get("indoor_map"));

            }

            PlaceAdapter.fillPlaceRow(miniDetailView, showInMiniDetail, AbstractMapFragment.this.getActivity().getBaseContext(), Controller.getInstance().getConfig().getShowDistance());

            //clear buffer (center on selection)
            gisBuffer = new SimpleGISBuffer();
            //show on map
            this.showSelectionOnMap(selected, selectedChild, center);
            refreshView();

        } else {

            miniDetailView.setVisibility(View.INVISIBLE);
            setBaseMap(null);
        }


    }

    private static long lastSelectTs = 0;
    protected boolean elementSelected(String elemId)
    {

        return Utils.equals(elemId,currentSelectedElement)||Utils.equals(elemId,currentSelectedChild);


    }
    protected void selectUid(final String uid, final boolean center) {

        System.err.println("select "+uid);
        AbstractMapFragment.this.getActivity().runOnUiThread(new Runnable() {
            public void run() {


                //start selection window ts (avoid  multiple selections in a single click)
                if (System.currentTimeMillis() - lastSelectTs < 100) {
                    //ignore
                    Log.d(TAG, "Ignoring uid=" + uid);
                    return;
                } else {
                    lastSelectTs = System.currentTimeMillis();
                }


                //if uid is infowindow id, go to detail!
                if (uid.equalsIgnoreCase(POINTERUID)) {   //select!
                    Controller.getInstance().setSelectedChild(currentSelectedChild);
                    Controller.getInstance().setSelectedUID(currentSelectedElement);
                    AbstractMapFragment.this.updateCenterIncontext();

                    Controller.getInstance().goToDetail(Controller.getInstance().getSelectedUID());
                    return;
                }


                final PlaceElement pl = Controller.getInstance().getDao().load(uid);
                if (pl == null) {

                    return;
                }


                if (Utils.contains(IPlaceElementDAO.PLACE_RELATED_ITEMS, pl.getType())) {

                    currentSelectedElement= (String) childrenParents.get(uid);
                    if (currentSelectedElement == null)
                    {
                        currentSelectedElement = Controller.getInstance().getSelectedUID();
                    }
                    currentSelectedChild = uid;
                    // Controller.getInstance().setSelectedChild(uid);
                    if (pl.getAttributes().get("mime").startsWith(PlaceElement.MIME_AUDIO)) {   //start audio playing

                        String[] playList = Controller.getInstance().getDao().getPlayList(Controller.getInstance().getSelectedUID());
                        //locate uid in playlist
                        int current = 0;
                        for (current = 0; current < playList.length; current++) {
                            if (playList[current].equals(uid)) {
                                break;
                            }
                        }
                        Controller.getInstance().playRelatedAudio(playList, current);

                    }


                } else {
                    currentSelectedChild = null;
                    currentSelectedElement = uid;
//                    Controller.getInstance().setSelectedChild(null);
//                    Controller.getInstance().setSelectedUID(uid);
                }


                miniDetailView.setVisibility(MINIDETAILVIEWFIXEDVISIBILITY);
                AbstractMapFragment.this.showSelection(center);

            }
        });
    }


    protected Bitmap getMarkerBitMap(PlaceElement elem)
    {
        Bitmap icon;
        if (elem.getCategory().equals("infowindow"))
        {


            //get template for infowindow

            LinearLayout infowindowTemplate = getInfoWindow(elem);

            icon = Bitmap.createBitmap(infowindowTemplate.getMeasuredWidth(), infowindowTemplate.getMeasuredHeight(), Bitmap.Config.ARGB_8888);



            Canvas canvas = new Canvas(icon);

            infowindowTemplate.draw(canvas);
            //scale to 0.75 size
            //  icon=Bitmap.createScaledBitmap(icon,(int) (icon.getWidth()*0.75),(int) ( icon.getHeight()*0.75), false);


        }else if(elem.getCategory().equals("GPS"))
        {   //apply rotation
            icon = Controller.getInstance().getDao().loadCategoryIcon(elem);
            Matrix mat = new Matrix();
            mat.postRotate(Controller.getInstance().getGmuContext().lastUserViewAngle);
            return Bitmap.createBitmap(icon, 0, 0,icon.getWidth(),icon.getHeight(), mat, true);


        }
        else
        {
            icon = Controller.getInstance().getDao().loadCategoryIcon(elem);
        }
        return icon;
    }

    protected LinearLayout getInfoWindow(PlaceElement elem)
    {
        LayoutInflater inflater = LayoutInflater.from(this.getActivity());
        //get template for infowindow

        LinearLayout infowindowTemplate =null;
        if(elem.getType().equalsIgnoreCase(PlaceElement.TYPE_GUIDE))
        {   infowindowTemplate =(LinearLayout) inflater.inflate(R.layout.infowindow_guide,
                null);
            if(!Utils.isEmpty(elem.getAttributes().get("rating")))
            {
                ((RatingBar)infowindowTemplate.findViewById(R.id.rating_bar)).setRating(Float.parseFloat(elem.getAttributes().get("rating")));
            }

            //update price
            TextView price= (TextView) infowindowTemplate.findViewById(R.id.price);
            if(Controller.getInstance().getConfig().getPurchaseManager()!=null)
            {
                if(Controller.getInstance().getDao().paymentNeeded(elem)&&
                        !Utils.isEmpty(elem.getAttributes().get("price")))
                {
                    price.setText(elem.getAttributes().get("price"));
                }
                else
                {
                    price.setText(" ");
                }
            }else
            {
                price.setText(R.string.free);
            }

        }else
        {
            infowindowTemplate =(LinearLayout) inflater.inflate(R.layout.infowindow_place,
                    null);
            TextView d = (TextView) infowindowTemplate.findViewById(R.id.distance);
            //update element distance
            List <PlaceElement> l=new ArrayList<PlaceElement>(); l.add(elem);
            Controller.getInstance().getDao().updateDistances(Controller.getInstance().getGmuContext().lastUserLocation, l);
            //show distance in not indoor elements
            if(Controller.getInstance().getConfig().getShowDistance()
                    &&Utils.isEmpty(elem.getAttributes().get("indoor_map")))
            {
                String distance =  Utils.formatDistance(elem.getDistanceToUser());

               if(!Utils.isEmpty(distance))
               {
                   d.setText(getString(R.string.distance_in).replace("#distance#",distance));
               }
                else
               {
                   d.setText("");
               }
            }else
            {
                d.setText("");
            }

        }


       // infowindowTemplate.layout(0, 0, Utils.convertDpToPixel(203,this.getActivity()), Utils.convertDpToPixel(90,this.getActivity()));
        TextView t = (TextView) infowindowTemplate.findViewById(R.id.title);
        t.setText(elem.getTitle());

        ImageView thumbnail = (ImageView) infowindowTemplate.findViewById(R.id.thumbnailimg);
        String mainImg=AbstractPlaceElementDAO.getMainImg(elem);
        if (!Utils.isEmpty(mainImg))
        {
            thumbnail.setImageBitmap(Controller.getInstance().getDao().loadImage(AbstractPlaceElementDAO.getMainImg(elem), Constants.PLACE_THUMBNAILDPI));

        } else {
            thumbnail.setImageResource(R.drawable.default_img);
        }




        infowindowTemplate.measure(0,0);

        infowindowTemplate.layout(0, 0, infowindowTemplate.getMeasuredWidth(), infowindowTemplate.getMeasuredHeight());



        return infowindowTemplate;

    }

    public static interface OnZoomChangeListener {
        public void onZoom(boolean canZoomIn, boolean canZoomOut);

    }

    protected MapPosition getInitialCenterPosition() {
        //center on last position
        MapPosition center =Controller.getInstance().getGmuContext().lastMapCenterLocation;
        if (center == null) {   //center on buffer center
            DisplayMetrics dm = new DisplayMetrics();
            this.getActivity().getWindowManager().getDefaultDisplay().getMetrics(dm);
            center = gisBuffer.getCenter(dm.widthPixels);
            if (center == null)
            {
                //center on guide
                PlaceElement guide = Controller.getInstance().getDao().load(Controller.getInstance().getDao().getBaseGuideId());

                center = new MapPosition();
                center.zoom = DEFAULT_ZOOM;
                if (guide != null && !Utils.isEmpty(guide.getPointWKT())) {
                    center.location =guide.getPointWKT();

                } else {   //center on gps position

                    center.location = MapUtils.location2WKT(Controller.getInstance().getGmuContext().lastUserLocation);
                }
            }else
            {   //limit minimum zoom
                // if(center.zoom>DEFAULT_ZOOM){center.zoom=DEFAULT_ZOOM;}

            }

        }
        return center;
    }


    private void refreshOnlineButtonMode() {


        if ((!connected) ||
                (!Controller.getInstance().allowOffline()) ||
                (!Utils.isEmpty(AbstractMapFragment.fixedMap)) ||
                (!Utils.isEmpty(currentBaseMap))
                ) {
            switchonline.setVisibility(View.GONE);
        } else if (Controller.getInstance().getGmuContext().offlineMode) {
            switchonline.setImageResource(R.drawable.btn_airplane);

        } else {
            switchonline.setImageResource(R.drawable.btn_airplane_off);
        }


    }

    private void showMapModeMsg() {
        String msg = null;
        if (Controller.getInstance().getGmuContext().offlineMode) {
            msg = getString(R.string.offline_mode_msg);
        } else {
            msg = getString(R.string.online_mode_msg);

        }
        Toast.makeText(AbstractMapFragment.this.getActivity(), msg, Toast.LENGTH_LONG).show();
    }


    public boolean onMapTap()
    {

        Log.i(TAG,"tap received!");
        //hide infowindow
        this.clearLayer(LAYERS.SELECTEDPLACES);

        return true;
    }
    public void onTouchEvent()
    {
        setLastTouchMs(System.currentTimeMillis());


    }
    public void onDraw() {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void enableAutoCenterMode()
    {
        this.autoCenterMode = true;

        this.showGPSPosition();


    }
    public void disableAutoCenterMode()
    {
        this.autoCenterMode = false;

        Controller.getInstance().setAutoCenter(autoCenterMode);

    }

    private void setLastTouchMs(long lastTouchMs) {
        this.lastTouchMs = lastTouchMs;
        this.disableAutoCenterMode();
    }
    private String getSelectedKey()
    {
        return this.currentSelectedElement+currentSelectedChild;
    }
}
