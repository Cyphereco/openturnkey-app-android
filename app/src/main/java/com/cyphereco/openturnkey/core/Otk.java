package com.cyphereco.openturnkey.core;

import android.content.Intent;
import android.nfc.NdefMessage;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Handler;
import android.os.Message;
import android.os.Parcelable;
import android.os.SystemClock;
import android.util.Log;

import com.blockcypher.exception.BlockCypherException;
import com.blockcypher.model.transaction.Transaction;
import com.cyphereco.openturnkey.core.protocol.Command;
import com.cyphereco.openturnkey.utils.BtcUtils;
import com.cyphereco.openturnkey.utils.CurrencyExchangeRate;
import com.cyphereco.openturnkey.webservices.BlockCypher;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Otk is the main processor of openturnkey.
 */
public class Otk {
    public static final String TAG = Otk.class.getSimpleName();

    /** Return value. */
    public static final int OTK_RETURN_OK = 0;
    public static final int OTK_RETURN_ERROR = 1;
    public static final int OTK_RETURN_ERROR_OP_IN_PROCESSING = 2;
    public static final int OTK_RETURN_ERROR_INVALID_OP = 3;


    private static final int OTK_MSG_GOT_UNSIGNED_TX = 0;
    private static final int OTK_MSG_BITCOIN_SENT = 1;
    private static final int OTK_MSG_SEND_BITCOIN_FAILED = 2;
    private static final int OTK_MSG_CURRENCY_EX_RATE_UPDATE = 3;

    public enum Operation {
        OTK_OP_NONE,
        OTK_OP_SIGN_PAYMENT,
        OTK_OP_GET_RECIPIENT_ADDRESS,
    }

    /** The place holder for the single object. */
    private static Otk mOtk = null;

    /** Event listener. */
    static OtkEventListener mEventListener = null;

    /* Operation. */
    static Operation mOp = Operation.OTK_OP_NONE;
    /* In processing. */
    boolean isInProcessing = false;
    // Command to write.
    static Command mCommandToWrite = Command.INVALID;
    /* Handler. */
    static Handler mHandler;

    /* Cached data. */
    static double mAmount;
    static String mFrom;
    static String mTo;
    static String mSessionId;
    static boolean mFeeIncluded;
    static long mTxFees;
    static List<String> mArgs = new ArrayList<String>();
    static CurrencyExchangeRate mCurrencyExRate;
    static Tag mTag;
    // Periodic Timer
    static Timer mTimer = new Timer();

    /**
     * Singleton retrieval of the OtkCoin.
     *
     * @return The singleton.
     */
    public static synchronized Otk getInstance() {
        Log.d(TAG, "getInstance()");
        if (null == mOtk) {
            mOtk = new Otk();
            mHandler = new Handler() {
                @Override
                public void handleMessage(Message msg) {
                    Log.d(TAG, "handle message:" + msg.what);
                    OtkEvent event;
                    switch (msg.what) {
                        case OTK_MSG_GOT_UNSIGNED_TX:
                            if (mOp != Operation.OTK_OP_SIGN_PAYMENT) {
                                /* Op cancelled, ignore it */
                                break;
                            }
                            List<String> toSignList = (List<String>)msg.obj;
                            mArgs.clear();
                            mArgs.addAll(toSignList);
                            writeSignCommand();
                            // don't clear mArgs now, it could  try to approch to OTK
                            break;
                        case OTK_MSG_SEND_BITCOIN_FAILED:
                            event = new OtkEvent(OtkEvent.Type.SEND_BITCOIN_FAIL, (String)msg.obj);
                            sendEvent(event);
                            clearOp();
                            break;
                        case OTK_MSG_CURRENCY_EX_RATE_UPDATE:
                            mCurrencyExRate  = (CurrencyExchangeRate) msg.obj;
                            event = new OtkEvent(OtkEvent.Type.CURRENCY_EXCHANGE_RATE_UPDATE, mCurrencyExRate);
                            sendEvent(event);
                            break;
                        case OTK_MSG_BITCOIN_SENT:
                            Transaction trans = (Transaction)msg.obj;
                            // TODO get raw hex of transaction
                            Tx tx = new Tx(trans.getHash(), mFrom, mTo, mAmount, BtcUtils.SatoshiToBtc(trans.getFees().longValue()), trans.getReceived(), "");
                            event = new OtkEvent(tx);
                            sendEvent(event);
                            clearOp();
                            // TODO: add transaction to database.
                            break;
                        default:
                    }
                }
            };

            // Timer task which calling get current exchange api.
            TimerTask task = new TimerTask() {
                public void run () {
                    CurrencyExchangeRate cer = BtcUtils.getCurrencyExchangeRate();
                    // Send message
                    Message msg = new Message();
                    msg.what = OTK_MSG_CURRENCY_EX_RATE_UPDATE;
                    msg.obj = cer;
                    mHandler.sendMessage(msg);


                }
            };
            mTimer.schedule(task,100,1000 * 60);
        }
        return mOtk;
    }

