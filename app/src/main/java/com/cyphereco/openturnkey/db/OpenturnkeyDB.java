package com.cyphereco.openturnkey.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.List;

/**
 * OpenturnkeyDB.java - This is used to operate database of OpenTurnKey
 */
public class OpenturnkeyDB {
    // private static final String TAG = OpenturnkeyDB.class.getSimpleName();

    /**
     * This is transaction log table name of database
     */
    static final String TABLE_TRANSACTION = "transactionLog";
    /**
     * This is address book table name of database
     */
    static final String TABLE_ADDRESSBOOK = "addrBook";

    // Columns of transaction table
    private static final String TX_KEY_ID = "_id";
    private static final String TX_HASH = "hash";
    private static final String TX_TIMESTAMP = "timestamp";
    private static final String TX_PAYER_ADDR = "payerAddr";
    private static final String TX_PAYEE_ADDR = "payeeAddr";
    private static final String TX_AMOUNT_SENT = "amountSent";
    private static final String TX_AMOUNT_RECV = "amountRecv";
    private static final String TX_RAW = "rawData";
    private static final String TX_BLOCK_HEIGHT = "blockHeight";
    private static final String TX_EXCHANGE_RATE = "exchangeRate";

    // Columns of address book
    private static final String ADDR_KEY_ID = "_id";
    private static final String ADDR_ADDRESS = "address";
    private static final String ADDR_ALIAS = "alias";

    /**
     * SQL command of creating transaction table
     */
    static final String CREATE_TRANSACTION_TABLE_SQL = "CREATE TABLE " + TABLE_TRANSACTION + " (" +
            TX_KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
            TX_TIMESTAMP + " DateTime NOT NULL, " +
            TX_HASH + " TEXT, " +
            TX_PAYER_ADDR + " TEXT NOT NULL, " +
            TX_PAYEE_ADDR + " TEXT NOT NULL, " +
            TX_AMOUNT_SENT + " DOUBLE, " +
            TX_AMOUNT_RECV + " DOUBLE, " +
            TX_RAW + " TEXT, " +
            TX_BLOCK_HEIGHT + " LONG," +
            TX_EXCHANGE_RATE + " TEXT " +
            ");";

    /**
     * SQL command of creating address book table
     */
    static final String CREATE_ADDRBOOK_TABLE_SQL = "CREATE TABLE " +
            TABLE_ADDRESSBOOK + " (" +
            ADDR_KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
            ADDR_ALIAS + " VARCHAR(128) UNIQUE NOT NULL, " +
            ADDR_ADDRESS + " VARCHAR(64) NOT NULL " +
            ");";

    // Database object
    private static SQLiteDatabase otkDB;

    private static RecordTransaction generateTransItemByQueryResult(Cursor cursor) {
        RecordTransaction recordTransaction = new RecordTransaction();

        recordTransaction.setId(cursor.getLong(0));
        recordTransaction.setTimestamp(cursor.getLong(1));
        recordTransaction.setHash(cursor.getString(2));
        recordTransaction.setPayer(cursor.getString(3));
        recordTransaction.setPayee(cursor.getString(4));
        recordTransaction.setAmountSent(cursor.getDouble(5));
        recordTransaction.setAmountRecv(cursor.getDouble(6));
        recordTransaction.setRawData(cursor.getString(7));
        recordTransaction.setBlockHeight(cursor.getLong(8));
        recordTransaction.setExchangeRate(cursor.getString(9));

        return recordTransaction;
    }

    private static RecordAddress generateAddrbookItemByQueryResult(Cursor cursor) {
        RecordAddress item = new RecordAddress();

        item.setId(cursor.getLong(0));
        item.setAlias(cursor.getString(1));
        item.setAddress(cursor.getString(2));

        return item;
    }

    public static void init(Context context) {
        if (otkDB == null) {
            otkDB = DBHelper.getDatabase(context);
        }
    }

