package org.gmu.utils;


import android.graphics.Bitmap;
import android.util.Log;

import org.gmu.config.IConfig;
import org.gmu.control.Controller;
import org.gmu.dao.OrderDefinition;
import org.gmu.pojo.PlaceElement;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

/**
 * User: ttg
 * Date: 16/11/12
 * Time: 16:03
 * To change this template use File | Settings | File Templates.
 */
public class PlaceElementComparator implements Comparator<PlaceElement> {
    public static Map<String, Integer> TYPE_PRIOR = new HashMap();
    private static final String TAG = PlaceElementComparator.class.getName();
    static {
        TYPE_PRIOR.put(PlaceElement.TYPE_GUIDE, 1);
        TYPE_PRIOR.put(PlaceElement.TYPE_GROUP, 1);
        TYPE_PRIOR.put(PlaceElement.TYPE_ROUTE, 1);
        TYPE_PRIOR.put(PlaceElement.TYPE_PLACE, 1);
        TYPE_PRIOR.put(PlaceElement.TYPE_MULTIMEDIA, 1);
        TYPE_PRIOR.put(PlaceElement.TYPE_INDOOR, 1);
    }

    private OrderDefinition orderDefinition;


    public PlaceElementComparator(OrderDefinition _orderDefinition) {

        orderDefinition = _orderDefinition;
        loadTypePrior();


    }
    private void loadTypePrior()
    {
        String prior= Controller.getInstance().getConfig().getAttribute(IConfig.TYPE_ORDER_PRIOR);
        if(!prior.equalsIgnoreCase(IConfig.TYPE_ORDER_PRIOR_SAME_PRIOR))
        {


            String[] tk=Controller.getInstance().getConfig().getAttributeArray(IConfig.TYPE_ORDER_PRIOR);
            for (int i = 0; i < tk.length; i++)
            {
                String type = tk[i];
                TYPE_PRIOR.put(type, i);
            }

        }else
        {
            //no place type priority
        }

    }
    public int compare(PlaceElement o1, PlaceElement o2) {

        //Log.d(TAG, "Compare " + o1 + "|" + o2);

        //1:apply fixed type  priority
        Integer prior1 = TYPE_PRIOR.get(o1.getType());
        Integer prior2 = TYPE_PRIOR.get(o2.getType());
        if (!prior1.equals(prior2))
        {
            return prior1.compareTo(prior2);

        }else
        {

            //apply precedence

            for (int i = 0; i < orderDefinition.prior.size(); i++) {
                String prior = orderDefinition.prior.get(i);

                if (prior.equalsIgnoreCase(OrderDefinition.PRIOR_RATTING)) {
                    // apply rating order
                    if (Utils.isEmpty(o1.getAttributes().get("rating")))
                    {
                        o1.getAttributes().put("rating","0");
                    }
                    if (Utils.isEmpty(o2.getAttributes().get("rating")))
                    {
                        o2.getAttributes().put("rating","0");
                    }

                    if (!Utils.isEmpty(o1.getAttributes().get("rating")) && !Utils.isEmpty(o2.getAttributes().get("rating"))) {
                        int order = Float.compare(new Float(o2.getAttributes().get("rating")), new Float(o1.getAttributes().get("rating")));
                        if (order != 0) {
                            //Log.d(TAG, "Eval rating");
                            return order;
                        }

                    }

                } else if (prior.equalsIgnoreCase(OrderDefinition.PRIOR_PREDEFINED)) {
                    if (!Utils.equals((String) o1.getAttributes().get("tmporder"), (String) o2.getAttributes().get("tmporder"))) {
                        //apply intrinsic priority
                        int ret=((String) o1.getAttributes().get("tmporder")).compareTo((String) o2.getAttributes().get("tmporder"));

                        //Log.d(TAG, "Eval predef");
                        return ret;
                    }

                } else {

                    if (prior.equalsIgnoreCase(OrderDefinition.PRIOR_DISTANCE))
                    {     //Log.d(TAG, "Eval dist");
                        if (o1.getDistanceToUser() != null && o2.getDistanceToUser() != null)
                        {
                            int comp = o1.getDistanceToUser().compareTo(o2.getDistanceToUser());
                            if (comp != 0)
                            {
                                return comp;


                            }
                        } else if (o1.getDistanceToUser() != null && o2.getDistanceToUser() == null) {
                            return -1;
                        } else if (o1.getDistanceToUser() == null && o2.getDistanceToUser() != null) {
                            return 1;
                        }
                    } else if (prior.equalsIgnoreCase(OrderDefinition.PRIOR_TITLE))
                    {    //Log.d(TAG, "Eval title");
                        //3:apply name priority
                        int order = o1.getTitle().compareTo(o2.getTitle());
                        if (order != 0) {
                            return order;
                        }
                    }


                }
            }
        }


        //default
        return o1.getTitle().compareTo(o2.getTitle());


    }
}