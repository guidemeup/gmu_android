package org.gmu.dao.impl.sqlite;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.io.File;

/**
 * User: ttg
 * Date: 14/01/13
 * Time: 11:55
 * To change this template use File | Settings | File Templates.
 */
public class PlaceElementSQLHelper extends SQLiteOpenHelper {





    public static final String TABLE_REL = "relation";
    public static final String COLUMN_RORDER = "sq_order";
    public static final String COLUMN_RTYPE = "type";
    public static final String COLUMN_RSOURCE = "source";
    public static final String COLUMN_RDESTINATION = "destination";

    public static final String[] ALLRELSCOLUMNS = {COLUMN_RORDER,
            COLUMN_RTYPE, COLUMN_RSOURCE, COLUMN_RDESTINATION};

    public static final String TABLE_ATTRIBS = "attribute";
    public static final String COLUMN_PARENT = "uid";
    public static final String COLUMN_NAME = "name";
    public static final String COLUMN_VALUE = "value";

    public static final String[] ALLATTRIBSCOLUMNS = {COLUMN_PARENT,
            COLUMN_NAME, COLUMN_VALUE};

    public static final String TABLE_PLACE = "place";
    public static final String COLUMN_ID = "uid";
    public static final String COLUMN_TITLE = "title";
    public static final String COLUMN_CATEGORY = "cat";
    public static final String COLUMN_TYPE = "type";
    public static final String COLUMN_WKT = "wkt";

    public static final String[] ALLPLACECOLUMNS = {COLUMN_ID,
            COLUMN_TITLE, COLUMN_CATEGORY, COLUMN_TYPE, COLUMN_WKT};


    public static final String TABLE_USERDATA = "userdata";
    public static final String COLUMN_UD_KEY = "key";
    public static final String COLUMN_UD_VALUE = "value";
    public static final String[] ALLUSERDATACOLUMNS = {COLUMN_UD_KEY,
            COLUMN_UD_VALUE};

    //private static final String DATABASE_NAME = "places.db";
    private static final int DATABASE_VERSION = 5;
    private final File mDatabaseFile;
    // Database creation sql statement
    private static final String DATABASE_CREATE = "create table "
            + TABLE_PLACE + "(" + COLUMN_ID
            + " text primary key , " + COLUMN_TITLE
            + " text not null, " + COLUMN_TYPE
            + " text not null, " + COLUMN_CATEGORY
            + " text, " + COLUMN_WKT
            + " text" +
            ");";

    // Database creation sql statement
    private static final String DATABASE_CREATE_ATT = "create table "
            + TABLE_ATTRIBS + "(" + COLUMN_PARENT
            + " text not null, " + COLUMN_NAME
            + " text not null, " + COLUMN_VALUE
            + " text" +
            ");";
    // Database creation sql statement
    private static final String DATABASE_CREATE_REL = "create table "
            + TABLE_REL + "(" + COLUMN_RORDER
            + " integer default 0, " + COLUMN_RTYPE
            + " text not null, " + COLUMN_RSOURCE
            + " text not null, " + COLUMN_RDESTINATION
            + " text" +
            ");";

    private static final String DATABASE_CREATE_CFG = "create table "
            + TABLE_USERDATA + "(" + COLUMN_UD_KEY
            + " text not null, " + COLUMN_UD_VALUE
            + " text" +
            ");";


    public static final String[][] INDEXES=new String[][]
            {       {TABLE_REL,COLUMN_RTYPE},
                    {TABLE_REL,COLUMN_RSOURCE},
                    {TABLE_REL,COLUMN_RDESTINATION},
                    {TABLE_PLACE,COLUMN_TITLE},
                    {TABLE_PLACE,COLUMN_TYPE},
                    {TABLE_ATTRIBS,COLUMN_PARENT}


            };

    private static final String getIndex(String[] idx)
    {
            return "CREATE INDEX "+ idx[0]+"_"+idx[1]+"_idx "+
                    "ON "+idx[0]+" ("+idx[1]+");";

    }




    public PlaceElementSQLHelper(Context context, String baseId) {
        super(context, baseId + ".db", null, DATABASE_VERSION);
        mDatabaseFile = context.getDatabasePath(baseId + ".db");
    }

    @Override
    public void onCreate(SQLiteDatabase database) {
        database.execSQL(DATABASE_CREATE);
        database.execSQL(DATABASE_CREATE_ATT);
        database.execSQL(DATABASE_CREATE_REL);
        database.execSQL(DATABASE_CREATE_CFG);
        addIndexes(database);

    }
    public void addIndexes(SQLiteDatabase database)
    {
        //add indexes if don't exists



        for (int i = 0; i < INDEXES.length; i++) {
            String[] index = INDEXES[i];
           if(existIndex(index , database)) return;

            database.execSQL(getIndex(index));
        }

    }

    private boolean existIndex(String[] idx ,SQLiteDatabase database)
    {


        Cursor cursor = database.rawQuery("PRAGMA index_info("+idx[0]+"_"+idx[1]+"_idx "+")",null);

        boolean found=cursor.getCount()>0;
        cursor.close();
        return found;


    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.w(PlaceElementSQLHelper.class.getName(),
                "Upgrading database from version " + oldVersion + " to "
                        + newVersion + ", which will destroy all old data");
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_ATTRIBS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_PLACE);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_REL);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USERDATA);
        onCreate(db);
    }

    public void deleteDataBase() {
        // try to delete the file
        mDatabaseFile.delete();
    }


    public static void main (String[] args)
    {
        for (int i = 0; i < INDEXES.length; i++) {
            String[] index = INDEXES[i];
            System.out.println(getIndex(index));
        }
    }


}
