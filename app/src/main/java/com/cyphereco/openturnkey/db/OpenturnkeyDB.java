package com.cyphereco.openturnkey.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * OpenturnkeyDB.java - This is used to operate database of OpenTurnKey
 *
 */
public class OpenturnkeyDB {
    /**
     * This is transaction log table name of database
     */
    public static final String TRANS_TABLE_NAME = "transactionLog";
    /**
     * This is address book table name of database
     */
    public static final String ADDR_BOOK_TABLE_NAME = "addrBook";

    // Columns of transaction table
    private static final String TRANS_KEY_ID_COL = "_id";
    private static final String TRANS_DATETIME_COL = "datetime";
    private static final String TRANS_PAYER_ADDR_COL = "payerAddr";
    private static final String TRANS_PAYEE_ADDR_COL = "payeeAddr";
    private static final String TRANS_AMOUNT_COL = "amount";
    private static final String TRANS_FEE_COL = "fee";
    private static final String TRANS_STATUS_COL = "status";
    private static final String TRANS_COMMENTS_COL = "comments";
    private static final String TRANS_RAW_DATA_COL = "rawData";

    // Columns of address book
    private static final String ADDRBOOK_KEY_ID_COL = "_id";
    private static final String ADDRBOOK_ADDR_COL = "address";
    private static final String ADDRBOOK_USR_NAME_COL = "userName";

    /**
     * SQL command of creating transaction table
     */
    public static final String CREATE_TRANS_TABLE_SQL = "CREATE TABLE " + TRANS_TABLE_NAME + " (" +
            TRANS_KEY_ID_COL + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
            TRANS_DATETIME_COL + " DateTime NOT NULL, " +
            TRANS_PAYER_ADDR_COL + " VARCHAR(64) NOT NULL, " +
            TRANS_PAYEE_ADDR_COL + " VARCHAR(64) NOT NULL, " +
            TRANS_AMOUNT_COL + " DOUBLE, " +
            TRANS_FEE_COL + " DOUBLE, " +
            TRANS_STATUS_COL + " INTEGER, " +
            TRANS_COMMENTS_COL + " TEXT, " +
            TRANS_RAW_DATA_COL + " TEXT " +
            ");";

    /**
     * SQL command of creating address book table
     */
    public static final String CREATE_ADDRBOOK_TABLE_SQL = "CREATE TABLE " +
            ADDR_BOOK_TABLE_NAME + " (" +
            ADDRBOOK_ADDR_COL + " VARCHAR(64) PRIMARY KEY NOT NULL, " +
            ADDRBOOK_USR_NAME_COL + " VARCHAR(128) NOT NULL " +
            ");";

    // Database object
    private SQLiteDatabase otkDB = null;

    private DBTransItem generateTransItemByQueryResult(Cursor cursor) {
        DBTransItem item = new DBTransItem();

        item.setId(cursor.getLong(0));
        item.setDatetime(cursor.getLong(1));
        item.setPayerAddr(cursor.getString(2));
        item.setPayeeAddr(cursor.getString(3));
        item.setAmount(cursor.getDouble(4));
        item.setFee(cursor.getDouble(5));
        item.setStatus(cursor.getInt(6));
        item.setComment(cursor.getString(7));
        item.setRawData(cursor.getString(8));

        return item;
    }

    private DBAddrItem generateAddrbookItemByQueryResult(Cursor cursor) {
        DBAddrItem item = new DBAddrItem();

        item.setAddress(cursor.getString(0));
        item.setName(cursor.getString(1));

        return item;
    }

    public OpenturnkeyDB(Context context) {
        otkDB = DBHelper.getDatabase(context);

    }

    public void close() {
        otkDB.close();
    }

    public int getTransactionCount() {
        int ret = 0;

        Cursor cursor = otkDB.rawQuery(
                "SELECT COUNT(*) FROM " + TRANS_TABLE_NAME,null);
        if (cursor.moveToNext()) {
            ret = cursor.getInt(0);
        }
        return ret;
    }

