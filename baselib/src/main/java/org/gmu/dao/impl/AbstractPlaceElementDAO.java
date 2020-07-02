package org.gmu.dao.impl;

import android.content.Context;
import android.graphics.Bitmap;
import android.location.Location;
import android.util.Log;
import android.widget.ImageView;
import org.gmu.config.Constants;
import org.gmu.control.Controller;
import org.gmu.dao.IPlaceElementDAO;
import org.gmu.dao.OrderDefinition;
import org.gmu.pojo.OfflineMapDefinition;
import org.gmu.pojo.PlaceElement;
import org.gmu.pojo.Relation;
import org.gmu.utils.FileUtils;
import org.gmu.utils.MapUtils;
import org.gmu.utils.Utils;
import org.gmu.utils.ZipFileSystem;
import org.gmu.utils.image.ImageDescriptor;
import org.gmu.utils.image.ImageLoader;
import org.mapsforge.map.android.rendertheme.AssetsRenderTheme;
import org.mapsforge.map.android.util.AndroidUtil;
import org.mapsforge.map.android.view.MapView;
import org.mapsforge.map.layer.renderer.TileRendererLayer;
import org.mapsforge.map.reader.MapFile;


import java.io.File;
import java.util.*;

/**
 * User: ttg
 * Date: 11/11/12
 * Time: 21:10
 * To change this template use File | Settings | File Templates.
 */
public abstract class AbstractPlaceElementDAO implements IPlaceElementDAO {


    private static final String TAG = AbstractPlaceElementDAO.class.getName();
    private ImageLoader imageLoader = null;
    private OfflineMapDefinition offlineMapdefinition = null;
    private String fixedMapPath;
    private Context context;
    private boolean fixedMapLoaded = false;
    protected String base;
    private Set<String> visibleOnMapItems = null;
    private Map<String, Boolean> parentsCache;

    public AbstractPlaceElementDAO(Context _context, String base) {
        super();
        context = _context;
        if (context == null) {
            System.out.println("pasa aqui?");

        }
        imageLoader = new ImageLoader(context);
        this.base = base;
        parentsCache = new HashMap();

    }

    public Set<String> visibleOnMapItems() {
        if (visibleOnMapItems == null) {
            if (getBaseGuideId().equals(Controller.getInstance().getConfig().getRootId())) {   //root guide allow to show  guides on  map
                visibleOnMapItems = VISIBLEELEMENTSONROOT;
            } else {
                visibleOnMapItems = VISIBLEELEMENTSONGUIDE;
            }
        }
        return visibleOnMapItems;
    }

    public boolean isParentPlaceElement(PlaceElement place) {
        if (place == null) return false;
        Boolean isP = parentsCache.get(place.getUid());
        if (isP != null) return isP;

        List children = null;
        if (Utils.contains(PARENT_ITEMS, place.getType())) {
            //ckeck guide or group  has links or is parent

            children = getRelations(place.getUid(), PARENT_AND_LINKS_RELATIONS);

        } else {
            //check if place has children
            children = getRelatedPlaces(place.getUid(), Relation.RELATION_PARENT, new HashSet(Arrays.asList(new String[]{PlaceElement.TYPE_PLACE})));

        }
        boolean ret = (children != null && children.size() > 0);
        parentsCache.put(place.getUid(), ret);
        return ret;


    }

    public List<PlaceElement> listAllGuides() {
        return this.list(null, null, new HashSet(Arrays.asList(new String[]{PlaceElement.TYPE_GUIDE})), 1, Controller.getInstance().getDistanceOrderDefinition());
    }

    public List<PlaceElement> list(PlaceElement filter, String parentUID, Set<String> types, OrderDefinition order) {
        return list(filter, parentUID, types, 999, order);
    }


    public void loadImageInView(ImageView imageView, String imageURI, int imageDPI) {    //get size from view
        imageLoader.loadInView(imageView, new ImageDescriptor(getRelatedFileDescriptor(imageURI), imageDPI));


    }

    public Bitmap loadImage(String imageURI, int imageDPI) {
        return (Bitmap) imageLoader.loadImage(new ImageDescriptor(getRelatedFileDescriptor(imageURI), imageDPI));
    }

    public Bitmap loadCategoryIcon(PlaceElement elem) {

        return (Bitmap) imageLoader.loadImage(new ImageDescriptor(getCategoryIcon(elem), Constants.ICONSIZEDPI, true));

    }

    public void loadCategoryIconInView(ImageView imageView, PlaceElement elem) {

        imageLoader.loadInView(imageView, new ImageDescriptor(getCategoryIcon(elem), Constants.ICONSIZEDPI, true));


    }


