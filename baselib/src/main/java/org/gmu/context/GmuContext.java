package org.gmu.context;

import android.location.Location;
import android.os.Bundle;
import org.gmu.control.Controller;
import org.gmu.dao.OrderDefinition;
import org.gmu.map.MapPosition;
import org.gmu.pojo.NavigationItem;
import org.gmu.pojo.PlaceElement;
import org.gmu.utils.Utils;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

/**
 * User: ttg
 * Date: 8/01/13
 * Time: 12:20
 * Contains context variables, representing application state
 */
public class GmuContext implements Serializable {
    private String currentGuide = null;
    private transient boolean updateInProgress=false;
    public PlaceElement filter = new PlaceElement();
    public Integer playingAudio;
    public String[] playList;
    public Stack<NavigationItem> navigationStack = new Stack();
    public String selectedChild = null;
    public String playingUID = null;
    public transient Location lastUserLocation;
    public transient float lastUserViewAngle=0;
    public long lastUpdated = 0;
    public boolean offlineMode = false;
    public boolean onlyGuidesDownloadedFilter = true;
    public String selectedListUID=null;
    public  String lastPrivateGuideAccessed;
    public  MapPosition lastMapCenterLocation;
    public OrderDefinition order=new OrderDefinition();
    //script that will be fired on the next microsite load

    public Map<String,String> targetedScriptInvocations=new HashMap<String, String>();

    public void setOnlyGuidesDownloadedFilter(boolean onlyGuidesDownloadedFilter) {
        this.onlyGuidesDownloadedFilter = onlyGuidesDownloadedFilter;
        if (filter != null)
        {
            this.filter.getAttributes().put("guidedownloaded", ("" + onlyGuidesDownloadedFilter).toUpperCase());
        }
    }

    public GmuContext() {
        lastUserLocation = null;

    }

    public void serializeToBundle(Bundle bundle) {
        String serial = Utils.objectToString(this);
        bundle.putString("gmucontext", serial);
    }

    public static GmuContext deserializeFromBundle(Bundle bundle) {
        return (GmuContext) Utils.stringToObject(bundle.getString("gmucontext"));
    }

    public void setCurrentGuide(String currentGuide) {
        this.currentGuide = currentGuide;
        ((NavigationItem) navigationStack.peek()).guideuid=currentGuide;
    }

    public String getCurrentGuide() {
        return currentGuide;
    }

    public String toString()
    {
        return "Current guide="+currentGuide+"\n"+
                "Stack size="+navigationStack.size();
    }

    public boolean isUpdateInProgress() {
        return updateInProgress;
    }

    public void setUpdateInProgress(boolean updateInProgress) {
        this.updateInProgress = updateInProgress;
    }
}
