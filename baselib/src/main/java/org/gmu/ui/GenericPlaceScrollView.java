package org.gmu.ui;
import android.content.Context;
import android.util.AttributeSet;
import android.widget.ScrollView;
import org.gmu.control.Controller;

/**
 * User: ttg
 * Date: 15/06/13
 * Time: 23:48
 * To change this template use File | Settings | File Templates.
 */
public class GenericPlaceScrollView  extends ScrollView
{

    private String UID=null;

    public GenericPlaceScrollView (Context context) {
        super(context);
    }

    public GenericPlaceScrollView (Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public GenericPlaceScrollView (Context context, AttributeSet attrs) {
        super(context, attrs);
    }
    public void setPlaceUID(String UID)
    {      this.UID=UID;

    }


    @Override
    protected void onScrollChanged(int x, int y, int oldx, int oldy)
    {
        super.onScrollChanged(x, y, oldx, oldy);
       try{
           //store scroll
           Controller.getInstance().setScrollIndex(UID,y);

       }catch (Exception ign){}

    }

}
