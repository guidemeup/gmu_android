package org.gmu.utils.worker;

import android.util.Log;
import org.gmu.utils.FileDescriptor;
import org.gmu.utils.NetUtils;
import org.gmu.utils.OnWorkEventListener;
import org.gmu.utils.StreamUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.Stack;

/**
 * User: ttg
 * Date: 25/04/13
 * Time: 15:06
 * To change this template use File | Settings | File Templates.
 */
public class DownloadWorker extends AbstractWorker
{   private static final String TAG = DownloadWorker.class.getName();
    private static final int MAX_RETRIES=2;
    private String serverPrefix;
    private File downloadDir;
    private long downloaded=0;

    public DownloadWorker(File downloadDir, Stack downloadQueue, String serverPrefix) {
        super(downloadQueue);
        this.serverPrefix = serverPrefix;
        this.downloadDir = downloadDir;

    }

    @Override
    protected long doTask(final Object work) throws Exception
    {
        FileDescriptor url = (FileDescriptor)work;
        String relativeUrl = url.name.replace(serverPrefix + "/", "");
        File targetFile = new File(downloadDir, relativeUrl);
        synchronized (super.workerQueue)
        {
            targetFile.getParentFile().mkdirs();
        }

        int tries=0;

        for(;;)
        {
            OutputStream fo=new FileOutputStream(targetFile);
            OutputStream[] out = new OutputStream[]{fo};

            try
            {
                StreamUtils.readIntoListener(out, NetUtils.getInputStream(url.name, 8192),

                        new OnWorkEventListener() {
                            public void onWorkOk(Object fd, long downloadPass)
                            {

                                downloaded += downloadPass;
                                eventListener.onWorkOk(work,downloaded);
                            }});

                //downloaded += url.size;
                Log.d(TAG,"Downloaded OK ["+downloaded+"B]: "+url.name);

                return downloaded;
            }catch (Exception t)
            {
                Log.e(TAG,"Error downloading="+url.name);
                if(tries<MAX_RETRIES)
                {
                    tries++;
                    downloaded=0;
                    Log.d(TAG,"Retry intent="+tries);
                    try{fo.close(); }catch (Exception ign){};
                }else
                {
                    throw t;
                }

            }

        }



    }
}
