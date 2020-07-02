package org.gmu.activities.alpha.rtk;


import org.gmu.config.ConfigBean;
import org.gmu.config.Constants;

/**
 * User: ttg
 * Date: 26/03/13
 * Time: 10:51
 * To change this template use File | Settings | File Templates.
 */
public class Config extends ConfigBean {

    public static  String PACKAGENAME= "org.gmu.activities.alpha.rtk";
    public static  String ROOT_ID="rtk-root";
    public static  String MAIN_GUIDEID=null;
    public static  String ROOT_NAME="Senderisme i Teca";
    public static String[] RESOURCEHIERARCHY=new String[] {ROOT_ID,Constants.GUIDEMEUP_ROOTID};


    public static String BASE_SERVER= "http://www.guidemeup.com";
    public static String BASE_SERVER_GUIDES = BASE_SERVER+"/guides";

    public Config()
    {
        super();
        setPackageName( PACKAGENAME);
        setRootId(ROOT_ID);
        setMainGuideId(MAIN_GUIDEID);
        setRootName(ROOT_NAME);
        setResourceHierarchy(RESOURCEHIERARCHY);
        setBaseServer(BASE_SERVER);
        setBaseServerGuides(BASE_SERVER_GUIDES);
        setAppType(APP_TYPE.STORE);

    } }