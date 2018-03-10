package com.fangjue.suzhoubusdb;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class BusDB extends SQLiteOpenHelper {
    private static BusDB instance;
    private static final int DATABASE_CURRENT_VERSION = 4;
    private static final String DATABASE_NAME_PREFIX = "BUSDB_";

    // Version 1: Only `buses` table.
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

    // Version 2: Add `lines` table.
    public static final String kTableLines = "lines";
    public static final String kCompanyId = "companyId";
    private static final String LINES_TABLE_CREATE =
            "CREATE TABLE " + kTableLines + " (" +
            kLineId + " VARCHAR(32) NOT NULL, " +
            kCompanyId + " VARCHAR(1) NOT NULL );";

    // Version 3: Add bus `categories` table.
    public static final String kTableCategories = "categories";
    public static final String kCategoryName = "name";
    public static final String kBusIdMin = "busIdMin";
    public static final String kBusIdMax = "busIdMax";
    private static final String CATEGORIES_TABLE_CREATE =
            "CREATE TABLE " + kTableCategories + " (" +
            kCategoryName + " VARCHAR(256) NOT NULL, " +
            kBusIdMin + " VARCHAR(16) NOT NULL, " +
            kBusIdMax + " VARCHAR(16) NOT NULL );";

    // Version 4: Add bus riding tables (`ridings` and `ridingRecords`).
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

    // Version 4: Add realtime tables (`realtimeLines` & `realtimeLineRelations`)
    public static final String kTableRealtimeLines = "realtimeLines";
    public static final String kLineName = "lineName";
    public static final String kLineDirection = "lineDirection";
    public static final String kLineGuid = "lineGuid";
    public static final String kLineStatus = "lineStatus";
    private static final String REALTIME_LINES_TABLE_CREATE =
            "CREATE TABLE " + kTableRealtimeLines + " ("  +
            kLineName + " VARCHAR(256), " +
            kLineDirection + " VARCHAR(900), " +
            kLineGuid + " VARCHAR(40), " +
            kLineStatus + " INTEGER DEFAULT 0, " +
            "PRIMARY KEY(" + kLineGuid + ") );";

    public static final String kTableRealtimeLineRelations = "realtimeLineRelations";
    public static final String kLineRelation = "relation";
    private static final String REALTIME_LINES_RELATIONS_TABLE_CREATE =
            "CREATE TABLE " + kTableRealtimeLineRelations + " (" +
            kLineGuid + " VARCHAR(40), " +
            kLineRelation + " VARCHAR(256), " +
            kLineName + " VARCHAR(256), " +
            kLineDirection + " VARCHAR(900) );";
    BusDB(Context context, String city) {
        super(context, DATABASE_NAME_PREFIX + city, null, DATABASE_CURRENT_VERSION);
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
        if (newVersion > DATABASE_CURRENT_VERSION)
            throw new UnsupportedOperationException();

        if (oldVersion <= 1) {
            // Upgrade from Version 1 to 2.
            db.execSQL(LINES_TABLE_CREATE);
        }

        if (oldVersion <= 2) {
            // Upgrade from Version 2 to 3.
            db.execSQL(CATEGORIES_TABLE_CREATE);
        }

        if (oldVersion <= 3) {
            // Upgrade from Version 3 to 4.
            db.execSQL(RIDING_RECORDS_TABLE_CREATE);
        }

        if (oldVersion <= 4) {
            // Upgrade from Version 4 to 5.
            db.execSQL(REALTIME_LINES_TABLE_CREATE);
            db.execSQL(REALTIME_LINES_RELATIONS_TABLE_CREATE);
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

    public List<RealtimeLine> queryRealtimeLines(String query, int limit) {
        ArrayList<RealtimeLine> results = new ArrayList<>();
        HashSet<String> relatedLinesGuidSet = new HashSet<>();
        HashMap<String, HashMap<String, ArrayList<RealtimeRelatedLine>>> relatedLines = new HashMap<>();
        String where = BusDB.kLineName + " LIKE '%' || ? || '%'";

        Cursor cursor = this.getReadableDatabase().query(BusDB.kTableRealtimeLineRelations,
                new String[]{BusDB.kLineName, BusDB.kLineDirection, BusDB.kLineGuid, BusDB.kLineRelation},
                where,
                new String[]{query},
                null, null, null);
        while(cursor.moveToNext()) {
            String name = cursor.getString(0);
            String direction = cursor.getString(1);
            String guid = cursor.getString(3);
            relatedLinesGuidSet.add(guid);
            if (!relatedLines.containsKey(name)) {
                relatedLines.put(name, new HashMap<String, ArrayList<RealtimeRelatedLine>>());
            }
            if (!relatedLines.get(name).containsKey(direction)) {
                relatedLines.get(name).put(direction, new ArrayList<RealtimeRelatedLine>());
            }
            relatedLines.get(name).get(direction).add(
                    new RealtimeRelatedLine(name, direction, guid, cursor.getString(3)));
        }
        cursor.close();

        cursor = this.getReadableDatabase().query(BusDB.kTableRealtimeLines,
                new String[]{BusDB.kLineGuid, BusDB.kLineName, BusDB.kLineDirection, BusDB.kLineStatus},
                where +  "AND " + BusDB.kLineStatus + " = 0",
                new String[]{query},
                null, null, null, limit > 0 ? Integer.toString(limit) : null);
        while (cursor.moveToNext()) {
            String guid = cursor.getString(0);
            if (!relatedLinesGuidSet.contains(guid)) {
                results.add(new RealtimeLine(guid, cursor.getString(1), cursor.getString(2), cursor.getInt(3)));
            }
        }
        cursor.close();

        for(Map.Entry<String, HashMap<String, ArrayList<RealtimeRelatedLine>>> lineEntry: relatedLines.entrySet()) {
            String name = lineEntry.getKey();
            HashMap<String, ArrayList<RealtimeRelatedLine>> directions = lineEntry.getValue();
            for(Map.Entry<String, ArrayList<RealtimeRelatedLine>> directionEntry: directions.entrySet()) {
                String direction = directionEntry.getKey();
                ArrayList<RealtimeRelatedLine> lines = directionEntry.getValue();
                results.add(0, new RealtimeLine(null, name, direction, 0, lines));
            }
        }

        if (results.size() > limit)
            return results.subList(0, limit);

        return results;
    }

    public static synchronized BusDB getInstance(Context context) {
        if (BusDB.instance == null) {
            BusDB.instance = new BusDB(context, "Suzhou");
        }
        return BusDB.instance;
    }
}
