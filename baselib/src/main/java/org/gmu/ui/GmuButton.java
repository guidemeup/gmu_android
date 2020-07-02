package org.gmu.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.Button;
import org.gmu.config.Constants;
import org.gmu.control.Controller;

/**
 * User: ttg
 * Date: 27/02/13
 * Time: 12:31
 * To change this template use File | Settings | File Templates.
 */
public class GmuButton extends Button {
    private static final String SCHEMANAME = "http://schemas.android.com/apk/res-auto";

    public GmuButton(Context context) {
        super(context);
    }

    public GmuButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        updateFonts(attrs);
    }

    public GmuButton(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);


    }

    private void updateFonts(AttributeSet attrs) {
        String ttfName = null;
        String ttfStyle = null;
        for (int i = 0; i < attrs.getAttributeCount(); i++) {


            ttfName = attrs.getAttributeValue(
                    SCHEMANAME, "ttf_name");

            ttfStyle = attrs.getAttributeValue(
                    SCHEMANAME, "ttf_style");

        }
        Controller.getInstance().updateFont(this, ttfName, ttfStyle);
    }
}
