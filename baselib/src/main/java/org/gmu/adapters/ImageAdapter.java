package org.gmu.adapters;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Gallery;
import android.widget.ImageView;
import org.gmu.config.Constants;
import org.gmu.control.Controller;

import java.util.HashSet;
import java.util.Iterator;

/**
 * User: ttg
 * Date: 17/11/12
 * Time: 10:40
 * To change this template use File | Settings | File Templates.
 */
public class ImageAdapter extends BaseAdapter {
    /**
     * The parent context
     */
    private Context myContext;
    // Put some images to project-folder: /res/drawable/
    // format: jpg, gif, png, bmp, ...
    private String[] myImageIds = {};

    /**
     * Simple Constructor saving the 'parent' context.
     */
    public ImageAdapter(Context c, String[] imageURIS) {
        this.myContext = c;
        //remove repeated images
        HashSet images = new HashSet();
        for (int i = 0; i < imageURIS.length; i++) {
            images.add(imageURIS[i]);

        }
        this.myImageIds = new String[images.size()];

        int i = 0;
        for (Iterator iterator = images.iterator(); iterator.hasNext(); ) {
            String next = (String) iterator.next();
            this.myImageIds[i] = next;
            i++;
        }

    }

    // inherited abstract methods - must be implemented
    // Returns count of images, and individual IDs
    public int getCount() {
        return this.myImageIds.length;
    }

    public Object getItem(int position) {
        return position;
    }

    public long getItemId(int position) {
        return position;
    }

    // Returns a new ImageView to be displayed,
    public View getView(int position, View convertView,
                        ViewGroup parent) {

        // Get a View to display image data
        ImageView iv = new ImageView(this.myContext);


        // iv.setImageResource(this.myImageIds[position]);

        // Image should be scaled somehow
        //iv.setScaleType(ImageView.ScaleType.CENTER);
        //iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
        iv.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        //iv.setScaleType(ImageView.ScaleType.FIT_CENTER);
        //iv.setScaleType(ImageView.ScaleType.FIT_XY);
        //iv.setScaleType(ImageView.ScaleType.FIT_END);

        // Set the Width & Height of the individual images
        iv.setLayoutParams(new Gallery.LayoutParams(200, 200));
        Controller.getInstance().getDao().loadImageInView(iv, this.myImageIds[position], Constants.DETAILDPI);
        return iv;
    }
}// ImageAdapter