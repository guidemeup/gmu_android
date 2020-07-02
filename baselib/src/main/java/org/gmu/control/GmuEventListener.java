package org.gmu.control;

import android.location.Location;

/**
 * User: ttg
 * Date: 12/11/12
 * Time: 12:56
 * To change this template use File | Settings | File Templates.
 */
public interface GmuEventListener {
    public static final int VIEW_MAP = 0, VIEW_DETAIL = 1, VIEW_LIST = 2, VIEW_MICROSITE = 3, VIEW_GUIDE_LIST = 4;

    public void goToDetail(String uid);

    public void playRelatedAudio(String[] playList, Integer current);

    public void close();

    public void switchView(int viewId);

    public void onLocationChanged(Location location);

    public void setFavorite(String uid, boolean remove);

    public void switchGuide(String guideId);

    public void refresh();

    public void setAutoCenter(boolean center);


}
