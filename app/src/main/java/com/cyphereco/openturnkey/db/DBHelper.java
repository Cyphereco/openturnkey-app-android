package com.cyphereco.openturnkey.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;

public class DBHelper extends SQLiteOpenHelper {

    private static final int DB_VERSION = 5;
    private static final String DB_NAME = "OpenTurnKey.db";

    private static SQLiteDatabase database = null;

    private DBHelper(Context context, String name, CursorFactory factory, int version) {
        super(context, name, factory, version);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Create transaction table
        db.execSQL(OpenturnkeyDB.CREATE_TRANS_TABLE_SQL);
        // Create address book table
        db.execSQL(OpenturnkeyDB.CREATE_ADDRBOOK_TABLE_SQL);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion == 4 && newVersion == 5) {
            //Add confirmations
            String sql = "alter table " + OpenturnkeyDB.TRANS_TABLE_NAME + " add COLUMN confirmations INTEGER";
            db.execSQL(sql);
        } else {
            // Drop original tables
            db.execSQL("DROP TABLE IF EXISTS " + OpenturnkeyDB.TRANS_TABLE_NAME);
            db.execSQL("DROP TABLE IF EXISTS " + OpenturnkeyDB.ADDR_BOOK_TABLE_NAME);
            // Create new version tables
            onCreate(db);
        }
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Drop original tables
        db.execSQL("DROP TABLE IF EXISTS " + OpenturnkeyDB.TRANS_TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + OpenturnkeyDB.ADDR_BOOK_TABLE_NAME);
        // Create new version tables
        onCreate(db);
    }

    static SQLiteDatabase getDatabase(Context context) {
        if (database == null || !database.isOpen()) {
            database = new DBHelper(context, DB_NAME,
                    null, DB_VERSION).getWritableDatabase();
        }

        return database;
    }
}
