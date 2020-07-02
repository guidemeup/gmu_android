package org.gmu.utils;

import android.os.Environment;
import android.widget.Toast;

import org.gmu.control.Controller;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.Date;

/**
 * Created by ttg on 06/05/2015.
 */
public class Log {

  //yo

    public static final int VERBOSE = 2;

    /**
     * Priority constant for the println method; use Log.d.
     */
    public static final int DEBUG = 3;

    /**
     * Priority constant for the println method; use Log.i.
     */
    public static final int INFO = 4;

    /**
     * Priority constant for the println method; use Log.w.
     */
    public static final int WARN = 5;

    /**
     * Priority constant for the println method; use Log.e.
     */
    public static final int ERROR = 6;

    /**
     * Priority constant for the println method.
     */
    public static final int ASSERT = 7;

    public static void  d(String tag, String msg)
    {
        appendLog("DEBUG[" + tag + "]" + msg);

    }
    public static void  i(String tag, String msg)
    {
        appendLog("INFO[" + tag + "]" + msg);

    }
    public static void  e(String tag, String msg)
    {

        appendLog("ERROR:[" + tag + "]" + msg);

    }
    public static void  w(String tag, String msg)
    {

        appendLog("WARN:[" + tag + "]" + msg);

    }
    public static void e(String tag, String msg, Throwable tr) {
        e(tag,  msg + '\n' + android.util.Log.getStackTraceString(tr));
    }
    public static void w(String tag, String msg, Throwable tr) {
        w(tag,  msg + '\n' + android.util.Log.getStackTraceString(tr));
    }
    public static void d(String tag, String msg, Throwable tr) {
        d(tag,  msg + '\n' + android.util.Log.getStackTraceString(tr));
    }


    public static synchronized  void appendLog(String text)
    { try
      {

        File logFile = new File(Environment.getExternalStorageDirectory(),"guidemeup.log");
        if (!logFile.exists())
        {

                logFile.createNewFile();

        }

            //BufferedWriter for performance, true to set append to file flag
            BufferedWriter buf = new BufferedWriter(new FileWriter(logFile, true));
            buf.append(new Date()+":"+text);
            buf.newLine();
            buf.close();
        }
        catch (Exception e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();

        }
    }

}
