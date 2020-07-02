package org.gmu.utils;

import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.EmptyStackException;
import java.util.Stack;

/**
 * User: ttg
 * Date: 25/01/13
 * Time: 8:50
 * Downloads an url Queue into a directory
 * @deprecated use DownloadWorker
 */
public class WorkerDownloader {
    private OnDownloadEvent eventListener = null;
    private boolean canceled = false;
    private boolean someError = false;
    private Stack<FileDescriptor> downloadQueue;
    private String serverPrefix;
    private File downloadDir;
    private long downloaded;

    public WorkerDownloader(File downloadDir, Stack<FileDescriptor> downloadQueue, String serverPrefix) {
        this.downloadQueue = downloadQueue;
        this.serverPrefix = serverPrefix;
        this.downloadDir = downloadDir;

    }

    public void startDownload(int numWorkers) throws Exception {
        Downloader[] downloaders = new Downloader[numWorkers];
        for (int i = 0; i < numWorkers; i++) {
            downloaders[i] = new Downloader();
            downloaders[i].start();

        }
        //join
        for (int i = 0; i < downloaders.length; i++) {
            try {
                downloaders[i].join();
            } catch (InterruptedException e) {
            }
        }
        if (someError) {
            throw new Exception("Queue download exception");
        }
    }

    public boolean hasSomeError() {
        return someError;
    }

    public void setOnDownloadListener(OnDownloadEvent listener) {
        eventListener = listener;
    }

    public void cancelDownload() {
        canceled = true;
    }

    public interface OnDownloadEvent {
        /**
         * Called some event happens
         */
        void onDownloadOk(FileDescriptor fd, long totalDownloaded);
    }


    private class Downloader extends Thread {
        public Downloader() {

        }

        public void run() {
            FileDescriptor url = null;
            try {
                for (; ; ) {
                    if (canceled || someError) {
                        break;
                    }
                    url = downloadQueue.pop();
                    String relativeUrl = url.name.replace(serverPrefix + "/", "");
                    File targetFile = new File(downloadDir, relativeUrl);
                    synchronized (downloadQueue) {
                        targetFile.getParentFile().mkdirs();
                    }
                    OutputStream[] out = new OutputStream[]{new FileOutputStream(targetFile)};
                    StreamUtils.readInto(out, NetUtils.getInputStream(url.name, 8192));
                    downloaded += url.size;
                    Log.d("Downloader", "Downloaded : " + downloaded+"-->"+targetFile.getName());
                    if (eventListener != null) eventListener.onDownloadOk(url, downloaded);
                }
            } catch (EmptyStackException ign) {
                //end of download queue--> OK
            } catch (Throwable error) {
                if (url != null) {
                    Log.e("Downloader", "Download: " + url.name);
                }
                Log.e("Downloader", "Error=", error);
                someError = true;
            }
        }

    }
}
