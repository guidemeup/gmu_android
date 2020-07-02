package org.gmu.config;

import android.location.LocationManager;


import org.gmu.base.R;
import org.gmu.control.Controller;
import org.gmu.dao.OrderDefinition;
import org.gmu.inappbilling.PurchaseManager;
import org.gmu.pojo.PlaceElement;
import org.gmu.utils.Utils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.gmu.utils.MapUtils.OSM_BASE_TILES;

/**
 * User: ttg
 * Date: 21/02/14
 * Time: 19:32
 * To change this template use File | Settings | File Templates.
 */
public class ConfigBean implements IConfig
{


    private String packageName;
    private String rootId;
    private String mainGuideId;
    private String rootName;
    private String[] resourceHierarchy;
    private String baseServerGuides;
    private String baseServer;
    private APP_TYPE appType;
    private String baseOnlineMap;
    private boolean showMap;
    private String baseOnlineExtension;
    private boolean defaultMapModeOffline=false;
    private boolean showDistance=true;
    private Integer inactiveCenterThresholdSecs=null;
    private boolean allwaysShowSelectedRouteMode=false;
    private int notSelectedRouteAlpha=70;
    private boolean payMapsAllowed=false;
    private PurchaseManager purchaseManager;
    public static int theme= R.style.GMU_Theme_Sherlock;
    private List<String> locationProviders= Arrays.asList(new String[]{LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER});
    private static Map<String,String> attribs=new HashMap();

  /* protected static final int[] AVAILABLE_THEMES =
            new int[]{R.style.Theme_Sherlock,
                    R.style.Theme_Sherlock_Light,
                    R.style.Theme_Sherlock_Light_DarkActionBar,
                    R.style.MyActionBarTheme,
                    R.style.GMU_Theme_Sherlock,
                    R.style.GMU_Theme_Sherlock_Light_DarkActionBar



            };*/


    public ConfigBean()
    {
        rootId= Constants.GUIDEMEUP_ROOTID;
        mainGuideId=null;
        rootName="GuideMeUp";
        resourceHierarchy=new String[] {IConfig.ROOT_ID};
        baseServer= "http://www.guidemeup.com";
        baseServerGuides = baseServer+"/guides";
        baseOnlineMap="http://"+OSM_BASE_TILES[0]+"/";
        baseOnlineExtension="png";
        appType=APP_TYPE.GUIDE;
        showMap=true;
        defaultMapModeOffline=false;
        inactiveCenterThresholdSecs=null;
        allwaysShowSelectedRouteMode=false;
        payMapsAllowed=false;
        //default attribs values

        setAttributeArray(IConfig.TYPE_ORDER_PRIOR, new String[]{PlaceElement.TYPE_GUIDE,
                PlaceElement.TYPE_GROUP,
                PlaceElement.TYPE_PLACE,
                PlaceElement.TYPE_ROUTE,
                PlaceElement.TYPE_MULTIMEDIA,
                PlaceElement.TYPE_INDOOR});


        setAttributeArray(IConfig.ORDER_DEFINITION,new  String[] {OrderDefinition.PRIOR_RATTING,
                OrderDefinition. PRIOR_PREDEFINED,OrderDefinition.PRIOR_DISTANCE,OrderDefinition.PRIOR_TITLE});
        setAttribute(IConfig.CLEAN_CENTER_ON_MICROSITE,"true");

    }
    public void setAttributeArray(String name,String[] values)
    {       setAttribute(name,Utils.arrayToValue(values));

    }
    public String[] getAttributeArray(String name)
    {       return Utils.valueToArray(getAttribute(name));

    }
    public void setAttribute(String name,String value)
    {
        attribs.put(name, value);

    }
    public String getAttribute(String name)
    {
        //try to get from current dao and guide
        try
        {
            String value= Controller.getInstance().getDao().load(Controller.getInstance().getDao().getBaseGuideId()).getAttributes().get(name);
            if(!Utils.isEmpty(value)) return value;

        }catch (Exception ign){}

        return attribs.get(name);
    }

    public boolean isDefaultMapModeOffline() {
        return defaultMapModeOffline;
    }

    public void setDefaultMapModeOffline(boolean defaultMapModeOffline) {
        this.defaultMapModeOffline = defaultMapModeOffline;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public String getRootId() {
        return rootId;
    }

    public void setRootId(String rootId) {
        this.rootId = rootId;
    }

    public String getMainGuideId() {
        return mainGuideId;
    }

    public void setMainGuideId(String mainGuideId) {
        this.mainGuideId = mainGuideId;
    }

    public String getRootName() {
        return rootName;
    }

    public void setRootName(String rootName) {
        this.rootName = rootName;
    }

    public String[] getResourceHierarchy() {
        return resourceHierarchy;
    }

    public void setResourceHierarchy(String[] resourceHierarchy) {
        this.resourceHierarchy = resourceHierarchy;
    }

    public String getBaseServerGuides() {
        return baseServerGuides;
    }

    public void setBaseServerGuides(String baseServerGuides) {
        this.baseServerGuides = baseServerGuides;
    }

    public String getBaseServer() {
        return baseServer;
    }

    public void setBaseServer(String baseServer) {
        this.baseServer = baseServer;
    }

    public APP_TYPE getAppType() {
        return appType;
    }

    public boolean showMap() {
        return showMap;
    }

    public void setShowMap(boolean showMap) {
        this.showMap = showMap;
    }

    public void setAppType(APP_TYPE appType) {
        this.appType = appType;
    }

    public String getBaseOnlineMap() {
        return baseOnlineMap;
    }

    public String getBaseOnlineExtension() {
        return baseOnlineExtension;
    }

    public void setBaseOnlineExtension(String baseOnlineExtension) {
        this.baseOnlineExtension = baseOnlineExtension;
    }

    public void setBaseOnlineMap(String baseOnlineMap) {
        this.baseOnlineMap = baseOnlineMap;
    }

    public boolean getShowDistance() {
        return showDistance;
    }

    public void setShowDistance(boolean showDistance) {
        this.showDistance = showDistance;
    }

    public Integer getInactiveCenterThresholdSecs() {
        return inactiveCenterThresholdSecs;
    }

    public boolean allwaysShowSelectedRouteMode() {
        return  allwaysShowSelectedRouteMode;
    }

    public void setInactiveCenterThresholdSecs(Integer inactiveCenterThresholdSecs) {
        this.inactiveCenterThresholdSecs = inactiveCenterThresholdSecs;
    }

    public void setAllwaysShowSelectedRouteMode(boolean bool)
    {
        this.allwaysShowSelectedRouteMode=bool;

    }

    public int getNotSelectedRouteAlpha() {
        return notSelectedRouteAlpha;
    }

    public void setNotSelectedRouteAlpha(int notSelectedRouteAlpha) {
        this.notSelectedRouteAlpha = notSelectedRouteAlpha;
    }

    public boolean isPayMapsAllowed() {
        return payMapsAllowed;
    }

    public void setPayMapsAllowed(boolean payMapsAllowed) {
        this.payMapsAllowed = payMapsAllowed;
    }

    public PurchaseManager getPurchaseManager() {
        return purchaseManager;
    }

    public void setPurchaseManager(PurchaseManager purchaseManager) {
        this.purchaseManager = purchaseManager;
    }

    public int getTheme() {
        return theme;
    }

    public void setTheme(int theme) {
        this.theme = theme;
    }

    public List<String> getLocationProviders() {
        return locationProviders;
    }

    public void setLocationProviders(List<String> locationProviders) {
        this.locationProviders = locationProviders;
    }
}
