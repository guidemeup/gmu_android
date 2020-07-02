package org.gmu.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.widget.LinearLayout;

/**
 * User: ttg
 * Date: 18/02/13
 * Time: 17:11
 * To change this template use File | Settings | File Templates.
 */
public class MapLayout extends LinearLayout {
    Paint textPaint = new Paint();

    public MapLayout(Context context) {
        super(context);
        setDrawP();
    }

    public MapLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        setDrawP();
    }

    public MapLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setDrawP();
    }

    private void setDrawP() {
        // Create out paint to use for drawing
        textPaint.setARGB(255, 200, 0, 0);
        textPaint.setTextSize(60);
        // This call is necessary, or else the
        // draw method will not be called.
        setWillNotDraw(false);
    }

    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        canvas.drawText("Hello World!", 50, 50, textPaint);
    }
}
