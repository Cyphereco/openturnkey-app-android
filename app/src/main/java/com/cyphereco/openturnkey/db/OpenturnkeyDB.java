package com.cyphereco.openturnkey.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

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
    private static final String TRANS_PAYEE_ADDR_COL = "payeeAddr";
    private static final String TRANS_CRYPTO_CURRENCY_COL = "cryptoCurrency";
    private static final String TRANS_CRYPTO_CURRENCY_AMOUNT_COL = "cryptoCurrencyAmount";
    private static final String TRANS_LOCAL_CURRENCY_COL = "localCurrency";
    private static final String TRANS_LOCAL_CURRENCY_AMOUNT_COL = "localCurrencyAmount";
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
            TRANS_PAYEE_ADDR_COL + " VARCHAR(64) NOT NULL, " +
            TRANS_CRYPTO_CURRENCY_COL + " INTEGER, " +
            TRANS_CRYPTO_CURRENCY_AMOUNT_COL + " INTEGER, " +
            TRANS_LOCAL_CURRENCY_COL + " INTEGER, " +
            TRANS_LOCAL_CURRENCY_AMOUNT_COL + " INTEGER, " +
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
        item.setPayeeAddr(cursor.getString(2));
        item.setCryptoCurrency(cursor.getInt(3));
        item.setCryptoCurrencyAmount(cursor.getInt(4));
        item.setLocalCurrency(cursor.getInt(5));
        item.setLocalCurrencyAmount(cursor.getInt(6));
        item.setStatus(cursor.getInt(7));
        item.setComment(cursor.getString(8));
        item.setRawData(cursor.getString(9));

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
        cv.put(TRANS_PAYEE_ADDR_COL, item.getPayeeAddr());
        cv.put(TRANS_CRYPTO_CURRENCY_COL, item.getCryptoCurrency());
        cv.put(TRANS_CRYPTO_CURRENCY_AMOUNT_COL, item.getCryptoCurrencyAmount());
        cv.put(TRANS_LOCAL_CURRENCY_COL, item.getLocalCurrency());
        cv.put(TRANS_LOCAL_CURRENCY_AMOUNT_COL, item.getLocalCurrencyAmount());
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
        cv.put(TRANS_PAYEE_ADDR_COL, item.getPayeeAddr());
        cv.put(TRANS_CRYPTO_CURRENCY_COL, item.getCryptoCurrency());
        cv.put(TRANS_CRYPTO_CURRENCY_AMOUNT_COL, item.getCryptoCurrencyAmount());
        cv.put(TRANS_LOCAL_CURRENCY_COL, item.getLocalCurrency());
        cv.put(TRANS_LOCAL_CURRENCY_AMOUNT_COL, item.getLocalCurrencyAmount());
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
