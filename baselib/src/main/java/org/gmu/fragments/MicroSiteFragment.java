package org.gmu.fragments;


import android.os.Bundle;
import android.util.Log;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.*;
import android.widget.LinearLayout;
import org.gmu.base.GmuMainActivity;
import org.gmu.base.R;
import org.gmu.config.IConfig;
import org.gmu.control.Controller;
import org.gmu.control.GmuEventListener;
import org.gmu.pojo.NavigationItem;
import org.gmu.utils.JsonUtils;
import org.gmu.utils.Utils;
import org.json.JSONObject;

import java.io.File;
import java.util.Locale;


/**
 * Created by IntelliJ IDEA.
 * User: acasquero
 * Date: 9/11/12
 * Time: 8:53
 * To change this template use File | Settings | File Templates.
 */
public class MicroSiteFragment extends GMUBaseFragment
{
    private static final String TAG = MicroSiteFragment.class.getName();
    private WebView myWebView;

    private LinearLayout main;
    private String indexPath = null;
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        setRetainInstance(false);
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        main = (LinearLayout) inflater.inflate(R.layout.fragment_welcomeview,
                container, false);
        refresh();

        return main;
    }

    public void onResume() {
        super.onResume();


        //TODO: refresh on resume

         refresh();
    }

    private void refresh()
    {


        resetContext();

        String indexPath2 = getIndexPage();
        if ((indexPath2 == null)||Utils.equals(indexPath2,indexPath))
        {   //don't open url
            return ;
        }
        indexPath=indexPath2;

        myWebView = (WebView) main.findViewById(R.id.mainwebview);
        myWebView.getSettings().setJavaScriptEnabled(true);
        myWebView.getSettings().setLightTouchEnabled(false);
        myWebView.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
        myWebView.getSettings().setAppCacheEnabled(false);

        myWebView.getSettings().setLoadWithOverviewMode(true);
        myWebView.getSettings().setUseWideViewPort(true);

        myWebView.setWebChromeClient(new WebChromeClient() {
            public void onConsoleMessage(String message, int lineNumber, String sourceID) {
                Log.d(TAG, message + " -- From line "
                        + lineNumber + " of "
                        + sourceID);
            }
            public void onReceivedError(WebView view, int errorCode,
                                        String description, String failingUrl) {
                MicroSiteFragment.onReceivedError(view, errorCode, description, failingUrl);
            }

            public boolean onConsoleMessage(ConsoleMessage cm) {

                return MicroSiteFragment.onConsoleMessage(cm);
            }





        });

        myWebView.addJavascriptInterface(new GMUCallbackInterface(), "Android");
        //add multitouch in android 2.3
        myWebView.setWebViewClient(new MyWebClient());
        myWebView.setBackgroundColor(0x00000000);  //transparent while loads
        myWebView.loadUrl("file://" + indexPath);

    }




    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);


    }

    public boolean goBack() {
        if (this.isVisible() && myWebView != null) {
            if (myWebView.canGoBack()) {
                myWebView.goBack();
                return true;
            }
        }
        return false;
    }


    public static String getIndexPage()
    {
        String micrositePath = null;
        if (Controller.getInstance().getDao().getBaseGuideId().equals(Controller.getInstance().getConfig().getRootId()))
        {
            micrositePath = getLocalizedIndex(Controller.getInstance().getDao().getRelatedFileDescriptor("places/root/microsite/index.html"));
        } else
        {    //micrositePath=Controller.getInstance().getDao().getRelatedFileDescriptor("places/root/microsite-basic/index.html");
            try {
                String m = Controller.getInstance().getDao().load(Controller.getInstance().getDao().getBaseGuideId()).getAttributes().get("microsite");
                if (Utils.isEmpty(m))
                {   //try default config
                    m="cache/microsites/"+Controller.getInstance().getDao().getBaseGuideId()+"";

                }
                String tmpmicrositePath = Controller.getInstance().getDao().getRelatedFileDescriptor(m + "/index.html");

                tmpmicrositePath=getLocalizedIndex(tmpmicrositePath);
                if(!new File(tmpmicrositePath).exists())
                {
                    //by pass
                    throw new Exception("dummy");
                }
                micrositePath = tmpmicrositePath;

            } catch (Exception ign) {  //try default page
                micrositePath = getLocalizedIndex(Controller.getInstance().getDao().getRelatedFileDescriptor("places/root/microsite/default.html"));
            }

        }
        return micrositePath;
    }


    private static String getLocalizedIndex(String micrositePath)
    {
        String locale= Locale.getDefault().getLanguage();
        String file=micrositePath.replace(".htm","_"+locale+".htm");
        if(!new File(file).exists())
        {
              return micrositePath;
        }else
        {
            return file;
        }




    }


    private class GMUCallbackInterface
    {
        @JavascriptInterface
        public void jumpToGuide(final String params)
        {
            MicroSiteFragment.this.getActivity().runOnUiThread(new Runnable() {
                public void run()
                {
                    synchronized (MicroSiteFragment.this.getActivity())
                    {   try
                        {   JSONObject js=new JSONObject(params);
                            String guideUID= (String) JsonUtils.getObject(js, "guideUID", "");
                            //set next script invocation

                            Controller.getInstance().getGmuContext().targetedScriptInvocations.put(guideUID,(String) JsonUtils.getObject(js, "nextAction", ""));
                            //set callback method (on back press)
                            Controller.getInstance().getGmuContext().targetedScriptInvocations.put(Controller.getInstance().getDao().getBaseGuideId(),(String) JsonUtils.getObject(js, "callbackAction", ""));
                            onAccessToGuideClick(guideUID,false);
                        } catch (Exception ign)
                    {

                        ign.printStackTrace();
                    }

                    }

                }
            });

        }
        @JavascriptInterface
        public void goToGMU(final String params)
        {

            MicroSiteFragment.this.getActivity().runOnUiThread(new Runnable() {
                public void run()
                {
                    synchronized (MicroSiteFragment.this.getActivity())
                    {

                        try
                        {
                            JSONObject js=new JSONObject(params);

                            Integer viewId= (Integer) JsonUtils.getObject(js, "viewId", new Integer(GmuEventListener.VIEW_LIST));

                            String groupID= (String) JsonUtils.getObject(js, "groupId", "");
                            boolean push=(Boolean) JsonUtils.getObject(js,"push",Boolean.FALSE);
                            String selectedUID=(String) JsonUtils.getObject(js, "selectedId", "");
                            boolean onlyDownloaded= (Boolean) JsonUtils.getObject(js,"onlyDownloaded",Boolean.TRUE);
                            Controller.getInstance().getGmuContext().setOnlyGuidesDownloadedFilter(onlyDownloaded) ;

                            String orderDef=  (String) JsonUtils.getObject(js, "order", "");

                            Controller.getInstance().getGmuContext().order.setPriorByString(orderDef);
                            if(Utils.isEmpty(groupID)&&
                                    ! Utils.equals(Controller.getInstance().getDao().getBaseGuideId(),Controller.getInstance().getConfig().getRootId()))
                            {   //guide (not store): preselect baseguide as group
                                groupID=Controller.getInstance().getDao().getBaseGuideId();

                            }



                            Controller.getInstance().setGroupFilter(groupID);

                            Controller.getInstance().setSelectedUID(selectedUID);
                            Controller.getInstance().setCurrentView(viewId);
                            if(!push)
                            {
                               if( viewId==GmuEventListener.VIEW_GUIDE_LIST)
                               {   //always try to update main guide at start

                                   ((GmuMainActivity) MicroSiteFragment.this.getActivity()).updateMainGuide();

                               }else
                               {
                                   switchToView(viewId);
                               }


                            }else
                            {
                                //only push  a navigation Item

                                NavigationItem item = new NavigationItem(selectedUID, groupID, viewId,Controller.getInstance().getGmuContext().getCurrentGuide());
                                Controller.getInstance().getGmuContext().navigationStack.push(item);

                            }

                        } catch (Exception ign) {
                            System.out.println("pasa aqui");
                            //ign.printStackTrace();
                        }

                    }

                }
            });





        }

        @JavascriptInterface
        public void goTo(final int viewId) {
            MicroSiteFragment.this.getActivity().runOnUiThread(new Runnable() {
                public void run() {
                    try {//push navigation
                        Controller.getInstance().pushDetail(Controller.getInstance().getSelectedUID());
                        //switch view
                        switchToView(viewId);
                    } catch (Exception ign) {
                        //when exit from splash screen sometimes the timer produces one error: ignore
                        //ign.printStackTrace();
                    }
                }
            });
        }
        @JavascriptInterface
        public void goToMainView()
        {
            goToMainView("false");
        }
        @JavascriptInterface
        public void goToMainView(final String push)

        {   try{
                     MicroSiteFragment.this.getActivity().runOnUiThread(new Runnable() {
                public void run() {
                    boolean pushM= (push!=null&&push.equalsIgnoreCase("TRUE"));
                    MicroSiteFragment.goToMainView(pushM);
                }
            });

            }catch(Exception ign)
            {
                System.out.println("pasa aqui");
            }

        }




        @JavascriptInterface
        public void goToDetail(final String uid)

        {
            MicroSiteFragment.this.getActivity().runOnUiThread(new Runnable() {
                public void run() {
                    try {

                        Controller.getInstance().goToDetail(uid);
                    } catch (Exception ign) {
                        //when exit from splash screen sometimes the timer produces one error: ignore
                        //ign.printStackTrace();
                    }
                }
            });
        }



    }


    public final static class MyWebClient extends WebViewClient {
        public void onReceivedError(WebView view, int errorCode,
                                    String description, String failingUrl) {
                 MicroSiteFragment.onReceivedError(view,errorCode,description,failingUrl);
        }

        public boolean onConsoleMessage(ConsoleMessage cm) {

            return MicroSiteFragment.onConsoleMessage(cm);
        }

        @Override
        public void onPageFinished(WebView view, String url)
        {      //api version set: 2 place detail support


               String pushedScriptInvocation=Controller.getInstance().getGmuContext().targetedScriptInvocations.get(Controller.getInstance().getDao().getBaseGuideId());
               if(!Utils.isEmpty(pushedScriptInvocation))
                {   //invoke pushed js action
                    String toInvoke="javascript:" +pushedScriptInvocation;
                    Controller.getInstance().getGmuContext().targetedScriptInvocations.remove(Controller.getInstance().getDao().getBaseGuideId());
                    view.loadUrl(toInvoke);
                }
                view.loadUrl("javascript:GMUAPI.setVersion(2);");
            view.loadUrl("javascript:GMUAPI.setVersison(2);");
        }



    }




    public static void onReceivedError(WebView view, int errorCode,
                                       String description, String failingUrl) {

        Log.e(TAG, description + " -- code: " + errorCode + " : url=" + failingUrl);

        //bypass error
        MicroSiteFragment.goToMainView(false);


    }

    public static boolean onConsoleMessage(ConsoleMessage cm) {
        Log.d(TAG, cm.message() + " -- From line "
                + cm.lineNumber() + " of "
                + cm.sourceId() );
        return true;
    }

    public static void goToMainView(boolean pushMicrosite)
    {
        try
        {
            //push microsite

            if(pushMicrosite) Controller.getInstance().pushDetail(Controller.getInstance().getDao().getBaseGuideId(), GmuEventListener.VIEW_MICROSITE);


            int viewId;
            //check if microsite has something to show, else bypass
            if (Controller.getInstance().getDao().getBaseGuideId().equals(Controller.getInstance().getConfig().getRootId())) {
                //by pass to guidelist
                viewId = GmuEventListener.VIEW_GUIDE_LIST;

            } else {
                //bypass to placelist
                viewId = GmuEventListener.VIEW_LIST;
                Controller.getInstance().setGroupFilter(Controller.getInstance().getDao().getBaseGuideId());
            }
            Controller.getInstance().setSelectedUID(null);
            switchToView(viewId);
        }catch (Exception ign)
        {   //TODO: ignorar el fallo cuando se sale dela app y no se espera que finalice el timer de la splash screen y
            Log.w(TAG,"Ignore error on mainview access:",ign);

        }
    }


    private static void  switchToView(int viewId)
    {


        Controller.getInstance().switchView(viewId);
    }

    private static  void resetContext()
    { if(Controller.getInstance().getConfig().getAttribute(IConfig.CLEAN_CENTER_ON_MICROSITE).equalsIgnoreCase("true"))
    {
        //clear map center
        Controller.getInstance().resetContext();
    }

    }

}
