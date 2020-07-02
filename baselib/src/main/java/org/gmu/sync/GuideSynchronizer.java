package org.gmu.sync;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;
import android.util.Pair;
import android.util.Property;


import org.gmu.base.R;
import org.gmu.config.Constants;
import org.gmu.control.Controller;
import org.gmu.dao.IPlaceElementDAO;
import org.gmu.dao.impl.AbstractPlaceElementDAO;
import org.gmu.dao.impl.sqlite.DBPlaceElementDAO;
import org.gmu.pojo.PlaceElement;
import org.gmu.utils.*;
import org.gmu.utils.FileDescriptor;
import org.gmu.utils.worker.AbstractWorker;
import org.gmu.utils.worker.DownloadWorker;
import org.gmu.utils.worker.FileMoveWorker;
import org.json.JSONObject;

import java.io.*;

import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * User: ttg
 * Date: 21/01/13
 * Time: 15:54
 * To change this template use File | Settings | File Templates.
 */
public class GuideSynchronizer extends AbstractSynchronizer implements IPlaceElementDAO.LoadListener {

    private static final String TAG = GuideSynchronizer.class.getName();
    private String guideId;

    private OnUpdateEvent listener;
    private File tmpDir;
    private File baseDir;
    private Context context;

    private DownloadWorker downloader = null;
    private String baseServerGuides;
    private boolean isPreproductionGuide=false;

    public GuideSynchronizer(String guideId, OnUpdateEvent listener, Context context) {
        this.listener = listener;
        this.guideId = guideId;
        this.context = context;

    }


    public void setStatus(AbstractSynchronizer.SYNCSTATE status) {
        this.status = status;
        if (this.status == AbstractSynchronizer.SYNCSTATE.CANCELED_BY_USER) {
            //interrupt downloader
            if (downloader != null) {
                downloader.cancel();
            }
        }
    }

