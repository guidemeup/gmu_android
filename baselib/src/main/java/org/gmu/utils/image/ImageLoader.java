package org.gmu.utils.image;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Debug;
import android.util.Log;
import android.widget.ImageView;


import org.gmu.base.R;
import org.gmu.utils.Utils;

import java.io.File;
import java.lang.ref.WeakReference;

/**
 * User: ttg
 * Date: 17/12/12
 * Time: 10:23
 * From:    http://developer.android.com/training/displaying-bitmaps/index.html
 */
public class ImageLoader {

    public static final String SCALEUPTAG="#scaleup#";
    private static final String TAG = ImageLoader.class.getName();
    private Bitmap mPlaceHolderBitmap;
    private BitmapCache bmpcache = null;
    private Context context;

    public ImageLoader(Context context) {
        bmpcache = new BitmapCache(context);
        this.context = context;
        mPlaceHolderBitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.loading_icon);

        //       loadImage("/storage/sdcard0/gmu/places/bcn-guide/image/walking.jpg");
    }


    public Bitmap loadImage(ImageDescriptor img) {
        try {
            if (Utils.isEmpty(img.uri)) return null;
            Log.d("Gallery:", "Load of=" + img.key);
            Log.d("Gallery:", "Current cache Size="+bmpcache.totalSize());

            Log.d("Gallery:", "Heap="+ Debug.getNativeHeapAllocatedSize()+"/"+Debug.getNativeHeapSize());

            Bitmap data = (Bitmap) bmpcache.getBitmapFromMemCache(img.key);
            if (data == null) {
                Log.d("Gmu", "from disk " + img.uri);
                int pxsize = Utils.convertDpToPixel(img.size, context);
                data = ImageUtils.decodeSampledBitmapFromURI(img.uri, pxsize, pxsize,img.key.contains(SCALEUPTAG));
                // Log.e("Gallery:","loaded "+data.getByteCount());
                // context.getResources().getAssets().open(uri));
            }

            bmpcache.addBitmapToMemoryCache(img.key, data);
            WeakReference<Bitmap> ret=new WeakReference<Bitmap> (data);
            return ret.get();


        } catch (Exception ign) {
            //return empty bitmap
            return BitmapFactory.decodeResource(context.getResources(), R.drawable.transparent);

        }

    }

    public void loadInView(ImageView imageView, ImageDescriptor img) {

        if (Utils.isEmpty(img.uri)
                ) return;

        if (cancelPotentialWork(img.key, imageView))
        {
            final BitmapWorkerTask task = new BitmapWorkerTask(imageView);
            final AsyncDrawable asyncDrawable =
                    new AsyncDrawable(context.getResources(), mPlaceHolderBitmap, task);
            imageView.setImageDrawable(asyncDrawable);
            try {
                task.execute(img);
            } catch (java.util.concurrent.RejectedExecutionException exec) {
                //TODO: system overloaded, ignore?
                Log.w(TAG, "on image loading");
            }
        }


    }


    private class BitmapWorkerTask extends AsyncTask<ImageDescriptor, Void, Bitmap> {
        private final WeakReference<ImageView> imageViewReference;
        private String key;

        public BitmapWorkerTask(ImageView imageView) {
            // Use a WeakReference to ensure the ImageView can be garbage collected
            imageViewReference = new WeakReference<ImageView>(imageView);
        }

        // Decode image in background.
        @Override
        protected Bitmap doInBackground(ImageDescriptor... params) {
            key = params[0].key;

            return loadImage(params[0]);


        }


        @Override
        protected void onPostExecute(Bitmap bitmap) {
            if (isCancelled()) {
                bitmap = null;
            }

            if (imageViewReference != null && bitmap != null) {
                final ImageView imageView = imageViewReference.get();
                final BitmapWorkerTask bitmapWorkerTask =
                        getBitmapWorkerTask(imageView);
                if (this == bitmapWorkerTask && imageView != null) {
                    imageView.setImageBitmap(bitmap);
                }
            }
        }

    }


    public static boolean cancelPotentialWork(String key, ImageView imageView) {
        final BitmapWorkerTask bitmapWorkerTask = getBitmapWorkerTask(imageView);

        if (bitmapWorkerTask != null) {
            final String bitmapData = bitmapWorkerTask.key;
            if ((bitmapData != null) && !bitmapData.equals(key)) {
                // Cancel previous task
                bitmapWorkerTask.cancel(true);
            } else {
                // The same work is already in progress
                return false;
            }
        }
        // No task associated with the ImageView, or an existing task was cancelled
        return true;
    }

    private static BitmapWorkerTask getBitmapWorkerTask(ImageView imageView) {
        if (imageView != null) {
            final Drawable drawable = imageView.getDrawable();
            if (drawable instanceof AsyncDrawable) {
                final AsyncDrawable asyncDrawable = (AsyncDrawable) drawable;
                return asyncDrawable.getBitmapWorkerTask();
            }
        }
        return null;
    }


    static class AsyncDrawable extends BitmapDrawable {
        private final WeakReference<BitmapWorkerTask> bitmapWorkerTaskReference;

        public AsyncDrawable(Resources res, Bitmap bitmap,
                             BitmapWorkerTask bitmapWorkerTask) {
            super(res, bitmap);
            bitmapWorkerTaskReference =
                    new WeakReference<BitmapWorkerTask>(bitmapWorkerTask);
        }

        public BitmapWorkerTask getBitmapWorkerTask() {
            return bitmapWorkerTaskReference.get();
        }
    }


}
