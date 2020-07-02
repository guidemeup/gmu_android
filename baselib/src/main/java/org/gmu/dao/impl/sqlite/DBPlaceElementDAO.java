package org.gmu.dao.impl.sqlite;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

import android.util.Log;

import androidx.collection.LruCache;

import org.gmu.config.Constants;
import org.gmu.dao.IPlaceElementDAO;
import org.gmu.dao.OrderDefinition;
import org.gmu.dao.impl.AbstractPlaceElementDAO;
import org.gmu.pojo.PlaceElement;
import org.gmu.pojo.Relation;
import org.gmu.pojo.UserData;
import org.gmu.utils.FileUtils;
import org.gmu.utils.KmlUtils;
import org.gmu.utils.PlaceElementComparator;
import org.gmu.utils.Utils;

import java.io.File;
import java.io.FileInputStream;
import java.util.*;

/**
 * User: ttg
 * Date: 14/01/13
 * Time: 12:00
 * To change this template use File | Settings | File Templates.
 */
public class DBPlaceElementDAO extends AbstractPlaceElementDAO implements IPlaceElementDAO
{
    private static boolean CACHE_INITIAL=false;

    //optimization: attributes than can be used for the initial db query filter
    private static final String[] SEARCHABLES_DB_ATTRIBS=new String[]{"url","contact"};



    private static final String TAG = DBPlaceElementDAO.class.getName();
    private LruCache<String, PlaceElement> placeElementCache = new LruCache<String, PlaceElement>(Constants.PLACES_CACHE);
    private LruCache<String, List<Relation>> relationsCache = new LruCache<String, List<Relation>>(Constants.RELATIONS_CACHE);

    private SQLiteDatabase database;
    private PlaceElementSQLHelper dbHelper;
    private LoadListener loadListener;

    public DBPlaceElementDAO(Context context, String base) {
        this( context,base,null);
    }

    public DBPlaceElementDAO(Context context, String base,LoadListener loadListener) {
        super(context, base);
        this.loadListener=loadListener;
        dbHelper = new PlaceElementSQLHelper(context, base);
        open();
        loadModelFromKml();
        if(CACHE_INITIAL)
        {
            //force silenty load to set up cache
            final DBPlaceElementDAO dis=this;

            Thread thread = new Thread()
            {
                public void run()
                {   try {
                    long start=System.currentTimeMillis();
                    dis.list(null, null, ALL, 100, null);
                    Log.i(TAG,"Cache ("+dis.base+") loaded in="+(System.currentTimeMillis()-start)+"ms");
                }catch (Exception ign)
                {
                    Log.i(TAG,"Cache not loaded ("+dis.base+") "+ign);
                }

                }
            };
            thread.setPriority(Thread.MIN_PRIORITY);
            thread.start();

        }



    }

    private void open() throws SQLException {
        if (database == null) database = dbHelper.getWritableDatabase();
        //check indexes to update guides without indexes
        dbHelper.addIndexes(database) ;

    }

    private void close() {
        if (database != null) dbHelper.close();
    }

