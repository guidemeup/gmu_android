package org.gmu.pojo;

import org.gmu.utils.Utils;

/**
 * User: ttg
 * Date: 2/01/13
 * Time: 13:12
 * To change this template use File | Settings | File Templates.
 */
public class DirectAccess extends PlaceElement implements Comparable<DirectAccess> {


    private PlaceElement delegate = null;


    public String getTitle() {
        if (Utils.isEmpty(super.getTitle())) {
            if (delegate != null) return delegate.getTitle();

        }
        return super.getTitle();
    }

    public Float getDistanceToUser() {
        if (super.getDistanceToUser() == null) {
            if (delegate != null) return delegate.getDistanceToUser();

        }
        return super.getDistanceToUser();
    }


    public String getType() {
        if (Utils.isEmpty(super.getType())) {
            if (delegate != null) return delegate.getType();

        }
        return super.getType();
    }


    public String getPointWKT() {
        if (Utils.isEmpty(super.getPointWKT())) {
            if (delegate != null) return delegate.getPointWKT();

        }
        return super.getPointWKT();
    }

    public String getWKT() {
        if (Utils.isEmpty(super.getWKT())) {
            if (delegate != null) return delegate.getWKT();

        }
        return super.getWKT();
    }

    public String getCategory() {
        if (Utils.isEmpty(super.getCategory())) {
            if (delegate != null) return delegate.getCategory();

        }
        return super.getCategory();
    }


    public PlaceElement getDelegate() {
        return delegate;
    }

    public void setDelegate(PlaceElement delegate) {
        this.delegate = delegate;
        //add delegation attributes
        super.attributes.setDelegation(delegate.getAttributes());
        //set parent
        super.attributes.put("parent", getSourceUID());

    }

    public static final String TYPE_ITINERARY_CONTENT = "itinerary_content";

    public String setSourceUID(String sourceId) {
        return this.getAttributes().put("source", sourceId);
    }

    public String getSourceUID() {
        return this.getAttributes().get("source");
    }

    public String getDestinationUID() {
        return this.getAttributes().get("destination");
    }

    public String getRelationType() {
        return this.getAttributes().get("linktype");
    }

    public String setRelationType(String rtype) {
        return this.getAttributes().put("linktype", rtype);
    }

    public Integer getOrder() {
        return Integer.parseInt(this.getAttributes().get("order"));
    }

    public String getKey() {
        return getSourceUID() + "#" + getRelationType();

    }

    public int compareTo(DirectAccess r) {
        return this.getOrder().compareTo(r.getOrder());
    }
}
