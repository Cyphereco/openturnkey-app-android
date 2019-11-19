package com.cyphereco.openturnkey.core;

import android.content.Context;
import android.content.Intent;
import android.nfc.NdefMessage;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Handler;
import android.os.Message;
import android.os.Parcelable;

import com.blockcypher.exception.BlockCypherException;
import com.blockcypher.model.transaction.Transaction;
import com.blockcypher.model.transaction.intermediary.IntermediaryTransaction;
import com.blockcypher.model.transaction.output.Output;
import com.cyphereco.openturnkey.R;
import com.cyphereco.openturnkey.core.protocol.Command;
import com.cyphereco.openturnkey.core.protocol.OtkState;
import com.cyphereco.openturnkey.db.DBTransItem;
import com.cyphereco.openturnkey.db.OpenturnkeyDB;
import com.cyphereco.openturnkey.utils.BtcUtils;
import com.cyphereco.openturnkey.utils.CurrencyExchangeRate;
import com.cyphereco.openturnkey.utils.Log4jHelper;
import com.cyphereco.openturnkey.utils.TxFee;
import com.cyphereco.openturnkey.webservices.BlockCypher;

import org.slf4j.Logger;

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
    static Logger logger = Log4jHelper.getLogger(TAG);

    /** Return value. */
    public static final int OTK_RETURN_OK = 0;
    public static final int OTK_RETURN_ERROR = 1;
    public static final int OTK_RETURN_ERROR_OP_IN_PROCESSING = 2;
    public static final int OTK_RETURN_ERROR_INVALID_OP = 3;
    public static final int OTK_RETURN_ERROR_NO_OP_PROCESSING = 4;


    private static final int OTK_MSG_GOT_UNSIGNED_TX = 0;
    private static final int OTK_MSG_BITCOIN_SENT = 1;
    private static final int OTK_MSG_SEND_BITCOIN_FAILED = 2;
    private static final int OTK_MSG_CURRENCY_EX_RATE_UPDATE = 3;
    private static final int OTK_MSG_TX_FEE_UPDATE = 4;
    private static final int OTK_MSG_COMPLETE_PAYMENT_FAILED = 5;

    public enum Operation {
        OTK_OP_NONE("None"),
        OTK_OP_READ_GENERAL_INFO("Read General Info"),
        OTK_OP_SIGN_PAYMENT("Sing Payment"),
        OTK_OP_GET_RECIPIENT_ADDRESS("Get Recipient Address"),
        OTK_OP_UNLOCK("Unlock"),
        OTK_OP_WRITE_NOTE("Write Note"),
        OTK_OP_GET_KEY("Get Key"),
        OTK_OP_SIGN_MESSAGE("Sign Message"),
        OTK_OP_CHOOSE_KEY("Choose Key"),
        OTK_OP_SET_PIN_CODE("Set Pin Code");

        private final String value;
        private Operation(String s) {
            value = s;
        }

        public String toString(){
            return value;
        }
    }

    /** The place holder for the single object. */
    private static Otk mOtk = null;

    /** Event listener. */
    static OtkEventListener mEventListener = null;
    static BalanceUpdateListener mBalanceUpdateListener = null;

    /* Operation. */
    static Operation mOp = Operation.OTK_OP_NONE;
    /* In processing. */
    static boolean isInProcessing = false;
    // Command to write.
    static Command mCommandToWrite = Command.INVALID;
    /* Handler. */
    static Handler mHandler;

    /* Cached data. */
    static double mAmount;
    static String mFrom;
    static String mTo;
    static String mSessionId;
    static String mMessageToSign;
    static boolean mFeeIncluded;
    static long mTxFees;
    static List<String> mArgs = new ArrayList<String>();
    static List<String> mSignatures = new ArrayList<String>();
    static CurrencyExchangeRate mCurrencyExRate;
    static Tag mTag;
    static boolean mIsAuthorized;
    static boolean mUsingMasterKey;
    static String mPin;
    // Periodic Timer
    static Timer mTimerRate = new Timer();
    static Timer mTimerTxFee = new Timer();

    // Application  context
    static Context mCtx;

    /**
     * Singleton retrieval of the OtkCoin.
     *
     * @return The singleton.
     */
    public static synchronized Otk getInstance(Context ctx) {
        logger.info("getInstance()");

        mCtx = ctx;

        if (null == mOtk) {
            mOtk = new Otk();
            mHandler = new Handler() {
                @Override
                public void handleMessage(Message msg) {
                    logger.info("handle message:" + msg.what);
                    OtkEvent event;
                    Tx tx;
                    switch (msg.what) {
                        case OTK_MSG_GOT_UNSIGNED_TX:
                            if (mOp != Operation.OTK_OP_SIGN_PAYMENT) {
                                logger.warn("Got unsigned tx but operation is cancelled.");
                                /* Op cancelled, ignore it */
                                break;
                            }

                            // Cache to sign list
                            UnsignedTx unsigendTx = (UnsignedTx)msg.obj;
                            List<String> toSignList = unsigendTx.getToSign();
                            mArgs.clear();
                            // Also clear cached signature
                            mSignatures.clear();
                            mArgs.addAll(toSignList);

                            // Send unsigned tx event
                            sendEvent(new OtkEvent(unsigendTx));
                            break;
                        case OTK_MSG_SEND_BITCOIN_FAILED:
                            event = new OtkEvent(OtkEvent.Type.SEND_BITCOIN_FAIL, (String)msg.obj);
                            sendEvent(event);
                            clearOp();
                            break;
                        case OTK_MSG_COMPLETE_PAYMENT_FAILED:
                            tx = (Tx)msg.obj;
                            if (tx == null) {
                                event = new OtkEvent(OtkEvent.Type.COMPLETE_PAYMENT_FAIL, "Unknown Reason");
                            }
                            else {
                                event = new OtkEvent(OtkEvent.Type.COMPLETE_PAYMENT_FAIL, tx);
                            }
                            sendEvent(event);
                            clearOp();
                            break;
                        case OTK_MSG_CURRENCY_EX_RATE_UPDATE:
                            mCurrencyExRate  = (CurrencyExchangeRate) msg.obj;
                            event = new OtkEvent(OtkEvent.Type.CURRENCY_EXCHANGE_RATE_UPDATE, mCurrencyExRate);
                            sendEvent(event);
                            break;
                        case OTK_MSG_TX_FEE_UPDATE:
                            event = new OtkEvent(OtkEvent.Type.TX_FEE_UPDATE, (TxFee) msg.obj);
                            sendEvent(event);
                            break;
                        case OTK_MSG_BITCOIN_SENT:
                            tx = (Tx)msg.obj;
                            event = new OtkEvent(OtkEvent.Type.SEND_BITCOIN_SUCCESS, tx);
                            sendEvent(event);
                            clearOp();
                            break;
                        default:
                    }
                }
            };

            // Timer task which calling get current exchange api.
            TimerTask task = new TimerTask() {
                public void run () {
                    CurrencyExchangeRate cer = BtcUtils.getCurrencyExchangeRate();
                    if (cer != null) {
                        // Send message
                        Message msg = new Message();
                        msg.what = OTK_MSG_CURRENCY_EX_RATE_UPDATE;
                        msg.obj = cer;
                        mHandler.sendMessage(msg);
                        // Cache it
                        mCurrencyExRate = cer;
                    }
                }
            };
            mTimerRate.schedule(task,100,1000 * 60);
            // Timer task which calling get current exchange api.
            TimerTask updateTxFeeTask = new TimerTask() {
                public void run () {
                    TxFee txFee = BtcUtils.getTxFee();
                    if (txFee != null) {
                        // Send message
                        Message msg = new Message();
                        msg.what = OTK_MSG_TX_FEE_UPDATE;
                        msg.obj = txFee;
                        mHandler.sendMessage(msg);
                    }
                }
            };
            mTimerTxFee.schedule(updateTxFeeTask,100,1000 * 60 * 60);
        }
        return mOtk;
    }

    static void writeSignCommand(String pin, boolean usingMasterKey) {
        logger.info("writeSignCommand:{} usingMasterKey:{}", pin, usingMasterKey);
        OtkEvent event;
        // Try to wirte maximum size we can
        List<String> args = new ArrayList<String>();
        int hashSize = Math.min(mArgs.size(), Configurations.maxSignaturesPerCommand);
        boolean isMore = false;
        if (hashSize < mArgs.size()) {
            isMore = true;
        }
        args = mArgs.subList(0, hashSize);
        if (OTK_RETURN_OK == Nfc.writeCommand(mTag, Command.SIGN, mSessionId, pin, args, isMore, usingMasterKey)) {
            // Command written, read signature(s).
            OtkData otkData = Nfc.read(mTag);
            if (otkData == null) {
                // OTK is not connected
                mCommandToWrite = Command.INVALID;
                event = new OtkEvent(OtkEvent.Type.APPROACH_OTK);
                sendEvent(event);
            }
            else {
                if (otkData.getType() != OtkData.Type.OTK_DATA_TYPE_SIGNATURE) {
                    logger.info("Expect signature but got " + otkData.getType().toString());
                    event = new OtkEvent(OtkEvent.Type.SIGN_FAILED);
                    sendEvent(event);
                    return;
                }
                if (mOp == Operation.OTK_OP_SIGN_PAYMENT) {
                    processSignature(otkData);
                }
                else {
                    // Must be sign message
                    sendEvent(new OtkEvent(OtkEvent.Type.SIGN_MESSAGE_SUCCESS, otkData, mMessageToSign, mUsingMasterKey));
                }
            }
        }
        else {
            // OTK is not connected, cache command
            mCommandToWrite = Command.SIGN;
            mPin = pin;
            event = new OtkEvent(OtkEvent.Type.APPROACH_OTK);
            sendEvent(event);
        }
    }

    static void writeUnlockCommand(String pin) {
        logger.info("writeUnlockCommand:" + pin);
        OtkEvent event;
        List<String> args = new ArrayList<String>();
        if (OTK_RETURN_OK == Nfc.writeCommand(mTag, Command.UNLOCK, mSessionId, pin, args, false, false)) {
            // Try to read tag
            OtkData otkData = Nfc.read(mTag);
            if (otkData == null) {
                // OTK is not connected
                mCommandToWrite = Command.INVALID;
                mPin = "";
                event = new OtkEvent(OtkEvent.Type.APPROACH_OTK);
                sendEvent(event);
            }
            else {
                if (otkData.getType() != OtkData.Type.OTK_DATA_TYPE_COMMAND_EXEC_SUCCESS) {
                    logger.info("Expect exec success but got " + otkData.getType().toString());
                    event = new OtkEvent(OtkEvent.Type.UNLOCK_FAIL);
                    sendEvent(event);
                    return;
                }
                // Success
                event = new OtkEvent(OtkEvent.Type.UNLOCK_SUCCESS, otkData);
                sendEvent(event);
                clearOp();
            }
        }
        else {
            // OTK is not connected, cache command
            mCommandToWrite = Command.UNLOCK;
            mPin = pin;
            event = new OtkEvent(OtkEvent.Type.APPROACH_OTK);
            sendEvent(event);
        }
    }

    static void writeOtkCommand(Command cmd, String pin, List<String> args, boolean isMore, boolean usingMasterKey) {
        logger.info("writeOtkCommand:{} {} {} {} {}", cmd, pin, args, isMore, usingMasterKey);
        OtkEvent event;
        if (OTK_RETURN_OK == Nfc.writeCommand(mTag, cmd, mSessionId, pin, args, isMore, usingMasterKey)) {
            // Try to read tag
            OtkData otkData = Nfc.read(mTag);
            if (otkData == null) {
                // OTK is not connected
                mCommandToWrite = Command.INVALID;
                mPin = "";
                event = new OtkEvent(OtkEvent.Type.APPROACH_OTK);
                sendEvent(event);
                return;
            }
            else {
                if (otkData.getType() == OtkData.Type.OTK_DATA_TYPE_COMMAND_EXEC_SUCCESS) {
                    if (cmd == Command.UNLOCK) {
                        sendEvent(new OtkEvent(OtkEvent.Type.UNLOCK_SUCCESS, otkData));
                    }
                    else if (cmd == Command.SET_NOTE) {
                        sendEvent(new OtkEvent(OtkEvent.Type.WRITE_NOTE_SUCCESS, otkData));
                    }
                    else if (cmd == Command.SET_PIN) {
                        sendEvent(new OtkEvent(OtkEvent.Type.SET_PIN_SUCCESS, otkData));
                    }
                    else if (cmd == Command.SET_KEY) {
                        sendEvent(new OtkEvent(OtkEvent.Type.CHOOSE_KEY_SUCCESS, otkData));
                    }
                    else {
                        logger.error("Not supported yet!");
                    }
                    clearOp();
                    return;
                }
                else {
                    // Fail case
                    logger.info("Expect exec success but got " + otkData.getType().toString());
                    if (cmd == Command.UNLOCK) {
                        sendEvent(new OtkEvent(OtkEvent.Type.UNLOCK_FAIL, otkData));

                    } else if (cmd == Command.SET_NOTE) {
                        sendEvent(new OtkEvent(OtkEvent.Type.WRITE_NOTE_FAIL, otkData));
                    } else if (cmd == Command.SET_PIN) {
                        sendEvent(new OtkEvent(OtkEvent.Type.SET_PIN_FAIL, otkData));
                    } else if (cmd == Command.SET_KEY) {
                        sendEvent(new OtkEvent(OtkEvent.Type.CHOOSE_KEY_FAIL, otkData));
                    } else {
                        logger.error("Not supported yet!");
                    }
                }
                return;

            }
        }
        else {
            // OTK is not connected, cache command
            mCommandToWrite = cmd;
            mPin = pin;
            event = new OtkEvent(OtkEvent.Type.APPROACH_OTK);
            sendEvent(event);
            return;
        }
    }

    /**
     * Method to process all NFC intents
     */
    public int processIntent(Intent intent, OtkEventListener listener) {
        logger.info("process intent");

        Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        if (tag != null) {
            if (mCommandToWrite != Command.INVALID) {
                mTag = tag;
                writeCommand(mPin);
                return OTK_RETURN_OK;
            }
            OtkData data = Nfc.read(tag);
            mTag = tag;
            return processOtkData(data, listener);
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
            return processOtkData(data, listener);
        }

        return OTK_RETURN_ERROR;
    }

    /**
     * Send event to listener.
     * @param event
     * @return
     */
    static private int sendEvent(OtkEvent event) {
        if (event.getType() == OtkEvent.Type.BALANCE_UPDATE) {
            if (mBalanceUpdateListener != null) {
                mBalanceUpdateListener.onOtkEvent(event);
                return OTK_RETURN_OK;
            }
        }
        if (mEventListener == null) {
            return OTK_RETURN_ERROR;
        }
        mEventListener.onOtkEvent(event);
        return OTK_RETURN_OK;
    }

    static private void processSignature(OtkData otkData) {
        String pubKey = otkData.getPublicKey();
        List<String> sigs = otkData.getSessionData().getRequestSigList();
        if (!otkData.getOtkState().getLockState().equals(OtkState.LockState.AUTHORIZED)) {
            mIsAuthorized = false;
        }
        else {
            // OTK is authorized. Get address, session id and generate transaction
            mIsAuthorized = true;
            mPin = "";
        }
        // Check if there are more hash to sign
        int sigSize = sigs.size();
        // Add signatures to cache.
        for (int i = 0; i < sigSize; i++) {
            mArgs.remove(0);
            mSignatures.add(sigs.get(i));
        }
        if (mArgs.size() > 0) {
            logger.info("We have more hash to sign. " + mArgs.size());
            // write remain hashs
            writeSignCommand(mPin, false);
        }
        else {
            // Got all signature(s), complete tx
            completePayment(pubKey, mSignatures);
        }
    }

    /**
     * Method to process read Otk data base on current state.
     */
    private int processOtkData(OtkData otkData, OtkEventListener listener) {
        if (otkData == null) {
            return OTK_RETURN_ERROR;
        }
        logger.info("processOtkData() mOp:{}", mOp);
        if (mOp == Operation.OTK_OP_READ_GENERAL_INFO || mOp == Operation.OTK_OP_NONE) {
            OtkEvent event = new OtkEvent(OtkEvent.Type.GENERAL_INFORMATION, otkData);
            if (listener != null) {
                listener.onOtkEvent(event);
                return OTK_RETURN_OK;
            }
            sendEvent(event);
            clearOp();
            return OTK_RETURN_OK;
        }

        if (mOp == Operation.OTK_OP_SIGN_PAYMENT) {
            if (otkData.getType() == OtkData.Type.OTK_DATA_TYPE_GENERAL_INFO) {
                if (isInProcessing == true) {
                    logger.error("It's already in processing.");
                    if (!mSessionId.equals(otkData.getSessionData().getSessionId())) {
                        // OTK must be restarted, consider failed.
                        sendEvent(new OtkEvent(OtkEvent.Type.SEND_BITCOIN_FAIL));
                        return OTK_RETURN_ERROR;
                    }
                    return OTK_RETURN_OK;
                }
                isInProcessing = true;
                // Check if OTK is authorized
                if (!otkData.getOtkState().getLockState().equals(OtkState.LockState.AUTHORIZED)) {
                    mIsAuthorized = false;
                }
                else {
                    // OTK is authorized. Get address, session id and generate transaction
                    mIsAuthorized = true;
                }
                mFrom = otkData.getSessionData().getAddress();
                mSessionId = otkData.getSessionData().getSessionId();
                sendBitcoin(mFrom, mTo, mAmount, mTxFees, mFeeIncluded);
                OtkEvent event = new OtkEvent(OtkEvent.Type.FIND_UTXO);
                sendEvent(event);
            }
            else if (otkData.getType() == OtkData.Type.OTK_DATA_TYPE_SIGNATURE) {
                // Got signature(s), process them
                OtkEvent event = new OtkEvent(OtkEvent.Type.OPERATION_IN_PROCESSING);
                sendEvent(event);
                processSignature(otkData);
            }
            else if (otkData.getType() == OtkData.Type.OTK_DATA_TYPE_COMMAND_EXEC_FAILURE) {
                OtkEvent event = new OtkEvent(OtkEvent.Type.COMMAND_EXECUTION_FAILED, otkData.getFailureReason());
                sendEvent(event);
                clearOp();
            }
            return OTK_RETURN_OK;
        }

        if (mOp == Operation.OTK_OP_GET_RECIPIENT_ADDRESS) {
            OtkEvent event = new OtkEvent(OtkEvent.Type.RECIPIENT_ADDRESS, otkData.getSessionData().getAddress());
            sendEvent(event);
            clearOp();
            return OTK_RETURN_OK;
        }

        if (mOp == Operation.OTK_OP_UNLOCK) {
            if (otkData.getType() == OtkData.Type.OTK_DATA_TYPE_GENERAL_INFO) {
                if (isInProcessing == true) {
                    logger.error("It's already in processing.");
                    if (!mSessionId.equals(otkData.getSessionData().getSessionId())) {
                        // OTK must be restarted, consider failed.
                        sendEvent(new OtkEvent(OtkEvent.Type.UNLOCK_FAIL));
                        return OTK_RETURN_ERROR;
                    }
                    return OTK_RETURN_OK;
                }
                // Check if the OTK is unlocked
                if (otkData.getOtkState().getLockState().equals(OtkState.LockState.UNLOCKED)) {
                    // It's already unlocked.
                    OtkEvent event = new OtkEvent(OtkEvent.Type.OTK_IS_NOT_LOCKED, otkData);
                    sendEvent(event);
                    clearOp();
                    return OTK_RETURN_OK;
                }

                // Processing unlock command
                isInProcessing = true;
                mSessionId = otkData.getSessionData().getSessionId();

                // Check if OTK is authorized
                if (!otkData.getOtkState().getLockState().equals(OtkState.LockState.AUTHORIZED)) {
                    // Send unauthorized event.
                    sendEvent(new OtkEvent(OtkEvent.Type.OTK_UNAUTHORIZED));
                    // clear cached command so that it won't write command without pin
                    mCommandToWrite = Command.INVALID;
                } else {
                    // Clear cached pin
                    mPin = "";
                    // Authorized. Write command
                    writeUnlockCommand(mPin);
                }
            } else if (otkData.getType() == OtkData.Type.OTK_DATA_TYPE_COMMAND_EXEC_FAILURE) {
                sendEvent(new OtkEvent(OtkEvent.Type.UNLOCK_FAIL, otkData.getFailureReason()));
            } else if (otkData.getType() == OtkData.Type.OTK_DATA_TYPE_COMMAND_EXEC_SUCCESS) {
                sendEvent(new OtkEvent(OtkEvent.Type.UNLOCK_SUCCESS, otkData));
            } else {
                logger.info("Unexpected data type:%s", otkData.getType());
            }
            return OTK_RETURN_OK;
        }
        if (mOp == Operation.OTK_OP_WRITE_NOTE) {
            logger.debug("otkData:{}", otkData.getType());
            if (otkData.getType() == OtkData.Type.OTK_DATA_TYPE_GENERAL_INFO) {
                if (isInProcessing == true) {
                    logger.error("It's already in processing.");
                    if (!mSessionId.equals(otkData.getSessionData().getSessionId())) {
                        // OTK must be restarted, consider failed.
                        sendEvent(new OtkEvent(OtkEvent.Type.WRITE_NOTE_FAIL));
                        return OTK_RETURN_ERROR;
                    }
                    return OTK_RETURN_OK;
                }

                // Processing command
                isInProcessing = true;
                mSessionId = otkData.getSessionData().getSessionId();

                // Check if OTK is authorized
                if (!otkData.getOtkState().getLockState().equals(OtkState.LockState.AUTHORIZED)) {
                    // Send unauthorized event.
                    sendEvent(new OtkEvent(OtkEvent.Type.OTK_UNAUTHORIZED));
                    // clear cached command so that it won't write command without pin
                    mCommandToWrite = Command.INVALID;
                } else {
                    // Clear cached pin
                    mPin = "";
                    // Authorized. Write command
                    writeOtkCommand(Command.SET_NOTE, mPin, mArgs, false, false);
                }
            } else if (otkData.getType() == OtkData.Type.OTK_DATA_TYPE_COMMAND_EXEC_FAILURE) {
                sendEvent(new OtkEvent(OtkEvent.Type.WRITE_NOTE_FAIL, otkData.getFailureReason()));
            } else if (otkData.getType() == OtkData.Type.OTK_DATA_TYPE_COMMAND_EXEC_SUCCESS) {
                sendEvent(new OtkEvent(OtkEvent.Type.WRITE_NOTE_SUCCESS, otkData));
            } else {
                logger.info("Unexpected data type:%s", otkData.getType());
            }
        }
        if (mOp == Operation.OTK_OP_SET_PIN_CODE) {
            logger.debug("otkData:{}", otkData.getType());
            if (otkData.getType() == OtkData.Type.OTK_DATA_TYPE_GENERAL_INFO) {
                if (isInProcessing == true) {
                    logger.error("It's already in processing.");
                    if (!mSessionId.equals(otkData.getSessionData().getSessionId())) {
                        // OTK must be restarted, consider failed.
                        sendEvent(new OtkEvent(OtkEvent.Type.SET_PIN_FAIL));
                        return OTK_RETURN_ERROR;
                    }
                    return OTK_RETURN_OK;
                }

                // Processing command
                isInProcessing = true;
                mSessionId = otkData.getSessionData().getSessionId();

                // Check if OTK is authorized
                if (!otkData.getOtkState().getLockState().equals(OtkState.LockState.AUTHORIZED)) {
                    // Send unauthorized event.
                    sendEvent(new OtkEvent(OtkEvent.Type.OTK_UNAUTHORIZED));
                    // clear cached command so that it won't write command without pin
                    mCommandToWrite = Command.INVALID;
                } else {
                    // Clear cached pin
                    mPin = "";
                    // Authorized. Write command
                    writeOtkCommand(Command.SET_PIN, mPin, mArgs, false, false);
                }
            } else if (otkData.getType() == OtkData.Type.OTK_DATA_TYPE_COMMAND_EXEC_FAILURE) {
                sendEvent(new OtkEvent(OtkEvent.Type.SET_PIN_FAIL, otkData.getFailureReason()));
            } else if (otkData.getType() == OtkData.Type.OTK_DATA_TYPE_COMMAND_EXEC_SUCCESS) {
                sendEvent(new OtkEvent(OtkEvent.Type.SET_PIN_SUCCESS, otkData));
            } else {
                logger.info("Unexpected data type:%s", otkData.getType());
            }
        }
        if (mOp == Operation.OTK_OP_CHOOSE_KEY) {
            logger.debug("otkData:{}", otkData.getType());
            if (otkData.getType() == OtkData.Type.OTK_DATA_TYPE_GENERAL_INFO) {
                if (isInProcessing == true) {
                    logger.error("It's already in processing.");
                    if (!mSessionId.equals(otkData.getSessionData().getSessionId())) {
                        // OTK must be restarted, consider failed.
                        sendEvent(new OtkEvent(OtkEvent.Type.CHOOSE_KEY_FAIL));
                        return OTK_RETURN_ERROR;
                    }
                    return OTK_RETURN_OK;
                }

                // Processing command
                isInProcessing = true;
                mSessionId = otkData.getSessionData().getSessionId();

                // Check if OTK is authorized
                if (!otkData.getOtkState().getLockState().equals(OtkState.LockState.AUTHORIZED)) {
                    // Send unauthorized event.
                    sendEvent(new OtkEvent(OtkEvent.Type.OTK_UNAUTHORIZED));
                    // clear cached command so that it won't write command without pin
                    mCommandToWrite = Command.INVALID;
                } else {
                    // Clear cached pin
                    mPin = "";
                    // Authorized. Write command
                    writeOtkCommand(Command.SET_KEY, mPin, mArgs, false, false);
                }
            } else if (otkData.getType() == OtkData.Type.OTK_DATA_TYPE_COMMAND_EXEC_FAILURE) {
                sendEvent(new OtkEvent(OtkEvent.Type.CHOOSE_KEY_FAIL, otkData.getFailureReason()));
            } else if (otkData.getType() == OtkData.Type.OTK_DATA_TYPE_COMMAND_EXEC_SUCCESS) {
                sendEvent(new OtkEvent(OtkEvent.Type.CHOOSE_KEY_SUCCESS, otkData));
            } else {
                logger.info("Unexpected data type:%s", otkData.getType());
            }
        }
        if (mOp == Operation.OTK_OP_GET_KEY) {
            if (otkData.getType() == OtkData.Type.OTK_DATA_TYPE_GENERAL_INFO) {
                // Write read KEY information command to OTK
                if (isInProcessing == true) {
                    logger.error("It's already in processing.");
                    if (!mSessionId.equals(otkData.getSessionData().getSessionId())) {
                        // OTK must be restarted, consider failed.
                        sendEvent(new OtkEvent(OtkEvent.Type.GET_KEY_FAIL));
                        return OTK_RETURN_ERROR;
                    }
                    return OTK_RETURN_OK;
                }

                // Processing  command
                isInProcessing = true;
                mSessionId = otkData.getSessionData().getSessionId();
                // Check if OTK is authorized
                if (!otkData.getOtkState().getLockState().equals(OtkState.LockState.AUTHORIZED)) {
                    // Send unauthorized event.
                    sendEvent(new OtkEvent(OtkEvent.Type.OTK_UNAUTHORIZED));
                    // clear cached command so that it won't write command without pin
                    mCommandToWrite = Command.INVALID;
                } else {
                    // Clear cached pin
                    mPin = "";
                    // Authorized. Write command
                    writeOtkCommand(Command.SHOW_KEY, mPin, mArgs, false, false);
                }
            }
            else if (otkData.getType() == OtkData.Type.OTK_DATA_TYPE_KEY_INFO) {
                sendEvent(new OtkEvent(OtkEvent.Type.GET_KEY_SUCCESS, otkData));
            }
        }
        if (mOp == Operation.OTK_OP_SIGN_MESSAGE) {
            logger.debug("otkData:{} msgToSign:{}", otkData.getType(), mArgs.get(0));

            if (otkData.getType() == OtkData.Type.OTK_DATA_TYPE_GENERAL_INFO) {
                if (isInProcessing == true) {
                    logger.error("It's already in processing.");
                    if (!mSessionId.equals(otkData.getSessionData().getSessionId())) {
                        // OTK must be restarted, consider failed.
                        sendEvent(new OtkEvent(OtkEvent.Type.SIGN_MESSAGE_FAIL));
                        return OTK_RETURN_ERROR;
                    }
                    return OTK_RETURN_OK;
                }

                mMessageToSign = mArgs.get(0);
                // Encode message
                mArgs.clear();
                byte[] encodedMessageToSign = BtcUtils.generateMessageToSign(mMessageToSign);
                mArgs.add(BtcUtils.bytesToHexString(encodedMessageToSign));
                // Processing command
                isInProcessing = true;
                mSessionId = otkData.getSessionData().getSessionId();

                // Check if OTK is authorized
                if (!otkData.getOtkState().getLockState().equals(OtkState.LockState.AUTHORIZED)) {
                    // Send unauthorized event.
                    sendEvent(new OtkEvent(OtkEvent.Type.OTK_UNAUTHORIZED));
                    // clear cached command so that it won't write command without pin
                    mCommandToWrite = Command.INVALID;
                } else {
                    // Clear cached pin
                    mPin = "";
                    // Authorized. Write command
                    writeOtkCommand(Command.SIGN, mPin, mArgs, false, mUsingMasterKey);
                }
            } else if (otkData.getType() == OtkData.Type.OTK_DATA_TYPE_SIGNATURE) {
                sendEvent(new OtkEvent(OtkEvent.Type.SIGN_MESSAGE_SUCCESS, otkData, mMessageToSign, mUsingMasterKey));
            } else if (otkData.getType() == OtkData.Type.OTK_DATA_TYPE_COMMAND_EXEC_FAILURE) {
                sendEvent(new OtkEvent(OtkEvent.Type.SIGN_MESSAGE_FAIL, otkData.getFailureReason()));
            } else if (otkData.getType() == OtkData.Type.OTK_DATA_TYPE_COMMAND_EXEC_SUCCESS) {
                sendEvent(new OtkEvent(OtkEvent.Type.SIGN_MESSAGE_SUCCESS, otkData, mMessageToSign, mUsingMasterKey));
            } else {
                logger.info("Unexpected data type:%s", otkData.getType());
            }
        }
        return OTK_RETURN_OK;
    }

     static private int clearOp() {
        logger.info("clearOp");
        mOp = Operation.OTK_OP_NONE;
        // Clear cached data
        mTo = "";
        mAmount = 0;
        mFeeIncluded = false;
        mTxFees = 0;
        mCommandToWrite = Command.INVALID;
        mPin = "";
        mIsAuthorized = false;
        isInProcessing = false;
        mMessageToSign = "";
        mUsingMasterKey = false;
        mArgs.clear();
        return OTK_RETURN_OK;
    }

    /**
     * Event listener interface
     */
    public interface OtkEventListener {
        void onOtkEvent(OtkEvent event);
    }

    /**
     * Event listener interface
     */
    public interface BalanceUpdateListener {
        void onOtkEvent(OtkEvent event);
    }

    /**
     * Event listener.
     */
    public int setEventListener(OtkEventListener eventListener) {
        mEventListener = eventListener;
        return OTK_RETURN_OK;
    }

    public int setBalanceListener(BalanceUpdateListener eventListener) {
        mBalanceUpdateListener = eventListener;
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
        logger.info("Set op to:" + op.name());
        mOp = op;
        return OTK_RETURN_OK;
    }

    /**
     * Set operation for choose key
     */
    public int setOperation(Operation op, String arg) {
        if (mOp != Operation.OTK_OP_NONE && isInProcessing == true) {
            /* Some operation is in processing, set another operation is not allowed.
             * Should cancel current operation first.
             */
            return OTK_RETURN_ERROR_OP_IN_PROCESSING;
        }
        logger.info("Set op to:{} arg:{}", op.name(), arg);
        mOp = op;
        mArgs.clear();
        mArgs.add(arg);
        return OTK_RETURN_OK;
    }

    /**
     * Set operation for sing message
     */
    public int setOperation(Operation op, String arg, boolean usingMasterKey) {
        if (mOp != Operation.OTK_OP_NONE && isInProcessing == true) {
            /* Some operation is in processing, set another operation is not allowed.
             * Should cancel current operation first.
             */
            return OTK_RETURN_ERROR_OP_IN_PROCESSING;
        }
        logger.info("Set op to:{} arg:{} usingMasterKey:{}", op.name(), arg, usingMasterKey);
        mOp = op;
        mArgs.clear();
        mArgs.add(arg);
        mUsingMasterKey = usingMasterKey;
        return OTK_RETURN_OK;
    }

    public int setNote(String note) {
        if (mOp != Operation.OTK_OP_NONE && isInProcessing == true) {
            /* Some operation is in processing, set another operation is not allowed.
             * Should cancel current operation first.
             */
            return OTK_RETURN_ERROR_OP_IN_PROCESSING;
        }
        logger.info("Set note:" + note);
        mOp = Operation.OTK_OP_WRITE_NOTE;
        mArgs.clear();
        mArgs.add(note);
        return OTK_RETURN_OK;
    }

    public int setPIN(String pin) {
        if (mOp != Operation.OTK_OP_NONE && isInProcessing == true) {
            /* Some operation is in processing, set another operation is not allowed.
             * Should cancel current operation first.
             */
            return OTK_RETURN_ERROR_OP_IN_PROCESSING;
        }
        logger.info("Set pin:" + pin);
        mOp = Operation.OTK_OP_SET_PIN_CODE;
        mArgs.clear();
        mArgs.add(pin);
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

        logger.info("Set op to:{} to:{} amount:{} fee:{} feeIncluded:{}", op.name(), to, amount, txFees, feeIncluded);
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
        logger.info("cancelOperation");
        clearOp();
        return OTK_RETURN_OK;
    }

    private int writeCommand(String pin) {
        if (mCommandToWrite == Command.INVALID) {
            logger.info("Command is INVALID");
            clearOp();
            return OTK_RETURN_ERROR;
        }

        if (mCommandToWrite == Command.SIGN) {
            writeSignCommand(pin, mUsingMasterKey);
            return OTK_RETURN_OK;
        }

        // Unlock, set note ...
        writeOtkCommand(mCommandToWrite, pin, mArgs, false, false);

        return OTK_RETURN_OK;
    }

    public int setPinForOperation(String pin) {
        if (mOp == Operation.OTK_OP_NONE || isInProcessing == false) {
            // No op is in processing
            return OTK_RETURN_ERROR_NO_OP_PROCESSING;
        }

        // Set command
        if (mOp == Operation.OTK_OP_SIGN_PAYMENT) {
            mCommandToWrite = Command.SIGN;
        } else if (mOp == Operation.OTK_OP_UNLOCK) {
            mCommandToWrite = Command.UNLOCK;
        } else if (mOp == Operation.OTK_OP_WRITE_NOTE) {
            mCommandToWrite = Command.SET_NOTE;
        } else if (mOp == Operation.OTK_OP_SET_PIN_CODE) {
            mCommandToWrite = Command.SET_PIN;
        } else if (mOp == Operation.OTK_OP_CHOOSE_KEY) {
            mCommandToWrite = Command.SET_KEY;
        } else if (mOp == Operation.OTK_OP_GET_KEY) {
            mCommandToWrite = Command.SHOW_KEY;
        } else if (mOp == Operation.OTK_OP_SIGN_MESSAGE) {
            mCommandToWrite = Command.SIGN;
        } else {
            logger.error("Operation {} is not implemented yet.", mOp.toString());
            return OTK_RETURN_ERROR;
        }
        writeCommand(pin);
        return OTK_RETURN_OK;
    }

    private void sendBitcoin(final String from, final String to, final double amount, final long txFees, final boolean feeIncluded) {
        Thread t = new Thread() {
            @Override
            public void run() {
                synchronized (this) {
                    try {
                        long amountInSatoshi = BtcUtils.btcToSatoshi(mAmount);
                        UnsignedTx unsigendTx = BlockCypher.getInstance(mCtx).sendBitcoin(from, to, amountInSatoshi, txFees, feeIncluded);
                        Message msg = new Message();
                        msg.what = OTK_MSG_GOT_UNSIGNED_TX;
                        msg.obj = unsigendTx;
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

    static private int completePayment(final String publicKey, final List<String> sigResult) {
        logger.info("completePayment");
        Thread t = new Thread() {
            @Override
            public void run() {
                synchronized (this) {
                    try {
                        Transaction trans = BlockCypher.getInstance(mCtx).completeSendBitcoin(publicKey, sigResult);
                        if (trans != null) {
                            // Success
                            Tx tx = new Tx(mFrom, mTo, trans, Tx.Status.STATUS_SUCCESS);
                            Message msg = new Message();
                            msg.what = OTK_MSG_BITCOIN_SENT;
                            msg.obj = tx;
                            mHandler.sendMessage(msg);
                        }
                        else {
                            // failed
                            Message msg = new Message();
                            msg.what = OTK_MSG_COMPLETE_PAYMENT_FAILED;
                            msg.obj = null;
                            mHandler.sendMessage(msg);
                        }
                    }
                    catch (BlockCypherException e) {
                        // parse error
                        String reason = BlockCypher.parseError(e.getBlockCypherError().getErrors().get(0).toString());
                        Tx tx = new Tx(mFrom, mTo, e.getBlockCypherError().getTx(), Tx.Status.STATUS_UNKNOWN_FAILURE, reason);
                        Message msg = new Message();
                        msg.what = OTK_MSG_COMPLETE_PAYMENT_FAILED;
                        msg.obj = tx;
                        mHandler.sendMessage(msg);
                    }
                    catch (Exception e) {
                        Message msg = new Message();
                        msg.what = OTK_MSG_COMPLETE_PAYMENT_FAILED;
                        msg.obj = null;
                        mHandler.sendMessage(msg);
                    }
                }
            }
        };
        t.start();
        return OTK_RETURN_OK;
    }

    public void getBalance(final String address) {
        Thread t = new Thread() {
            @Override
            public void run() {
                synchronized (this) {
                    BigDecimal b = BlockCypher.getInstance(mCtx).getBalance(address);
                    OtkEvent event = new OtkEvent(OtkEvent.Type.BALANCE_UPDATE, address, b, mCurrencyExRate);
                    sendEvent(event);
                }
            }
        };

        t.start();
    }

    public void confirmPayment() {
        logger.info("confirm payment");
        // Make sure op
        if (mOp != Operation.OTK_OP_SIGN_PAYMENT) {
            logger.warn("Confirm payment but operation is cancelled.");
            /* Op cancelled, ignore it */
            return;
        }
        // Check if it's authorized
        if (mIsAuthorized == false) {
            // Send unauthorized event.
            sendEvent(new OtkEvent(OtkEvent.Type.OTK_UNAUTHORIZED));
            // clear cached command so that it won't write command without pin
            mCommandToWrite = Command.INVALID;
        }
        else {
            // Clear cached pin
            mPin = "";
            // Authorized. Write command
            writeSignCommand(mPin, false);
        }
    }
}