    private synchronized void loadModelFromKml()
    {
        //obtain lastupdated ts
        long lastUpdate = 0;
        boolean mustReload = false;
        UserData ud = this.loadUserData();
        try {
            lastUpdate = Long.parseLong(ud.get(UserData.LASTUPDATED));
        } catch (Exception ign) {
        }
        database.beginTransaction();
        try {
            if(loadListener!=null){loadListener.onDAOLoadEvent("",0);}
            List dataP = new LinkedList();
            List dataR = new LinkedList();
            String[] ret = new File(getRelatedFileDescriptor(BASE_DIR + "/kml")).list();

            if (ret == null) {   //no kml found, dao not loaded
                return;

            }

            for (int i = 0; i < ret.length; i++) {
                String kml = ret[i];
                if (!kml.toUpperCase().endsWith(".KML")) continue;
                File f = new File(getRelatedFileDescriptor("base/kml/") + kml);
                if (f.lastModified() > lastUpdate) {
                    mustReload = true;
                    break;
                }

            }
            if (!mustReload) {
                return;
            }
            Log.i(TAG,"start");
            for (int i = 0; i < ret.length; i++) {
                String kml = ret[i];
                if (!kml.toUpperCase().endsWith(".KML")) continue;
                File f = new File(getRelatedFileDescriptor("base/kml/") + kml);


                KmlUtils kmlU = new KmlUtils(new FileInputStream(f));
                dataP.addAll(kmlU.getPlaceElements());
                dataR.addAll(kmlU.getRelations());
            }
            Log.i(TAG,"KML Loaded");

            if(loadListener!=null){loadListener.onDAOLoadEvent("",30);}



            //drop data base places  (TODO: beware favs relations)
            database.delete(PlaceElementSQLHelper.TABLE_ATTRIBS, null, null);
            database.delete(PlaceElementSQLHelper.TABLE_PLACE, null, null);

            Log.i(TAG,"Tables deleted");
            if(loadListener!=null){loadListener.onDAOLoadEvent("",35);}
            int deletedrel = database.delete(PlaceElementSQLHelper.TABLE_REL, " not type='fav'", null);

            //insert or update  place Element
            for (int i = 0; i < dataP.size(); i++)
            {

                if(loadListener!=null){loadListener.onDAOLoadEvent("",Utils.obtainRange(35,80,i,dataP.size()));}
                PlaceElement o = (PlaceElement) dataP.get(i);
                saveOrUpdate(o);
            }
            Log.i(TAG,"Insert place done");
            if(loadListener!=null){loadListener.onDAOLoadEvent("",80);}
            //insert or update relations
            for (int i = 0; i < dataR.size(); i++)
            {
                Relation o = (Relation) dataR.get(i);
                saveOrUpdateRelation(o);
            }
            Log.i(TAG,"Insert relation done");
            if(loadListener!=null){loadListener.onDAOLoadEvent("",85);}
            //load attribs from relations
            prepareDB();
            Log.i(TAG,"Prepare DB done");
            //store lastupdated
            ud.put(UserData.LASTUPDATED, "" + System.currentTimeMillis());
            saveOrUpdateUserData(ud);
            //clear caches
            placeElementCache = new LruCache<String, PlaceElement>(Constants.PLACES_CACHE);
            relationsCache = new LruCache<String, List<Relation>>(Constants.RELATIONS_CACHE);
            Log.i(TAG,"Commit!!");
            database.setTransactionSuccessful();


            if(loadListener!=null){loadListener.onDAOLoadEvent("",100);}



            Log.i(TAG,"End!");
        } catch (Exception ign) {
            Log.d(TAG, "error creando dao", ign);
            throw new RuntimeException(ign);
        }finally {
            database.endTransaction();
        }

    }

    private void saveOrUpdateUserData(UserData userData) {
        if (userData == null) return;
        Iterator<String> it = userData.keySet().iterator();
        while (it.hasNext()) {
            String key = it.next();
            ContentValues values = new ContentValues();
            values.put(PlaceElementSQLHelper.COLUMN_UD_KEY, key);
            values.put(PlaceElementSQLHelper.COLUMN_UD_VALUE, userData.get(key));
            int updated = database.update(PlaceElementSQLHelper.TABLE_USERDATA, values, PlaceElementSQLHelper.COLUMN_UD_KEY + "='" + key + "'", null);
            if (updated == 0) {
                long insertId = database.insert(PlaceElementSQLHelper.TABLE_USERDATA, null,
                        values);
            }
        }


    }

