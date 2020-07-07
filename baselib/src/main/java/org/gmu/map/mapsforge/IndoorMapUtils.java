package org.gmu.map.mapsforge;

import java.io.File;

public class IndoorMapUtils {

    private static String basePath;

    private final byte startZoom, maxZoom;

    public IndoorMapUtils(String _basePath) {
        basePath = _basePath;

        //detect zoom bounds
        int startZoomR = 18;
        int maxZoomR = 0;
        for (int i = 0; i < 19; i++) {
            File f = new File(_basePath + "/" + i);
            if (f.exists()) {
                if (i < startZoomR) {
                    startZoomR = i;
                }
                if (i > maxZoomR) {
                    maxZoomR = i;
                }

            }

        }


        startZoom = (byte) maxZoomR;
        maxZoom = (byte) maxZoomR;


    }

    public byte getStartZoom() {
        return startZoom;
    }

    public byte getMaxZoom() {
        return maxZoom;
    }
}
