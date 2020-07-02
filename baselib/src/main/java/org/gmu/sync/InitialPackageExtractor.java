package org.gmu.sync;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;
import org.gmu.dao.impl.sqlite.DBPlaceElementDAO;
import org.gmu.utils.StreamUtils;
import org.gmu.utils.Utils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * User: ttg
 * Date: 3/03/13
 * Time: 11:57
 * Extracts startup package on data directory
 * @deprecated Integrated in GuideSynchronizer
 */
public class InitialPackageExtractor extends AbstractSynchronizer {
    private static String TAG = InitialPackageExtractor.class.getName();
    private Context context = null;
    private String assetFile = null;
    private OnUpdateEvent listener = null;
    private String guideId;

    public InitialPackageExtractor(Context context, String guideId, String assetFile, OnUpdateEvent listener) {
        this.guideId = guideId;
        this.assetFile = assetFile;
        this.listener = listener;
        this.context = context;

    }

    @Override
    public String getResultGuideId() {
        return this.guideId;
    }

    public void doWork() throws Exception {
        //check if startup package is on assets directory
        AssetManager assetManager = context.getAssets();

        int files = 0;

        // To get names of all files inside the "Files" folder
        try {
            listener.onEvent("Open file", 0);
            ZipInputStream zis = new ZipInputStream(assetManager.open(assetFile));

            try {
                ZipEntry ze;

                while ((ze = zis.getNextEntry()) != null) {
                    checkCancel();
                    String filename = ze.getName();
                    File dest = new File(Utils.getFilePath(filename));
                    if (ze.isDirectory()) {
                        dest.mkdirs();
                    } else {
                        //copy file
                        OutputStream destO = new FileOutputStream(dest);
                        StreamUtils.readInto(new OutputStream[]{destO}, zis, false);
                        files++;
                    }
                    Log.d(TAG, "Extracted " + filename + " Size=[" + ze.getCompressedSize() + "/" + ze.getSize() + "]");
                    listener.onEvent("Extracted " + files + " files", 30);
                }
            } finally {
                zis.close();
            }

            listener.onEvent("Updating guide Database", 90);
            //6: DAO update/reload
            DBPlaceElementDAO db = new DBPlaceElementDAO(context, guideId);
            db.destroy();
            listener.onEvent("Finish!", 100);
            this.setStatus(SYNCSTATE.LAST_VERSION_UPDATED);


        } catch (Exception e1) {


            this.setStatus(SYNCSTATE.UPDATE_ERROR);

            throw e1;
        }


    }
}
