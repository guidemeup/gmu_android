package org.gmu.fragments;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.os.Bundle;

import androidx.fragment.app.DialogFragment;


/**
 * User: ttg
 * Date: 25/09/2014
 * Time: 17:03
 * To change this template use File | Settings | File Templates.
 */
public class LoadingDialogFragment extends DialogFragment {
    ProgressDialog _dialog;
    public LoadingDialogFragment() {
        // use empty constructors. If something is needed use onCreate's
    }

    @Override
    public Dialog onCreateDialog(final Bundle savedInstanceState) {

        _dialog = new ProgressDialog(getActivity());
        this.setStyle(STYLE_NO_TITLE, getTheme()); // You can use styles or inflate a view
        _dialog.setMessage("Loading.."); // set your messages if not inflated from XML

        _dialog.setCancelable(false);

        return _dialog;
    }
}