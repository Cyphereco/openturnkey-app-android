package com.cyphereco.openturnkey.core;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Handler;
import android.os.Message;

import com.blockcypher.exception.BlockCypherException;
import com.cyphereco.openturnkey.R;
import com.cyphereco.openturnkey.core.protocol.Command;
import com.cyphereco.openturnkey.utils.BtcUtils;
import com.cyphereco.openturnkey.utils.ExchangeRate;
import com.cyphereco.openturnkey.utils.Log4jHelper;
import com.cyphereco.openturnkey.utils.TxFee;
import com.cyphereco.openturnkey.webservices.BlockChainInfo;
import com.cyphereco.openturnkey.webservices.BlockCypher;

import org.jetbrains.annotations.NotNull;
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
    private static Logger logger = Log4jHelper.getLogger(TAG);

    /**
     * Return value.
     */
    static final int OTK_RETURN_OK = 0;
    static final int OTK_RETURN_ERROR = 1;


    private static final int OTK_MSG_GOT_UNSIGNED_TX = 0;
    private static final int OTK_MSG_BITCOIN_SENT = 1;
    private static final int OTK_MSG_SEND_BITCOIN_FAILED = 2;
    private static final int OTK_MSG_CURRENCY_EX_RATE_UPDATE = 3;
    private static final int OTK_MSG_TX_FEE_UPDATE = 4;
    private static final int OTK_MSG_COMPLETE_PAYMENT_FAILED = 5;
    private static final int OTK_MSG_SESSION_TIMED_OUT = 6;
    private static final int OTK_MSG_READ_RESPONSE_TIMED_OUT = 7;
    private static final int OTK_MSG_CHECK_BALANCE = 8;


    public enum Operation {
        OTK_OP_NONE("None"),
        OTK_OP_READ_GENERAL_INFO("Read General Info"),
        OTK_OP_SIGN_PAYMENT("Sign Payment");

        private final String value;

        Operation(String s) {
            value = s;
        }

        @NotNull
        public String toString() {
            return value;
        }
    }

    /**
     * The place holder for the single object.
     */
    private static Otk mOtk = null;

    /**
     * Event listener.
     */
    private static OtkEventListener mEventListener = null;
    private static BalanceUpdateListener mBalanceUpdateListener = null;

    /* Operation. */
    private static Operation mOp = Operation.OTK_OP_NONE;
    /* In processing. */
    private static boolean isInProcessing = false;
    /* Handler. */
    private static Handler mHandler;

    /* Cached data. */
    private static double mAmount;
    private static String mFrom;
    private static String mTo;
    private static boolean mFeeIncluded;
    private static long mTxFees;
    private static List<String> mArgs = new ArrayList<>();
    private static List<String> mSignatures = new ArrayList<>();
    private static ExchangeRate mCurrencyExRate;
    // Periodic Timer
    private static Timer mTimerRate = new Timer();
    private static Timer mTimerTxFee = new Timer();
    private static Timer mTimerWriteCommand = null;
    private static Timer mTimerReadResponse = null;

    // Application  context
    private static Context mCtx;

    /**
     * Singleton retrieval of the OtkCoin.
     *
     * @return The singleton.
     */
    @SuppressLint("HandlerLeak")
    public static synchronized Otk getInstance(Context ctx) {
        logger.debug("getInstance()");

        mCtx = ctx;

        if (null == mOtk) {
            mOtk = new Otk();
            mHandler = new Handler() {
                @Override
                public void handleMessage(Message msg) {
                    OtkEvent event;
                    Tx tx;
                    Command cmd;
                    switch (msg.what) {
                        case OTK_MSG_GOT_UNSIGNED_TX:
                            logger.debug("Received OTK_MSG_GOT_UNSIGNED_TX");
                            if (mOp != Operation.OTK_OP_SIGN_PAYMENT) {
                                logger.warn("Got unsigned tx but operation is cancelled.");
                                /* Op cancelled, ignore it */
                                break;
                            }

                            // Cache to sign list
                            UnsignedTx unsigendTx = (UnsignedTx) msg.obj;
                            List<String> toSignList = unsigendTx.getToSign();
                            mArgs.clear();
                            // Also clear cached signature
                            mSignatures.clear();
                            mArgs.addAll(toSignList);

                            // Send unsigned tx event
                            sendEvent(new OtkEvent(unsigendTx));
                            break;
                        case OTK_MSG_SEND_BITCOIN_FAILED:
                            logger.debug("Received OTK_MSG_SEND_BITCOIN_FAILED");

                            event = new OtkEvent(OtkEvent.Type.SEND_BITCOIN_FAIL, (String) msg.obj);
                            sendEvent(event);
                            clearOp();
                            break;
                        case OTK_MSG_COMPLETE_PAYMENT_FAILED:
                            logger.debug("Received OTK_MSG_COMPLETE_PAYMENT_FAILED");

                            String desc = (String) msg.obj;
                            if (desc == null) {
                                desc = mCtx.getString(R.string.unknown_reason);
                            }
                            event = new OtkEvent(OtkEvent.Type.COMPLETE_PAYMENT_FAIL, desc);
                            sendEvent(event);
                            clearOp();
                            break;
                        case OTK_MSG_CURRENCY_EX_RATE_UPDATE:
                            logger.debug("Received OTK_MSG_CURRENCY_EX_RATE_UPDATE");

                            mCurrencyExRate = (ExchangeRate) msg.obj;
                            event = new OtkEvent(OtkEvent.Type.CURRENCY_EXCHANGE_RATE_UPDATE, mCurrencyExRate);
                            sendEvent(event);
                            break;
                        case OTK_MSG_TX_FEE_UPDATE:
                            logger.debug("Received OTK_MSG_TX_FEE_UPDATE");

                            event = new OtkEvent(OtkEvent.Type.TX_FEE_UPDATE, (TxFee) msg.obj);
                            sendEvent(event);
                            break;
                        case OTK_MSG_BITCOIN_SENT:
                            logger.debug("Received OTK_MSG_BITCOIN_SENT");

                            tx = (Tx) msg.obj;
                            event = new OtkEvent(OtkEvent.Type.SEND_BITCOIN_SUCCESS, tx);
                            sendEvent(event);
                            clearOp();
                            break;
                        case OTK_MSG_SESSION_TIMED_OUT:
                            logger.debug("Received OTK_MSG_SESSION_TIMED_OUT");

                            cmd = (Command) msg.obj;
                            sendEvent(new OtkEvent(OtkEvent.Type.SESSION_TIMED_OUT, cmd.toString()));
                            break;
                        case OTK_MSG_READ_RESPONSE_TIMED_OUT:
                            logger.debug("Received OTK_MSG_READ_RESPONSE_TIMED_OUT");

                            cmd = (Command) msg.obj;
                            sendEvent(new OtkEvent(OtkEvent.Type.READ_RESPONSE_TIMED_OUT, cmd.toString()));
                            break;
                        case OTK_MSG_CHECK_BALANCE:
                            logger.debug("Received OTK_MSG_CHECK_BALANCE");

                            if (mOp == Operation.OTK_OP_SIGN_PAYMENT) {
                                BigDecimal b = (BigDecimal) msg.obj;
                                double allFunds = BtcUtils.satoshiToBtc(b.longValue());
                                double txFees = BtcUtils.satoshiToBtc(mTxFees);
                                double sendAmount = mAmount;

                                logger.info("OTK Balance:{}", allFunds);

                                if (mAmount > 0 && mFeeIncluded) {
                                    sendAmount = mAmount - txFees;
                                } else if (mAmount < 0) {
                                    sendAmount = allFunds - txFees;
                                }

                                if (allFunds < sendAmount + txFees || sendAmount < 0) {
                                    // Insufficient amount
                                    sendEvent(new OtkEvent(OtkEvent.Type.SEND_BITCOIN_FAIL, mCtx.getString(R.string.balance_insufficient)));
                                    break;
                                }
                                // Continue payment
                                logger.debug("Pay amount=" + (allFunds - txFees) + ", Fees=" + BtcUtils.satoshiToBtc(mTxFees));
                                sendBitcoin(mFrom, mTo, mTxFees, mFeeIncluded);
                                event = new OtkEvent(OtkEvent.Type.FIND_UTXO);
                                sendEvent(event);
                                break;
                            }
                            // Ignore it if it's not in sign payment process
                            break;
                        default:
                    }
                }
            };

            // Timer task which calling get current exchange api.
            TimerTask task = new TimerTask() {
                public void run() {
                    ExchangeRate cer = BtcUtils.getCurrencyExchangeRate();
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
            mTimerRate.schedule(task, 100, 1000 * 60 * Configurations.INTERVAL_EXCHANGE_RATE_UPDATE);
            // Timer task which calling get current exchange api.
            TimerTask updateTxFeeTask = new TimerTask() {
                public void run() {
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
            mTimerTxFee.schedule(updateTxFeeTask, 100, 1000 * 60 * Configurations.INTERVAL_TX_FEE_UPDATE);

            // Timer task which calling get current exchange api.
        }
        return mOtk;
    }

    static private void sendEvent(OtkEvent event) {
        if (event.getType() == OtkEvent.Type.BALANCE_UPDATE) {
            if (mBalanceUpdateListener != null) {
                mBalanceUpdateListener.onOtkEvent(event);
                return;
            }
        }
        if (mEventListener == null) {
            return;
        }
        mEventListener.onOtkEvent(event);
    }

    static private void processResponseRead() {
        if (mTimerReadResponse != null) {
            mTimerReadResponse.cancel();
        }
    }

    static private void processCommandWritten() {
        if (mTimerWriteCommand != null) {
            mTimerWriteCommand.cancel();
        }
    }

    static private void clearOp() {
        logger.debug("clearOp");
        mOp = Operation.OTK_OP_NONE;
        // Clear cached data
        mTo = "";
        mAmount = 0;
        mFeeIncluded = false;
        mTxFees = 0;
        isInProcessing = false;
        boolean mUsingMasterKey = false;
        mArgs.clear();
        processCommandWritten();
        processResponseRead();
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
    public void setEventListener(OtkEventListener eventListener) {
        mEventListener = eventListener;
    }

    public void setBalanceListener(BalanceUpdateListener eventListener) {
        mBalanceUpdateListener = eventListener;
    }

    /**
     * Set operation to pay
     */
    public void setOperation(Operation op, String to, double amount, boolean feeIncluded, long txFees) {
        if (mOp != Operation.OTK_OP_NONE && isInProcessing) {
            /* Some operation is in processing, set another operation is not allowed.
             * Should cancel current operation first.
             */
            return;
        }

        if (op != Operation.OTK_OP_SIGN_PAYMENT) {
            return;
        }

        logger.debug("Set op to:{} to:{} amount:{} fee:{} feeIncluded:{}", op.name(), to, amount, txFees, feeIncluded);
        mOp = op;
        // Cache data
        mTo = to;
        mAmount = amount;
        mFeeIncluded = feeIncluded;
        mTxFees = txFees;

    }

    public void cancelOperation() {
        logger.debug("cancelOperation");
        clearOp();
    }

    private static void sendBitcoin(final String from, final String to, final long txFees, final boolean feeIncluded) {
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
                    } catch (BlockCypherException e) {
                        // Failed
                        Message msg = new Message();
                        msg.what = OTK_MSG_SEND_BITCOIN_FAILED;
                        // parse error
                        msg.obj = BlockCypher.parseError(e.getBlockCypherError().getErrors().get(0).toString());
                        mHandler.sendMessage(msg);
                    } catch (Exception e) {
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

    public void getBalance(final String address) {
        Thread t = new Thread() {
            @Override
            public void run() {
                synchronized (this) {
                    BigDecimal b = BlockChainInfo.getBalance(address);
                    if (mOp == Operation.OTK_OP_SIGN_PAYMENT) {
                        Message msg = new Message();
                        msg.what = OTK_MSG_CHECK_BALANCE;
                        msg.obj = b;
                        mHandler.sendMessage(msg);
                    } else {
                        OtkEvent event = new OtkEvent(OtkEvent.Type.BALANCE_UPDATE, address, b, mCurrencyExRate);
                        sendEvent(event);
                    }
                }
            }
        };

        t.start();
    }

    public void confirmPayment() {
        logger.debug("confirm payment");
        // Make sure op
        if (mOp != Operation.OTK_OP_SIGN_PAYMENT) {
            logger.warn("Confirm payment but operation is cancelled.");
            /* Op cancelled, ignore it */
        }
    }
}
