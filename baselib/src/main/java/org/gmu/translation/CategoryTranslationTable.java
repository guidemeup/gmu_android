package org.gmu.translation;

import android.content.Context;
import android.widget.TextView;
import org.gmu.base.R;
import org.gmu.utils.Utils;

import java.util.HashMap;
import java.util.Map;

/**
 * User: ttg
 * Date: 27/03/13
 * Time: 15:31
 * To change this template use File | Settings | File Templates.
 */
public class CategoryTranslationTable
{

    public static void setAttributeTranslation(Context context,TextView view,String attributeName)
    {   int resId= Utils.getStringResource(context, "cat_"+attributeName);

        if(resId!=0)
        {         view.setText(context.getString(resId));

        } else
        {   view.setText(attributeName);

        }
    }


}
