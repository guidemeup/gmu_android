package org.gmu.ui;

import android.content.Context;
import android.util.AttributeSet;

import androidx.appcompat.widget.SearchView;

/**
 * User: ttg
 * Date: 7/02/13
 * Time: 11:24
 * superclass to detect collapse action
 */
public class PlaceSearchView extends SearchView {
    private OnCollapseListener onCollapseListener = null;

    public PlaceSearchView(Context context) {
        super(context, null);
    }

    public PlaceSearchView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setOnCollapseListener(OnCollapseListener l) {
        onCollapseListener = l;
    }

    @Override
    public void onActionViewCollapsed() {
        if (onCollapseListener != null) {
            onCollapseListener.onCollapse();
        }
        super.onActionViewCollapsed();
    }


    public interface OnCollapseListener {
        /**
         * The user is attempting to close the SearchView is collapse.
         */
        void onCollapse();
    }

}




