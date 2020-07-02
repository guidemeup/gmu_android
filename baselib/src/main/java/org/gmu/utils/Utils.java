package org.gmu.utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Environment;
import android.provider.Browser;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.ImageSpan;
import android.text.style.URLSpan;
import android.text.util.Linkify;
import android.util.*;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.gmu.base.R;
import org.gmu.control.Controller;
import org.gmu.ui.GMUImageGetter;
import org.gmu.ui.GMURLSpan;
import org.gmu.ui.ListTagHandler;

import java.io.*;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

/**
 * User: ttg
 * Date: 14/11/12
 * Time: 19:08
 * To change this template use File | Settings | File Templates.
 */
public class Utils {
    private static final String[] HTMLTAGS = {"<b>", "<br>", "<u>", "<i>", "<p>", "<ul>","<a ","<div","<img","<p ","<table"};
    public static final String ARRAY_VALUE_SEPARATOR=",";
    public static boolean isHtml(String html) {   //TODO: optimize : ?boolean containsHTML = value.matches(".*\\<[^>]+>.*");
        for (int i = 0; i < HTMLTAGS.length; i++) {
            String htmltag = HTMLTAGS[i];
            if (html.contains(htmltag)) return true;
        }

        return false;

    }
    public static int obtainRange(int rangeMin,int rangeMax,long originalRange,long maxOriginalRange)
    {    if(maxOriginalRange==0) return 0;
         int max=rangeMax-rangeMin;

        return  (int) (((originalRange*max)/ maxOriginalRange)+rangeMin) ;

    }
    public static void fillFromHtml(String textHtml, Context context, TextView tv, GMUImageGetter imageGetter)
    {   //1: check if is html
        if (!isHtml(textHtml))
        {    //linkify
            tv.setText(textHtml);
            Linkify.addLinks(tv, Linkify.ALL);
            return;
        }
        //2: convert
        ListTagHandler handler=new ListTagHandler();
        Spanned spantext = Html.fromHtml(textHtml, imageGetter,handler );

        //3 remove invalid links

        Object[] spans = spantext.getSpans(0, spantext.length(), Object.class);
        for (Object span : spans)
        {
            int start = spantext.getSpanStart(span);
            int end = spantext.getSpanEnd(span);
            int flags = spantext.getSpanFlags(span);
            if (span instanceof URLSpan)
            {
                //replace span to deal intent not found errors
                URLSpan urlSpan = (URLSpan) span;

                //check if system can deal intent
                if (!isIntentAvailable(context, urlSpan.getURL()))
                {
                    ((Spannable) spantext).removeSpan(span);
                }

            }else if (span instanceof ImageSpan)
            {
                //correct drawable size if img attribs are specified
                ImageSpan imgSpan = (ImageSpan) span;
                Map atts=handler.getImgAttribsByUrl(imgSpan.getSource());
                if(atts!=null)
                {
                   if( imgSpan.getDrawable().getBounds().width()==0||imgSpan.getDrawable().getBounds().height()==0)
                   {    //transparent img--> ignore


                   }else
                   {
                       int width=-1;
                       int height=-1;
                       try
                       {
                           width=Integer.parseInt(""+atts.get("width"));
                           height=Integer.parseInt(""+atts.get("height"));

                           imageGetter.setDrawableBounds(imgSpan.getDrawable(),height,width);

                       }catch(Exception ign)
                       {

                       }
                   }




                }




            }
        }

        //4:linkify pass over result (adds: phone numbers,emails, etc..)

        spans = spantext.getSpans(0, spantext.length(), URLSpan.class);
        tv.setAutoLinkMask(Linkify.ALL);
        tv.setMovementMethod(LinkMovementMethod.getInstance());
        tv.setText(spantext);
         //pass 1: merge spans
        SpannableString ss = (SpannableString) tv.getText();
        for (int i = 0; i < spans.length; i++)
        {
            URLSpan spanOld = (URLSpan) spans[i];


            int end = spantext.getSpanEnd(spanOld);
            int start = spantext.getSpanStart(spanOld);

            ss.setSpan(spanOld, start, end, 0);
        }
       spans=spantext.getSpans(0, spantext.length(), URLSpan.class);
        //pass 2 apply interception
        ss = (SpannableString) tv.getText();
        for (int i = 0; i < spans.length; i++)
        {
            URLSpan spanOld = (URLSpan) spans[i];
            //create span interceptor
            GMURLSpan span=new GMURLSpan(spanOld.getURL());
            int end = spantext.getSpanEnd(spanOld);
            int start = spantext.getSpanStart(spanOld);
            ss.removeSpan(spanOld);
            ss.setSpan(span, start, end, 0);
        }


    }

