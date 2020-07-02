package org.gmu.pojo;

import java.io.Serializable;
import java.util.*;

/**
 * User: ttg
 * Date: 19/11/12
 * Time: 17:48
 * To change this template use File | Settings | File Templates.
 */
public class NavigationItem implements Serializable
{
    public String uid;
    public String guideuid;
    public int viewAreaid;
    public String groupFilter;
    //contains per UID scroll index
    public Map<String,Integer> scrollIndexes=new HashMap<String,Integer>();

    public NavigationItem(String uid, String groupFilter, int viewAreaid,String guideuid) {
        this.uid = uid;
        this.viewAreaid = viewAreaid;
        this.groupFilter = groupFilter;
        this.guideuid =guideuid;

    }
}
