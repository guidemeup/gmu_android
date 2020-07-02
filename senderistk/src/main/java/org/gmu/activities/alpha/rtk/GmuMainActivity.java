package org.gmu.activities.alpha.rtk;

import android.os.Build;
import android.os.Bundle;
import android.os.StrictMode;

import org.gmu.control.Controller;

import java.lang.reflect.Method;

/**
 * User: ttg
 * Date: 11/03/13
 * Time: 13:44
 * To change this template use File | Settings | File Templates.
 */
public class GmuMainActivity extends org.gmu.base.GmuMainActivity
{
    @Override
    public void onCreate(Bundle savedInstanceState)
    {   Controller.getInstance().setConfig(new Config());
        super.onCreate(savedInstanceState);
        disableFileExposureCheck();


    }
    @Override
    protected void onResume() {
        Controller.getInstance().setConfig(new Config());
        super.onResume();

    }
    @Override
    public void onStart()
    {   Controller.getInstance().setConfig(new Config());
        super.onStart();
    }


}
