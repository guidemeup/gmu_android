package org.gmu.fragments.maps;


import android.content.res.Resources;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.*;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import org.gmu.base.R;
import org.gmu.config.Constants;
import org.gmu.control.Controller;

import org.gmu.fragments.AbstractMapFragment;


import org.gmu.map.mapsforge.GMUGroupLayer;
import org.gmu.map.mapsforge.GMUMapsForgeMapView;
import org.gmu.map.mapsforge.GMUMarker;
import org.gmu.pojo.OfflineMapDefinition;
import org.gmu.pojo.PlaceElement;
import org.gmu.utils.MapUtils;
import org.gmu.utils.Utils;
import org.mapsforge.core.graphics.Join;
import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.Point;
import org.mapsforge.map.android.graphics.AndroidBitmap;
import org.mapsforge.map.android.graphics.AndroidGraphicFactory;
import org.mapsforge.map.android.graphics.GMUPaint;
import org.mapsforge.map.android.util.AndroidUtil;
import org.mapsforge.map.android.view.MapView;
import org.mapsforge.map.layer.GroupLayer;

import org.mapsforge.map.layer.Layer;
import org.mapsforge.map.layer.cache.InMemoryTileCache;
import org.mapsforge.map.layer.cache.TileCache;
import org.mapsforge.map.layer.cache.TileStore;
import org.mapsforge.map.layer.cache.TwoLevelTileCache;
import org.mapsforge.map.layer.download.TileDownloadLayer;
import org.mapsforge.map.layer.download.tilesource.OnlineTileSource;
import org.mapsforge.map.layer.overlay.Marker;
import org.mapsforge.map.layer.overlay.Polyline;

import org.mapsforge.core.graphics.Bitmap;
import org.mapsforge.core.graphics.Paint;
import org.mapsforge.core.graphics.Style;
import org.mapsforge.map.layer.renderer.TileRendererLayer;
import org.mapsforge.map.layer.tilestore.TileStoreLayer;
import org.mapsforge.map.model.IMapViewPosition;
import org.mapsforge.map.reader.MapFile;
import org.mapsforge.map.rendertheme.InternalRenderTheme;


import java.io.File;
import java.util.*;

import static org.gmu.utils.MapUtils.OSM_BASE_TILES;


/**
 * Created by IntelliJ IDEA.
 * User: acasquero
 * Date: 9/11/12
 * Time: 8:53
 * To change this template use File | Settings | File Templates.
 */
public class MapsForgeMapFragment extends AbstractMapFragment implements AbstractMapFragment.MapEventListener {

    private static final byte MAX_MAP_ZOOM=19;

    private static final String TAG = MapsForgeMapFragment.class.getName();


    private static Hashtable<LAYERS, Object> layersImpl;
    private static Hashtable<LAYERS, List<Marker>> items;

    private static Hashtable<String, Polyline> markersLine;
    private static Set<String> childrenLine;


    private MapView myOpenMapView;


    private ViewGroup mapBaseContainer;


    private boolean indoorMode = false;


    private boolean showingOffline = false;


    private TileCache tileCache;
    private Layer baseMapLayer;

    private Paint defaultPaint;


