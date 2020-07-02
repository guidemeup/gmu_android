package org.gmu.inappbilling.dummy;

import android.app.Activity;

import org.gmu.inappbilling.PurchaseItem;
import org.gmu.inappbilling.PurchaseListener;
import org.gmu.inappbilling.PurchaseManager;

import java.util.HashMap;
import java.util.List;

/**
 * Created by ttg on 20/01/2015.
 */
public class DummyPurchaseManager implements PurchaseManager
{




    public void queryItems(List<String> items, PurchaseListener listener,final Activity activity)
    {
        HashMap<String,PurchaseItem> ret=new HashMap<String, PurchaseItem>();
        for (int i = 0; i < items.size(); i++) {
            String itemId = items.get(i);
            PurchaseItem p=new PurchaseItem();
            p.price=i+" Eur";
            p.purchased=false;
            p.id=itemId;
            ret.put(itemId,p);

        }
        listener.onQueryEnds(ret, PurchaseListener.RESULT_OK);
    }

    public void startPurchase(String itemId,  PurchaseListener listener,final Activity activity) {
        PurchaseItem p=new PurchaseItem();
        p.price="0 Eur";
        p.purchased=true;
        p.id=itemId;
        listener.onPurchaseEnds(p, PurchaseListener.RESULT_OK);
    }

    public void dispose() {

    }
}
