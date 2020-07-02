package org.gmu.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;
import org.gmu.control.Controller;

/**
 * User: ttg
 * Date: 27/02/13
 * Time: 12:31
 * To change this template use File | Settings | File Templates.
 */
public class GmuTextView extends TextView {
    private static final String SCHEMANAME = "http://schemas.android.com/apk/res-auto";

    public GmuTextView(Context context) {
        super(context);
    }

    public GmuTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        updateFonts(attrs);
    }

    public GmuTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        updateFonts(attrs);


    }

    private void updateFonts(AttributeSet attrs) {
        String ttfName = null;
        String ttfStyle = null;
        String selectable = null;
        for (int i = 0; i < attrs.getAttributeCount(); i++) {

            selectable = attrs.getAttributeValue(
                    SCHEMANAME, "ttf_selectable");
            ttfName = attrs.getAttributeValue(
                    SCHEMANAME, "ttf_name");

            ttfStyle = attrs.getAttributeValue(
                    SCHEMANAME, "ttf_style");

        }
        if (selectable != null) {
            this.setOnLongClickListener(new OnLongClickListener() {

                public boolean onLongClick(View v) {
                    GmuTextView.this.setCursorVisible(true);
                    return false;
                }
            });

        }


        Controller.getInstance().updateFont(this, ttfName, ttfStyle);
    }
}