    protected String getPersistableId() {
        return this.getClass().getSimpleName();
    }


    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {


        AndroidGraphicFactory.createInstance( this.getActivity().getApplication());

        LinearLayout main = (LinearLayout) inflater.inflate(R.layout.fragment_mapviewmapsforge,
                container, false);

        super.addCommonView(main);


        mapBaseContainer = (ViewGroup) main.findViewById(R.id.maparea);
        createMapview();

        //config map


        Drawable defaultMarker = getResources().getDrawable(R.drawable.ic_action_favorite_on);

        layersImpl = new Hashtable<LAYERS, Object>();
        items = new Hashtable<LAYERS, List<Marker>>();

        //center map on guide location
        doInitialCenter();





        //initialize markers lines geometries (one by place with associated line)
        synchronized (myOpenMapView.getLayerManager().getLayers())
        {
            markersLine = new Hashtable<String, Polyline>();
            childrenLine = new HashSet();
            layersImpl.put(LAYERS.MARKERSGEOMETRY, new GMUGroupLayer(""+LAYERS.MARKERSGEOMETRY));
            myOpenMapView.getLayerManager().getLayers().add((Layer) layersImpl.get(LAYERS.MARKERSGEOMETRY));


            //initialize selected line
            LAYERS[] l = new LAYERS[]{LAYERS.LINES};
            for (int i = 0; i < l.length; i++) {
                LAYERS layers = l[i];


                layersImpl.put(layers, new GMUGroupLayer(""+layers));
                myOpenMapView.getLayerManager().getLayers().add((Layer) layersImpl.get(layers));
            }


            GMUGroupLayer pointsOverlay = new GMUGroupLayer("pointsOverlay");


            layersImpl.put(LAYERS.MARKERS, pointsOverlay);
            layersImpl.put(LAYERS.CHILDREN, pointsOverlay);
            layersImpl.put(LAYERS.SELECTEDPLACES, pointsOverlay);
            layersImpl.put(LAYERS.GPS, pointsOverlay);

            //create pointer list
            LAYERS[] values = LAYERS.values();
            for (int i = values.length - 1; i >= 0; i--) {
                LAYERS value = values[i];
                Object t = layersImpl.get(value);
                if (t instanceof GMUGroupLayer) {
                    items.put(value, new ArrayList<Marker>());

                }
            }


            myOpenMapView.getLayerManager().getLayers().add(pointsOverlay);

        }


        return main;

    }

    public void onDestroyView() {
        Log.d(TAG, "MapsForgeFragment ondestroy view");
        super.onDestroyView();


        mapBaseContainer.removeAllViews();

        mapBaseContainer = null;
        myOpenMapView.destroyAll();
        AndroidGraphicFactory.clearResourceMemoryCache();
        tileCache = null;
        Log.d(TAG, "MapsForgeFragment ondestroy view ends");
    }

    public void onResume() {
        super.onResume();


        //save center before do any base map switch
        updateCenterIncontext();


        //check if offline mode has changed
        if (!indoorMode) {
            if (Controller.getInstance().getGmuContext().offlineMode != this.showingOffline) {
                this.switchMap(null);
            }
        }

        if(baseMapLayer!=null&&baseMapLayer instanceof TileDownloadLayer)
        {
            ((TileDownloadLayer) baseMapLayer).onResume();
        }
    }


    public void onPause() {
        Log.d("EEO", "MapsForgeFragment pause");
        super.onPause();

        updateCenterIncontext();


        if(baseMapLayer!=null&&baseMapLayer instanceof TileDownloadLayer)
        {
            ((TileDownloadLayer) baseMapLayer).onPause();
        }
        Log.d("EEO", "MapsForgeFragment pause ends");

    }