    public String[] getLinkedResourcesURIs(String uid, String type) {
        try {
            List<String> retorno = new ArrayList<String>();
            //add main_img
            PlaceElement pl = load(uid);
            String main = AbstractPlaceElementDAO.getMainImg(pl);

            retorno.add(main);


            String base = PLACES_DIR + "/" + uid + "/" + type;
            File f = new File(getRelatedFileDescriptor(base));
            String[] ret = f.list();

            for (int i = 0; ret != null && i < ret.length; i++) {

                retorno.add(base + "/" + ret[i].replace(" ", "%20"));
            }
            return retorno.toArray(new String[retorno.size()]);

        } catch (Exception ign) {
            System.out.print(ign);
        }
        return null;
    }


    public String getPlaceResourcePath(String path) {
        return getRelatedFileDescriptor(PLACES_DIR + "/" + path);
    }

    public String getRelatedFileDescriptor(String path) {
        try {
            //load with inheritance  and fail tolerance
            String file = getRelatedFileDescriptor(path, base);

            for (int i = 0; (!new File(file).exists()) && i < Controller.getInstance().getConfig().getResourceHierarchy().length; i++) {   //search in hierarchy

                file = getRelatedFileDescriptor(path, Controller.getInstance().getConfig().getResourceHierarchy()[i]);
            }
            return file;


        } catch (Exception ign) {
            //file not found
            return null;
        }
    }

    private String getRelatedFileDescriptor(String path, String basePath) {
        try {
            //load with inheritance  and fail tolerance
            String file = Utils.getFilePath(basePath + "/" + path);
            //check on zip
            ZipFileSystem zi=ZipFileSystem.getInstance();
            String name=zi.getFile(basePath + "/" + path);



            //escape spaces on filenames
            return file.replace(" ", "%20");


        } catch (Exception ign) {
            //file not found
            return null;
        }
    }


    public String getCategoryIcon(PlaceElement elem) {
        String category = "poi";
        boolean onMap = !Utils.isEmpty(elem.getWKT());
        String customIcon = elem.getAttributes().get("custom_icon");
        if (!Utils.isEmpty(customIcon)) {    //custom place icon
            return getRelatedFileDescriptor(customIcon);

        } else if (
                (Utils.isEmpty(elem.getCategory())) ||
                        (elem.getCategory().equalsIgnoreCase(Constants.EMPTYCATEGORY) && onMap)

                ) {   //show default icon category
            category = "poi";

        } else {
            category = elem.getCategory();

        }

        return getRelatedFileDescriptor(BASE_DIR + "/icons/" + category + ".png");
    }

    public List<PlaceElement> getRelatedPlaceElements(String uid, Set<String> relationTypes, Set<String> placeElementTypes) {

        List<PlaceElement> relatedObjects = new ArrayList<PlaceElement>();

        Iterator<String> it = relationTypes.iterator();
        while (it.hasNext()) {
            String relationType = it.next();
            relatedObjects.addAll(getRelatedPlaces(uid, relationType, placeElementTypes));
        }

        return relatedObjects;
    }


    public PlaceElement getParent(String uid) {
        List<PlaceElement> rels = getRelatedPlaces(uid, Relation.RELATION_PARENT, null);
        if (rels == null || rels.size() == 0) return null;
        else return rels.get(0);
    }

    public String[] getPlayList(String placeUID) {   //get related multimedia
        List<PlaceElement> l = getRelatedPlaceElements(placeUID, IPlaceElementDAO.PARENT_AND_LINKS_RELATIONS, PLACE_RELATED_ITEMS);

        String[] ret = new String[l.size()];
        for (int i = 0; i < l.size(); i++) {
            PlaceElement placeElement = l.get(i);
            ret[i] = placeElement.getUid();
        }
        return ret;

    }


    private List<PlaceElement> getRelatedPlaces(String sourcePlaceUID, String relationType, Set<String> placeElementTypes) {
        List<PlaceElement> retorno = new ArrayList<PlaceElement>();
        List<Relation> rels = getRelations(sourcePlaceUID, relationType);
        Collections.sort(rels);
        for (int i = 0; i < rels.size(); i++) {
            Relation relation = rels.get(i);

            if (placeElementTypes == null || Utils.contains(placeElementTypes, relation.getDestinationType())) {
                retorno.add(this.load(relation.getDestinationUID()));
            }

        }

        return retorno;
    }


