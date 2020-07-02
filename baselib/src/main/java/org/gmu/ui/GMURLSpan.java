package org.gmu.ui;

import android.content.Intent;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.text.style.ClickableSpan;
import android.text.style.URLSpan;
import android.view.View;
import org.gmu.config.Constants;
import org.gmu.control.Controller;
import org.gmu.dao.IPlaceElementDAO;
import org.gmu.pojo.PlaceElement;
import org.gmu.track.Tracker;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * User: ttg
 * Date: 29/05/13
 * Time: 14:04
 * To change this template use File | Settings | File Templates.
 */
public class GMURLSpan extends URLSpan
{

    public GMURLSpan(String url) {
        super(url);
    }

    public void onClick(View widget)
    { //check if there's a place with this url

        PlaceElement pl = new PlaceElement();
        pl.getAttributes().put("url", this.getURL());
        List<PlaceElement> found = Controller.getInstance().getDao().list(pl,null ,
                IPlaceElementDAO.ITEMS_WITH_URL, 999, null);
        if (found.size() > 0)
        {
            //go to detail
            PlaceElement elem= found.get(0);

            if(elem.getType().equals(PlaceElement.TYPE_MULTIMEDIA))
            {      //open in intent (maximize img)
                String uri = Controller.getInstance().getDao().getRelatedFileDescriptor(elem.getAttributes().get("main_img"));
                File file = new File(uri);

                Intent intent = new Intent();
                intent.setAction(Intent.ACTION_VIEW);

                intent.setDataAndType(Uri.fromFile(file), "image/*");
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                Controller.getInstance().getContext().startActivity(intent);
            } else
            {
                Controller.getInstance().goToDetail(elem.getUid());
            }

        }else
        {
            //track click and open in browser
            LinkedHashMap<String,String> params=new LinkedHashMap<String,String>();
            String text="guideuid:"+Controller.getInstance().getDao().getBaseGuideId()+"#url:"+this.getURL();
            params.put("name",text);
            Tracker.getInstance().sendEvent(Controller.getInstance().getDao().getBaseGuideId(),"user_action", "link_open", params);

            super.onClick(widget);
        }
    }
}
