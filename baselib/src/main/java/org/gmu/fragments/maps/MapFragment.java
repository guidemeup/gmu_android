package org.gmu.fragments.maps;


import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.widget.LinearLayout;


import org.gmu.base.R;
import org.gmu.control.Controller;
import org.gmu.dao.IPlaceElementDAO;
import org.gmu.fragments.AbstractMapFragment;
import org.gmu.pojo.PlaceElement;
import org.gmu.utils.Utils;
import org.gmu.webview.WebClient;

import java.util.List;


/**
 * Created by IntelliJ IDEA.
 * User: acasquero
 * Date: 9/11/12
 * Time: 8:53
 * To change this template use File | Settings | File Templates.
 */
public class MapFragment extends AbstractMapFragment {
    private static final String TAG = MapFragment.class.getName();

    private boolean offline = false;
    private boolean indoorMode = false;


    private WebView myWebView;


    private boolean pageLoaded = false;


    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        setRetainInstance(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {


        LinearLayout main = (LinearLayout) inflater.inflate(R.layout.fragment_mapview,
                container, false);

        super.addCommonView(main);

        myWebView = (WebView) main.findViewById(R.id.mapwebview);
        myWebView.getSettings().setJavaScriptEnabled(true);
        myWebView.getSettings().setLightTouchEnabled(false);
        //TODO: improves performance on Galaxy S?
        //myWebView.getSettings().setCacheMode( WebSettings.LOAD_NO_CACHE );
        //myWebView.getSettings().setBuiltInZoomControls(true);
        myWebView.setWebChromeClient(new WebChromeClient() {
            public void onConsoleMessage(String message, int lineNumber, String sourceID) {
                Log.d(TAG, message + " -- From line "
                        + lineNumber + " of "
                        + sourceID);
            }

        });

        myWebView.addJavascriptInterface(new GMUMapViewJavaScriptInterface(), "Android");
        //add multitouch in android 2.3
        WebClient wc = new WebClient(myWebView);
        openIndex();


        return main;
    }

    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);


    }


    public void openIndex() {
        pageLoaded = false;
        myWebView.loadUrl("file:///android_asset/www/gmumap.html");
        //wait and set map
        wait4MapReady();


        String call = "javascript:init()";

        myWebView.loadUrl(call);
        createLayers();
        pageLoaded = false;


    }


    private void addPlaceElements(List<PlaceElement> elems) {

        String call = "javascript:mobileMap.startMarkerAddTransaction()";
        myWebView.loadUrl(call);

        for (int i = 0; i < elems.size(); i++) {
            PlaceElement elem = elems.get(i);
            drawElement(elem, false, LAYERS.MARKERS, false);

        }
        call = "javascript:mobileMap.endMarkerAddTransaction()";
        myWebView.loadUrl(call);


    }


    protected void drawElement(PlaceElement elem, boolean drawGeometry, LAYERS targetLayer, boolean center) {

        if ((Utils.isEmpty(elem.getPointWKT()))
                || !Utils.equals(currentBaseMap, elem.getAttributes().get("indoor_map"))) {
            //only show elements in base map or with geometry
            return;
        }

        //draw marker
        String call = "javascript:mobileMap.addElement(" + elemToJSON(elem, "" + targetLayer, drawGeometry, center) + ")";
        myWebView.loadUrl(call);


    }


    public void show() {
        wait4MapReady();
        previousSelectedKey = null;
        addPlaceElements(Controller.getInstance().getDao().list(Controller.getInstance().getFilter(), Controller.getInstance().getGroupFilter(), IPlaceElementDAO.VISIBLEONLIST_ITEMS, Controller.getInstance().getDistanceOrderDefinition()));

        showSelection(true);

    }

    @Override
    protected void switchMap(String relativeMapPath) {
        String call;
        //show indoor map if needed
        if (!Utils.isEmpty(relativeMapPath)) {
            relativeMapPath = Controller.getInstance().getDao().getRelatedFileDescriptor(relativeMapPath);
            call = "javascript:mobileMap.setIndoorMap('" + relativeMapPath + "','" + relativeMapPath + "',2)";
            myWebView.loadUrl(call);
            indoorMode = false;
        } else {
            String baseTile = "undefined";
            if (offline) {
                relativeMapPath = Utils.getFilePath(Utils.getFilePath(Controller.getInstance().getDao().getOfflineMapData().mapPath));

                baseTile = "'" + relativeMapPath + "'";
            }
            call = "javascript:mobileMap.backToBaseMap(" + baseTile + ")";
            myWebView.loadUrl(call);

            indoorMode = false;

        }
    }

    @Override
    protected void zoomIn() {
        String call = "javascript:mobileMap.zoomIn()";
        myWebView.loadUrl(call);

    }

    @Override
    protected void zoomOut() {
        String call = "javascript:mobileMap.zoomOut()";
        myWebView.loadUrl(call);
    }

    protected void clearLayer(LAYERS layer) {
        myWebView.loadUrl("javascript:mobileMap.clearLayerByUID('" + layer + "')");
    }

    @Override
    protected void refreshView() {
        //do nothing
    }


    private void switchOffline(boolean offline) {
        if (this.offline == offline) return;
        this.offline = offline;

        if (!indoorMode) {   //change map to base
            this.switchMap(null);
        }
    }

    private String elemToJSON(PlaceElement elem, String layerUID, boolean drawGeometry, boolean center) {
        return "{wkt:'" + elem.getPointWKT() + "'," +
                "uid:'" + elem.getUid() + "',iconuri:'" + Controller.getInstance().getDao().getCategoryIcon(elem) + "'," +
                "targetLayerUID:'" + layerUID + "'," +
                "draw:" + drawGeometry + ",center:" + center + "}";

    }


    private class GMUMapViewJavaScriptInterface {
        public void selectUid(final String uid) {
            if (!Utils.equals(uid, POINTERUID)) {
                MapFragment.this.selectUid(uid, false);
            }

        }

        public void pageLoaded() {
            pageLoaded = true;
        }

    }

    /**
     * Waits until map is ready (TODO: workaround, solucionar!!)
     */
    private void wait4MapReady() {
        long start = System.currentTimeMillis();
        while (!pageLoaded) {
            try {
                if ((System.currentTimeMillis() - start) > 30000) {
                    throw new RuntimeException("Error loading map!!");
                }
                Thread.sleep(10);

            } catch (InterruptedException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }

        }


    }


    private void createLayers() {

        myWebView.loadUrl("javascript:mobileMap.clearAllLayers()");


        myWebView.loadUrl("javascript:mobileMap.addLayer('" + LAYERS.LINES + "',{})");
        myWebView.loadUrl("javascript:mobileMap.addLayer('" + LAYERS.MARKERS + "',{callback:Android})");
        myWebView.loadUrl("javascript:mobileMap.addLayer('" + LAYERS.CHILDREN + "',{callback:Android})");
        myWebView.loadUrl("javascript:mobileMap.addLayer('" + LAYERS.SELECTEDPLACES + "',{callback:Android})");


    }

    protected void doInitialCenter() {
        //TODO: implement!!

    }


}
