package org.gmu.utils;


import java.io.*;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.util.LinkedList;
import java.util.List;

/**
 * User: ttg
 * Date: 11/12/12
 * Time: 13:33
 * To change this template use File | Settings | File Templates.
 */
public class FileUtils {


    public static int replaceInFiles(String toReplace, String replace, File sourceDir, String extensionFilter) throws Exception {
        List<File> files = new LinkedList();
        listAll(files, sourceDir, extensionFilter);
        List<String> fileNames = new LinkedList<String>();
        for (int i = 0; i < files.size(); i++) {
            fileNames.add(files.get(i).getAbsolutePath());

        }
        files.clear();
        int touched = 0;
        for (int i = 0; i < fileNames.size(); i++) {
            String name = fileNames.get(i);
            System.out.println("dealing=" + name);

            String contents = readFileNonBlock(name);
            System.gc();
            if (contents.contains(toReplace)) {
                contents = contents.replace(toReplace, replace);
                FileWriter wr = new FileWriter(name);
                wr.write(contents);
                wr.close();
                touched++;
            }

        }
        return touched;
    }

    /**
     * Moves files inside a directory to another
     *
     * @param srcFile
     * @param desFile
     */
    public static void moveFiles(File srcFile, File desFile) {
        if (srcFile.isDirectory()) {
            if (!desFile.isDirectory()) {
                desFile.delete();
            }
            desFile.mkdirs();
            String[] f = srcFile.list();
            for (int i = 0; i < f.length; i++) {   //move al files
                String s = f[i];
                moveFiles(new File(srcFile.getAbsolutePath() + "/" + s), new File(desFile.getAbsolutePath() + "/" + s));
            }

        } else {
            if (desFile.isDirectory()) {
                deleteDirectory(desFile);
            } else {
                desFile.delete();
            }
            srcFile.renameTo(desFile);
        }

    }

    public static String readFile(String path) throws IOException {
        FileInputStream stream = new FileInputStream(new File(path));
        try {
            FileChannel fc = stream.getChannel();
            MappedByteBuffer bb = fc.map(FileChannel.MapMode.READ_ONLY, 0, fc.size());
    /* Instead of using default, pass in a decoder. */
            return Charset.defaultCharset().decode(bb).toString();
        } finally {
            stream.close();
        }
    }

    /**
     * Work around to MappedByteBuffer than non blocks file
     *
     * @param path
     * @return
     * @throws Exception
     */
    public static String readFileNonBlock(String path) throws Exception {
        BufferedReader r = null;
        try {
            BufferedReader reader = new BufferedReader(new FileReader(path));
            //Setup a BufferedReader here
            StringBuffer buf = new StringBuffer();
            String line = reader.readLine();
            while (line != null) {


                buf.append(line + '\n');
                line = reader.readLine();
            }
            return buf.toString();
        } finally {
            if (r != null) r.close();
        }
    }

    public static void copyFile(File src, File destination) {
        try {
            copyToFile(new FileInputStream(src), destination);
        } catch (Exception ign) {
            throw new RuntimeException(ign);
        }
    }

    public static long copyToFile(InputStream src, File destination) {
        try {
            //make destination directory
            new File(destination.getParent()).mkdirs();
            OutputStream fo = new BufferedOutputStream(new FileOutputStream(destination));

           return StreamUtils.readInto(new OutputStream[]{fo}, src,true);



        } catch (Exception ign) {
            throw new RuntimeException(ign);
        }
    }


    public static boolean deleteDirectory(File path) {
        if (path.exists()) {
            File[] files = path.listFiles();
            for (int i = 0; i < files.length; i++) {
                if (files[i].isDirectory()) {
                    deleteDirectory(files[i]);
                } else {
                    files[i].delete();
                }
            }
        }
        return (path.delete());
    }

    /**
     * Determines the encoding of the specified file. If a UTF16 Byte Order Mark (BOM) is found an encoding of "UTF16" is returned.
     * If a UTF8 BOM is found an encoding of "UTF8" is returned. Otherwise the default encoding is returned.
     *
     * @param filePath file path
     * @return "UTF8", "UTF16", or default encoding.
     */
    public static Charset getEncoding(String filePath, Charset defaultEncoding) {
        Charset encoding = defaultEncoding;

        BufferedReader bufferedReader = null;

        try {
            // In order to read files with non-default encoding, specify an encoding in the FileInputStream constructor.
            bufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream(filePath)));

            char buffer[] = new char[3];
            int length = bufferedReader.read(buffer);

            if (length >= 2) {
                if ((buffer[0] == (char) 0xff && buffer[1] == (char) 0xfe) /* UTF-16, little endian */ ||
                        (buffer[0] == (char) 0xfe && buffer[1] == (char) 0xff) /* UTF-16, big endian */) {
                    encoding = Charset.forName("UTF16");
                }
            }
            if (length >= 3) {
                if (buffer[0] == (char) 0xef && buffer[1] == (char) 0xbb && buffer[2] == (char) 0xbf) /* UTF-8 */ {
                    encoding = Charset.forName("UTF8");
                }
            }
        } catch (IOException ex) {
        } finally {
            if (bufferedReader != null) {
                try {
                    bufferedReader.close();
                } catch (IOException ex) {
                }
            }
        }

        return encoding;
    }

    public static void listAll(List<File> ret, File dir, String extensionFilter) {

        File[] files = dir.listFiles();
        for (int i = 0; i < files.length; i++) {
            File file = files[i];
            if (file.isDirectory()) {
                listAll(ret, file, extensionFilter);
            } else {
                if (file.getName().toUpperCase().endsWith(extensionFilter)) {
                    ret.add(file);
                }
            }
        }

    }



}
