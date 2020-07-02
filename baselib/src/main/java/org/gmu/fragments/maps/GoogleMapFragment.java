package org.gmu.fragments.maps;


import android.graphics.*;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import com.google.android.gms.maps.*;
import com.google.android.gms.maps.model.*;
import org.gmu.base.R;
import org.gmu.control.Controller;
import org.gmu.fragments.AbstractMapFragment;

import org.gmu.pojo.PlaceElement;
import org.gmu.utils.MapUtils;
import org.gmu.utils.Utils;


import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;


/**
 * Created by IntelliJ IDEA.
 * User: acasquero
 * Date: 9/11/12
 * Time: 8:53
 * To change this template use File | Settings | File Templates.
 */
public class GoogleMapFragment extends AbstractMapFragment implements OnMapReadyCallback  {


    private static final String TAG = GoogleMapFragment.class.getName();


    private MapView myOpenMapView;
    private ViewGroup mapBaseContainer;
    private Byte previousZoom = null;


    private static Hashtable<LAYERS, Object> layersImpl;
    private GoogleMap map;


    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        setRetainInstance(true);

    }



    @Override
    public void onMapReady(GoogleMap map) {
    

        this.map=map;
        //config map

        previousZoom = 14;


        doInitialCenter();


        layersImpl = new Hashtable<LAYERS, Object>();


        PolylineOptions rectOptions = new PolylineOptions();

        rectOptions.color(Color.BLUE);
        rectOptions.geodesic(true);

        // Get back the mutable Polyline
        Polyline polyline = this.map.addPolyline(rectOptions);

        layersImpl.put(LAYERS.MARKERS, new ArrayList<Marker>());
        layersImpl.put(LAYERS.CHILDREN, new ArrayList<Marker>());
        layersImpl.put(LAYERS.SELECTEDPLACES, new ArrayList<Marker>());
        layersImpl.put(LAYERS.GPS, new ArrayList<Marker>());
        layersImpl.put(LAYERS.LINES, polyline);
        //set listener
        this.map.setOnMarkerClickListener(listener);
        this.map.setOnMapClickListener(listener);
        this.map.setInfoWindowAdapter(infoAdapter);



    }



    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {


        LinearLayout main = (LinearLayout) inflater.inflate(R.layout.fragment_mapviewmapsforge,
                container, false);

        super.addCommonView(main);


        mapBaseContainer = (ViewGroup) main.findViewById(R.id.maparea);
        createMapview();
        if (myOpenMapView != null) {
            myOpenMapView.onCreate(savedInstanceState);
        }
        myOpenMapView.getMapAsync(this);

        //config map

        previousZoom = 14;


        doInitialCenter();


        layersImpl = new Hashtable<LAYERS, Object>();


        PolylineOptions rectOptions = new PolylineOptions();

        rectOptions.color(Color.BLUE);
        rectOptions.geodesic(true);

        // Get back the mutable Polyline
        Polyline polyline = this.map.addPolyline(rectOptions);

        layersImpl.put(LAYERS.MARKERS, new ArrayList<Marker>());
        layersImpl.put(LAYERS.CHILDREN, new ArrayList<Marker>());
        layersImpl.put(LAYERS.SELECTEDPLACES, new ArrayList<Marker>());
        layersImpl.put(LAYERS.GPS, new ArrayList<Marker>());
        layersImpl.put(LAYERS.LINES, polyline);
        //set listener
        this.map.setOnMarkerClickListener(listener);
        this.map.setOnMapClickListener(listener);
        this.map.setInfoWindowAdapter(infoAdapter);


        return main;
    }

    public void onDestroyView() {
        super.onDestroyView();

        if (myOpenMapView != null) {
            myOpenMapView.onDestroy();
        }


    }

    public void onResume() {
        super.onResume();

        if (myOpenMapView != null) {
            myOpenMapView.onResume();
        }
    }


    public void onPause() {
        super.onPause();
        if (myOpenMapView != null) {
            myOpenMapView.onPause();
            //TODO: store center location in context

        }
    }


    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);


    }

    public void show() {


        previousSelectedKey = null;
        addPlaceElements(Controller.getInstance().getDao().list(Controller.getInstance().getFilter(), Controller.getInstance().getGroupFilter(), Controller.getInstance().getDao().visibleOnMapItems(), Controller.getInstance().getDistanceOrderDefinition()));

        PlaceElement selected = Controller.getInstance().getDao().load(Controller.getInstance().getSelectedUID());
        PlaceElement selectedChild = Controller.getInstance().getDao().load(Controller.getInstance().getSelectedChild());
        if (selectedChild != null) {
            selected = selectedChild;
        }
        if (selected != null) {
            this.selectUid(selected.getUid(), true);
        }

        //showSelection(true);

    }

    @Override
    protected void switchMap(String relativeMapPath) {
        refreshView();

    }

    @Override
    protected void zoomIn() {

        this.map.animateCamera(CameraUpdateFactory.zoomIn());

    }

    @Override
    protected void zoomOut() {
        this.map.animateCamera(CameraUpdateFactory.zoomOut());
    }

    @Override
    protected void drawElement(PlaceElement elem, boolean drawGeometry, LAYERS targetLayer, boolean center) {

        if (layersImpl.get(targetLayer) == null) return;

        Log.d(TAG, "draw=" + elem.getUid());
        if (Utils.isEmpty(elem.getPointWKT())
                || !Utils.equals(currentBaseMap, elem.getAttributes().get("indoor_map"))) {
            //only show elements in base map or with geometry
            return;
        }


        if (targetLayer.equals(LAYERS.LINES)) {
            //show path if exist

            Polyline route = (Polyline) layersImpl.get(targetLayer);


            List<Location> path = MapUtils.toLocation(elem.getWKT());
            List<LatLng> poly = new ArrayList();
            if (path.size() > 1) {

                for (int i = 0; i < path.size(); i++) {

                    poly.add(MapUtils.location2GMaps(path.get(i)));

                }

                route.setPoints(poly);
            }
        } else {
            List<Marker> l = (List<Marker>) layersImpl.get(targetLayer);
            l.add(getMarker(elem));

        }

        if (center) {
            List<Location> path = MapUtils.toLocation(elem.getPointWKT());
            if (path.size() > 0) {
                this.map.animateCamera(CameraUpdateFactory.newLatLngZoom(MapUtils.location2GMaps(path.get(0)), 14));


            }


        }
    }


    private void clearAllLayers() {
        //clear points overlay
        clearLayer(LAYERS.MARKERS);

        clearLayer(LAYERS.LINES);
    }


    @Override
    protected void clearLayer(LAYERS layer) {
        Object o = layersImpl.get(layer);
        if (o instanceof Polyline) {
            ((Polyline) o).setPoints(new ArrayList<LatLng>());
        } else {
            List<Marker> m = (List<Marker>) o;
            for (int i = 0; i < m.size(); i++) {
                Marker marker = m.get(i);

                if (marker != null) {
                    marker.remove();
                }

            }
            layersImpl.put(layer, new ArrayList<Marker>());
        }
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

        GoogleMapOptions options = new GoogleMapOptions();
        options.mapType(GoogleMap.MAP_TYPE_NORMAL)
                .compassEnabled(false)
                .rotateGesturesEnabled(false)
                .tiltGesturesEnabled(false).zoomControlsEnabled(false);

        myOpenMapView = new MapView(mapBaseContainer.getContext(), options);
        myOpenMapView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        ViewGroup mapContainer = new RelativeLayout(mapBaseContainer.getContext());
        mapContainer.addView(myOpenMapView, new RelativeLayout.LayoutParams(-1, -1));
        mapBaseContainer.addView(mapContainer);
        try {
            MapsInitializer.initialize(mapContainer.getContext());
        } catch (Exception ign) {
            ign.printStackTrace();
        }
        //refresh offline
        this.switchMap(null);


    }

    private Marker getMarker(PlaceElement elem) {
        Marker ret = null;
        List<Location> points = MapUtils.toLocation(elem.getPointWKT());
        if (points.size() > 0) {


            Bitmap icon = getMarkerBitMap(elem);

            MarkerOptions mo = new MarkerOptions()
                    .position(MapUtils.location2GMaps(points.get(0)))
                    .title(elem.getUid()).icon(BitmapDescriptorFactory.fromBitmap(icon));

            if (elem.getCategory().equals("infowindow")) {   //ignore

                mo = mo.anchor(0.5f, 0f);
            }
            ret = this.map.addMarker(mo);
            if (elem.getCategory().equals("infowindow")) {   //toucgh infowindow to set marker on top
                ret.showInfoWindow();
            }

        }

        return ret;
    }

    private MapListener listener = new MapListener();


    protected void doInitialCenter() {

        Location guideCenter = MapUtils.WKT2Location(getInitialCenterPosition().location);
        LatLng lastlocation =
                MapUtils.location2GMaps(guideCenter);
        if (lastlocation != null)
            this.map.animateCamera(CameraUpdateFactory.newLatLngZoom(lastlocation, 14));


    }


    private class MapListener implements com.google.android.gms.maps.GoogleMap.OnMapClickListener, GoogleMap.OnMarkerClickListener {
        public boolean onMarkerClick(Marker marker) {

            Log.d(TAG, "Click on marker:" + marker.getTitle());
            GoogleMapFragment.this.clearLayer(LAYERS.SELECTEDPLACES);
            GoogleMapFragment.this.selectUid(marker.getTitle(), false);
            marker.hideInfoWindow();
            return true;
        }

        public void onMapClick(LatLng latLng) {
            Log.d(TAG, "Click on map!");
            //clear infowindow
            GoogleMapFragment.this.clearLayer(LAYERS.SELECTEDPLACES);

        }
    }

    private GoogleMap.InfoWindowAdapter infoAdapter = new GoogleMap.InfoWindowAdapter() {
        public View getInfoWindow(Marker marker) {
            Log.d(TAG, "Show infowindow=" + marker.getTitle());
            //dummy layout
            LinearLayout l = new LinearLayout(GoogleMapFragment.this.getActivity());
            return l;
            //return GoogleMapFragment.this.getInfoWindow(Controller.getInstance().getDao().load(marker.getTitle()));
        }

        public View getInfoContents(Marker marker) {
            return null;  //To change body of implemented methods use File | Settings | File Templates.
        }
    };

}
