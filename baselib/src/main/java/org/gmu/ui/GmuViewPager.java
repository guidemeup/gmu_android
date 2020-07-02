package org.gmu.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;

import androidx.viewpager.widget.ViewPager;

/**
 * User: ttg
 * Date: 26/04/13
 * Time: 14:20
 * To change this template use File | Settings | File Templates.
 */
public class GmuViewPager extends ViewPager
{   private boolean pageenabled=true;
    public GmuViewPager(Context context) {
        super(context);
    }

    public GmuViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
    }


    float mStartDragX;
    OnSwipeOutListener mListener;


    public void setOnSwipeOutListener(OnSwipeOutListener listener) {
        mListener = listener;
    }


    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if(mListener==null){return super.onInterceptTouchEvent(ev);}
        float x = ev.getX();
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mStartDragX = x;
                break;
            case MotionEvent.ACTION_MOVE:
                if (mStartDragX < x && getCurrentItem() == 0)
                {
                    mListener.onSwipeOutAtStart();

                } else if (mStartDragX > x && getCurrentItem() == getAdapter().getCount() - 1) {
                    mListener.onSwipeOutAtEnd();

                }
                break;
        }
        return super.onInterceptTouchEvent(ev);
    }


    public interface OnSwipeOutListener {
        public void onSwipeOutAtStart();
        public void onSwipeOutAtEnd();
    }

}
