package org.gmu.config;

import org.gmu.inappbilling.PurchaseManager;

import java.util.List;

/**
 * User: ttg
 * Date: 26/03/13
 * Time: 10:42
 * To change this template use File | Settings | File Templates.
 */
public  interface IConfig
{

    public static String ROOT_ID= Constants.GUIDEMEUP_ROOTID;
    public static enum APP_TYPE {
        STORE, GUIDE
    }
    public String getPackageName();
    public String getRootId();
    public String getMainGuideId();
    public String getRootName();
    public String[] getResourceHierarchy();
    public String getBaseServerGuides();
    public String getBaseServer();
    public APP_TYPE getAppType();
    public boolean showMap();
    public String getBaseOnlineMap();
    public String getBaseOnlineExtension();
    public boolean isDefaultMapModeOffline();

    public void setDefaultMapModeOffline(boolean defaultMapModeOffline);

    public boolean getShowDistance();

    public void setShowDistance(boolean showDistance);

    public Integer getInactiveCenterThresholdSecs();
    public boolean allwaysShowSelectedRouteMode();
    public void setAllwaysShowSelectedRouteMode(boolean bool);

    public int getNotSelectedRouteAlpha();

    public void setNotSelectedRouteAlpha(int notSelectedRouteAlpha);

    public boolean isPayMapsAllowed();
    public void setPayMapsAllowed(boolean payMapsAllowed);

    public PurchaseManager getPurchaseManager();
    public void setPurchaseManager(PurchaseManager purchaseManager);
    public int getTheme();
    public void setTheme(int theme);
    public List<String> getLocationProviders();
    public void setLocationProviders(List<String> locationProviders);

    public String getAttribute(String name);
    public void setAttribute(String name, String value);
    public void setAttributeArray(String name, String[] values);
    public String[] getAttributeArray(String name);
    /**Attributes**/

     public static final String TYPE_ORDER_PRIOR="cfg_type_order_prior";
     public static final String TYPE_ORDER_PRIOR_SAME_PRIOR="SAME_PRIOR";
     public static final String ORDER_DEFINITION="cfg_order_definition";
     public static final String CLEAN_CENTER_ON_MICROSITE="cfg_clean_center_onms";

}
