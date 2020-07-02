package org.gmu.utils;

import android.os.Environment;

import org.gmu.control.Controller;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;


/**
 * Created by ttg on 22/09/2015.
 */
public class ZipFileSystem
{
    public static final String STARTUP_PACKAGE_PATH="startup.zip";
    private static Set<String> inProgress=new HashSet<String>();
    private static ZipFile theFile=null;
    private static ZipFileSystem theInstance;
    private static boolean enabled=true;

    public static  synchronized ZipFileSystem getInstance()
    {
        try
        {
            if(theInstance==null)
            {
                theInstance=new ZipFileSystem();
                if(enabled)
                {
                    File f=new File(Environment.getExternalStorageDirectory()+"/Android/obb/"+Controller.getInstance().getConfig().getPackageName() );
                    if(f.exists()&&f.isDirectory())
                    {
                        File[] files=f.listFiles();
                        long currentFtime=0;
                        for (int i = 0; i < files.length; i++) {
                            File file = files[i];
                            if(file.getName().toUpperCase().endsWith(".OBB"))
                            {   //get most recent obb
                                if(file.lastModified()>currentFtime)
                                {
                                    theFile=new ZipFile(file);
                                    currentFtime=file.lastModified();
                                }


                            }
                        }

                        enabled=(theFile!=null);
                    }else
                    {
                        enabled=false;
                    }

                }


            }

        }catch (Exception ign){ign.printStackTrace();enabled=false;}
        return theInstance;
    }

    public String getFile(String path)
    {
        if(!enabled) return null;
        while(inProgress.contains(path))
        {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        File dest = new File(Utils.getFilePath(path));
        synchronized (theInstance)
        {
            if(!dest.exists())
            {
                inProgress.add(path);

            } else
            {
                return path;
            }
        }
         OutputStream destO=null;
         try
         {
             ZipEntry ze=theFile.getEntry(path);
             if(ze!= null)
             {
                if(!dest.getParentFile().exists())
                {
                    dest.getParentFile().mkdirs();
                }

                 destO = new FileOutputStream(dest);
                 StreamUtils.readInto(new OutputStream[]{destO}, theFile.getInputStream(ze), false);
                 return path;
             }else
             {
                 return null;
             }



         }catch (Exception ign)
         {
             dest.delete();
         }
         finally {
             if(destO!=null)
             {
                 try {
                     destO.close();
                 } catch (IOException ign) { }
             }
             inProgress.remove(path);
         }

        return null;




    }

}
