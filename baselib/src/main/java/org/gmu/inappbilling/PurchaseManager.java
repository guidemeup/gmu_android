package org.gmu.inappbilling;

import android.app.Activity;
import android.content.Context;

import java.util.List;

/**
 * Created by ttg on 20/01/2015.
 */
public interface PurchaseManager
{


    public void queryItems(List<String> items, PurchaseListener listener, Activity activity);
    public void startPurchase(String itemId, PurchaseListener listener, Activity activity);
    public void dispose();
}
