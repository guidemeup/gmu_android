package org.gmu.fragments;

import android.app.ProgressDialog;
import android.content.DialogInterface;

import android.util.Log;

import androidx.fragment.app.Fragment;

import org.gmu.base.GmuFragmentActivity;

import org.gmu.base.R;
import org.gmu.control.Controller;
import org.gmu.control.GmuEventListener;
import org.gmu.ui.GuideDownloadProgressDialog;

/**
 * User: ttg
 * Date: 18/01/13
 * Time: 17:19
 * To change this template use File | Settings | File Templates.
 */
public class GMUBaseFragment extends Fragment {
    private static final String TAG = GMUBaseFragment.class.toString();
    private ProgressDialog progress;



    public void onResume() {
        dismissLoadingDialog();
        super.onResume();
        ((GmuFragmentActivity) this.getActivity()).onFragmentResumed(this);

    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Log.d(TAG, "Fragment destroyed:" + this);

    }


    protected void onAccessToGuideClick(final String guideUID)
    {
       onAccessToGuideClick( guideUID,true);
    }
    protected void onAccessToGuideClick(final String guideUID,final boolean backToGuideList)
    {


        GuideDownloadProgressDialog dialog = new GuideDownloadProgressDialog(this.getActivity(), guideUID,true);
        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            public void onDismiss(DialogInterface dialog) {
                if (((GuideDownloadProgressDialog) dialog).showSyncMessages(true)) {
                    switchGuide(guideUID,backToGuideList);
                }else
                {   //clear pushed script invocation
                    Controller.getInstance().getGmuContext().targetedScriptInvocations.remove(guideUID);

                }
            }
        });

    }
    protected void switchGuide(String uid,final boolean backToGuideList) {
        if(backToGuideList)
        {
            //pop detail from stack (back press will go to guide list)
            Controller.getInstance().popDetail();
            Controller.getInstance().setCurrentView(GmuEventListener.VIEW_GUIDE_LIST);
        }


        //push detail to guide
        Controller.getInstance().pushDetail(uid);
        //switch dao
        Controller.getInstance().switchGuide(uid);
        //start with microsite
        Controller.getInstance().switchView(GmuEventListener.VIEW_MICROSITE);
    }



    protected void showLoadingDialog() {

        if (progress == null) {
            progress = new ProgressDialog(this.getActivity());
            progress.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            progress.setIndeterminate(true);
            progress.setMessage(getString(R.string.syncguide));
            progress.setCancelable(false);
        }
        progress.show();
    }

    protected void dismissLoadingDialog() {

        if (progress != null && progress.isShowing()) {
            progress.dismiss();
        }
    }


}
