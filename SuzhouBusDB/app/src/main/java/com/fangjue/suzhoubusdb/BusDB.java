package com.fangjue.suzhoubusdb;

import java.util.ArrayList;
import java.util.List;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class BusDB extends SQLiteOpenHelper {
    private static final int DATABASE_VERSION = 3;
    private static final String DATABASE_NAME_PREFIX = "BUSDB_";
    public static final String kTableBuses = "buses";
    public static final String kBusId = "busId";
    public static final String kLicenseId = "licenseId";
    public static final String kLineId = "lineId";
    public static final String kRevision = "revision";
    public static final String kComments = "comments";
    private static final String BUSES_TABLE_CREATE =
            "CREATE TABLE " + kTableBuses + " (" +
                    kBusId + " VARCHAR(16), " +
                    kLicenseId + " VARCHAR(16), " +
                    kLineId + " VARCHAR(32), " +
                    kRevision + " INTEGER DEFAULT 0, " + // Not used currently.
                    kComments + " TEXT);";

    // Version 2: Add lines table.
    public static final String kTableLines = "lines";
    public static final String kCompanyId = "companyId";
    private static final String LINES_TABLE_CREATE =
            "CREATE TABLE " + kTableLines + " (" +
            kLineId + " VARCHAR(32) NOT NULL, " +
            kCompanyId + " VARCHAR(1) NOT NULL );";

    // Version 3: Add bus categories table.
    public static final String kTableCategories = "categories";
    public static final String kCategoryName = "name";
    public static final String kBusIdMin = "busIdMin";
    public static final String kBusIdMax = "busIdMax";
    private static final String CATEGORIES_TABLE_CREATE =
            "CREATE TABLE " + kTableCategories + " (" +
            kCategoryName + " VARCHAR(256) NOT NULL, " +
            kBusIdMin + " VARCHAR(16) NOT NULL, " +
            kBusIdMax + " VARCHAR(16) NOT NULL );";

    // Version 4: Add bus riding tables (ridings and details).
    public static final String kTableRidings = "ridings";
    public static final String kRidingId = "ridingId";
    public static final String kDate = "date";
    public static final String kTags = "tags";
    private static final String RIDINGS_TABLE_CREATE =
            "CREATE TABLE " + kTableRidings + " (" +
            kRidingId + " INTEGER NOT NULL PRIMARY KEY, " +
            kDate + " DATE NOT NULL, " +
            kTags + " VARCHAR(1024), " +
            kComments + " TEXT );";


    public static final String kTableRidingRecords = "ridingRecords";
    public static final String kTime = "time";
    public static final String kPlace = "place";
    public static final String kLine = "line";
    public static final String kAction = "action";
    private static final String RIDING_RECORDS_TABLE_CREATE =
            "CREATE TABLE " + kTableRidingRecords + " (" +
            kTime + " TIMESTAMP NOT NULL, " +
            kPlace + " VARCHAR(256), " +
            kLine + " VARCHAR(256), " +
            kBusId + " VARCHAR(16), " +
            kRidingId + " INTEGER, " +
            kAction + " VARCHAR(64), " +
            kComments + " TEXT, " +
            "FOREIGN KEY(" + kRidingId + ") REFERENCES " + kTableRidings + "(" + kRidingId + ") );";

    BusDB(Context context, String city) {
        super(context, DATABASE_NAME_PREFIX + city, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(BUSES_TABLE_CREATE +
                        LINES_TABLE_CREATE +
                        CATEGORIES_TABLE_CREATE
        );
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (newVersion > 3)
            throw new UnsupportedOperationException();

        if (oldVersion <= 1) {
            // Upgrade from Version 1 to 2.
            db.execSQL(LINES_TABLE_CREATE);
        }

        if (oldVersion <= 2) {
            // Upgrade from Version 2 to 3.
            db.execSQL(CATEGORIES_TABLE_CREATE);
        }
    }


    /**
     * Returns all buses belonging to the category specified.
     * @param category Bus category name. Returns all buses if null.
     * @return A list of buses belonging to the category.
     */
    public ArrayList<Bus> getBusesByCategory(String category) {
        String where = null;
        List<String> whereArgs = new ArrayList<>();

        if (category != null) {
            Cursor cursor = this.getReadableDatabase().query(kTableCategories,
                    new String[]{kBusIdMin, kBusIdMax},
                    kCategoryName + " = ?", new String[]{category}, null, null, null);
            while(cursor.moveToNext()) {
                String clause = "(" + kBusId + " >= ? AND " + kBusId + " <= ?)";
                if (where == null)
                    where = clause;
                else
                    where += " OR " + clause;
                whereArgs.add(cursor.getString(0));
                whereArgs.add(cursor.getString(1));
            }
            cursor.close();
        }

        return this.queryBuses(where,
                where == null ? null : whereArgs.toArray(new String[whereArgs.size()]), kBusId);
    }

    public ArrayList<Bus> queryBuses(String where, String[] whereArgs, String orderBy) {
        return this.queryBuses(where, whereArgs, orderBy, null);
    }

    public ArrayList<Bus> queryBuses(String where, String[] whereArgs, String orderBy, String limit) {
        return this.queryBuses(where, whereArgs, null, null, orderBy, limit);
    }

    public ArrayList<Bus> queryBuses(String where, String[] whereArgs,
                                     String groupBy, String having, String orderBy, String limit) {
        ArrayList<Bus> buses = new ArrayList<>();

        Cursor cursor = this.getReadableDatabase().query(BusDB.kTableBuses,
                new String[]{BusDB.kBusId, BusDB.kLicenseId, BusDB.kLineId, BusDB.kComments},
                where, whereArgs, groupBy, having, orderBy, limit);
        while (cursor.moveToNext()) {
            buses.add(new Bus(cursor.getString(0), cursor.getString(1), cursor.getString(2),
                    cursor.getString(3)));
        }
        cursor.close();

        return buses;
    }
}