    private UserData loadUserData() {
        UserData ret = new UserData();
        Cursor cursor = database.query(PlaceElementSQLHelper.TABLE_USERDATA,
                PlaceElementSQLHelper.ALLUSERDATACOLUMNS, null, null, null, null, null);

        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            ret.put(cursor.getString(0), cursor.getString(1));
            cursor.moveToNext();
        }
        // Make sure to close the cursor
        cursor.close();
        return ret;

    }

    private void saveOrUpdateRelation(Relation rl) {
        ContentValues values = new ContentValues();
        values.put(PlaceElementSQLHelper.COLUMN_RORDER, rl.getOrder());
        values.put(PlaceElementSQLHelper.COLUMN_RTYPE, rl.getType());
        values.put(PlaceElementSQLHelper.COLUMN_RSOURCE, rl.getSourceUID());
        values.put(PlaceElementSQLHelper.COLUMN_RDESTINATION, rl.getDestinationUID());
        //delete existent (update)
        deleteRelation(rl);


        long insertId = database.insert(PlaceElementSQLHelper.TABLE_REL, null,
                values);

    }

    private void deleteRelation(Relation rl) {
        String query = PlaceElementSQLHelper.COLUMN_RSOURCE + "='" + rl.getSourceUID() + "' AND " +
                PlaceElementSQLHelper.COLUMN_RTYPE + "='" + rl.getType() + "' AND ";
        if (Utils.isEmpty(rl.getDestinationUID())) {
            query += PlaceElementSQLHelper.COLUMN_RDESTINATION + " IS NULL";

        } else {
            query += PlaceElementSQLHelper.COLUMN_RDESTINATION + "='" + rl.getDestinationUID() + "'";
        }


        int deleted = database.delete(PlaceElementSQLHelper.TABLE_REL, query, null);

    }

    private void saveOrUpdate(PlaceElement pl) {saveOrUpdateOptimized(pl,false);}

    public void saveOrUpdate(List<PlaceElement> pl)
    {
        //do in transaction
        database.beginTransaction();
        try {
            for (int i = 0; i < pl.size(); i++) {
                PlaceElement placeElement = pl.get(i);
                saveOrUpdateOptimized(placeElement,false);
            }
            database.setTransactionSuccessful();
            Log.i(TAG,"End!");
        } catch (Exception ign) {
            Log.d(TAG, "error updatando", ign);

        }finally {
            database.endTransaction();
        }

    }


    private void save(PlaceElement pl) {saveOrUpdateOptimized(pl,true);}
    private void saveOrUpdateOptimized(PlaceElement pl,boolean fixInsert) {
        if (pl == null||Utils.isEmpty(pl.getUid())) return;


        ContentValues values = new ContentValues();
        values.put(PlaceElementSQLHelper.COLUMN_TITLE, pl.getTitle());
        values.put(PlaceElementSQLHelper.COLUMN_TYPE, pl.getType());
        values.put(PlaceElementSQLHelper.COLUMN_ID, pl.getUid());
        values.put(PlaceElementSQLHelper.COLUMN_WKT, pl.getWKT());
        values.put(PlaceElementSQLHelper.COLUMN_CATEGORY, pl.getCategory());

        //update in db
        int updated =0;
        if(!fixInsert)
        {
            updated = database.update(PlaceElementSQLHelper.TABLE_PLACE, values, PlaceElementSQLHelper.COLUMN_ID + "='" + pl.getUid() + "'", null);

        }

        if (updated == 0)
        {
            long insertId = database.insert(PlaceElementSQLHelper.TABLE_PLACE, null,
                    values);
        }else
        {  //remove previous attribs

            database.delete(PlaceElementSQLHelper.TABLE_ATTRIBS, PlaceElementSQLHelper.COLUMN_PARENT + "='" + pl.getUid() + "'", null);
        }
        //insert attribs
        Iterator it = pl.getAttributes().keySet().iterator();
        values = new ContentValues();
        values.put(PlaceElementSQLHelper.COLUMN_PARENT, pl.getUid());
        while (it.hasNext())
        {
            String next = (String) it.next();
            String value = pl.getAttributes().get(next);
            values.put(PlaceElementSQLHelper.COLUMN_NAME, next);
            values.put(PlaceElementSQLHelper.COLUMN_VALUE, value);
            database.insert(PlaceElementSQLHelper.TABLE_ATTRIBS, null,
                    values);
        }

        //placeElementCache.put(pl.getUid(),pl);
    }

    public List<PlaceElement> list(PlaceElement filter, String parentUID, Set<String> types, Integer maxHierachyDistance
            , OrderDefinition order) {



        Log.d(TAG,"list, parentUID:"+parentUID+"|filter="+filter);

        if (filter == null) filter = new PlaceElement();

        if (filter.getAttributes().containsKey("favorite")) {   //favorite filter always search in maxHierarchy
            //TODO: repasar comportamiento (maxima jerarquia, tipos, etc)
            maxHierachyDistance = 999;


        }


        long start = System.currentTimeMillis();
        boolean startWithParent = false;
        LinkedHashSet added = new LinkedHashSet();

        List filtered = new LinkedList();

        //locate candidate uids

        Set<Relation> candidate = new HashSet<Relation>();
        if (Utils.isEmpty(parentUID))

        {

            //no filter, all places are candidate
            candidate.addAll(createDummyRelations(listPlaces(filter)));

        } else {   //start with parent (TODO: delete)


            PlaceElement parent = this.load(parentUID);
            if(parent==null)
            {
                System.out.println("Pasaqui?");
            }

            if (Utils.contains(types, parent.getType()))
            {   startWithParent = true;
                added.add(parentUID);
                filtered.add(parent);
            }

            //locate candidate uids

            getRelatedUids(parentUID, candidate, maxHierachyDistance);
        }
        Log.i(TAG, "list getRelations takes=" + (System.currentTimeMillis() - start) + "ms");
        start = System.currentTimeMillis();
        //apply filter
        Iterator<Relation> it = candidate.iterator();
        while (it.hasNext())
        {
            Relation r = it.next();
            String uid = r.getDestinationUID();
            if(    (r.getDestinationType()!=null)
                    &&
                    (added.contains(uid)||!Utils.contains(types, r.getDestinationType()))
               )
            {   //apply type filter before load (faster),if relation is not dummy
                continue;
            }


            PlaceElement placeElement = this.load(uid);
            if (placeElement == null) {
                System.out.println("to que es");
            } else
            {   //set tmp order value to allow order by relation order

                placeElement.getAttributes().put("tmporder", genOrderKey(r));


            }

            if (!Utils.contains(types,placeElement.getType())||!inFilter(filter, placeElement)) {
                continue;
            }
            added.add(uid);
            filtered.add(placeElement);
        }
        Log.i(TAG, "list filter+load takes=" + (System.currentTimeMillis() - start) + "ms, total="+filtered.size());
        start = System.currentTimeMillis();

        //sort elements

        if (order != null) {
            List<PlaceElement> toOrder = filtered;
            this.updateDistances(order.userLocation, filtered);
            if (startWithParent && filtered.size() > 0) {   //maintain parent always at start of the list
                toOrder = filtered.subList(1, filtered.size());
            }

            try
            {
                Collections.sort(toOrder, new PlaceElementComparator(order));
            } catch(Throwable ign)
            {   //TODO: temporal  hasta trzar error
                Log.e(TAG,"Error in sort ",ign);


            }


        }


        Log.i(TAG, "order takes=" + (System.currentTimeMillis() - start) + "ms (results=" + filtered.size() + ")");
        return filtered;


    }

    public PlaceElement load(String uid) {
        if (Utils.isEmpty(uid)) return null;
        PlaceElement pl = placeElementCache.get(uid);
        if (pl != null) return pl;
        Cursor cursor = database.query(PlaceElementSQLHelper.TABLE_PLACE,
                PlaceElementSQLHelper.ALLPLACECOLUMNS, PlaceElementSQLHelper.COLUMN_ID + "='" + uid + "'", null, null, null, null);

        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            pl = cursorToPlaceElement(cursor);

            //load attribs
            loadAttribs(pl);
            cursor.moveToNext();

        }
        // Make sure to close the cursor
        cursor.close();

        if (pl != null) {
            updateRepresentativePoint(pl);
            if (pl.getType().equalsIgnoreCase(PlaceElement.TYPE_GUIDE)) {

                pl.getAttributes().put("guidedownloaded", "" + isGuideDownloaded(pl.getUid()));
            }
            placeElementCache.put(uid, pl);

        } else {
            Log.d("EEO", "warning: uid not found=" + uid + " in dao=" + this.getBaseGuideId());
        }

        return pl;
    }

    public List<Relation> getRelations(String sourcePlaceUID, String relationType)
    {
        String key = sourcePlaceUID + "#" + relationType;
        List<Relation> ret = relationsCache.get(key);
        if (ret != null) return ret;
        ret = new LinkedList<Relation>();

        String SQL="SELECT r.sq_order,r.type,r.source,r.destination,p.type FROM relation r,place p WHERE " +
                "r.source='"+sourcePlaceUID+"' AND r.type='"+relationType+"' AND r.destination=p.uid ORDER BY sq_order ASC ";

        Cursor cursor = database.rawQuery(SQL,null);


        cursor.moveToFirst();
        while (!cursor.isAfterLast())
        {
            Relation r = new Relation();
            r.setOrder(cursor.getInt(0));
            r.setType(cursor.getString(1));
            r.setSourceUID(cursor.getString(2));
            r.setDestinationUID(cursor.getString(3));
            r.setDestinationType(cursor.getString(4));
            ret.add(r);
            cursor.moveToNext();

        }
        // Make sure to close the cursor
        cursor.close();

        relationsCache.put(key, ret);
        return ret;


    }

    //TODO: store fav on relation!!
    public void addToFavs(String uid) {    //add relation

        PlaceElement p = this.load(uid);
        p.getAttributes().put("favorite", "true");
        saveOrUpdate(p);
        Relation rl = new Relation();
        rl.setSourceUID(uid);
        rl.setType("fav");
        saveOrUpdateRelation(rl);


    }

    public void delFromFavs(String uid) {
        PlaceElement p = this.load(uid);
        p.getAttributes().remove("favorite");
        saveOrUpdate(p);
        Relation rl = new Relation();
        rl.setSourceUID(uid);
        rl.setType("fav");
        deleteRelation(rl);

    }

    public void destroy() {
        this.close();
    }


    private void loadAttribs(PlaceElement pl) {
        Cursor cursor = database.query(PlaceElementSQLHelper.TABLE_ATTRIBS,
                PlaceElementSQLHelper.ALLATTRIBSCOLUMNS, PlaceElementSQLHelper.COLUMN_PARENT + "='" + pl.getUid() + "'", null, null, null, null);

        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            pl.getAttributes().put(cursor.getString(1), cursor.getString(2));
            cursor.moveToNext();
        }
        // Make sure to close the cursor
        cursor.close();


    }


    private PlaceElement cursorToPlaceElement(Cursor cursor) {
        PlaceElement pl = new PlaceElement();
        pl.setUid(cursor.getString(0));
        pl.setTitle(cursor.getString(1));
        pl.setCategory(cursor.getString(2));
        pl.setType(cursor.getString(3));
        pl.setWKT(cursor.getString(4));


        return pl;

    }

    private void getRelatedUids(String currentId, Set<Relation> destination, int deep)
    {
        if ((deep == 0) || Utils.isEmpty(currentId)) return;
        List<Relation> r = getRelations(currentId, IPlaceElementDAO.PARENT_AND_LINKS_RELATIONS);
        for (int i = 0; i < r.size(); i++)
        {
            Relation relation = r.get(i);
            if (destination.add(relation))
            {
                getRelatedUids(relation.getDestinationUID(), destination, deep - 1);
            }

        }
    }


    /**
     * Denormalizes model to optimize execution
     */
    private void prepareDB() {


        //parent
        database.execSQL("insert into attribute (uid,name,value) select destination,'parent',source from relation where type='parent'");

        //indoor
        database.execSQL("insert into attribute (uid,name,value) select destination,'indoor_map',source from relation where type='insidemap'");
        //favorites  (delete orphan and update attrib)
        database.execSQL("delete from relation where type='fav' and source not in (select uid from place)");
        database.execSQL("insert into attribute (uid,name,value) select source,'favorite','true' from relation where type='fav'");

        //assign empty WKTs from parent WKT
//            database.execSQL("update place set wkt=(\n" +
//                    "select parent.wkt from place parent,relation r\n" +
//                    "where r.source=parent.uid\n" +
//                    "and r.destination=place.uid\n" +
//                    "and r.type='parent')\n" +
//                    "\n" +
//                    "where \n" +
//                    "wkt='' or wkt is null");


    }

    private List<String> listPlaces(PlaceElement filter)
    {

        List<String> params=new ArrayList<String>();
        String baseSQL="SELECT p.UID FROM place p";
        String WHERESQL="";

        if (!Utils.isEmpty(filter.getTitle()))
        {   WHERESQL=" WHERE p.title LIKE ?";

            params.add("%"+filter.getTitle()+"%");
        }

        //add initial attribute filters
        boolean baseSQLWithAt=false;

        for (int i = 0; i < SEARCHABLES_DB_ATTRIBS.length; i++) {
            String searchablesDbAttrib = SEARCHABLES_DB_ATTRIBS[i];
            if(!Utils.isEmpty(filter.getAttributes().get(searchablesDbAttrib)))
            {
                if(!baseSQLWithAt)
                {   if(Utils.isEmpty(WHERESQL)){ WHERESQL=" WHERE ";} else {WHERESQL+=" AND ";};
                    baseSQLWithAt=true;
                    baseSQL+=" ,attribute a";
                    WHERESQL+=" p.uid=a.uid AND ((a.value=? AND a.name='"+searchablesDbAttrib+"')";

                }else
                {
                    WHERESQL+=" OR  (a.value=? AND a.name='"+searchablesDbAttrib+"')";
                }

                params.add(filter.getAttributes().get(searchablesDbAttrib));
                Log.d("EEEO",baseSQL+WHERESQL);

            }
        }
        if(baseSQLWithAt) WHERESQL+=")" ;

        String[] parameters=null;
        if(params.size()!=0){parameters=params.toArray(new String[params.size()]);}

        List ret = new LinkedList<String>();
        Cursor cursor = database.rawQuery(baseSQL+WHERESQL,parameters);

        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            ret.add(cursor.getString(0));

            cursor.moveToNext();

        }
        // Make sure to close the cursor
        cursor.close();
        return ret;
    }

    public void deleteGuide() {
        super.deleteGuide();
        //delete database
        this.close();
        dbHelper.deleteDataBase();
        database = null;

    }

    private List<Relation> createDummyRelations(List<String> uids)
    {
        List l = new LinkedList();
        for (int i = 0; i < uids.size(); i++)
        {
            String uid = uids.get(i);
            Relation r = new Relation();
            r.setSourceUID(uid);
            r.setDestinationUID(uid);
            r.setOrder(-1);
            l.add(r);
        }

        return l;
    }

    private static String genOrderKey(Relation rel) {
        String number = "00000" + rel.getOrder();
        number = number.substring(number.length() - 5);
        return rel.getSourceUID() + number;
    }





}