    static void writeSignCommand() {
        Log.d(TAG, "writeSignCommand");
        OtkEvent event;
        if (OTK_RETURN_OK == Nfc.writeCommand(mTag, Command.SIGN, mSessionId, mArgs)) {
            // Command written, read signature(s).
            //SystemClock.sleep(1000);
            OtkData otkData = Nfc.read(mTag);
            if (otkData == null) {
                // OTK is not connected
                mCommandToWrite = Command.INVALID;
                event = new OtkEvent(OtkEvent.Type.APPROACH_OTK);
                sendEvent(event);
            }
            else {
                if (otkData.getType() != OtkData.Type.OTK_DATA_TYPE_SIGNATURE) {
                    Log.d(TAG, "Expect signature but got " + otkData.getType().toString());
                    event = new OtkEvent(OtkEvent.Type.SIGN_FAILED);
                    sendEvent(event);
                }
                // Got signature(s), complete tx
                completePayment(otkData.mPublicKey, otkData.getSessionData().getRequestSigList());
            }
        }
        else {
            // OTK is not connected, cache command
            mCommandToWrite = Command.SIGN;
            event = new OtkEvent(OtkEvent.Type.APPROACH_OTK);
            sendEvent(event);
        }
    }
    /**
     * Method to process all NFC intents
     */
    public int processIntent(Intent intent) {
        Log.d(TAG, "process intent");

        Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        if (tag != null) {
            if (mCommandToWrite != Command.INVALID) {
                mTag = tag;
                if (mCommandToWrite == Command.SIGN) {
                    writeSignCommand();
                }
                return OTK_RETURN_OK;
            }
            OtkData data = Nfc.read(tag);
            mTag = tag;
            return processOtkData(data);
        }

        // tag is null, Try NdefMessage in intent
        NdefMessage[] msgs;
        Parcelable[] rawMsgs = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
        if (rawMsgs != null) {
            msgs = new NdefMessage[rawMsgs.length];
            for (int i = 0; i < rawMsgs.length; i++) {
                msgs[i] = (NdefMessage) rawMsgs[i];
            }
            OtkData data = Nfc.read(msgs);
            return processOtkData(data);
        }

        return OTK_RETURN_ERROR;
    }

    /**
     * Send event to listener.
     * @param event
     * @return
     */
    static private int sendEvent(OtkEvent event) {
        if (mEventListener == null) {
            return OTK_RETURN_ERROR;
        }
        mEventListener.onOtkEvent(event);
        return OTK_RETURN_OK;
    }

    /**
     * Method to process read Otk data base on current state.
     */
    private int processOtkData(OtkData otkData) {
        if (otkData == null) {
            return OTK_RETURN_ERROR;
        }
        if (mOp == Operation.OTK_OP_NONE) {
            OtkEvent event = new OtkEvent(OtkEvent.Type.GENERAL_INFORMATION, otkData);
            sendEvent(event);
            clearOp();
            return OTK_RETURN_OK;
        }

        if (mOp == Operation.OTK_OP_SIGN_PAYMENT) {
            if (otkData.getType() == OtkData.Type.OTK_DATA_TYPE_GENERAL_INFO) {
                // Get address, session id and generate transaction
                mFrom = otkData.getSessionData().getAddress();
                mSessionId = otkData.getSessionData().getSessionId();
                sendBitcoin(mFrom, mTo, mAmount, mTxFees);
                OtkEvent event = new OtkEvent(OtkEvent.Type.OPERATION_IN_PROCESSING);
                sendEvent(event);
            }
            else if (otkData.getType() == OtkData.Type.OTK_DATA_TYPE_SIGNATURE) {
                // Got signature(s), complete tx
                completePayment(otkData.mPublicKey, otkData.getSessionData().getRequestSigList());
            }
            return OTK_RETURN_OK;
        }

        if (mOp == Operation.OTK_OP_GET_RECIPIENT_ADDRESS) {
            OtkEvent event = new OtkEvent(OtkEvent.Type.RECIPIENT_ADDRESS, otkData.getSessionData().getAddress());
            sendEvent(event);
            clearOp();
            return OTK_RETURN_OK;
        }

        return OTK_RETURN_OK;
    }

