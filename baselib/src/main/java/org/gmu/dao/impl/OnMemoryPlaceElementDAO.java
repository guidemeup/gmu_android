package org.gmu.dao.impl;

import android.content.Context;
import org.gmu.dao.IPlaceElementDAO;
import org.gmu.dao.OrderDefinition;
import org.gmu.pojo.PlaceElement;
import org.gmu.pojo.Relation;
import org.gmu.utils.KmlUtils;
import org.gmu.utils.PlaceElementComparator;
import org.gmu.utils.Utils;

import java.io.File;
import java.io.FileInputStream;
import java.util.*;

/**
 * User: ttg
 * Date: 11/11/12
 * Time: 21:10
 * To change this template use File | Settings | File Templates.
 *
 * @deprecated use DBDAO
 */
public class OnMemoryPlaceElementDAO extends AbstractPlaceElementDAO implements IPlaceElementDAO {

    private static int MAXSEARCHRESULTS = 40;

    private List<Object> baseList = null;

    private Hashtable placeElementTable = new Hashtable();
    protected Hashtable<String, List<Relation>> relationsTable = new Hashtable<String, List<Relation>>();


    public OnMemoryPlaceElementDAO(Context _context, String base) {
        super(_context, base);

    }

    public List<PlaceElement> list(PlaceElement filter, String parentUID, Set<String> types, Integer maxHierachyDistance, OrderDefinition order) {
        HashSet added = new HashSet();


        if (baseList == null) {
            baseList = loadModelFromKml();

        }

        boolean filterByNameKO = true;
        boolean filterByGroupKO = true;
        List filtered = new ArrayList();

        if (Utils.isEmpty(parentUID))

        {
            parentUID = null;
        } else {   //start with group (TODO: delete)
            PlaceElement parent = this.load(parentUID);
            if (Utils.contains(types, parent.getType())) {
                added.add(parentUID);
                filtered.add(parent);
            }

        }


        for (int i = 0; i < baseList.size() && filtered.size() < MAXSEARCHRESULTS; i++) {


            PlaceElement placeElement = (PlaceElement) baseList.get(i);
            if (!Utils.contains(types, placeElement.getType()))

            {
                continue;
            }
            //search minimum link or parent distance

            int distance = 1;
            if (parentUID != null) {   //filter by group
                distance = getMinLinkDistance(parentUID, placeElement.getUid());
            }


            if (distance == -1) {
                filterByGroupKO = true;
            } else if (distance > maxHierachyDistance) {
                filterByGroupKO = true;
            } else {
                filterByGroupKO = false;
            }
            filterByNameKO = !inFilter(filter, placeElement);


            if ((!filterByGroupKO && !filterByNameKO)) {
                if (!added.contains(placeElement.getUid())) {
                    added.add(placeElement.getUid());
                    filtered.add(placeElement);
                }

            }

        }
        return filtered;
    }

    public PlaceElement load(String uid) {
        if (uid == null) return null;
        if (baseList == null) {
            baseList = loadModelFromKml();

        }

        return (PlaceElement) placeElementTable.get(uid);
    }


    public List<Relation> getRelations(String sourcePlaceUID, String relationType) {

        Relation template = new Relation();
        template.setSourceUID(sourcePlaceUID);
        template.setType(relationType);
        List<Relation> retorno = relationsTable.get(template.getKey());
        if (retorno == null) retorno = new ArrayList<Relation>();
        return retorno;


    }


    public String[] getPlayList(String placeUID) {   //get related multimedia
        List<PlaceElement> l = getRelatedPlaceElements(placeUID, IPlaceElementDAO.PARENT_AND_LINKS_RELATIONS, PLACE_RELATED_ITEMS);

        String[] ret = new String[l.size()];
        for (int i = 0; i < l.size(); i++) {
            PlaceElement placeElement = l.get(i);
            ret[i] = placeElement.getUid();
        }
        return ret;

    }

    public void addToFavs(String uid) {    //add relation

        PlaceElement p = this.load(uid);
        p.getAttributes().put("favorite", "true");

    }

    public void delFromFavs(String uid) {
        PlaceElement p = this.load(uid);
        p.getAttributes().remove("favorite");

    }

    public void destroy() {
        //To change body of implemented methods use File | Settings | File Templates.
    }