    protected List<Relation> getRelations(String sourcePlaceUID, Set<String> relationTypes) {
        List<Relation> ret = new ArrayList<Relation>();
        Iterator<String> relationTypesI = relationTypes.iterator();
        while (relationTypesI.hasNext()) {
            String relationType = relationTypesI.next();
            ret.addAll(this.getRelations(sourcePlaceUID, relationType));
        }

        return ret;
    }

    protected boolean inFilter(PlaceElement filter, PlaceElement pl) {
        if (filter == null) return true;

        Iterator keys = filter.getAttributes().keySet().iterator();
        String key, value, atValue;
        while (keys.hasNext()) {
            key = (String) keys.next();
            if (key.equalsIgnoreCase("guidedownloaded") && !pl.getType().equalsIgnoreCase(PlaceElement.TYPE_GUIDE)) {   //ignore download filter in non guide elements
                continue;
            }


            value = (String) filter.getAttributes().get(key);
            if (Utils.isEmpty(value)) continue;
            atValue = pl.getAttributes().get(key);
            if (Utils.isEmpty(atValue) || (!atValue.toUpperCase().contains(value.toUpperCase()))) {
                return false;

            }

        }

        return true;


    }


    public OfflineMapDefinition getOfflineMapData()
    {
        if (offlineMapdefinition != null) return offlineMapdefinition;
        OfflineMapDefinition ret = new OfflineMapDefinition();
        byte maxZoom = 22;
        try {
            maxZoom = new Byte(this.load(base).getAttributes().get("maxzoom"));

        } catch (Exception ign) {
        }

        //try to obtain from current guide
        String offlineMap = null;
        if (this.load(base) != null) {
            offlineMap = this.load(base).getAttributes().get("offlinemap");
        }




        if (!Utils.isEmpty(offlineMap))
        {
            offlineMap = getRelatedFileDescriptor(offlineMap);
            //payed maps
            if(Controller.getInstance().getConfig().isPayMapsAllowed()||
                    this.isPreProductionGuide(this.getBaseGuideId())
                    )
            {   String payPath=offlineMap+".pay";
                File payed=new File(payPath);
                if(payed.exists()) offlineMap= payPath;
            }


            File f = new File(offlineMap);
            if (f.isDirectory())
            {   //search if zoom dir exist
                while (maxZoom != 0) {
                    File tileDir = new File(f, "" + maxZoom);
                    if (tileDir.exists()) {
                        break;
                    }
                    maxZoom--;
                }

            }


        } else {
            offlineMap = Controller.getInstance().getConfig().getRootId() + "/" + Constants.BASEMAPDIR + "/offline";


        }

        if(!(new File(offlineMap).exists()))
        {
            //try to get default store map
            offlineMap = getRelatedFileDescriptor("base/icons/vectorial/defaultOffline.map");
            if(!(new File(offlineMap).exists()))
            {   //try to get from cache
                offlineMap = getRelatedFileDescriptor("cache/vectorial/defaultOffline.map");
            }
        }


        ret.mapPath = offlineMap;
        ret.maxZoom = maxZoom;
        offlineMapdefinition = ret;
        return ret;

    }

    public String getBaseGuideId() {
        return base;
    }

    public String getFixedMap() {
        if (!fixedMapLoaded) {
            PlaceElement guide = this.load(this.getBaseGuideId());
            if (guide != null) {
                fixedMapPath = guide.getAttributes().get("fixed_map");
                fixedMapLoaded = true;
            }
        }

        return fixedMapPath;
    }

    /**
     * Calculates representative point
     *
     * @param elem
     */
    protected void updateRepresentativePoint(PlaceElement elem) {
        //if elem have geometry ==> first point
        Location l = MapUtils.WKT2Location(elem.getWKT());
        if (l != null) {
            elem.setPointWKT(MapUtils.location2WKT(l));
        } else {   //inherit from parent
            PlaceElement parent = getParent(elem.getUid());
            if (parent != null) {
                elem.setPointWKT(parent.getPointWKT());
            }

        }
    }

    public void updateDistances(Location userLocation, List<PlaceElement> placeElements) {
        if (userLocation != null) {   //initialize distances
            for (int i = 0; i < placeElements.size(); i++) {
                PlaceElement placeElement = placeElements.get(i);
                Location l = MapUtils.WKT2Location(placeElement.getPointWKT());
                if (l != null) {
                    placeElement.setDistanceToUser(l.distanceTo(userLocation));
                }
            }

        }
    }



    public static boolean isGuideDownloaded(String guideUID) { //ckeck is version file is downloaded

        File oldVersionFile = new File(Utils.getFilePath("/" + guideUID + "/" + Constants.VERSIONFILENAME));
        return oldVersionFile.exists();

    }

