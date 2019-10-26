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
    private static final String TAG = OpenturnkeyDB.class.getSimpleName();

    /**
     * This is transaction log table name of database
     */
    static final String TRANS_TABLE_NAME = "transactionLog";
    /**
     * This is address book table name of database
     */
    static final String ADDR_BOOK_TABLE_NAME = "addrBook";

    // Columns of transaction table
    private static final String TRANS_KEY_ID_COL = "_id";
    private static final String TRANS_DATETIME_COL = "datetime";
    private static final String TRANS_HASH_COL = "hash";
    private static final String TRANS_PAYER_ADDR_COL = "payerAddr";
    private static final String TRANS_PAYEE_ADDR_COL = "payeeAddr";
    private static final String TRANS_AMOUNT_COL = "amount";
    private static final String TRANS_AMOUNT_UNIT_STR_COL = "amountTypeStr";
    private static final String TRANS_FEE_COL = "fee";
    private static final String TRANS_FEE_UNIT_STR_COL = "feeTypeStr";
    private static final String TRANS_STATUS_COL = "status";
    private static final String TRANS_COMMENTS_COL = "comments";
    private static final String TRANS_RAW_DATA_COL = "rawData";

    // Columns of address book
    private static final String ADDRBOOK_ADDR_COL = "address";
    private static final String ADDRBOOK_USR_NAME_COL = "userName";

    /**
     * SQL command of creating transaction table
     */
    static final String CREATE_TRANS_TABLE_SQL = "CREATE TABLE " + TRANS_TABLE_NAME + " (" +
            TRANS_KEY_ID_COL + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
            TRANS_DATETIME_COL + " DateTime NOT NULL, " +
            TRANS_HASH_COL + " VARCHAR(128), " +
            TRANS_PAYER_ADDR_COL + " VARCHAR(64) NOT NULL, " +
            TRANS_PAYEE_ADDR_COL + " VARCHAR(64) NOT NULL, " +
            TRANS_AMOUNT_COL + " DOUBLE, " +
            TRANS_AMOUNT_UNIT_STR_COL + " VARCHAR(32), " +
            TRANS_FEE_COL + " DOUBLE, " +
            TRANS_FEE_UNIT_STR_COL + " VARCHAR(32), " +
            TRANS_STATUS_COL + " INTEGER, " +
            TRANS_COMMENTS_COL + " TEXT, " +
            TRANS_RAW_DATA_COL + " TEXT " +
            ");";

    /**
     * SQL command of creating address book table
     */
    static final String CREATE_ADDRBOOK_TABLE_SQL = "CREATE TABLE " +
            ADDR_BOOK_TABLE_NAME + " (" +
            ADDRBOOK_USR_NAME_COL + " VARCHAR(128) PRIMARY KEY NOT NULL, " +
            ADDRBOOK_ADDR_COL + " VARCHAR(64) NOT NULL " +
            ");";

    // Database object
    private SQLiteDatabase otkDB;

    private DBTransItem generateTransItemByQueryResult(Cursor cursor) {
        DBTransItem item = new DBTransItem();

        item.setId(cursor.getLong(0));
        item.setDatetime(cursor.getLong(1));
        item.setHash(cursor.getString(2));
        item.setPayerAddr(cursor.getString(3));
        item.setPayeeAddr(cursor.getString(4));
        item.setAmount(cursor.getDouble(5));
        item.setAmountUnitString(cursor.getString(6));
        item.setFee(cursor.getDouble(7));
        item.setFeeUnitString(cursor.getString(8));
        item.setStatus(cursor.getInt(9));
        item.setComment(cursor.getString(10));
        item.setRawData(cursor.getString(11));

        return item;
    }

    private DBAddrItem generateAddrbookItemByQueryResult(Cursor cursor) {
        DBAddrItem item = new DBAddrItem();

        item.setName(cursor.getString(0));
        item.setAddress(cursor.getString(1));

        return item;
    }

    public OpenturnkeyDB(Context context) {
        otkDB = DBHelper.getDatabase(context);

    }

    public int getTransactionCount() {
        int ret = 0;

        try (Cursor cursor = otkDB.rawQuery(
                "SELECT COUNT(*) FROM " + TRANS_TABLE_NAME,null)) {
            if (cursor.moveToNext()) {
                ret = cursor.getInt(0);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ret;
    }

    public DBTransItem addTransaction(DBTransItem item) {
        ContentValues cv = new ContentValues();

        cv.put(TRANS_DATETIME_COL, item.getDatetime());
        cv.put(TRANS_HASH_COL, item.getHash());
        cv.put(TRANS_PAYER_ADDR_COL, item.getPayerAddr());
        cv.put(TRANS_PAYEE_ADDR_COL, item.getPayeeAddr());
        cv.put(TRANS_AMOUNT_COL, item.getAmount());
        cv.put(TRANS_AMOUNT_UNIT_STR_COL, item.getAmountUnitString());
        cv.put(TRANS_FEE_COL, item.getFee());
        cv.put(TRANS_FEE_UNIT_STR_COL, item.getFeeUnitString());
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

        if (otkDB.update(TRANS_TABLE_NAME, cv, where, null) > 0) {
            return true;
        }
        return false;
    }

    public boolean deleteTransactionById(long id) {
        String where = TRANS_KEY_ID_COL + "=" + id;

        if (otkDB.delete(TRANS_TABLE_NAME, where , null) > 0) {
            return true;
        }
        return false;
    }

    public DBTransItem getTransactionItemById(long id) {
        DBTransItem item = null;
        try (Cursor cursor = otkDB.query(TRANS_TABLE_NAME, null,
                TRANS_KEY_ID_COL + "=?", new String[]{String.valueOf(id)},
                null, null, null, null)) {
            if (1 == cursor.getCount()) {
                cursor.moveToNext();
                item = generateTransItemByQueryResult(cursor);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return item;
    }

    public List<DBTransItem> getAllTransaction() {
        List<DBTransItem> result = new ArrayList<>();

        try (Cursor cursor = otkDB.query(TRANS_TABLE_NAME, null, null,
                null, null, null, null, null)) {
            while (cursor.moveToNext()) {
                result.add(generateTransItemByQueryResult(cursor));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    public boolean clearTransactionTable() {
        try {
            otkDB.execSQL("DROP TABLE IF EXISTS " + TRANS_TABLE_NAME);
            otkDB.execSQL(CREATE_TRANS_TABLE_SQL);
        } catch (Exception e) {
            Log.e(TAG, "clearTransactionTable FAILd. e: " + e);
            return false;
        }
        return true;
    }

    public int getAddrbookCount() {
        int ret = 0;

        try (Cursor cursor = otkDB.rawQuery(
                "SELECT COUNT(*) FROM " + ADDR_BOOK_TABLE_NAME, null)) {
            if (cursor.moveToNext()) {
                ret = cursor.getInt(0);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ret;
    }

    public boolean addAddress(DBAddrItem item) {
        // Check if this item is in database already
        DBAddrItem addrItem = getAddressItemByAlias(item.getName());

        if (null == addrItem) {
            ContentValues cv = new ContentValues();

            cv.put(ADDRBOOK_ADDR_COL, item.getAddress());
            cv.put(ADDRBOOK_USR_NAME_COL, item.getName());

            if (0 > otkDB.insert(ADDR_BOOK_TABLE_NAME, null, cv)) {
                return false;
            }
            return true;
        }
        else {
            return updateAddressbook(item);
        }
    }

    public boolean updateAddressbook(DBAddrItem item) {
        // Check if this item is in database already
        DBAddrItem addrItem = getAddressItemByAlias(item.getName());

        if (null == addrItem) {
            return addAddress(item);
        }
        else {
            ContentValues cv = new ContentValues();

            cv.put(ADDRBOOK_ADDR_COL, item.getAddress());
            return otkDB.update(ADDR_BOOK_TABLE_NAME, cv,
                    ADDRBOOK_USR_NAME_COL + "=?",new String[] { item.getName() }) > 0;
        }
    }

    public boolean deleteAddressbookByAddr(String address) {
        if (0 < otkDB.delete(ADDR_BOOK_TABLE_NAME, ADDRBOOK_ADDR_COL + "=?" , new String[]{address})) {
            return true;
        }
        return false;
    }

    public boolean deleteAddressbookByAlias(String alias) {
        if (0 < otkDB.delete(ADDR_BOOK_TABLE_NAME, ADDRBOOK_USR_NAME_COL + "=?" , new String[]{alias})) {
            return true;
        }
        return false;
    }

    public List<DBAddrItem> getAllAddressbook() {
        List<DBAddrItem> result = new ArrayList<>();
        Cursor cursor = otkDB.query(ADDR_BOOK_TABLE_NAME, null,null,
                null, null,null,null,null);
        while (cursor.moveToNext()) {
            result.add(generateAddrbookItemByQueryResult(cursor));
        }

        cursor.close();
        return result;
    }

    public DBAddrItem getAddressItemByAlias(String alias) {
        DBAddrItem item = null;

        try (Cursor cursor = otkDB.query(ADDR_BOOK_TABLE_NAME, null,
                ADDRBOOK_USR_NAME_COL + "=?", new String[]{alias},
                null, null, null, null)) {
            if (1 == cursor.getCount()) {
                cursor.moveToNext();
                item = generateAddrbookItemByQueryResult(cursor);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return item;
    }
}
