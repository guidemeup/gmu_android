package org.gmu.pojo;

import org.gmu.utils.Utils;

import java.io.Serializable;
import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: acasquero
 * Date: 8/11/12
 * Time: 17:31
 * To change this template use File | Settings | File Templates.
 */
public class PlaceElement implements Serializable {
    public static final String TYPE_ROUTE = "route";
    public static final String TYPE_PLACE = "place";
    public static final String TYPE_GROUP = "group";

    public static final String MIME_AUDIO = "audio/";
    public static final String TYPE_MULTIMEDIA = "multimedia";
    public static final String TYPE_GUIDE = "guide";
    public static final String TYPE_INDOOR = "indoor";


    private String uid;
    private String title;
    private String type;
    private String WKT;
    private String category;
    private String pointWKT;

    private transient Float distanceToUser = null;


    protected DelegationHashMap<String, String> attributes = new DelegationHashMap<String, String>();

    public Float getDistanceToUser() {
        return distanceToUser;
    }

    public void setDistanceToUser(float distanceToUser) {
        this.distanceToUser = distanceToUser;
    }

    public String getTitle()
    {
        return title;
    }

    public void setTitle(String title) {
        //always capitalize title
        this.title =Utils.capitalizeString(  title);
        this.getAttributes().put("name", title);
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getWKT() {
        return WKT;
    }

    public void setWKT(String WKT) {
        this.WKT = WKT;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public Map<String, String> getAttributes() {
        return attributes;
    }

    public String getPointWKT() {
        return pointWKT;
    }

    public void setPointWKT(String pointWKT) {
        this.pointWKT = pointWKT;
    }

    public boolean equals(Object o) {
        return Utils.equals(this.getUid(), ((PlaceElement) o).getUid());
    }

    public String toString()
    {

        return "Title:"+this.getTitle()+". Dist="+this.getDistanceToUser();

    }

}
