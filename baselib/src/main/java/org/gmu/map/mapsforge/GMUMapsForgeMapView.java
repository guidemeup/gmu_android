package org.gmu.map.mapsforge;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.MotionEvent;
import org.gmu.fragments.AbstractMapFragment;
import org.gmu.utils.Log;
import org.mapsforge.map.android.view.MapView;


/**
 * User: ttg
 * Date: 18/02/13
 * Time: 18:11
 * To change this template use File | Settings | File Templates.
 */
public class GMUMapsForgeMapView extends MapView {

    private static final String TAG = AbstractMapFragment.class.getName();
    private AbstractMapFragment.MapEventListener onMapEventListener = null;

    public GMUMapsForgeMapView(Context context) {
        super(context);
        setDrawP();
    }

    public GMUMapsForgeMapView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        setDrawP();
    }




    private void setDrawP() {

        setWillNotDraw(false);
    }

    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (onMapEventListener != null) onMapEventListener.onDraw();
    }

    public void setMapEventListener(AbstractMapFragment.MapEventListener onDrawListener) {
        this.onMapEventListener = onDrawListener;
    }



    @Override
     public void onZoomEvent() {
         super.onZoomEvent();
        if (onMapEventListener != null) {
            onMapEventListener.onZoomLevelChanged();
        }

    }


    @Override
    public void  onMoveEvent() {
        super.onMoveEvent();


    }
    @Override
    public boolean onTouchEvent(MotionEvent motionEvent)
    {   if (onMapEventListener != null) {onMapEventListener.onTouchEvent();}
        int action = motionEvent.getAction();

        if (action != MotionEvent.ACTION_UP)
        {
            prevAction = action;
        }else
        {
           // checkIsMapTapped();
        }

        Boolean r= super.onTouchEvent(motionEvent);

        System.err.println("touch action="+action+" handled="+r);
        return r;


    }



    //begin GMU: map tap logic
    private int prevAction = MotionEvent.ACTION_DOWN;


    private void checkIsMapTapped() {

        //GMU: if tap hasn't been handled call onMapTap
        if (prevAction == MotionEvent.ACTION_DOWN)
        {
            onMapEventListener.onMapTap();


        }
    }

    //end GMU: map tap logic

}