    private synchronized List<Object> loadModelFromKml() {
        if (baseList != null) return baseList;
        try {
            List dataP = new ArrayList();
            List dataR = new ArrayList();
            String[] ret = new File(getRelatedFileDescriptor(BASE_DIR + "/kml")).list();


            for (int i = 0; i < ret.length; i++) {
                String kml = ret[i];
                if (!kml.toUpperCase().endsWith(".KML")) continue;
                KmlUtils kmlU = new KmlUtils(new FileInputStream(getRelatedFileDescriptor("base/kml/") + kml));
                dataP.addAll(kmlU.getPlaceElements());
                dataR.addAll(kmlU.getRelations());
            }


            loadHierarchy(dataP);
            loadRelations(dataR);
            Collections.sort(dataP, new PlaceElementComparator(new OrderDefinition()));
            return dataP;
        } catch (Exception ign) {
            throw new RuntimeException(ign);
        }

    }

    private void loadRelations(List<Object> data) {
        relationsTable = new Hashtable<String, List<Relation>>();
        for (int i = 0; i < data.size(); i++) {
            Relation relation = (Relation) data.get(i);
            List<Relation> rels = relationsTable.get(((Relation) relation).getKey());
            if (rels == null) {
                rels = new ArrayList();
            }
            rels.add((Relation) relation);
            relationsTable.put(((Relation) relation).getKey(), rels);
            //update redundant children "parent" , "indoor_map" and "favorite" attributes to optimize searches


            if (relation.getType().equals(Relation.RELATION_PARENT)) {
                PlaceElement p = (PlaceElement) placeElementTable.get(relation.getDestinationUID());
                if (p != null) {
                    p.getAttributes().put("parent", relation.getSourceUID());
                    //set WKT if empty
                    if (Utils.isEmpty(p.getWKT())) {
                        PlaceElement parent = (PlaceElement) placeElementTable.get(relation.getSourceUID());
                        p.setWKT(parent.getWKT());

                    }

                }
            } else if (relation.getType().equals(Relation.RELATION_INSIDEMAP)) {
                PlaceElement p = (PlaceElement) placeElementTable.get(relation.getDestinationUID());
                if (p != null) {
                    p.getAttributes().put("indoor_map", relation.getSourceUID());
                }
            } else if (relation.getType().equals(Relation.RELATION_FAV)) {
                PlaceElement p = (PlaceElement) placeElementTable.get(relation.getSourceUID());
                if (p != null) {
                    p.getAttributes().put("favorite", "true");
                }
            }

        }
    }

    private void loadHierarchy(List<Object> data) {
        placeElementTable = new Hashtable();

        for (int i = 0; i < data.size(); i++) {
            PlaceElement placeElement = (PlaceElement) data.get(i);
            placeElementTable.put(placeElement.getUid(), placeElement);

        }

    }

    /**
     * returns min hierarchy distance between 2 places counting only parent relation
     *
     * @param groupUID
     * @param elemUID
     * @return
     */
    private int getHierarchyDistance(String groupUID, String elemUID) {
        if (!Utils.isEmpty(groupUID) && elemUID.equals(groupUID)) return 0;
        PlaceElement pl = (PlaceElement) placeElementTable.get(elemUID);
        String uid = pl.getAttributes().get("parent");

        //TODO: guides haven't parents (crear root uid?)
        if (Utils.isEmpty(groupUID) && pl.getType().equals(PlaceElement.TYPE_GUIDE) && Utils.isEmpty(uid)) return 1;

        if (Utils.isEmpty(uid)) {
            return -1;
        }
        int distance = getHierarchyDistance(groupUID, uid);
        if (distance == -1) {
            return -1;
        } else return 1 + distance;


    }

    /**
     * returns min hierarchy distance between 2 places counting links and parents
     *
     * @param groupUID
     * @param elemUID
     * @return
     */
    private int getMinLinkDistance(String groupUID, String elemUID) {

        //1 search parent distance
        int minDistParent = getHierarchyDistance(groupUID, elemUID);
        int minHieDistance = -1;
        if (minDistParent == -1 || minDistParent > 1) {
            //2 search link distance

            List<PlaceElement> targets = getRelatedPlaceElements(groupUID, PARENT_AND_LINKS_RELATIONS, null);

            for (int i = 0; targets != null && i < targets.size(); i++) {
                PlaceElement placeElement = targets.get(i);

                int retDist = getMinLinkDistance(placeElement.getUid(), elemUID);
                if ((retDist != -1)) {
                    if ((minHieDistance == -1) || (retDist < minHieDistance)) {
                        minHieDistance = 1 + retDist;
                    }

                }
                if (minHieDistance == 1) {   //stop process minimum reached
                    return 1;
                }


            }
        }
        return Utils.minPositive(minDistParent, minHieDistance);

    }


}
