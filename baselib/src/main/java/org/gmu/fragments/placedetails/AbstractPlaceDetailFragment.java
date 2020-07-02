package org.gmu.fragments.placedetails;

import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import org.gmu.control.Controller;
import org.gmu.control.GmuEventListener;
import org.gmu.dao.IPlaceElementDAO;
import org.gmu.fragments.GMUBaseFragment;
import org.gmu.pojo.PlaceElement;
import org.gmu.ui.GuideDownloadProgressDialog;
import org.gmu.utils.Utils;

import java.util.List;

/**
 * User: ttg
 * Date: 28/02/13
 * Time: 18:26
 * To change this template use File | Settings | File Templates.
 */
public class AbstractPlaceDetailFragment extends GMUBaseFragment {
    protected String UID;


    public void resume(Bundle savedInstanceState) {

        if (Utils.isEmpty(UID)) {
            try {
                UID = savedInstanceState.getString("uid");
            } catch (Exception ign) {
            }
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState)
    {


        super.onSaveInstanceState(outState);
        outState.putString("uid", this.UID);



    }


    public AbstractPlaceDetailFragment() {
        super();

    }

    public void setUID(String UID)
    {

        this.UID = UID;



    }

}
