package org.gmu.dao;

import org.gmu.utils.Utils;

import java.util.HashMap;

/**
 * User: ttg
 * Date: 12/06/13
 * Time: 18:42
 * To change this template use File | Settings | File Templates.
 */
public class SchemaDAO
{
    private static final String NO_CROP_INDICATOR="" +
            "#no_mainimg_crop";

    private static final String GALLERY_INDICATOR="" +
            "#show_gallery";



    private static HashMap<String,String[]> SCHEMAS=new HashMap();
    static
    {

        SCHEMAS.put("place-news",new String[]{"description", "address", "timetable", "contact", "copyright","author","creation_date"});
        SCHEMAS.put("place",new String[]{"rt_status","description", "address", "timetable", "contact", "copyright","author"});
        SCHEMAS.put("route-news",new String[]{"description", "address", "timetable", "contact", "copyright","author","creation_date"});
        SCHEMAS.put("route",new String[]{"description", "timetable", "contact", "copyright","author"});
        SCHEMAS.put("multimedia",new String[]{"description", "timetable", "contact", "copyright","author"});
        SCHEMAS.put("group",new String[]{"description", "timetable", "contact", "copyright","author"});
        SCHEMAS.put("guide",new String[]{"description", "timetable", "contact", "copyright","author"});

    }
   public static String[] loadSchemaVisibility(String type,String schema)
   {    String[] ret=null;

        if(!Utils.isEmpty(schema))
        {   schema=schema.replace(NO_CROP_INDICATOR,"");
            schema=schema.replace(GALLERY_INDICATOR,"");
        }
        if(!Utils.isEmpty(schema))
        {
            ret=SCHEMAS.get(type+"-"+schema);
        }

       if(ret==null)
       {   //schema not found, return parent
           ret=SCHEMAS.get(type);
       }
       return  ret;

   }

  public static boolean mustCropVertically(String schema)
  {      if(Utils.isEmpty(schema)) return true;
         return !schema.contains(NO_CROP_INDICATOR);


  }

    public static boolean showGallery(String schema)
    {
        if(Utils.isEmpty(schema)) return false;
        return schema.contains(GALLERY_INDICATOR);


    }

}
