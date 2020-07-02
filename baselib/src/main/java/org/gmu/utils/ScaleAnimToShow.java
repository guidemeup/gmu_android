package org.gmu.utils;

import android.view.View;
import android.view.ViewGroup;
import android.view.animation.ScaleAnimation;
import android.view.animation.Transformation;


/**
 * User: ttg
 * Date: 19/11/12
 * Time: 12:48
 * To change this template use File | Settings | File Templates.
 */
public class ScaleAnimToShow extends ScaleAnimation {

    private View mView;

    private ViewGroup.MarginLayoutParams mLayoutParams;

    private int mMarginBottomFromY, mMarginBottomToY;

    private boolean mVanishAfter = false;

    public ScaleAnimToShow(float toX, float fromX, float toY, float fromY, int duration, View view, boolean vanishAfter) {
        super(fromX, toX, fromY, toY);

        setDuration(duration);
        mView = view;
        mVanishAfter = vanishAfter;
        mLayoutParams = (ViewGroup.MarginLayoutParams) view.getLayoutParams();
        mView.setVisibility(View.VISIBLE);
        int height = mView.getHeight();
        //mMarginBottomFromY = (int) (height * fromY) + mLayoutParams.bottomMargin + height;
        //mMarginBottomToY = (int) (0 - ((height * toY) + mLayoutParams.bottomMargin)) + height;

        mMarginBottomFromY = 0;
        mMarginBottomToY = height;

    }

    @Override
    protected void applyTransformation(float interpolatedTime, Transformation t) {
        super.applyTransformation(interpolatedTime, t);
        if (interpolatedTime < 1.0f) {
            int newMarginBottom = (int) ((mMarginBottomToY - mMarginBottomFromY) * interpolatedTime) - mMarginBottomToY;
            mLayoutParams.setMargins(mLayoutParams.leftMargin, mLayoutParams.topMargin, mLayoutParams.rightMargin, newMarginBottom);
            mView.getParent().requestLayout();
            //Log.v("CZ","newMarginBottom..." + newMarginBottom + " , mLayoutParams.topMargin..." + mLayoutParams.topMargin);
        }
    }

}