    public String getResultGuideId() {
        return this.guideId;
    }
    public void doWork() throws Exception {
        try {
            checkStartupPackage();
            boolean isCurrentGuidePre=isCurrentGuidePre();
            if(isCurrentGuidePre)
            {   //set guide prefix to allow update from pre environment
                if(!guideId.endsWith(Constants.PREPRODUCTION_GUIDE_SUFFIX)) guideId=guideId+Constants.PREPRODUCTION_GUIDE_SUFFIX;

            }
            isPreproductionGuide=guideId.endsWith(Constants.PREPRODUCTION_GUIDE_SUFFIX);
            if(isPreproductionGuide)
            {
                //set remote server to testguides
                baseServerGuides=Controller.getInstance().getConfig().getBaseServerGuides().replace("guides","testguides");
                guideId=guideId.replace(Constants.PREPRODUCTION_GUIDE_SUFFIX,"");
                //remove production guide if exist
                if(isCurrentGuidePre)
                {
                    removeNotDraftGuide();
                }
            }else
            {
                baseServerGuides=Controller.getInstance().getConfig().getBaseServerGuides();
            }

            //-1: if guideuid is a token get uid
           if(guideId.startsWith(Constants.TOKEN_PREFIX))
           {
              String devuid= Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);

               if(true)
               {
                   List<Pair<String, String>> params=new LinkedList<Pair<String, String>>();
                   if(isCurrentGuidePre){params.add(new Pair<String, String>("env","true"));}
                   params.add( new Pair<String, String>("token",guideId));
                   params.add(new Pair<String, String>("devuid",devuid));
                   params.add(new Pair<String, String>("oper","decode"));
                   JSONObject js=new JSONObject(new String(NetUtils.postUrl((baseServerGuides+Constants.GET_TOKEN_QUERY), params)));
                   this.guideId=js.getJSONObject("token").getString("guideuid");
               }else
               {    //by pass token manager

                   this.guideId=guideId.replace("gmut_","");
               }





           }




            //0: obtain old version file
            File oldVersionFile = new File(Utils.getFilePath("/" +guideId+ "/" + Constants.VERSIONFILENAME));
            if (oldVersionFile.exists()) {
                this.setStatus(SYNCSTATE.OLD_VERSION);
            } else {
                this.setStatus(SYNCSTATE.NO_VERSION_FOUND);
            }


            //1: download listFile
            tmpDir = new File(Utils.getFilePath("/cache/gmutmp"));
            tmpDir.mkdirs();
            baseDir = new File(Utils.getFilePath("/"));

            File f = new File(tmpDir.getAbsolutePath() + "/" + guideId+ "/" + Constants.VERSIONFILENAME);

            downloadFileList(f);

            //2: obtain lists
            Map<String, FileDescriptor> oldList = parseFileList(oldVersionFile);
            Map<String, FileDescriptor> newList = parseFileList(f);
            List<FileDescriptor> toDownload = getNewFiles(oldList, newList);




            //3: download
            download(toDownload,15,40);
            listener.onEvent(context.getString(R.string.updating), 40);
            checkCancel();
            //4: move downloaded files
            boolean existOld= oldVersionFile.exists();
             try {

                 moveFiles(toDownload,40,63);
             } catch (Exception ign)
             {
                 //if error , remove incomplete version indicator
                 if(!existOld)
                 {
                     oldVersionFile.delete();
                     File upd=new File(Utils.getFilePath("/" +guideId+ "/" + Constants.UPDATEFILENAME));
                     upd.delete();

                 }
                 throw ign;
             }



            listener.onEvent("Removing old files", 63);
            checkCancel();
            //5:delete old
            List toRemove = getDeletedFiles(oldList, newList);
            checkCancel();
            delete(toRemove);
            checkCancel();
            listener.onEvent("Updating guide Database", 65);
            //6: DAO update/reload

            DBPlaceElementDAO db = new DBPlaceElementDAO(context, guideId,this);
            //set preproduction indicator
            db.setPreProductionGuide(guideId,isPreproductionGuide);
            //copy guide to root if doesn't exist
            if(Controller.getInstance().getDao().load(guideId)==null)
            {
                db.copyGuideDefinition(Controller.getInstance().getConfig().getRootId());
            }

            db.destroy();


            if(guideId.equals(Controller.getInstance().getDao().getBaseGuideId()))
            {   //reload data
                Controller.getInstance().reloadDAO();

            }


            //listener.onDAOLoadEvent("Finish!", 100);
            UpdateStateChecker.setGuideState(guideId, UpdateStateChecker.UPDATESTATE.UPDATED);
            this.setStatus(SYNCSTATE.LAST_VERSION_UPDATED);
        } catch (Exception log)
        {
            if (this.getStatus().equals(SYNCSTATE.NO_VERSION_FOUND)) {
                this.setStatus(SYNCSTATE.UPDATE_ERROR);
            } else
            {
                this.setStatus(SYNCSTATE.UPDATE_WARNING);
            }
            throw log;
        }
    }



    private void delete(List<String> urls) throws Exception {
        for (int i = 0; i < urls.size(); i++) {
            String url = urls.get(i);
            String relativeUrl = url.replace(baseServerGuides, "");
            File f = new File(Utils.getFilePath(relativeUrl));
            f.delete();

        }

    }


    private void checkStartupPackage() throws Exception
    {
        //create app data directory
        Utils.getFilePath("");


		File startupPackageFile=new File(Utils.getFilePath(ZipFileSystem.STARTUP_PACKAGE_PATH));

        //check version in app data




        if( Utils.needPackageUpdate(context)||!AbstractPlaceElementDAO.isGuideDownloaded(Controller.getMainGuideId()))
        {   //extract contents

            List<InputStream> sources=new ArrayList<InputStream>();

            //extract startup packages
            String obbStartupFilePath=ZipFileSystem.getInstance().getFile(ZipFileSystem.STARTUP_PACKAGE_PATH);

            try{
                sources.add(context.getResources().openRawResource(R.raw.root));
            }catch (Exception ign){};

            if(obbStartupFilePath!=null)
            {
                sources.add(new FileInputStream(startupPackageFile));
            }
            long totalSize=0;
            for (int i = 0; i < sources.size(); i++) {
                InputStream in= sources.get(i);
                totalSize+=in.available();
            }
            long csize=0;

            for (int i = 0; i < sources.size(); i++)
            {
                InputStream in = sources.get(i);
                ZipInputStream zis = new ZipInputStream(in);


                try {
                    ZipEntry ze;

                    while ((ze = zis.getNextEntry()) != null) {
                        csize+=ze.getCompressedSize();
                        checkCancel();
                        String filename = ze.getName();
                        File dest = new File(Utils.getFilePath(filename));
                        if (ze.isDirectory()) {

                        } else {
                            //copy file
                         boolean created=   dest.getParentFile().mkdirs();
                            OutputStream destO = new FileOutputStream(dest);
                            StreamUtils.readInto(new OutputStream[]{destO}, zis, false);

                        }
                        int realPercent=Utils.obtainRange(0,15,csize,totalSize);
                        listener.onEvent("Extracted " + filename , realPercent);
                        Log.d(TAG, "Extracted " + filename + " Size=[" + ze.getCompressedSize() + "/" + ze.getSize() + "]");

                    }
                }
                catch(Exception ign)
                {
                    Log.e(TAG,"Error unzip startup package",ign);
                }
                finally {
                    zis.close();
                    if(in!=null){try{in.close();}catch (Exception ign){}}


                }


            }
            //remove extracted package if exist
            startupPackageFile.delete();



        }

        Utils.setPackageUpdated(context);

    }




    private long getTotalSize(Stack<FileDescriptor> urls) {
        long total = 0;
        for (int i = 0; i < urls.size(); i++) {
            total += urls.get(i).size;

        }
        return total;
    }

    private Stack<FileDescriptor> removeAlreadyDownloaded(List<FileDescriptor> urls) {
        Stack<FileDescriptor> ret = new Stack<FileDescriptor>();
        String guideDir = "/" + guideId;
        for (int i = 0; i < urls.size(); i++) {
            FileDescriptor url = urls.get(i);
            String relativeUrl = url.name.replace(baseServerGuides, "");


            if (checkExistent(new File(tmpDir, relativeUrl), url) ||
                    (!relativeUrl.startsWith(guideDir) && checkExistent(new File(baseDir, relativeUrl), url))
                    ) {   //ignore if:
                //--file already downloaded in tmp: ignore or
                //-- file already downloaded in destination folder and is out of guide context (in order to ignore common files)
            } else {
                ret.add(url);

            }

        }
        return ret;
    }

    private boolean checkExistent(File targetFile, FileDescriptor url) {
        if ((targetFile.exists()) && (targetFile.length() == url.size)) {

            Log.d(TAG,"File "+targetFile.getAbsolutePath()+" exist [it will be ignored from download queue.]");
            return true;
        }
        return false;
    }

    private void downloadFileList(File f) throws Exception {
        new File(f.getParent()).mkdirs();
        FileOutputStream o = new FileOutputStream(f);
        o.write(NetUtils.getURL(baseServerGuides + "/" + guideId + "/" + Constants.VERSIONFILENAME));
        o.close();
    }

    private static Map<String, FileDescriptor> parseFileList(File filePath) throws Exception {
        HashMap<String, FileDescriptor> ret = new HashMap<String, FileDescriptor>();
        if (!filePath.exists()) return ret;
        BufferedReader in = new BufferedReader(new FileReader(filePath));
        String line = in.readLine();
        while (!Utils.isEmpty(line)) {
            FileDescriptor f = new FileDescriptor(line.split("#"));
            ret.put(f.name, f);
            line = in.readLine();
        }
        in.close();
        return ret;

    }

    private List<String> getDeletedFiles(Map<String, FileDescriptor> oldList, Map<String, FileDescriptor> newList) {
        List<String> ret = new ArrayList<String>();
        //return candidate files to delete (ONLY DELETE CHILDREN FILES FROM GUIDEDIR!!)
        String guideDir = baseServerGuides + "/" + guideId;
        Iterator<String> oldKeys = oldList.keySet().iterator();
        while (oldKeys.hasNext()) {
            String next = oldKeys.next();
            if (!newList.containsKey(next) && next.startsWith(guideDir)) {
                ret.add(next);
            }

        }
        return ret;
    }

    private  List<FileDescriptor> getNewFiles(Map<String, FileDescriptor> oldList, Map<String, FileDescriptor> newList) {
        List<FileDescriptor> ret = new ArrayList<FileDescriptor>();
        //return new files
        Iterator<String> d = newList.keySet().iterator();
        while (d.hasNext()) {
            String key = d.next();
            Long newTs = newList.get(key).ts;
            Long newSize = newList.get(key).size;
            FileDescriptor old = oldList.get(key);
            if(old==null)
            {   //try to obtain from resources (file will be copied on tmp folder )
                obtainResource(key);

            }

             if (old == null || (newTs > old.ts))
            {
                ret.add(newList.get(key));
            }

        }

        return ret;
    }
    private FileDescriptor obtainResource(String key)
    {     //try to obtain resource from raw folder
        try
        {



            String assetFile=key.replace(baseServerGuides+"/", "");
            InputStream in=context.getAssets().open(assetFile);

            //copy into tmpDir
            File out = new File(tmpDir,assetFile);
            long size= FileUtils.copyToFile(in,out);
            return new FileDescriptor(key, Long.MAX_VALUE, size);


        } catch (Exception ign)
        {
            return null;
        }



    }


    public void moveFiles(final List<FileDescriptor> downloaded,final int initial, final int end)  throws Exception
    {   String toRep= baseServerGuides + "/";

        Stack moveQueue = new Stack();


        for (int i = 0; i < downloaded.size(); i++)
        {   moveQueue.push(downloaded.get(i));
        }
        FileMoveWorker mover=new FileMoveWorker(moveQueue,tmpDir,baseDir,toRep);
        mover.setOnWorkEventListener(new OnWorkEventListener() {
            public void onWorkOk(Object worked, long total)
            {
                int realPercent=Utils.obtainRange(initial,end,total,downloaded.size());
                Log.i(TAG,"Moved="+total+" "+ ((FileDescriptor)worked).name);
                listener.onEvent("Moving files", realPercent);

            }
        });
        //move files on workers
        mover.startWork(10);

    }


    /**
     * Asynchronous download queue****
     */
    private void download(List<FileDescriptor> urls,final int initial, final int end) throws Exception
    {

        Stack downloadQueue = removeAlreadyDownloaded(urls);
        if (!downloadQueue.empty())
        {
            listener.onEvent("Downloading " + downloadQueue.size() + " files", initial);
            final long totalKB = getTotalSize(downloadQueue) / 1024;


            downloader = new DownloadWorker(tmpDir, downloadQueue, baseServerGuides);
            downloader. setOnWorkEventListener(new OnWorkEventListener() {
                public void onWorkOk(Object fd, long totalDownloaded) {
                    long downloaded = totalDownloaded / 1024;

                    int pec = Utils.obtainRange(initial, end, downloaded, totalKB);
                    listener.onEvent(context.getString(R.string.downloaded) + " " + downloaded + "/" + totalKB + "KB", pec);

                }
            });
            downloader.startWork(Constants.MAXDOWNLOADWORKERS);
        }

        //check error
        GuideSynchronizer.this.checkCancel();


    }


    public void onDAOLoadEvent(String msg, int percent)
    {
        int realPercent=Utils.obtainRange(65,100,percent,100);

        listener.onEvent("Updating guide Database", realPercent);
        //Log.d("EOO",System.currentTimeMillis()+" "+percent);

        //To change body of implemented methods use File | Settings | File Templates.
    }


    private void removeNotDraftGuide()
    {
        try
        {   File oldFile=new File(Utils.getFilePath("/" + guideId + "/" + Constants.UPDATEFILENAME));
            if(oldFile.exists())
            {
                DBPlaceElementDAO db = new DBPlaceElementDAO(context, guideId,new IPlaceElementDAO.LoadListener()
                {
                    public void onDAOLoadEvent(String msg, int percent){}
                });
                boolean isPrepro=db.isPreProductionGuide(guideId);

                db.destroy();
                if(!isPrepro)
                {   //delete guide to replace with preproduction env
                    Controller.getInstance().deleteGuide(guideId);

                }

            }


        } catch (Exception ign){}


    }

    private boolean isCurrentGuidePre()
    {    boolean ret=false;
        File oldFile=new File(Utils.getFilePath("/" + guideId + "/" + Constants.UPDATEFILENAME));
        if(oldFile.exists())
        {
            DBPlaceElementDAO db = new DBPlaceElementDAO(context, guideId,new IPlaceElementDAO.LoadListener()
            {
                public void onDAOLoadEvent(String msg, int percent){}
            });
            ret= db.isPreProductionGuide(guideId);

            db.destroy();


        }
        return ret;
    }



}
