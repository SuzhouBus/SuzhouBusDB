package com.fangjue.suzhoubusdb;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class BusDBOpenHelper extends SQLiteOpenHelper {
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
                    kRevision + " INTEGER DEFAULT 0, " +
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

    BusDBOpenHelper(Context context, String city) {
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
}
