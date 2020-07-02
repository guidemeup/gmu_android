package org.gmu.utils;

import android.util.Log;
import android.util.Xml;

import org.gmu.pojo.PlaceElement;
import org.gmu.pojo.Relation;
import org.xmlpull.v1.XmlPullParser;

import java.io.*;
import java.util.*;

/**
 * User: ttg
 * Date: 11/11/12
 * Time: 18:40
 * To change this template use File | Settings | File Templates.
 */
public class KmlUtils {


    private Hashtable totalElementsCheck = new Hashtable();
    private static final int MAXELEMENTS_X_CATEGORY = 20000;
    private ArrayList places = new ArrayList();
    private ArrayList relations = new ArrayList();

    public KmlUtils(InputStream f) throws Exception {
        totalElementsCheck = new Hashtable();
        String lastNode = "";

        StringBuffer WKT = new StringBuffer();
        String name = null;
        Map<String, String> attributes = null;
        String lastAttrName = "";
        boolean isLine = true;


        XmlPullParser xpp = Xml.newPullParser();
        xpp.setInput(new BufferedReader(new InputStreamReader(f)));
        int eventType = xpp.getEventType();
        while (eventType != XmlPullParser.END_DOCUMENT) {

            if (eventType == XmlPullParser.START_TAG) {
                lastNode = xpp.getName();

                if (lastNode.equals("LineString")) {
                    isLine = true;
                    WKT.append("MULTILINESTRING((");

                } else if (lastNode.equals("Point")) {
                    isLine = false;
                    WKT.append("POINT(");

                } else if (lastNode.equals("Placemark")) {
                    Object pl = parseElement(WKT.toString(), attributes);

                    if (pl != null) {
                        if (pl instanceof PlaceElement) {
                            places.add(pl);
                        } else if (pl instanceof Relation) {
                            relations.add(pl);
                        }
                    }


                    WKT = new StringBuffer();
                    name = null;
                    attributes = new HashMap<String, String>();

                } else if (lastNode.equals("SimpleData")) {
                    lastAttrName = xpp.getAttributeValue(0);
                    attributes.put(lastAttrName, "");
                }
            } else if (eventType == XmlPullParser.TEXT) {
                if (lastNode.equals("name")) {
                    String text = xpp.getText().trim();
                    if (text.length() > 0) {   //avoids CDATA parsing errors
                        name = text;
                    }
                } else if (lastNode.equals("coordinates")) {
                    String text = xpp.getText().trim();
                    if (!text.equals("")) {
                        WKT.append(text);
                        if (isLine) {
                            WKT.append("))");
                        } else {
                            WKT.append(")");
                        }
                    }


                } else if (lastNode.equals("SimpleData")) {
                    String text = xpp.getText().trim();
                    if (!text.equals("")) {
                        attributes.put(lastAttrName, text);
                    }


                }


            }
            eventType = xpp.next();

        }
        Object pl = parseElement(WKT.toString(), attributes);

        if (pl != null) {
            if (pl instanceof PlaceElement)
            {
                places.add(pl);
            } else if (pl instanceof Relation) {
                relations.add(pl);
            }
        }


    }


    public List<Object> getPlaceElements() {
        return places;
    }

    public List<Relation> getRelations() {
        return relations;
    }

    private Object parseElement(String WKT, Map<String, String> attribs) {


        if (attribs == null) return null;

        String type = attribs.get("type");
        String name = attribs.get("name");


        if (Utils.contains(Relation.ALL_TYPES, type)) {
            Relation rel = new Relation();
            rel.setSourceUID(attribs.get("source"));
            rel.setDestinationUID(attribs.get("destination"));
            rel.setType(type);
            try {
                rel.setOrder(Integer.parseInt(attribs.get("order")));
            } catch (Exception ign) {
            }
            return rel;
        } else {
            PlaceElement pl = new PlaceElement();


            StringBuilder b = new StringBuilder("");
            boolean firstSpace = true;
            char readed;
            for (int i = 0; i < WKT.length(); i++) {
                readed = WKT.charAt(i);
                if (Character.isWhitespace(readed)) {
                    if (firstSpace) {
                        b.append(",");
                        firstSpace = false;
                    }

                } else {
                    firstSpace = true;
                    if (readed == ',') {
                        b.append(' ');
                    } else {
                        b.append(readed);
                    }
                }
            }

            pl.setWKT(b.toString());
            pl.setUid(attribs.get("id"));
            if (Utils.isEmpty(name)) {
                name ="Untitled";// pl.getUid();
            }
            pl.setCategory(attribs.get("category"));
            pl.setType(type);
            pl.setTitle(name);
            Utils.copyMap(attribs, pl.getAttributes());

            if (!acceptInCategory(pl)) return null;
            return pl;
        }//else
    }

    private boolean acceptInCategory(PlaceElement el) {
        if (el.getType().equals(PlaceElement.TYPE_GROUP)) return true;

        Integer total = (Integer) totalElementsCheck.get(el.getCategory());
        if (total == null) total = 0;
        total++;
        totalElementsCheck.put(el.getCategory(), total);
        return total < MAXELEMENTS_X_CATEGORY;
    }


}
