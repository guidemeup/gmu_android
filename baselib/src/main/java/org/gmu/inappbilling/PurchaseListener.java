package org.gmu.inappbilling;

import android.app.Activity;

import java.util.Map;

/**
 * Created by ttg on 20/01/2015.
 */
public interface PurchaseListener
{
    public static final int RESULT_OK=1;
    public static final int RESULT_KO=2;

    public void onQueryEnds(Map<String, PurchaseItem> result, int resultCode);

    public void onPurchaseEnds(PurchaseItem purchasedItem, int resultCode);


}