    static private int clearOp() {
        Log.d(TAG, "clearOp");
        mOp = Operation.OTK_OP_NONE;
        // Clear cached data
        mTo = "";
        mAmount = 0;
        mFeeIncluded = false;
        mTxFees = 0;
        mCommandToWrite = Command.INVALID;
        return OTK_RETURN_OK;
    }

    /**
     * Event listener interface
     */
    public interface OtkEventListener {
        void onOtkEvent(OtkEvent event);
    }

    /**
     * Event listener.
     */
    public int setEventListener(OtkEventListener eventListener) {
        mEventListener = eventListener;
        return OTK_RETURN_OK;
    }

    /**
     * Set operation
     */
    public int setOperation(Operation op) {
        if (mOp != Operation.OTK_OP_NONE && isInProcessing == true) {
            /* Some operation is in processing, set another operation is not allowed.
             * Should cancel current operation first.
             */
            return OTK_RETURN_ERROR_OP_IN_PROCESSING;
        }
        Log.d(TAG, "Set op to:" + op.name());
        mOp = op;
        return OTK_RETURN_OK;
    }

    /**
     * Set operation to pay
     */
    public int setOperation(Operation op, String to, double amount, boolean feeIncluded, long txFees) {
        if (mOp != Operation.OTK_OP_NONE && isInProcessing == true) {
            /* Some operation is in processing, set another operation is not allowed.
             * Should cancel current operation first.
             */
            return OTK_RETURN_ERROR_OP_IN_PROCESSING;
        }

        if (op != Operation.OTK_OP_SIGN_PAYMENT) {
            return OTK_RETURN_ERROR_INVALID_OP;
        }

        Log.d(TAG, "Set op to:" + op.name());
        mOp = op;
        // Cache data
        mTo = to;
        mAmount = amount;
        mFeeIncluded = feeIncluded;
        mTxFees = txFees;

        return OTK_RETURN_OK;
    }

    /**
     * Cancel operation.
     * @return
     */
    public int cancelOperation() {
        clearOp();
        return OTK_RETURN_OK;
    }

    public void sendBitcoin(final String from, final String to, final double amount, final long txFees) {
        Thread t = new Thread() {
            @Override
            public void run() {
                synchronized (this) {
                    try {
                        long amountInSatoshi = BtcUtils.BtcToSatoshi(mAmount);
                        List<String> toSignList = BlockCypher.getInstance().sendBitcoin(from, to, amountInSatoshi, false, txFees);
                        Message msg = new Message();
                        msg.what = OTK_MSG_GOT_UNSIGNED_TX;
                        msg.obj = toSignList;
                        mHandler.sendMessage(msg);
                    }
                    catch (BlockCypherException e) {
                        // Failed
                        Message msg = new Message();
                        msg.what = OTK_MSG_SEND_BITCOIN_FAILED;
                        // parse error
                        String reason = BlockCypher.parseError(e.getBlockCypherError().getErrors().get(0).toString());
                        msg.obj = reason;
                        mHandler.sendMessage(msg);
                    }
                    catch (Exception e) {
                        // Failed
                        Message msg = new Message();
                        msg.what = OTK_MSG_SEND_BITCOIN_FAILED;
                        msg.obj = e.toString();
                        mHandler.sendMessage(msg);
                    }
                }
            }
        };
        t.start();
    }

    static int completePayment(final String publicKey, final List<String> sigResult) {
        Thread t = new Thread() {
            @Override
            public void run() {
                synchronized (this) {
                    Transaction tx = BlockCypher.getInstance().completeSendBitcoin(publicKey, sigResult);
                    if (tx != null) {
                        Message msg = new Message();
                        msg.what = OTK_MSG_BITCOIN_SENT;
                        msg.arg1 = tx.getFees().intValue();
                        msg.obj = tx;
                        mHandler.sendMessage(msg);
                    } else {
                        Message msg = new Message();
                        msg.what = OTK_MSG_SEND_BITCOIN_FAILED;
                        mHandler.sendMessage(msg);
                    }
                }
            }
        };
        t.start();
        return OTK_RETURN_OK;
    }
}
