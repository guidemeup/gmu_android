package org.gmu.dao;

import android.location.Location;

import org.gmu.config.IConfig;
import org.gmu.control.Controller;
import org.gmu.utils.Utils;

import java.io.Serializable;
import java.util.*;

/**
 * User: ttg
 * Date: 14/02/13
 * Time: 13:04
 * ordenation parameters to execute DAO list function
 */
public class OrderDefinition implements Serializable {

    public transient Location userLocation = null;
    public static final String PRIOR_DISTANCE="distance";
    public static final String PRIOR_TITLE="title";
    public static final String PRIOR_RATTING="ratting";
    public static final String PRIOR_PREDEFINED="predefined";
    public List<String>  prior;
    ;

    public  OrderDefinition ()
    {
        setPriorByString(null);
    }

    public void updateLocation(Location userLocation) {
        this.userLocation = userLocation;
    }
    public void setPriorByString(String def)
    {
        if(Utils.isEmpty(def))
        {   //set default
            prior= Arrays.asList(new String[] {PRIOR_RATTING,PRIOR_PREDEFINED,PRIOR_DISTANCE,PRIOR_TITLE});

        } else
        {

            prior= Arrays.asList(Utils.valueToArray(def,","));


        }

    }


}
