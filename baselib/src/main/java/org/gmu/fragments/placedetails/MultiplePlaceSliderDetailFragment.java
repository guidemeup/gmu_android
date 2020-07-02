package org.gmu.fragments.placedetails;

import org.gmu.control.Controller;
import org.gmu.pojo.PlaceElement;
import org.gmu.utils.Utils;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * User: ttg
 * Date: 25/06/13
 * Time: 18:31
 * To change this template use File | Settings | File Templates.
 */
public class MultiplePlaceSliderDetailFragment extends MultiplePlaceDetailFragment
{

    protected void removeNotVisibleElements(List<PlaceElement> result,String parentId)
    {
        List<PlaceElement> toRemove=new LinkedList<PlaceElement>();

            for (int i = 0; i < result.size(); i++) {
                PlaceElement placeElement = result.get(i);

                if(Utils.equals(parentId,placeElement.getUid()))
                {   //first element--> parent

                    if (placeElement.getType().equals(PlaceElement.TYPE_GUIDE) || Utils.isEmpty(placeElement.getAttributes().get("description")))
                    {   //only show parent if it has description and is parent
                         toRemove.add(placeElement);
                    }

                }else
                {   //only show elements
                    if(!placeElement.getType().equals(PlaceElement.TYPE_PLACE))
                    {
                        toRemove.add(placeElement);
                    }


                }
            }
        result.removeAll(toRemove);

    }

    public void onResume()
    {
        super.onResume();
        if (this.UID != null)
        {
               List<PlaceElement> p=  Controller.getInstance().getFilteredItems();
               String parentID="";


               removeNotVisibleElements(p, Controller.getInstance().getGroupFilter());
               this.setUIDs(p);
               if(!UIDs.contains(this.UID))
               {   //uid not in result (direct access from search view):  only show detail from this uid
                   UIDs=new ArrayList<String>();
                   UIDs.add(this.UID);

               }


        }

        //TODO: refresh on resume (no hay manera de hacer que se refresque al restaurar instancia con el pager)!!
        showElements();
    }







}
