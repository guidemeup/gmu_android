package org.gmu.dao;

import android.graphics.Bitmap;
import android.location.Location;
import android.widget.ImageView;
import org.gmu.pojo.OfflineMapDefinition;
import org.gmu.pojo.PlaceElement;
import org.gmu.pojo.Relation;
import org.mapsforge.map.android.view.MapView;
import org.mapsforge.map.layer.renderer.TileRendererLayer;


import java.io.File;
import java.util.*;

/**
 * User: ttg
 * Date: 2/01/13
 * Time: 10:07
 * To change this template use File | Settings | File Templates.
 */
public interface IPlaceElementDAO {
    public static final String PLACES_DIR = "places";
    public static final String BASE_DIR = "base";
    public static final Set<String> ALL = new HashSet(Arrays.asList(new String[]{PlaceElement.TYPE_PLACE, PlaceElement.TYPE_ROUTE,PlaceElement.TYPE_MULTIMEDIA,PlaceElement.TYPE_GROUP,PlaceElement.TYPE_GUIDE,}));


    public static final Set<String> ITEMS_WITH_URL = new HashSet(Arrays.asList(new String[]{PlaceElement.TYPE_PLACE, PlaceElement.TYPE_ROUTE,PlaceElement.TYPE_MULTIMEDIA}));
    public static final Set<String> DETAIL_ROOT_ITEMS = new HashSet(Arrays.asList(new String[]{PlaceElement.TYPE_PLACE, PlaceElement.TYPE_ROUTE}));
    public static final Set<String> VISIBLEONLIST_ITEMS = new HashSet(Arrays.asList(new String[]{PlaceElement.TYPE_GUIDE, PlaceElement.TYPE_PLACE, PlaceElement.TYPE_GROUP, PlaceElement.TYPE_ROUTE}));
    public static final Set<String> PLACE_RELATED_ITEMS = new HashSet(Arrays.asList(new String[]{PlaceElement.TYPE_MULTIMEDIA}));
    public static final Set<String> GROUP_ITEMS = new HashSet(Arrays.asList(new String[]{PlaceElement.TYPE_GROUP}));
    public static final Set<String> GUIDE_ITEMS =new HashSet(Arrays.asList( new String[]{PlaceElement.TYPE_GUIDE}));
    public static final Set<String> PARENT_ITEMS = new HashSet(Arrays.asList(new String[]{PlaceElement.TYPE_GROUP, PlaceElement.TYPE_GUIDE}));
    public static final Set<String> PARENT_AND_LINKS_RELATIONS = new HashSet(Arrays.asList(new String[]{Relation.RELATION_PARENT, Relation.RELATION_LINK}));

    public static final Set<String> VISIBLEELEMENTSONROOT = new HashSet(Arrays.asList(new String[]{PlaceElement.TYPE_GUIDE, PlaceElement.TYPE_PLACE, PlaceElement.TYPE_ROUTE}));

    public static final Set<String> VISIBLEELEMENTSONGUIDE = new HashSet(Arrays.asList(new String[]{ PlaceElement.TYPE_PLACE, PlaceElement.TYPE_ROUTE}));



    public Set<String> visibleOnMapItems();

    public boolean isParentPlaceElement(PlaceElement place);

    public List<PlaceElement> listAllGuides();

    public List<PlaceElement> list(PlaceElement filter, String parentUID, Set<String> types, OrderDefinition order);

    public List<PlaceElement> list(PlaceElement filter, String parentUID, Set<String> types, Integer maxHierachyDistance, OrderDefinition order);

    public PlaceElement load(String uid);

    public void loadImageInView(ImageView imageView, String imageURI, int imageDPI);

    public Bitmap loadImage(String imageURI, int imageDPI);

    public Bitmap loadCategoryIcon(PlaceElement elem);

    public void loadCategoryIconInView(ImageView imageView, PlaceElement elem);

    public String[] getLinkedResourcesURIs(String uid, String type);

    public String getPlaceResourcePath(String path);

    public String getRelatedFileDescriptor(String path);
    public String getIndoorFilePath(String path);

    public String getCategoryIcon(PlaceElement elem);

    public List<PlaceElement> getRelatedPlaceElements(String uid, Set<String> relationTypes, Set<String> placeElementTypes);

    public List<Relation> getRelations(String sourcePlaceUID, String relationType);

    public PlaceElement getParent(String uid);



    public String[] getPlayList(String placeUID);

    public void addToFavs(String uid);

    public void delFromFavs(String uid);

    public OfflineMapDefinition getOfflineMapData();

    public String getBaseGuideId();

    public void destroy();

    public String getFixedMap();



    public void deleteGuide();
    public  void updateDistances(Location userLocation, List<PlaceElement> placeElements);

    public interface LoadListener
    {
        /**
         * Called some event happens
         */
        void onDAOLoadEvent(String msg, int percent);


    }


    public void copyGuideDefinition(String destinationGuideId);
    public void saveOrUpdate(List<PlaceElement> pl) ;


    public boolean isPreProductionGuide(String guideUid);
    public void setPreProductionGuide(String guideUid, boolean set);

    public void  setMapsForgeRenderTheme(int densityDpi, TileRendererLayer tileRendererLayer);

    public  boolean paymentNeeded(PlaceElement placeElement);



}
