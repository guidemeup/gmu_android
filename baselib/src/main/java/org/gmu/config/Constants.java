package org.gmu.config;

/**
 * User: ttg
 * Date: 22/03/13m
 * Time: 12:57
 * To change this template use File | Settings | File Templates.
 */
public class Constants {
    public static String GUIDEMEUP_ROOTID = "root";
    public static final boolean AUDIO_GUIDE_ENABLED = false;

    public static final int INFOWINDOWDPI = 80;
    public static final int ICONSIZEDPI = 30;


    public static final int DETAILDPI =150;
    public static final int PLACE_THUMBNAILDPI = DETAILDPI;    // same size , better quality but less space in cache
    public static final int GUIDE_THUMBNAILDPI = DETAILDPI;
    public static final String BASEMAPDIR = "cache";


    public static final int MAXDOWNLOADWORKERS = 12;
    public static final int PLACES_CACHE = 2000;
    public static final int RELATIONS_CACHE = 6000;

    public static final String VERSIONFILENAME = "version.txt";
    public static final String UPDATEFILENAME = "update.txt";
    public static final long MINIMUM_UPDATE_PERIOD = 60 * 1000;
    public static final boolean SHOWINFOINGUIDELIST = false;

    public static final double HEIGHT_PLACE_DETAIL_RATIO = 9.0 / 16.0;
    public static final double HEIGHT_GUIDE_DETAIL_RATIO = 1.5/ 3.0;
    public static final Integer DEFAULTORIENTATION = null;
    public static final String SPECIALATTRIBS_PREFIX="gmuextra_";


    public static final String LAYOUT_PLACE_SLIDER="place_slider";

    public static final String[] RT_VALUES=new String[]{"rt_ts","rt_status","custom_icon","id"};

    public static  String SOLR_QUERY= "/gmusrch/select/?version=2.2&start=0&rows=1000&wt=json&fl={return_att}";
    public static  String SOLR_FILTER="rt_ts:[{rt_ts} TO *] AND (id:({uids}))";
    public static final String GET_TOKEN_QUERY="/gmusrv/access";

    static
    {   String filter=RT_VALUES[0];
        for (int i = 1; i < RT_VALUES.length; i++) {
            String rtValue = RT_VALUES[i];
            filter=filter+","+rtValue;
        }
        SOLR_QUERY=SOLR_QUERY.replace("{return_att}",filter);
    }
    public static final String TOKEN_PREFIX ="gmut_";
    public static final String PREPRODUCTION_GUIDE_SUFFIX="_pre";
    public static final String PREPRODUCTION_ATTRIB="preproduction";
   public static final String EMPTYCATEGORY="empty";

   public static final String LINESTYLE_THIN="thinline";


}