    public DBTransItem addTransaction(DBTransItem item) {
        ContentValues cv = new ContentValues();

        cv.put(TRANS_DATETIME_COL, item.getDatetime());
        cv.put(TRANS_PAYER_ADDR_COL, item.getPayerAddr());
        cv.put(TRANS_PAYEE_ADDR_COL, item.getPayeeAddr());
        cv.put(TRANS_AMOUNT_COL, item.getAmount());
        cv.put(TRANS_FEE_COL, item.getFee());
        cv.put(TRANS_STATUS_COL, item.getStatus());
        cv.put(TRANS_COMMENTS_COL, item.getComment());
        cv.put(TRANS_RAW_DATA_COL, item.getRawData());

        long id = otkDB.insert(TRANS_TABLE_NAME, null, cv);
        item.setId(id);

        return item;
    }

    public boolean updateTransaction(DBTransItem item) {
        ContentValues cv = new ContentValues();

        cv.put(TRANS_DATETIME_COL, item.getDatetime());
        cv.put(TRANS_PAYER_ADDR_COL, item.getPayeeAddr());
        cv.put(TRANS_PAYEE_ADDR_COL, item.getPayeeAddr());
        cv.put(TRANS_AMOUNT_COL, item.getAmount());
        cv.put(TRANS_FEE_COL, item.getFee());
        cv.put(TRANS_STATUS_COL, item.getStatus());
        cv.put(TRANS_COMMENTS_COL, item.getComment());
        cv.put(TRANS_RAW_DATA_COL, item.getRawData());

        String where = TRANS_KEY_ID_COL + "=" + item.getId();

        return otkDB.update(TRANS_TABLE_NAME, cv, where, null) > 0;
    }

    public boolean deleteTransactionById(long id) {
        String where = TRANS_KEY_ID_COL + "=" + id;
        return otkDB.delete(TRANS_TABLE_NAME, where , null) > 0;
    }

    public DBTransItem getTransactionItemById(long id) {
        Cursor cursor = otkDB.query(TRANS_TABLE_NAME, null,
                TRANS_KEY_ID_COL + "=?", new String[] {String.valueOf(id)},
                null, null, null, null);
        if (1 == cursor.getCount()) {
            cursor.moveToNext();
            return generateTransItemByQueryResult(cursor);
        }
        return null;
    }

    public List<DBTransItem> getAllTransaction() {
        List<DBTransItem> result = new ArrayList<>();
        Cursor cursor = otkDB.query(TRANS_TABLE_NAME, null,null,null,
                null,null,null,null);
        while (cursor.moveToNext()) {
            result.add(generateTransItemByQueryResult(cursor));
        }

        cursor.close();
        return result;
    }

    public int getAddrbookCount() {
        int ret = 0;

        Cursor cursor = otkDB.rawQuery(
                "SELECT COUNT(*) FROM " + ADDR_BOOK_TABLE_NAME,null);
        if (cursor.moveToNext()) {
            ret = cursor.getInt(0);
        }
        return ret;
    }

    public DBAddrItem addAddress(DBAddrItem item) {
        ContentValues cv = new ContentValues();

        cv.put(ADDRBOOK_ADDR_COL, item.getAddress());
        cv.put(ADDRBOOK_USR_NAME_COL, item.getName());

        if (0 > otkDB.insert(ADDR_BOOK_TABLE_NAME, null, cv)) {
            return null;
        }
        return item;
    }

    public boolean updateAddressbook(DBAddrItem item) {
        ContentValues cv = new ContentValues();

        cv.put(ADDRBOOK_USR_NAME_COL, item.getName());

        String where = ADDRBOOK_ADDR_COL + "=" + item.getAddress();

        return otkDB.update(ADDR_BOOK_TABLE_NAME, cv, where, null) > 0;
    }

    public boolean deleteAddressbookByAddr(String address) {
        return otkDB.delete(ADDR_BOOK_TABLE_NAME,
                ADDRBOOK_ADDR_COL + "=?" , new String[]{address}) > 0;
    }

    public List<DBAddrItem> getAllAddressbook() {
        List<DBAddrItem> result = new ArrayList<>();
        Cursor cursor = otkDB.query(ADDR_BOOK_TABLE_NAME, null,null,null,
                null,null,null,null);
        while (cursor.moveToNext()) {
            result.add(generateAddrbookItemByQueryResult(cursor));
        }

        cursor.close();
        return result;
    }
}