    protected void updateCenterIncontext() {   //save last center
        if (!indoorMode) {
            IMapViewPosition mapPosition = myOpenMapView.getModel().mapViewPosition;

            if (mapPosition != null && placesLoaded) {
                //save center in context
                LatLong geoPoint = mapPosition.getCenter();
                Controller.getInstance().getGmuContext().lastMapCenterLocation = new org.gmu.map.MapPosition();

                Controller.getInstance().getGmuContext().lastMapCenterLocation.location = MapUtils.location2WKT(MapUtils.mapsForge2Location(geoPoint));
                Controller.getInstance().getGmuContext().lastMapCenterLocation.zoom = mapPosition.getZoomLevel();

            }
        }


    }


    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);


    }

    public void show() {

        previousSelectedKey = null;
        addPlaceElements(getVisiblePlaceElements());
        PlaceElement selected = null;
        if (currentSelectedElement == null) {
            selected = Controller.getInstance().getDao().load(Controller.getInstance().getSelectedUID());
            PlaceElement selectedChild = Controller.getInstance().getDao().load(Controller.getInstance().getSelectedChild());
            if (selectedChild != null) {
                selected = selectedChild;
            }
            if (selected != null) {
                this.selectUid(selected.getUid(), true);
            }
        } else {
            showSelection(false);
        }
        //center in bbox
        doInitialCenter();


    }

    @Override
    protected void switchMap(String relativeMapPath) {
        if (myOpenMapView == null) {
            //not started
            return;
        }
        if (!Utils.isEmpty(fixedMap)) {  //ignore and always fix map
            relativeMapPath = fixedMap;

        }
        if (!Utils.isEmpty(relativeMapPath)) {
            //create a TilestoreLayer with inmemory tile cache


            TileStoreLayer tileStoreLayer = new TileStoreLayer(createInMemoryTileCache(new File(Controller.getInstance().getDao().getIndoorFilePath(relativeMapPath))),
                    this.myOpenMapView.getModel().mapViewPosition, AndroidGraphicFactory.INSTANCE, false);
            setBaseMapLayer(tileStoreLayer);


            indoorMode = true;

            myOpenMapView.getModel().mapViewPosition.setZoomLevel((byte) 2);


        } else {


            Integer maxZoom = null;
            if (Controller.getInstance().getGmuContext().offlineMode) {

                maxZoom = new Integer(switchOfflineMap());


                showingOffline = true;

            } else {
                setBaseMapLayer(createOnlineLayer());

                showingOffline = false;
            }


            //center map on guide location
            doInitialCenter();


            indoorMode = false;


        }
        refreshView();

    }

    @Override
    protected void zoomIn() {
        int initZoom= myOpenMapView.getModel().mapViewPosition.getZoomLevel();
        myOpenMapView.getModel().mapViewPosition.zoomIn();
        Log.i(TAG,"Zoom in="+initZoom+"-->"+myOpenMapView.getModel().mapViewPosition.getZoomLevel());
        myOpenMapView.onZoomEvent();
    }

    @Override
    protected void zoomOut() {
        int initZoom= myOpenMapView.getModel().mapViewPosition.getZoomLevel();
        myOpenMapView.getModel().mapViewPosition.zoomOut();
        Log.i(TAG,"Zoom in="+initZoom+"-->"+myOpenMapView.getModel().mapViewPosition.getZoomLevel());
        myOpenMapView.onZoomEvent();
    }

    @Override
    protected void drawElement(PlaceElement elem, boolean drawGeometry, LAYERS targetLayer, boolean center) {
        if (layersImpl.get(targetLayer) == null) return;

        if (Utils.isEmpty(elem.getPointWKT())

                || !Utils.equals(currentBaseMap, elem.getAttributes().get("indoor_map"))) {
            //only show elements in base map or with geometry
            return;
        }


        if (targetLayer.equals(LAYERS.LINES) || targetLayer.equals(LAYERS.MARKERSGEOMETRY)) {
            //show path if exist
            if (!MapUtils.isLine(elem.getWKT())) return;
            Object container = layersImpl.get(targetLayer);
            GMUGroupLayer pathElements = (GMUGroupLayer) container;

            if (targetLayer.equals(LAYERS.LINES)) {
                List<Layer> l = pathElements.layers;
                for (int i = 0; i < l.size(); i++) {
                    Polyline layer = (Polyline) l.get(i);
                    layer.clear();

                }


            }
            Paint linePaint = getDefaultPaint();
            if (!Utils.isEmpty(elem.getAttributes().get("linecolor"))) {
                //change color
                String color = elem.getAttributes().get("linecolor").replace("#", "").replace("0x", "#");
                String transparency = "FF";
                color = transparency + color.substring(color.length() - 6);
                color = "#" + color;
                boolean isSelected = elementSelected(elem.getUid());
                 linePaint = createLinePath(elem.getAttributes().get("linecolor"), elem.getAttributes().get("linestyle"), !isSelected);


            }

            List<Location> path = MapUtils.toLocation(elem.getWKT());
            if (path.size() > 1) {
                List<LatLong> p = new ArrayList<>();
                for (int i = 0; i < path.size(); i++) {
                    LatLong geoPoint = MapUtils.location2MapsForge(path.get(i));
                    gisBuffer.addPoint(geoPoint);
                    p.add(geoPoint);
                }

                Polyline toAdd = null;
                if (targetLayer.equals(LAYERS.MARKERSGEOMETRY)) {   //update transparency

                    toAdd = markersLine.get(elem.getUid());
                    if (toAdd == null) {
                        toAdd = new Polyline(linePaint, AndroidGraphicFactory.INSTANCE);
                        ((Polyline) toAdd).setPoints(p);

                        pathElements.addLayer(toAdd);
                        //force set displaymode on children
                        pathElements.setDisplayModel(pathElements.getDisplayModel());


                    } else {
                        toAdd.setPaintStroke(linePaint);

                        pathElements.requestRedraw();
                    }
                    markersLine.put(elem.getUid(), toAdd);

                } else {
                    toAdd = new Polyline(linePaint, AndroidGraphicFactory.INSTANCE);
                    ((Polyline) toAdd).setPoints(p);
                    pathElements.addLayer(toAdd);
                    //force set displaymode on children
                    pathElements.setDisplayModel(pathElements.getDisplayModel());
                }
                toAdd.requestRedraw();

            }
        } else {
            GMUGroupLayer itemElements = (GMUGroupLayer) layersImpl.get(targetLayer);

            Marker oi = getMarker(elem);
            if (oi != null) {
                if (!elem.getCategory().equalsIgnoreCase("GPS")) {
                    gisBuffer.addPoint(oi.getLatLong());
                }

                itemElements.addLayer(oi);
                items.get(targetLayer).add(oi);
                //force set displaymode on children
                itemElements.setDisplayModel(itemElements.getDisplayModel());


            }
            if (drawGeometry) {

                drawElement(elem, true, LAYERS.MARKERSGEOMETRY, false);
                if (targetLayer.equals(LAYERS.CHILDREN)) {   //store children line uid
                    childrenLine.add(elem.getUid());
                }
            }
            itemElements.requestRedraw();
        }

        if (center) {
            List<Location> path = MapUtils.toLocation(elem.getPointWKT());
            if (path.size() > 0) {
                // myOpenMapView.getController().setCenter(MapUtils.osm2MapsForge(path.get(0)));
                byte zoom = myOpenMapView.getModel().mapViewPosition.getZoomLevel();
                if (myOpenMapView.getModel().mapViewPosition.getZoomLevelMax() < zoom) {
                    zoom = myOpenMapView.getModel().mapViewPosition.getZoomLevelMax();
                }


                myOpenMapView.getModel().mapViewPosition.setCenter(MapUtils.location2MapsForge(path.get(0)));
                myOpenMapView.getModel().mapViewPosition.setZoomLevel(zoom,true);
            }


        }

    }


    private void clearAllLayers() {

        //clear points overlay
        clearLayerNative(LAYERS.MARKERS);

        clearLayerNative(LAYERS.LINES);
        clearLayerNative(LAYERS.MARKERSGEOMETRY);

    }


    private void clearLayerNative(LAYERS layer) {


        Object y = layersImpl.get(layer);
        if (y == null) return;
        List<Layer> toClear;
        if (y instanceof HashMap) {
            toClear = new ArrayList(((HashMap) y).values());
        } else {
            toClear = new ArrayList<Layer>();
            toClear.add((Layer) y);
        }
        if (layer.equals(LAYERS.CHILDREN)) {
            clearChildrenLines();

        } else if (layer.equals(LAYERS.MARKERSGEOMETRY)) {
            markersLine = new Hashtable<String, Polyline>();
            childrenLine = new HashSet<String>();
        }


        for (int i = 0; i < toClear.size(); i++) {
            Layer o = toClear.get(i);

            if (o instanceof GMUGroupLayer) {
                ((GMUGroupLayer) o).layers.clear();

            } else if (o instanceof Polyline) {
                ((Polyline) o).clear();
            }
        }
    }

    @Override
    protected void clearLayer(LAYERS layer) {

        Log.d(TAG,"clear layer="+layer);
        //clear individual items
        Object y = layersImpl.get(layer);
        if (y == null) return;

        if (layer.equals(LAYERS.CHILDREN)) {
            clearChildrenLines();

        }


        List<Layer> toClear;
        if (y instanceof HashMap) {
            toClear = new ArrayList(((HashMap) y).values());
        } else {
            toClear = new ArrayList<Layer>();
            toClear.add((Layer) y);
        }
        for (int i2 = 0; i2 < toClear.size(); i2++) {
            Layer o = toClear.get(i2);

            if (o instanceof GMUGroupLayer) {

                List l = items.get(layer);
                for (int i = 0;l!=null&& i < l.size(); i++) {
                    GMUMarker marker = (GMUMarker) l.get(i);
                    Log.d(TAG,"clear marker="+marker.getTitle());

                    ((GMUGroupLayer) o).removeLayer(marker);

                }
                items.put(layer, new ArrayList<Marker>());

            } else if (o instanceof Polyline) {
                ((Polyline) o).clear();
            }
            o.requestRedraw();
        }

    }

    private void clearChildrenLines() {
        Iterator it = childrenLine.iterator();
        Object container = layersImpl.get(LAYERS.MARKERSGEOMETRY);
        GMUGroupLayer pathElements = (GMUGroupLayer) container;
        //remove children lines on markers geometry
        while (it.hasNext()) {
            String next = (String) it.next();
            Polyline line = markersLine.get(next);
            pathElements.removeLayer(line);
            markersLine.remove(next);

        }
        childrenLine = new HashSet<String>();


    }

    @Override
    protected void refreshView() {

        myOpenMapView.invalidate();
        // myOpenMapView.getOverlayController().redrawOverlays(); //method 1

        // myOpenMapView..redraw();
    }


    private void addPlaceElements(List<PlaceElement> elems) {


        clearAllLayers();

        for (int i = 0; i < elems.size(); i++) {
            PlaceElement elem = elems.get(i);


            drawElement(elem, true, LAYERS.MARKERS, false);


        }

        refreshView();


    }


    private void createMapview() {


        myOpenMapView = new GMUMapsForgeMapView(this.getActivity().getApplicationContext());
        setBaseMapLayer(createOnlineLayer());

        ((GMUMapsForgeMapView) myOpenMapView).setMapEventListener(this);
        myOpenMapView.setBuiltInZoomControls(false);
        myOpenMapView.setClickable(true);


        ViewGroup mapContainer = new RelativeLayout(mapBaseContainer.getContext());
        mapContainer.addView(myOpenMapView, new RelativeLayout.LayoutParams(-1, -1));
        mapBaseContainer.addView(mapContainer);
        //refresh offline mode
        this.switchMap(null);

    }

    /**
     * BEGIN MAP EVENT LISTENER IMPL*
     */


    public void onZoomLevelChanged() {
        byte zoomLevel = myOpenMapView.getModel().mapViewPosition.getZoomLevel();
        boolean zoomInEnabled = zoomLevel < myOpenMapView.getModel().mapViewPosition.getZoomLevelMax();
        boolean zoomOutEnabled = zoomLevel > myOpenMapView.getModel().mapViewPosition.getZoomLevelMin();


        Log.i(TAG,"Zoom level changed to "+zoomLevel);
        //Not necessary ,map position  handles max zoom level
        //  onZoomChangeListener.onZoom(zoomInEnabled, zoomOutEnabled);
    }


    /**
     * END MAP EVENT LISTENER IMPL*
     */


    private Marker getMarker(final PlaceElement elem) {
        Marker ret = null;
        List<Location> points = MapUtils.toLocation(elem.getPointWKT());
        if (points.size() > 0) {

            Bitmap icon = new AndroidBitmap(getMarkerBitMap(elem));
            icon.incrementRefCount();

            int verticalOffset=-icon.getHeight() / 2;
            if (elem.getCategory().equals("infowindow"))
            {
                verticalOffset=icon.getHeight() / 2;

            }

            //set uid in title to access on touch
            ret = new GMUMarker(elem.getUid(),MapUtils.location2MapsForge(points.get(0)), icon, 0, verticalOffset) {


                @Override
                public boolean onTap(LatLong geoPoint, org.mapsforge.core.model.Point viewPosition,
                                     Point tapPoint) {


                    if(viewPosition==null)
                    {
                        viewPosition = myOpenMapView.getMapViewProjection().toPixels(this.getPosition());
                    }
                    if (contains(viewPosition, tapPoint))
                    {
                        Log.d(TAG,"TAP ON "+getTitle());
                        MapsForgeMapFragment.this.selectUid(getTitle(), false);
                        return true;
                    }
                    return false;

                }
            };


        }
        return ret;
    }


    private static Path makePathDash(float ratio) {
        Path p = new Path();
        p.moveTo(ratio * 4, 0 * ratio);
        p.lineTo(0 * ratio, -4 * ratio);
        p.lineTo(8 * ratio, -4 * ratio);
        p.lineTo(12 * ratio, 0 * ratio);
        p.lineTo(8 * ratio, 4 * ratio);
        p.lineTo(0 * ratio, 4 * ratio);
        return p;
    }

    private Layer createOnlineLayer() {
        PlaceElement guide = Controller.getInstance().getDao().load(Controller.getInstance().getDao().getBaseGuideId());

        String hostname=null;
        String extension = "png";
        String protocol = "http";
        String prefix = "";
        int port = 80;
        if (guide != null && !Utils.isEmpty(guide.getAttributes().get("onlinemap")))
        {
            if(MapUtils.isValidMapTileSource(guide.getAttributes().get("onlinemap")))
            {
                hostname = guide.getAttributes().get("onlinemap");
            }

        }

        if(Utils.isEmpty(hostname)){
            hostname = Controller.getInstance().getConfig().getBaseOnlineMap();
            extension = (Controller.getInstance().getConfig().getBaseOnlineExtension());

        }

        if (hostname.toLowerCase().startsWith("https")) {
            port = 443;
            protocol = "https";
        }
        hostname = hostname.replace(protocol + "://", "");
        int i = hostname.indexOf("/");
        if (i == -1) {
            hostname = hostname;
        } else {
            prefix = "/" + hostname.substring(i+1) + "/";
            if(prefix.equals("//")){prefix="/";}
            hostname = hostname.substring(0, i);
//
        }

        OnlineTileSource onlineTileSource =null;
       if(OSM_BASE_TILES[0].equals(hostname))
       {    //apply base OSM
           onlineTileSource= new OnlineTileSource(OSM_BASE_TILES,80);
           protocol="http";

       }   else
       {
           onlineTileSource = new OnlineTileSource(new String[]{hostname},
               port);

       }



        onlineTileSource.setName("online").setAlpha(false)
                .setBaseUrl(prefix)
                .setParallelRequestsLimit(8).setProtocol(protocol).setTileSize(256)
                .setZoomLevelMax((byte) MAX_MAP_ZOOM).setZoomLevelMin((byte) 0)
                .setExtension(extension);
        onlineTileSource.setUserAgent("mapsforge-samples-android");

        return new TileDownloadLayer(createTileCache(),
                myOpenMapView.getModel().mapViewPosition, onlineTileSource,
                AndroidGraphicFactory.INSTANCE);


    }

    private byte switchOfflineMap() {
        OfflineMapDefinition m = Controller.getInstance().getDao().getOfflineMapData();
        File f = new File(m.mapPath);
        if (f.exists()) {
            if (!f.isDirectory()) {   //vectorial

                TileRendererLayer tileRendererLayer = AndroidUtil.createTileRendererLayer(createTileCache(),
                        myOpenMapView.getModel().mapViewPosition, new MapFile(f), InternalRenderTheme.DEFAULT, false, true, false);

                Resources resources = this.getActivity().getResources();
                DisplayMetrics metrics = resources.getDisplayMetrics();
                //dpi dependant style
                Controller.getInstance().getDao().setMapsForgeRenderTheme(metrics.densityDpi, tileRendererLayer);

                setBaseMapLayer(tileRendererLayer);

            } else {  //tile

                TileStoreLayer tileStoreLayer = new TileStoreLayer(createInMemoryTileCache(f),
                        this.myOpenMapView.getModel().mapViewPosition, AndroidGraphicFactory.INSTANCE, false);
                setBaseMapLayer(tileStoreLayer);


            }

        }

        this.myOpenMapView.getModel().mapViewPosition.setZoomLevelMax(m.maxZoom);
        return m.maxZoom;


    }


    protected void doInitialCenter() {
        try {
            Location location;
            byte zoom = 18;

            //locate selected
            PlaceElement selected = Controller.getInstance().getDao().load(Controller.getInstance().getSelectedUID());
            PlaceElement selectedChild = Controller.getInstance().getDao().load(Controller.getInstance().getSelectedChild());
            if (selectedChild != null) {
                selected = selectedChild;
            }

            org.gmu.map.MapPosition guideCenter = getInitialCenterPosition();
            zoom = guideCenter.zoom;
            location = MapUtils.WKT2Location(guideCenter.location);
            if (selected != null) {
                //center on selected

                location = MapUtils.WKT2Location(selected.getPointWKT());

            }


            Log.i(TAG, "Set center on zoom=" + zoom + "location=" + location + " ");

            if (location != null) {
                myOpenMapView.getModel().mapViewPosition.setCenter(MapUtils.location2MapsForge(location));
            }

            if (zoom > myOpenMapView.getModel().mapViewPosition.getZoomLevelMax()) {


                zoom = myOpenMapView.getModel().mapViewPosition.getZoomLevelMax();
            }

            Log.i(TAG, "Set center on zoom=" + zoom + "location=" + location + " ");

            myOpenMapView.getModel().mapViewPosition.setZoomLevel(zoom,true);


        } catch (Exception ign) {
            Log.e(TAG, "Error on center", ign);
        }
    }







    private Paint createLinePath(String lineColor, String lineStyle, boolean transparent) {


        int color = Color.BLUE;
        //custom color
        if (!Utils.isEmpty(lineColor)) {
            //change color
            String colorS = lineColor.replace("#", "").replace("0x", "#");
            String transparency = "FF";


            colorS = transparency + colorS.substring(colorS.length() - 6);
            colorS = "#" + colorS;
            color = Color.parseColor(colorS);
        }
        GMUPaint wayDefaultPaintFill = new GMUPaint();

        Resources resources = this.getActivity().getResources();
        DisplayMetrics metrics = resources.getDisplayMetrics();
        //adjust line size to dpi
        float scale = (float) (((float) metrics.densityDpi * 1.5) / 240.0);
        wayDefaultPaintFill.setColor(color);

        boolean applyDefaultStyle = true;
        //custom style

        if (!Utils.isEmpty(lineStyle)) {
            applyDefaultStyle = false;
            if (lineStyle.equalsIgnoreCase(Constants.LINESTYLE_THIN)) {
                wayDefaultPaintFill.setStyle(Style.STROKE);
                wayDefaultPaintFill.setStrokeWidth(4);
                wayDefaultPaintFill.setStrokeJoin(Join.ROUND);
                if (transparent) wayDefaultPaintFill.setPathEffect(new DashPathEffect(new float[] {4,4}, 0));

            } else {
                applyDefaultStyle = true;
            }

        }
        if (applyDefaultStyle) {
            //directorinal dash
             PathEffect pE = new PathDashPathEffect(makePathDash(scale), 12 * scale, 0,
                   PathDashPathEffect.Style.MORPH);

            wayDefaultPaintFill.setStyle(Style.STROKE);
            wayDefaultPaintFill.setPathEffect(pE);
            wayDefaultPaintFill.setStrokeWidth(7);
            wayDefaultPaintFill.setStrokeJoin(Join.ROUND);
            if (transparent) {
                // wayDefaultPaintFill.set
                wayDefaultPaintFill.setAlpha(Controller.getInstance().getConfig().getNotSelectedRouteAlpha());
            }


        }

        return wayDefaultPaintFill;

    }

    private List<PlaceElement> getVisiblePlaceElements() {

        return Controller.getInstance().getDao().list(Controller.getInstance().getFilter(), Controller.getInstance().getGroupFilter(), Controller.getInstance().getDao().visibleOnMapItems(), Controller.getInstance().getDistanceOrderDefinition());


    }

    protected TileCache createInMemoryTileCache(File tileStoreDir) {

        if (tileCache != null) tileCache.destroy();
        TileStore tileStore = new TileStore(tileStoreDir, ".png", AndroidGraphicFactory.INSTANCE);


        InMemoryTileCache memoryTileCache = new InMemoryTileCache(AndroidUtil.getMinimumCacheSize(Controller.getInstance().getContext(),
                myOpenMapView.getModel().displayModel.getTileSize(),
                myOpenMapView.getModel().frameBufferModel.getOverdrawFactor(), this.getScreenRatio()));
        tileCache = new TwoLevelTileCache(memoryTileCache, tileStore);
        return tileCache;
    }

    protected TileCache createTileCache() {
        boolean persistent = false;
        if (tileCache != null) tileCache.destroy();

        tileCache = AndroidUtil.createTileCache(Controller.getInstance().getContext(), getPersistableId(),
                myOpenMapView.getModel().displayModel.getTileSize(), this.getScreenRatio(),
                myOpenMapView.getModel().frameBufferModel.getOverdrawFactor(), persistent);
        return tileCache;
    }

    protected float getScreenRatio() {
        return 1.0f;
    }

    protected void setBaseMapLayer(Layer layer) {
        synchronized (myOpenMapView.getLayerManager().getLayers())
        {
            if (baseMapLayer != null) {
                myOpenMapView.getLayerManager().getLayers().remove(baseMapLayer);


            }
            //limit zoom
            myOpenMapView.getModel().mapViewPosition.setZoomLevelMax(MAX_MAP_ZOOM);
           
            myOpenMapView.getLayerManager().getLayers().add(0, layer, true);
        }

        baseMapLayer = layer;

    }


    private Paint getDefaultPaint()
    {
        if(defaultPaint==null)
        {
            defaultPaint=createLinePath(null,null, true);
        }
        return defaultPaint;
    }


}
