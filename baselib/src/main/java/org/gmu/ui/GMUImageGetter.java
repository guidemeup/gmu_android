package org.gmu.ui;

import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.text.Html;
import android.view.Gravity;

import org.gmu.base.R;
import org.gmu.control.Controller;
import org.gmu.dao.IPlaceElementDAO;
import org.gmu.pojo.PlaceElement;
import org.gmu.utils.Utils;

import java.util.List;

/**
 * User: ttg
 * Date: 4/12/13
 * Time: 19:45
 * To change this template use File | Settings | File Templates.
 */
public class GMUImageGetter implements Html.ImageGetter
{
    //set max to avoid memory problems (TODO: do lazy load/unload on images and increase)
    private static final int MAXIMAGERESOLUTION=640;
    private static int DEFAULT=1;
    //adjust to 1:1 dip and align left
    private static int SCALEDIMGSIZE=3;
    //adjust to 1:1 dip, fill screen width and align center
    private static int CENTERINORIGINALPX_VISUAL=2;

    int selectedType= SCALEDIMGSIZE;
    private String ignoredResource ="";
    private int width=-1;
    private int imgwidth=-1;
    private int targetDps=-1;
    private Context context;
    private  android.content.res.Resources resources;
    public void setWidth(int width)
    {
        this.width=width;
    }
    public void setImgWidth(int width)
    {
        this.imgwidth=width;
        if(this.imgwidth>MAXIMAGERESOLUTION) this.imgwidth=MAXIMAGERESOLUTION;
    }

    public void setContext(Context context) {
        this.context = context;
        this.resources = context.getResources();
    }

    public void setIgnoredResource(String ignoredResource) {
       if(!Utils.isEmpty(ignoredResource))     this.ignoredResource = ignoredResource;
    }

    public Drawable getDrawable(String source)
    {   //locate image on related images in the guide
        Drawable drawable = null;
        PlaceElement pl = new PlaceElement();
        pl.getAttributes().put("url", source);
        List<PlaceElement> found = Controller.getInstance().getDao().list(pl, null,
                IPlaceElementDAO.PLACE_RELATED_ITEMS, 999, null);
        if ((found.size() > 0))
        {
             if(targetDps==-1)
             {
                 targetDps= Utils.convertPixelToDp(imgwidth,context);

             }


            if(Utils.equals(found.get(0).getAttributes().get("main_img"),ignoredResource))
            {   //ignore image (main img) -->return empty image
                return  getEmpty();
            } else
            {
                drawable = new BitmapDrawable(resources, Controller.getInstance().getDao().loadImage(found.get(0).getAttributes().get("main_img"), targetDps));

            }


        } else
        {   //not found: put transparent resource
            return  getEmpty();
        }
        // Important
        //set rectangle to fill text view == center image

        ((BitmapDrawable)drawable).setGravity(Gravity.CENTER_HORIZONTAL);

        setDrawableBounds(drawable);


        return drawable;
    }

    private Drawable getEmpty()
    {

       Drawable drawable = resources.getDrawable(R.drawable.transparent);
       drawable.setBounds(0, 0, 0, 0);
       return drawable;
    }




    public void  setDrawableBounds(Drawable drawable)
    {
         setDrawableBounds(drawable,drawable.getIntrinsicHeight(),drawable.getIntrinsicWidth());
    }

    public void  setDrawableBounds(Drawable drawable,int drawableHeight,int drawableWidth)
    {
        int targetWidth=width;
        int targetHeight=drawableHeight;

        if(selectedType==CENTERINORIGINALPX_VISUAL)
        {
            ((BitmapDrawable)drawable).setGravity(Gravity.FILL);
            targetWidth=Utils.convertDpToPixel(drawableWidth,context);
            targetHeight=Utils.convertDpToPixel(drawableHeight,context);

            if(targetWidth>width)
            {   targetHeight=(targetHeight*width)/targetWidth;
                targetWidth=width;

            }
            int  right= targetWidth+((width-targetWidth)/2);
            if(right>width) right=targetWidth;

            drawable.setBounds((width-targetWidth)/2, 0, right, targetHeight);
        }  else   if(selectedType==SCALEDIMGSIZE)
        {
            ((BitmapDrawable)drawable).setGravity(Gravity.FILL);
            targetWidth=Utils.convertDpToPixel(drawableWidth,context);
            targetHeight=Utils.convertDpToPixel(drawableHeight,context);

            if(targetWidth>width)
            {   targetHeight=(targetHeight*width)/targetWidth;
                targetWidth=width;

            }
            int  right= targetWidth;
            if(right>width) right=targetWidth;

            drawable.setBounds(0, 0, right, targetHeight);

        }else  if(selectedType==DEFAULT)
        {   //no scale to 1:1 dpi
            int  right= targetWidth+((width-targetWidth)/2);
            if(right>width) right=targetWidth;

            drawable.setBounds((width-targetWidth)/2, 0, right, targetHeight);

        }


    }
}
