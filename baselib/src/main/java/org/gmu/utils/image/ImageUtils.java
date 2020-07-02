package org.gmu.utils.image;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.util.Log;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.lang.ref.WeakReference;

/**
 * User: ttg
 * Date: 17/12/12
 * Time: 9:58
 * To change this template use File | Settings | File Templates.
 */
public class ImageUtils {


    public static int calculateInSampleSize(
            BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            if (width > height) {
                inSampleSize = Math.round((float) height / (float) reqHeight);
            } else {
                inSampleSize = Math.round((float) width / (float) reqWidth);
            }
        }
        return inSampleSize;
    }

    /**
     * Loads an image without scale
     * @param uri
     * @param reqWidth
     * @param reqHeight
     * @return
     * @deprecated
     * @throws FileNotFoundException
     */
    public static Bitmap decodeSampledBitmapFromURI2(String uri, int reqWidth, int reqHeight) throws FileNotFoundException {

        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;

        BitmapFactory.decodeStream(new FileInputStream(uri), null, options);
        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeStream(new FileInputStream(uri), null, options);
    }


    public static  BitmapFactory.Options readImageMetadata(String uri) throws FileNotFoundException
    {

        // decode image size (decode metadata only, not the whole image)
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(new FileInputStream(uri), null, options);
        return options;

    }

    public static Bitmap decodeSampledBitmapFromURI(String uri, int reqWidth, int reqHeight,boolean scaleUp) throws FileNotFoundException
    {

        int inWidth = 0;
        int inHeight = 0;



        BitmapFactory.Options options=   readImageMetadata( uri);



        // save width and height
        inWidth = options.outWidth;
        inHeight = options.outHeight;


        // decode full image pre-resized

        options = new BitmapFactory.Options();
        // calc rought re-size (this is no exact resize)
        options.inSampleSize = Math.max(inWidth/reqWidth, inHeight/reqHeight);
        if(!scaleUp&&options.inSampleSize<1)
        {   //if req image > real image, preserve size
            reqWidth=inWidth;
            reqHeight=inHeight;
            options.inSampleSize=1;

        }
        // decode full image
        WeakReference<Bitmap> bm= new WeakReference<Bitmap>( BitmapFactory.decodeStream(new FileInputStream(uri), null, options));
        Bitmap roughBitmap =bm.get();

        // calc exact destination size
        Matrix m = new Matrix();
        RectF inRect = new RectF(0, 0, roughBitmap.getWidth(), roughBitmap.getHeight());
        RectF outRect = new RectF(0, 0, reqWidth, reqHeight);
        m.setRectToRect(inRect, outRect, Matrix.ScaleToFit.CENTER);
        float[] values = new float[9];
        m.getValues(values);

        // resize bitmap
        return  Bitmap.createScaledBitmap(roughBitmap, (int) (roughBitmap.getWidth() * values[0]), (int) (roughBitmap.getHeight() * values[4]), true);

    }



}
