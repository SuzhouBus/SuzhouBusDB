package com.fangjue.suzhoubusdb;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class BusDBOpenHelper extends SQLiteOpenHelper {
    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME_PREFIX = "BUSDB_";
    public static final String BUSES_TABLE_NAME = "buses";
    public static final String KEY_BUS_ID = "busId";
    public static final String KEY_LICENSE_ID = "licenseId";
    public static final String KEY_LINE_ID = "lineId";
    public static final String KEY_REVISION = "revision";
    public static final String KEY_COMMENTS = "comments";
    private static final String BUSES_TABLE_CREATE =
            "CREATE TABLE " + BUSES_TABLE_NAME + " (" +
            KEY_BUS_ID + " VARCHAR(16), " +
            KEY_LICENSE_ID + " VARCHAR(16), " +
            KEY_LINE_ID + " VARCHAR(32), " +
            KEY_REVISION + " INTEGER DEFAULT 0, " +
            KEY_COMMENTS + " TEXT);";
    BusDBOpenHelper(Context context, String city) {
        super(context, DATABASE_NAME_PREFIX + city, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(BUSES_TABLE_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        throw new UnsupportedOperationException();
    }
}
