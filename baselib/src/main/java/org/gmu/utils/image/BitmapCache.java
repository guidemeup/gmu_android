package org.gmu.utils.image;

import android.app.ActivityManager;
import android.content.Context;
import android.graphics.Bitmap;

import androidx.collection.LruCache;


/**
 * User: ttg
 * Date: 29/11/12
 * Time: 8:57
 * To change this template use File | Settings | File Templates.
 */
public class BitmapCache {

    private LruCache<String, Bitmap> mMemoryCache;

    public BitmapCache(Context context) {
        final int memClass = ((ActivityManager) context.getSystemService(
                Context.ACTIVITY_SERVICE)).getMemoryClass();

        // Use 1/8th of the available memory for this memory cache.
        int cacheSize = 1024 * 1024 * memClass / 8;


        mMemoryCache = new LruCache<String, Bitmap>(cacheSize) {
            @Override
            protected int sizeOf(String key, Bitmap bitmap) {

                return bitmap.getRowBytes() * bitmap.getHeight();


            }

            protected void entryRemoved (boolean evicted,String key, Bitmap bitmap, Bitmap newValue)
            {
                   super.entryRemoved(evicted,key,bitmap,newValue);




            }


        };
    }

    public void addBitmapToMemoryCache(String key, Bitmap bitmap) {
        if (getBitmapFromMemCache(key) == null) {
            mMemoryCache.put(key, bitmap);
        }
    }

    public Bitmap getBitmapFromMemCache(String key) {
        return mMemoryCache.get(key);
    }
    public String totalSize()
    {
        return ""+mMemoryCache.size()+" from " +mMemoryCache.maxSize();
    }
}
