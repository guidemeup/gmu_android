package org.mapsforge.map.android.graphics;

import android.graphics.PathEffect;


/**
 *Extension of default MapsForge Paint in order to use some core Android Paint  features
 */
public class GMUPaint  extends  org.mapsforge.map.android.graphics.AndroidPaint{

    public GMUPaint() {
        super();

    }
    public void setPathEffect(PathEffect pE )
    {
        this.paint.setPathEffect(pE);
    }
    public void setAlpha(int a) {
        this.paint.setAlpha(a);
    }
}
