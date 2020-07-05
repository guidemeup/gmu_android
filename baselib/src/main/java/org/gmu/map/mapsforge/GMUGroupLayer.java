package org.gmu.map.mapsforge;

import android.util.Log;

import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.Point;
import org.mapsforge.map.layer.GroupLayer;
import org.mapsforge.map.layer.Layer;
import org.mapsforge.map.model.DisplayModel;


/**
 * Extends grouplayer to avoid ConcurrentModificationException errors
 */

public class GMUGroupLayer extends GroupLayer {
    private Object writeMutex=new Object();

    private static final String TAG = GMUGroupLayer.class.getName();
    private String name;
    public GMUGroupLayer(String name) {
        super();
        this.name=name;


    }
    @Override
    public void draw(BoundingBox boundingBox, byte zoomLevel, org.mapsforge.core.graphics.Canvas canvas, Point topLeftPoint) {

        synchronized (this.layers)
        {

            try
            {
                super.draw(boundingBox,zoomLevel,canvas,topLeftPoint);
            }catch (Exception ign)
            {   //TODO: mapsforge bug?
               Log.w(TAG,"ignored draw error on layer= "+name,ign);
            }
        }

    }
    public void  addLayer(Layer toAdd)
    {
        synchronized (this.layers)
        {
            this.layers.add(toAdd);
        }
    }
    public void  removeLayer(Layer toRemove)
    {
        synchronized (this.layers)
        {
            this.layers.remove(toRemove);
        }
    }


    @Override
    public void onDestroy() {
        Log.d(TAG,"On edstroy "+this.name);
       
            super.onDestroy();
    
        Log.d(TAG,"On edstroy ends "+this.name);
    }


    @Override
    public boolean onLongPress(LatLong tapLatLong, Point layerXY, Point tapXY) {
        Log.d(TAG,"On longpres "+this.name);
        try{
            return super.onLongPress(tapLatLong,layerXY,tapXY);
        }finally {
            Log.d(TAG,"On longpres ends"+this.name);
        }

    }

    @Override
    public boolean onTap(LatLong tapLatLong, Point layerXY, Point tapXY) {
        Log.d(TAG,"On tap "+this.name);
        try{

            return super.onTap(tapLatLong,layerXY,tapXY);
        }finally {
            Log.d(TAG,"On tap ends"+this.name);
        }

    }

    @Override
    public synchronized void setDisplayModel(DisplayModel displayModel) {
        Log.d(TAG,"On setDispModel "+this.name);
        synchronized (layers)
        { super.setDisplayModel(displayModel);

        }
        Log.d(TAG,"On setDispModel ends"+this.name);
    }




}