    public static boolean isIntentAvailable(Context context, String URL)
    {
        final PackageManager packageManager = context.getPackageManager();
        Uri uri = Uri.parse(URL);
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        intent.putExtra(Browser.EXTRA_APPLICATION_ID, context.getPackageName());
        List resolveInfo =
                packageManager.queryIntentActivities(intent,
                        PackageManager.MATCH_DEFAULT_ONLY);
        if (resolveInfo.size() > 0) {
            return true;
        }
        return false;
    }

    public static int minPositive(int a, int b) {
        if (a < 0) return b;
        if (b < 0) return a;
        if (a < b) return a;
        return b;
    }

    public static void unbindDrawables(View view) {
        if (view.getBackground() != null) {
            view.getBackground().setCallback(null);
        }
        if (view instanceof ViewGroup) {
            for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) {
                unbindDrawables(((ViewGroup) view).getChildAt(i));
            }
            ((ViewGroup) view).removeAllViews();
        }
    }





    public static boolean equals(Object a, Object b) {
        a = checkNull(a);
        b = checkNull(b);
        if (((a == null) || (b == null))) {
            return (a == null && b == null);
        } else {
            return a.equals(b);
        }
    }

    private static Object checkNull(Object b) {
        if (("" + b).trim().equals("")) return null;
        else return b;
    }

    public static boolean contains(Object[] array, Object objectToFind) {
        if (objectToFind == null) return false;
        for (int i = 0; i < array.length; i++) {
            Object o = array[i];
            if (o.equals(objectToFind)) {
                return true;
            }
        }
        return false;
    }

    public static boolean contains(Set array, Object objectToFind) {
        if (objectToFind == null) return false;
        return array.contains(objectToFind);

    }



    public static boolean isEmpty(String dato) {
        return dato == null || dato.trim().equals("");
    }

    public static void toggleVisibility(View v2, boolean visible) {
        if (!visible)
        {
            v2.startAnimation(new ScaleAnimToHide(1.0f, 1.0f, 1.0f, 0.0f, 500, v2, true));

        } else {
            v2.startAnimation(new ScaleAnimToShow(1.0f, 1.0f, 1.0f, 0.0f, 500, v2, true));
        }

    }

    public static String getFilePath(String filePath) {
        try {


            File root = Environment.getExternalStorageDirectory();
            File dir = new File(root,  "/Android/data/" + Controller.getInstance().getConfig().getPackageName());
            if (!dir.exists()) {
                boolean bo=dir.mkdir();
                System.out.println(bo);
            }

            return dir.getAbsolutePath() + "/" + filePath;

        } catch (Exception ign) {
            throw new RuntimeException(ign);
        }

    }

    public static String formatMs(int ms) {
        int secs = ms / 1000;
        int m = (int) Math.floor(secs / 60);
        String s = "0" + (secs - (60 * m));
        s = s.substring(s.length() - 2);

        return m + "m:" + s + "s";

    }

    public static void copyMap(Map source, Map destination) {
        Iterator it = source.keySet().iterator();
        while (it.hasNext()) {
            Object next = it.next();
            destination.put(next, source.get(next));
        }

    }

    public static int convertDpToPixel(float dp, Context context) {
        Resources resources = context.getResources();
        DisplayMetrics metrics = resources.getDisplayMetrics();
        float px = dp * (metrics.densityDpi / 160f);
        return (int) px;
    }

    public static int convertPixelToDp(float pixel, Context context) {
        Resources resources = context.getResources();
        DisplayMetrics metrics = resources.getDisplayMetrics();
        float dp = pixel * (160f / metrics.densityDpi);
        return (int) dp;
    }

    public static String dumpMemory() {

        Runtime runtime = Runtime.getRuntime();

        NumberFormat format = NumberFormat.getInstance();

        StringBuilder sb = new StringBuilder();
        long maxMemory = runtime.maxMemory();
        long allocatedMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();

        sb.append("free memory: " + format.format(freeMemory / 1024) + "<br/>");
        sb.append("allocated memory: " + format.format(allocatedMemory / 1024) + "<br/>");
        sb.append("max memory: " + format.format(maxMemory / 1024) + "<br/>");
        sb.append("total free memory: " + format.format((freeMemory + (maxMemory - allocatedMemory)) / 1024) + "<br/>");
        return sb.toString();
    }

    public static String objectToString(Serializable object) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            new ObjectOutputStream(out).writeObject(object);
            byte[] data = out.toByteArray();
            out.close();

            out = new ByteArrayOutputStream();
            Base64OutputStream b64 = new Base64OutputStream(out, Base64.DEFAULT);
            b64.write(data);
            b64.close();
            out.close();

            return new String(out.toByteArray());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static Object stringToObject(String encodedObject) {
        try {
            return new ObjectInputStream(new Base64InputStream(
                    new ByteArrayInputStream(encodedObject.getBytes()), Base64.DEFAULT)).readObject();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String formatDistance(Float distance)
    {   if(distance==null) return "";

        int m=distance.intValue();

        if(m<1000) return m+" m";
        else
        {   double ret=(m/100)/10.0;
            return ret+" Km";
        }



    }


    public static int getStringResource(Context context, String name)
    {
        int resId = context.getResources().getIdentifier(name, "string",Controller.getInstance().getConfig().getPackageName());
        return resId;
    }
    public static int getStyleResource(String name)
    {
        if(name.equalsIgnoreCase("GMU_Theme_Sherlock_Light_DarkActionBar"))
        {
            return  R.style.GMU_Theme_Sherlock_Light_DarkActionBar;
        }else
        {
            return   R.style.GMU_Theme_Sherlock;
        }

    }


   public static boolean isTablet(Activity context)
    {
        DisplayMetrics displaymetrics = new DisplayMetrics();
        context.getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
        int height = displaymetrics.heightPixels;
        int wwidth = displaymetrics.widthPixels;
        int density=   displaymetrics.densityDpi;
        int orientation=context.getResources().getConfiguration().orientation;
        int rotation = context.getWindowManager().getDefaultDisplay().getRotation();


//        if(VERSION.RELEASE.startsWith("2")){
//				return false;
//			}else if(VERSION.RELEASE.startsWith("3")){
//				return true;
//			}else{
//				return true;
//			}
        //detect if landscape is the default orientation of the device
        return orientation== Configuration.ORIENTATION_LANDSCAPE&&rotation==0;
    }

    public static String capitalizeString(String string)
    {   if(string==null) return null;


        char[] chars = string.toCharArray();

        for (int i = 0; i < chars.length; i++)
        {
            if ( Character.isLetter(chars[i]))
            {
                chars[i] = Character.toUpperCase(chars[i]);
                break;
            }
        }
        return String.valueOf(chars);
    }


    public static String arrayToValue(String[] values)
    {
        StringBuilder fvalue=new StringBuilder();
        for (int i = 0; i < values.length; i++) {
            String value = values[i];
            if(i!=0) fvalue.append(ARRAY_VALUE_SEPARATOR);
            fvalue.append(value);

        }
        return fvalue.toString();
    }
    public static String[] valueToArray(String value)
    {
        return valueToArray(value,ARRAY_VALUE_SEPARATOR);
    }
    public static String[] valueToArray(String value,String sep)
    {   ArrayList<String> a=new ArrayList<String>();
        if(Utils.isEmpty(value)) return null;

        StringTokenizer tk=new StringTokenizer(value,sep);

        while(tk.hasMoreTokens()) {
            a.add(tk.nextToken());
        }


        return (String[]) a.toArray(new String[a.size()]);
    }

    public static int getVersion(Context context) {
        try {
            PackageInfo pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), PackageManager.GET_META_DATA);
            return pInfo.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            return 0;
        }
    }


    public static boolean needPackageUpdate(Context context)
    {
        //1: obtain package update version

        File version= new File(Utils.getFilePath("version_"+ Utils.getVersion(context)));
        return !version.exists();

    }

    public static void setPackageUpdated(Context context)
    {
        //1: obtain package update version

        File version= new File(Utils.getFilePath("version_"+ Utils.getVersion(context)));
        try {
            version.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}