    public static int getTransactionCount() {
        int ret = 0;

        try (Cursor cursor = otkDB.rawQuery(
                "SELECT COUNT(*) FROM " + TABLE_TRANSACTION, null)) {
            if (cursor.moveToNext()) {
                ret = cursor.getInt(0);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ret;
    }

    public static RecordTransaction insertTransaction(RecordTransaction recordTransaction) {
        RecordTransaction r = getTransactionByHash(recordTransaction.getHash());
        if (r != null) return r;

        ContentValues cv = new ContentValues();

        cv.put(TX_TIMESTAMP, recordTransaction.getTimestamp());
        cv.put(TX_HASH, recordTransaction.getHash());
        cv.put(TX_PAYER_ADDR, recordTransaction.getPayer());
        cv.put(TX_PAYEE_ADDR, recordTransaction.getPayee());
        cv.put(TX_AMOUNT_SENT, recordTransaction.getAmountSent());
        cv.put(TX_AMOUNT_RECV, recordTransaction.getAmountRecv());
        cv.put(TX_RAW, recordTransaction.getRawData());
        cv.put(TX_BLOCK_HEIGHT, recordTransaction.getBlockHeight());
        cv.put(TX_EXCHANGE_RATE, recordTransaction.getExchangeRate());

        long id = otkDB.insert(TABLE_TRANSACTION, null, cv);

        // something wrong, insert failed
        if (id < 0) return null;

        recordTransaction.setId(id);
        return recordTransaction;
    }

    public static boolean updateTransaction(RecordTransaction recordTransaction) {
        ContentValues cv = new ContentValues();

        cv.put(TX_TIMESTAMP, recordTransaction.getTimestamp());
        cv.put(TX_HASH, recordTransaction.getHash());
        cv.put(TX_PAYER_ADDR, recordTransaction.getPayer());
        cv.put(TX_PAYEE_ADDR, recordTransaction.getPayee());
        cv.put(TX_AMOUNT_SENT, recordTransaction.getAmountSent());
        cv.put(TX_AMOUNT_RECV, recordTransaction.getAmountRecv());
        cv.put(TX_RAW, recordTransaction.getRawData());
        cv.put(TX_BLOCK_HEIGHT, recordTransaction.getBlockHeight());
        cv.put(TX_EXCHANGE_RATE, recordTransaction.getExchangeRate());

        String where = TX_KEY_ID + "=" + recordTransaction.getId();

        try {
            // should be only 1 record affected, if return not 1, must be something wrong
            return (otkDB.update(TABLE_TRANSACTION, cv, where, null) == 1);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public static boolean deleteTransactionById(long id) {
        String where = TX_KEY_ID + "=" + id;

        try {
            return (otkDB.delete(TABLE_TRANSACTION, where, null) == 1);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public static boolean deleteTransaction(RecordTransaction recordTransaction) {
        return deleteTransactionById(recordTransaction.getId());
    }

    public static RecordTransaction getTransactionByHash(String hash) {
        RecordTransaction item = null;
        try (Cursor cursor = otkDB.query(TABLE_TRANSACTION, null,
                TX_HASH + "=?", new String[]{String.valueOf(hash)},
                null, null, null, null)) {
            if (1 == cursor.getCount()) {
                cursor.moveToNext();
                item = generateTransItemByQueryResult(cursor);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return item;
    }

    public static RecordTransaction getTransactionById(long id) {
        RecordTransaction item = null;
        try (Cursor cursor = otkDB.query(TABLE_TRANSACTION, null,
                TX_KEY_ID + "=?", new String[]{String.valueOf(id)},
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

    public static List<RecordTransaction> getAllTransactions() {
        List<RecordTransaction> result = new ArrayList<>();

        try (Cursor cursor = otkDB.query(TABLE_TRANSACTION, null, null,
                null, null, null, null, null)) {
            while (cursor.moveToNext()) {
                result.add(generateTransItemByQueryResult(cursor));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    public static boolean clearTransactionTable() {
        try {
            otkDB.execSQL("DROP TABLE IF EXISTS " + TABLE_TRANSACTION);
            otkDB.execSQL(CREATE_TRANSACTION_TABLE_SQL);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public static int getAddrbookCount() {
        int ret = 0;

        try (Cursor cursor = otkDB.rawQuery(
                "SELECT COUNT(*) FROM " + TABLE_ADDRESSBOOK, null)) {
            if (cursor.moveToNext()) {
                ret = cursor.getInt(0);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ret;
    }

    public static RecordAddress insertAddress(RecordAddress recordAddress) {
        if (recordAddress.getId() > 0) {
            // exist recordAddress, update instead of insert new
            updateAddressbook(recordAddress);
            return recordAddress;
        }

        ContentValues cv = new ContentValues();

        cv.put(ADDR_ADDRESS, recordAddress.getAddress());
        cv.put(ADDR_ALIAS, recordAddress.getAlias());

        long id = otkDB.insert(TABLE_ADDRESSBOOK, null, cv);

        if (id < 0) return null;

        recordAddress.setId(id);
        return recordAddress;
    }

    public static boolean updateAddressbook(RecordAddress item) {
        if (item.getId() > 0) {
            ContentValues cv = new ContentValues();

            cv.put(ADDR_ADDRESS, item.getAddress());
            cv.put(ADDR_ALIAS, item.getAlias());
            String where = ADDR_KEY_ID + "=" + item.getId();

            try {
                return (otkDB.update(TABLE_ADDRESSBOOK, cv, where, null) > 0);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return false;
        }
        return false;
    }

    public static RecordAddress getAddressByAlias(String alias) {
        RecordAddress item = null;

        try (Cursor cursor = otkDB.query(TABLE_ADDRESSBOOK, null,
                ADDR_ALIAS + "=?", new String[]{alias},
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

    public static boolean deleteAddressByAddr(String address) {
        try {
            return (otkDB.delete(TABLE_ADDRESSBOOK, ADDR_ADDRESS + "=?", new String[]{address}) > 0);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public static boolean deleteAddressbookByAlias(String alias) {
        try {
            return (otkDB.delete(TABLE_ADDRESSBOOK, ADDR_ALIAS + "=?", new String[]{alias}) > 0);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public static List<RecordAddress> getAllAddresses() {
        List<RecordAddress> result = new ArrayList<>();
        Cursor cursor = otkDB.query(TABLE_ADDRESSBOOK, null, null,
                null, null, null, null, null);
        while (cursor.moveToNext()) {
            result.add(generateAddrbookItemByQueryResult(cursor));
        }

        cursor.close();
        return result;
    }
}
