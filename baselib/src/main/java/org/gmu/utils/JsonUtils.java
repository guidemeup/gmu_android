package org.gmu.utils;

import org.json.JSONObject;

/**
 * User: ttg
 * Date: 17/03/14
 * Time: 18:21
 * To change this template use File | Settings | File Templates.
 */
public class JsonUtils
{



    public static Object getObject(JSONObject js, String name,Object def)
    {
       try
       {     if(def instanceof Integer)
            {
                return new Integer(js.getInt(name));
            }else if(def instanceof Boolean)
           {
                return js.getBoolean(name);
           }else
            {
                return js.getString(name) ;

            }

       } catch (Exception ign)
       {
           return  def;
       }


    }



}
