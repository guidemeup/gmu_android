package org.gmu.pojo;

import org.gmu.utils.Utils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * User: ttg
 * Date: 2/01/13
 * Time: 13:12
 * To change this template use File | Settings | File Templates.
 */
public class Relation implements Comparable<Relation>
{


    public static final String RELATION_PARENT = "parent";
    public static final String RELATION_LINK = "link";
    public static final String RELATION_INSIDEMAP = "insidemap";
    public static final String RELATION_FAV = "fav";
    public static final String[] ALL_TYPES = new String[]{RELATION_PARENT, RELATION_LINK, RELATION_INSIDEMAP, RELATION_FAV};


    private Integer order = null;
    private String type;
    private String sourceUID;
    private String destinationUID;
    private String destinationType;

    public Integer getOrder() {
        return order;
    }

    public void setOrder(Integer order) {
        this.order = order;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getSourceUID() {
        return sourceUID;
    }

    public void setSourceUID(String sourceUID) {
        this.sourceUID = sourceUID;
    }

    public String getDestinationUID() {
        return destinationUID;
    }

    public void setDestinationUID(String destinationUID) {
        this.destinationUID = destinationUID;
    }

    public int compareTo(Relation r) {
        if (this.getOrder() == null || r.getOrder() == null) return 0;
        return this.getOrder().compareTo(r.getOrder());
    }

    public String getKey() {
        return getSourceUID() + "#" + getType();

    }

    public String getDestinationType() {
        return destinationType;
    }

    public void setDestinationType(String destinationType) {
        this.destinationType = destinationType;
    }
}
