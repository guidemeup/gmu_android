package org.gmu.utils;

import android.util.Log;

import java.io.*;

/**
 * Created GMU
 * User: acasquero
 * Date: 27-may-2011
 * Time: 16:16:51
 */
public class StreamUtils {
    public static byte[] inputStreamToBytes(InputStream in) throws IOException {

        ByteArrayOutputStream out = new ByteArrayOutputStream(1024);

        readInto(new OutputStream[]{out}, in);


        return out.toByteArray();
    }

    public static ByteArrayInputStream consolidateStream(InputStream in) throws Exception {
        byte[] readed = inputStreamToBytes(in);

        return new ByteArrayInputStream(readed);
    }
    public static void readInto(OutputStream[] out, InputStream in) throws IOException {


        readInto( out, in,true);
    }

    public static long readInto(OutputStream[] out, InputStream in, boolean closeInput,OnWorkEventListener listener) throws IOException {
        byte[] buffer = new byte[8192];
        int len;
        long readed=0;
        while ((len = in.read(buffer)) >= 0)
        {
            for (int i = 0; i < out.length; i++) {
                OutputStream outputStream = out[i];
                outputStream.write(buffer, 0, len);

            }

            if(listener!=null)
            {
                listener.onWorkOk(null,len);

            }
            readed+=len;
        }


        if (closeInput) in.close();
        for (int i = 0; i < out.length; i++) {
            OutputStream outputStream = out[i];
            outputStream.close();
        }
        return readed;
    }

    public static long readInto(OutputStream[] out, InputStream in,boolean closeInput) throws IOException {
        return readInto(out, in, closeInput,null);
    }
    public static void readIntoListener(OutputStream[] out, InputStream in,OnWorkEventListener listener) throws IOException {
        readInto(out, in, true,listener);
    }


    public static Thread backgroundReadInto(File file, OutputStream out, InputStream in) throws IOException {
        Thread pipedThread = new PipedThread(in, out, file);
        pipedThread.start();
        return pipedThread;
    }

    private static class PipedThread extends Thread {
        private InputStream in;
        private OutputStream out;
        private File outFile;

        public PipedThread(InputStream _in, OutputStream _out, File _file) {
            in = _in;
            out = _out;
            outFile = _file;

        }

        public void run() {
            try {
                OutputStream[] outs;

                if (outFile != null) {
                    outs = new OutputStream[]{out, new BufferedOutputStream(new FileOutputStream(outFile))};
                } else {
                    outs = new OutputStream[]{out};
                }

                StreamUtils.readInto(outs, in);
                if (outFile != null) {
                    Log.i("PipedThread", "Cached ok>" + outFile.getAbsolutePath());
                } else {
                    Log.i("PipedThread", "Cached ok!");
                }
            } catch (Exception ign) {
                Log.e("PipedThread", "Error reading in=" + ign.getMessage(), ign);
                if (outFile != null) outFile.deleteOnExit();
            }
        }

    }



}
