package org.gmu.utils;

import java.io.File;
import java.io.FileWriter;

/**
 * User: ttg
 * Date: 22/03/13
 * Time: 13:11
 * To change this template use File | Settings | File Templates.
 */
public class ApkMaker {

    public static void main(String args[]) throws Exception {
        String basedir = args[0];
        String apkid = args[1];
        String currentVersion = "base";
        File versionFile = new File(basedir + "\\version.txt");
        //0 get current version
        try {
            String readedVersion = FileUtils.readFileNonBlock(versionFile.getAbsolutePath()).trim();
            if (readedVersion != null) currentVersion = readedVersion;
        } catch (Exception ign) {
        }


        //1: copy files

        FileUtils.copyFile(new File(basedir + "/project/AndroidManifest-" + apkid + ".xml"), new File(basedir + "/project/AndroidManifest.xml"));
        FileUtils.copyFile(new File(basedir + "\\project\\src\\org\\gmu\\config\\" + apkid + ".tpl"), new File(basedir + "\\project\\src\\org\\gmu\\config\\Constants.java"));

        //2: replace imports
        String template = "import org.gmu.activities.alpha.";

        System.out.println("Files touched=" + FileUtils.replaceInFiles(template + currentVersion + ".R", template + apkid + ".R", new File(basedir + "\\project\\src"), ".JAVA"));
        //3: write version
        FileWriter wr = new FileWriter(versionFile);
        wr.write(apkid);
        wr.close();

    }
}
