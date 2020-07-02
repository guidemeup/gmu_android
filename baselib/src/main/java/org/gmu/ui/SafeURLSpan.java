package org.gmu.ui;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Parcel;
import android.provider.Browser;
import android.text.style.URLSpan;
import android.view.View;

/**
 * User: ttg
 * Date: 5/03/13
 * Time: 13:49
 * deals onclick errors on invalid urls
 */
public class SafeURLSpan extends URLSpan {
    public SafeURLSpan(String url) {
        super(url);
    }

    public SafeURLSpan(Parcel src) {
        super(src);
    }

    @Override
    public void onClick(View widget) {
        Uri uri = Uri.parse(getURL());
        Context context = widget.getContext();
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        intent.putExtra(Browser.EXTRA_APPLICATION_ID, context.getPackageName());
        try {
            context.startActivity(intent);
        } catch (ActivityNotFoundException ign) {
            System.out.println("error");

        }
    }
}