    public void deleteGuide() {   //delete files
        FileUtils.deleteDirectory(new File(Utils.getFilePath("/" + this.getBaseGuideId())));


    }

    public static String getMainImg(PlaceElement elem) {
        String img = elem.getAttributes().get("logo_img");
        if (Utils.isEmpty(img)) img = elem.getAttributes().get("main_img");
        return img;

    }

    public void copyGuideDefinition(String destinationGuideId) {
        //copy guide.kml to root
        try {
            String sourceGuideId = this.getBaseGuideId();

            //copy guide definition to destination dao
            File destination = new File(getRelatedFileDescriptor("base/kml/private-" + sourceGuideId + ".kml", destinationGuideId));
            FileUtils.copyFile(new File(getRelatedFileDescriptor("base/kml/guides.kml", sourceGuideId)), destination);
            //copy logo and main img
            PlaceElement baseG = this.load(sourceGuideId);
            if (!Utils.isEmpty(baseG.getAttributes().get("logo_img"))) {
                destination = new File(getRelatedFileDescriptor(baseG.getAttributes().get("logo_img"), destinationGuideId));
                FileUtils.copyFile(new File(getRelatedFileDescriptor(baseG.getAttributes().get("logo_img"), sourceGuideId)), destination);
            }
            if (!Utils.isEmpty(baseG.getAttributes().get("main_img"))) {
                destination = new File(getRelatedFileDescriptor(baseG.getAttributes().get("main_img"), destinationGuideId));
                FileUtils.copyFile(new File(getRelatedFileDescriptor(baseG.getAttributes().get("main_img"), sourceGuideId)), destination);
            }


        } catch (Exception ign) {
            ign.printStackTrace();
        }

    }

    public void saveOrUpdate(List<PlaceElement> pl) {
        throw new RuntimeException("not implemented");
    }

    public boolean isPreProductionGuide(String guideUid) {
        PlaceElement guide = this.load(guideUid);
        if (guide == null) return false;
        return !Utils.isEmpty(guide.getAttributes().get(Constants.PREPRODUCTION_ATTRIB));


    }

    public void setPreProductionGuide(String guideUid, boolean set) {
        PlaceElement guide = this.load(guideUid);
        if (guide == null) return;
        if (set) {
            guide.getAttributes().put(Constants.PREPRODUCTION_ATTRIB, "true");
        } else {
            guide.getAttributes().put(Constants.PREPRODUCTION_ATTRIB, "");
        }

        List<PlaceElement> pl = new LinkedList<PlaceElement>();
        pl.add(guide);
        this.saveOrUpdate(pl);


    }

    public String getIndoorFilePath(String path) {
        //1:try to locate abspath
        File dir = new File(getRelatedFileDescriptor(path));
        if (!dir.exists()) {
            dir = new File(getRelatedFileDescriptor("cache/indoor/" + this.getBaseGuideId() + "/" + path));

        }
        if(!dir.exists())
        {   //locate global map
            dir = new File(getRelatedFileDescriptor("base/icons/indoor/" + path));

        }
        return dir.getAbsolutePath();


    }

    public void setMapsForgeRenderTheme(int densityDpi, TileRendererLayer tileRendererLayer ) {

        try {
            String density = "";
            if (densityDpi < 240) {
                density = "";

            } else if (densityDpi < 450) {
                density = "_L";

            } else {
                density = "_XL";
            }
            // try to load from filesystem

            String basePath = this.getRelatedFileDescriptor("base/icons/android/themes/Elevate{dpi}.xml".replace("{dpi}", density));

            File f = new File(basePath);

            if (f.exists()) {




                AssetsRenderTheme ar=new AssetsRenderTheme(Controller.getInstance().getContext(), "",basePath);

                tileRendererLayer.setXmlRenderTheme(ar);


            } else {   //use default
                float scale = (float) (((float) densityDpi * 1.3) / 240.0);

                tileRendererLayer.setTextScale(scale);


            }


        } catch (Exception ign) {

        }


    }
    public  boolean paymentNeeded(PlaceElement placeElement)
    {   //TODO quitar check de download si se quiere que la store pueda invalidadr descargas
        //guide without info about purchase state or not purchased
        return (Controller.getInstance().getConfig().getPurchaseManager()!=null
                &&
                (!isGuideDownloaded(placeElement.getUid()))
                        &&
                (Utils.isEmpty(placeElement.getAttributes().get("purchased"))||
                        placeElement.getAttributes().get("purchased").equalsIgnoreCase("false"))
        );


    }

}
