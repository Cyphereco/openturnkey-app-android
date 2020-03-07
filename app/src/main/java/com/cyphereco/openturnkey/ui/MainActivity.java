package com.cyphereco.openturnkey.ui;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.nfc.NfcAdapter;
import android.os.CountDownTimer;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Handler;
import android.os.Message;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.support.v7.widget.Toolbar;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.cyphereco.openturnkey.R;
import com.cyphereco.openturnkey.core.Otk;
import com.cyphereco.openturnkey.core.OtkData;
import com.cyphereco.openturnkey.core.OtkEvent;
import com.cyphereco.openturnkey.core.Tx;
import com.cyphereco.openturnkey.core.UnsignedTx;
import com.cyphereco.openturnkey.core.protocol.Command;
import com.cyphereco.openturnkey.core.protocol.OtkCommand;
import com.cyphereco.openturnkey.core.protocol.OtkRequest;
import com.cyphereco.openturnkey.core.protocol.OtkState;
import com.cyphereco.openturnkey.db.DBTransItem;
import com.cyphereco.openturnkey.db.OpenturnkeyDB;
import com.cyphereco.openturnkey.utils.AlertPrompt;
import com.cyphereco.openturnkey.utils.BtcUtils;
import com.cyphereco.openturnkey.utils.ExchangeRate;
import com.cyphereco.openturnkey.utils.LocalCurrency;
import com.cyphereco.openturnkey.utils.Log4jHelper;
import com.cyphereco.openturnkey.core.NfcHandler;
import com.cyphereco.openturnkey.utils.QRCodeUtils;

import org.slf4j.Logger;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Queue;
import java.util.TimeZone;

public class MainActivity extends AppCompatActivity
        implements
        DialogAuthByPin.DialogAuthByPinListener,
        DialogAddNote.DialogAddNoteListener,
        DialogSetPIN.DialogSetPINListener,
        FragmentPay.FragmentPayListener,
        FragmentOtk.FragmentOtkListener {

    public static final String TAG = MainActivity.class.getSimpleName();
    static Logger logger = Log4jHelper.getLogger(TAG);
    
    public static Preferences pref;
    
    private static String currentActivity = "";
    private static String payToAddress = "";
    private static String qrScanText = "";

    public static final int REQUEST_CODE_QR_CODE = 0;
    public static final int REQUEST_CODE_CHOOSE_KEY = 3;
    public static final int REQUEST_CODE_ADDRESS_EDIT = 4;
    public static final int REQUEST_CODE_TRANSACTION_INFO = 5;
    public static final int REQUEST_CODE_SIGN_MESSAGE = 6;
    public static final String KEY_QR_CODE = "KEY_QR_CODE";
    public static final String KEY_CHOOSE_KEY = "KEY_CHOOSE_KEY";
    public static final String KEY_OTK_DATA = "KEY_OTK_DATA";
    public static final String KEY_MESSAGE_TO_SIGN = "KEY_MESSAGE_TO_SIGN";
    public static final String KEY_SIGN_VALIDATE_MESSAGE = "KEY_SIGN_VALIDATE_MESSAGE";
    public static final String KEY_USING_MASTER_KEY = "KEY_USING_MASTER_KEY";
    public static final String KEY_ADDRESS_EDITOR_TEMP_ALIAS = "KEY_ADDRESS_EDITOR_TEMP_ALIAS";
    public static final String KEY_ADDRESS_EDITOR_TEMP_ADDR = "KEY_ADDRESS_EDITOR_TEMP_ADDR";
    public static final int REQUEST_RESULT_CODE_REPAY = 1000;
    public static final int REQUEST_RESULT_CODE_READ_NFC = 1001;

    private static final String BEGIN_BITCOIN_SIGNED_MESSAGE = "-----BEGIN BITCOIN SIGNED MESSAGE-----";
    private static final String AMOUNT_EQUAL_TO = "amount=";

    private Menu toolbarMenu = null;

    private static Queue<OtkRequest> otkRequestQueue = new LinkedList<>();
    private boolean includeFee = false;
    private boolean useFixAddr = false;
    private String mFixedAddress = "";
    static private Otk mOtk = null;

    private FragmentExtOtkData mFragmentPay = null;
    private FragmentExtOtkData mFragmentOtk = null;
    private FragmentExtOtkData mFragmentHistory = null;
    private FragmentExtOtkData mFragmentAddrbook = null;
    private static FragmentExtOtkData mSelectedFragment = null;


    private ExchangeRate mCurrencyExRate;
    AlertDialog.Builder mProgressDialogBuilder = null;
    AlertDialog mProgressDialog = null;
    static AlertDialog mStatusDialog = null;
    static AlertDialog mConfirmOpDialog = null;
    AlertDialog.Builder mConfirmTerminateOpDialogBuilder = null;
    AlertDialog.Builder mConfirmPaymentDialogBuilder = null;
    AlertDialog mConfirmPaymentDialog = null;
    AlertDialog.Builder mCommandResultDialogBuilder = null;
    AlertDialog mCommandResultDialog = null;
    private boolean mOperationConfirmed;
    private boolean mConfirmPaymentDialogResultValue;

    private Otk.Operation mOp = Otk.Operation.OTK_OP_NONE;
    private boolean mIsOpInProcessing = false;
    private String mRecipientAddress = "";

    private boolean mWaitingAddressFromAddrEditor = false;
    private String mAddressEditorTempAlias = "";
    private String mAddressEditorTempAddress = "";
    private long mAddressEditorDBId = 0;
    private static boolean mEnableReadOtk = false;

    public static final int FRAGMENT_PAY = R.id.nav_menu_pay;
    public static final int FRAGMENT_OTK = R.id.nav_menu_openturnkey;
    public static final int FRAGMENT_HISTORY = R.id.nav_menu_history;
    public static final int FRAGMENT_ADDRBOOK = R.id.nav_menu_addresses;

    private static float x1;
    static final int MIN_DISTANCE = 150;
    private static FragmentManager fm = null;

    DialogAuthByPin mDialogAuthByPin;
    private static BottomNavigationView bottomNav;

    private int[] navItems = {
            FRAGMENT_PAY,
            FRAGMENT_OTK,
            FRAGMENT_HISTORY,
            FRAGMENT_ADDRBOOK
    };

    public static void setCurrentActivity(String activityName) {
        currentActivity = activityName;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        pref = new Preferences(getApplication());

        fm = getSupportFragmentManager();
        
        setContentView(R.layout.activity_main);

        mFragmentPay = new FragmentPay();
        mFragmentOtk = new FragmentOtk();
        mFragmentHistory = new FragmentHistory();
        mFragmentAddrbook = new FragmentAddrbook();
        mSelectedFragment = mFragmentPay;

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        bottomNav = findViewById(R.id.bottom_navigation);

        /*
         Declaration for bottom navigation bar.
         */
        BottomNavigationView.OnNavigationItemSelectedListener navListener =
                new BottomNavigationView.OnNavigationItemSelectedListener() {
                    @Override
                    public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
                        Toolbar toolbar = findViewById(R.id.toolbar);
                        Menu menu = toolbar.getMenu();
                        menu.clear();

                        switch (menuItem.getItemId()) {
                            case FRAGMENT_HISTORY:
                                mSelectedFragment = mFragmentHistory;
                                break;
                            case FRAGMENT_ADDRBOOK:
                                mSelectedFragment = mFragmentAddrbook;
                                break;
                            case FRAGMENT_OTK:
                                mSelectedFragment = mFragmentOtk;
                                break;
                            default:
                                mSelectedFragment = mFragmentPay;
                                break;
                        }

                        getSupportFragmentManager().beginTransaction().replace(
                                R.id.frame_main, mSelectedFragment).commit();
                        return true;
                    }
                };

        bottomNav.setOnNavigationItemSelectedListener(navListener);
        navToFragment(FRAGMENT_PAY);

        /* init NFC. */
        NfcAdapter mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if (mNfcAdapter == null) {
            AlertPrompt.alert(this, getString(R.string.nfc_unavailable));
        }

        mProgressDialogBuilder = new AlertDialog.Builder(MainActivity.this);
        mConfirmTerminateOpDialogBuilder = new AlertDialog.Builder(MainActivity.this);
        mConfirmPaymentDialogBuilder = new AlertDialog.Builder(MainActivity.this);
        mCommandResultDialogBuilder = new AlertDialog.Builder(MainActivity.this);

        mOtk = Otk.getInstance(getApplicationContext());
        mOtk.setEventListener(new Otk.OtkEventListener() {
            @Override
            public void onOtkEvent(OtkEvent event) {
                OtkEvent.Type type = event.getType();
                logger.debug("onOtkEvent:" + type.toString());
                /* TODO: process event */
                if (type == OtkEvent.Type.CURRENCY_EXCHANGE_RATE_UPDATE) {
                    // Cache rate
                    mCurrencyExRate = event.getCurrencyExRate();
                    if (mSelectedFragment instanceof FragmentPay) {
                        // Update rate
                        ((FragmentPay) mSelectedFragment).updateCurrencyExchangeRate(mCurrencyExRate);
                    }
                }
                else if (type == OtkEvent.Type.TX_FEE_UPDATE) {
                    // Store to preference
                    Preferences.setTxFee(event.getTxFee());
                } else if (type == OtkEvent.Type.APPROACH_OTK) {
                    hideProgressDialog();
                    // Stop cancel timer
                    if (mSelectedFragment instanceof FragmentOtk) {
                        ((FragmentOtk) mSelectedFragment).stopCancelTimer();
                    }
                } else if (type == OtkEvent.Type.FIND_UTXO || type == OtkEvent.Type.CHECKING_BALANCE_FOR_PAYMENT) {
                    // Stop cancel timer
                    if (mSelectedFragment instanceof FragmentOtk) {
                        // Update rate
                        ((FragmentOtk) mSelectedFragment).stopCancelTimer();
                    }
                    // Hide status dialog
                    hideStatusDialog();
                    // Show progress spin circle
                    showProgressDialog(getString(R.string.check_balance));
                } else if (type == OtkEvent.Type.OPERATION_IN_PROCESSING) {
                    // Hide status dialog
                    hideStatusDialog();
                    // Show progress spin circle
                    showProgressDialog(getString(R.string.processing));
                } else if (type == OtkEvent.Type.GENERAL_INFORMATION) {
                    Intent intent = new Intent(getApplicationContext(), ActivityOpenturnkeyInfo.class);
                    intent.putExtra(KEY_OTK_DATA, event.getData());
                    startActivity(intent);
                } else if (type == OtkEvent.Type.SEND_BITCOIN_SUCCESS) {
                    hideProgressDialog();
                    Tx tx = event.getTx();
                    // Add transaction to database.
                    addTxToDb(tx);
                    // Make sure we are in FragmentOtk
                    if (mSelectedFragment instanceof FragmentOtk) {
                        // Go back to pay fragment
                        mOp = Otk.Operation.OTK_OP_NONE;
                        mIsOpInProcessing = false;
                        /* TODO: Go to history page and show the tx */
                        navToFragment(FRAGMENT_HISTORY);
                        // Show tx
                        dialogBtcSent(tx);
                    }

                } else if ((type == OtkEvent.Type.SEND_BITCOIN_FAIL) ||
                        (type == OtkEvent.Type.COMMAND_EXECUTION_FAILED) ||
                        (type == OtkEvent.Type.SESSION_ID_MISMATCH)) {
                    // Hide progress
                    hideProgressDialog();
                    // Show error in dialog
                    String s;
                    if (type == OtkEvent.Type.SESSION_ID_MISMATCH) {
                        s = getString(R.string.communication_error);
                    } else {
                        s = getString(R.string.try_later) + "\n\n" +
                                "{" + event.getFailureReason() + "}";
                    }

                    dialogSentBtcFailed(parseFailureReason(s));

                    mOp = Otk.Operation.OTK_OP_NONE;
                    mIsOpInProcessing = false;
                    mOtk.cancelOperation();
                    // Make sure we are in FragmentOtk
                    if (mSelectedFragment instanceof FragmentOtk) {
                        // Go back to pay fragment
                        navToFragment(FRAGMENT_PAY);
                    }
                } else if (type == OtkEvent.Type.COMPLETE_PAYMENT_FAIL) {
                    // Hide progress
                    hideProgressDialog();
                    mOp = Otk.Operation.OTK_OP_NONE;
                    mIsOpInProcessing = false;
                    mOtk.cancelOperation();
                    // Make sure we are in FragmentOtk
                    if (mSelectedFragment instanceof FragmentOtk) {
                        /* TODO: Go to history page and show the tx */
                        navToFragment(FRAGMENT_HISTORY);
                        // Show error in dialog
                        String reason;
                        if (event.getTx() != null) {
                            // Add tx to db
                            addTxToDb(event.getTx());
                            reason = event.getTx().getDesc();
                        } else {
                            reason = parseFailureReason(event.getFailureReason());
                        }
                        String s = getString(R.string.try_later) + "\n\n" +
                                "{" + reason + "}";
                        dialogSentBtcFailed(s);
                    }

                } else if (type == OtkEvent.Type.OTK_UNAUTHORIZED) {
                    // Dismiss progress dialog
                    hideProgressDialog();
                    // Show pre-auth with pin dialog
                    dialogAuthByPin();
                } else if (type == OtkEvent.Type.UNSIGNED_TX) {
                    if (mOp != Otk.Operation.OTK_OP_SIGN_PAYMENT) {
                        logger.error("Got unsigned tx but sign payment is already terminated");
                    } else {
                        // Show dialog for user to confirm the payment
                        UnsignedTx utx = event.getUnsignedTx();
                        if (showConfirmPaymentDialog(utx)) {
                            // Confirm payment
                            mOtk.confirmPayment();
                        } else {
                            // Cancel payment
                            onCancelButtonClick();
                        }
                    }
                } else if (type == OtkEvent.Type.OTK_IS_NOT_LOCKED) {
                    hideStatusDialog();
                    showCommandResultDialog(getString(R.string.unlock_failed), getString(R.string.otk_is_not_locked));
                    mOp = Otk.Operation.OTK_OP_NONE;
                    if (mSelectedFragment instanceof FragmentOtk) {
                        ((FragmentOtk) mSelectedFragment).updateOperation(mOp);
                    }
                    mIsOpInProcessing = false;
                    mOtk.cancelOperation();
                    Intent intent = new Intent(getApplicationContext(), ActivityOpenturnkeyInfo.class);
                    intent.putExtra(KEY_OTK_DATA, event.getData());
                    startActivity(intent);

                } else if (type == OtkEvent.Type.UNLOCK_SUCCESS) {
                    hideStatusDialog();
                    showCommandResultDialog(getString(R.string.unlock_success), getString(R.string.otk_is_unlocked));
                    mOp = Otk.Operation.OTK_OP_NONE;
                    mIsOpInProcessing = false;
                    mOtk.cancelOperation();
                    if (mSelectedFragment instanceof FragmentOtk) {
                        ((FragmentOtk) mSelectedFragment).updateOperation(mOp);
                    }
                    Intent intent = new Intent(getApplicationContext(), ActivityOpenturnkeyInfo.class);
                    intent.putExtra(KEY_OTK_DATA, event.getData());
                    startActivity(intent);
                } else if (type == OtkEvent.Type.UNLOCK_FAIL) {
                    hideStatusDialog();
                    mOp = Otk.Operation.OTK_OP_NONE;
                    mIsOpInProcessing = false;
                    mOtk.cancelOperation();
                    showStatusDialog(getString(R.string.unlock_failed), parseFailureReason(event.getFailureReason()));
                    if (mSelectedFragment instanceof FragmentOtk) {
                        ((FragmentOtk) mSelectedFragment).updateOperation(mOp);
                    }
                } else if (type == OtkEvent.Type.WRITE_NOTE_SUCCESS) {
                    hideStatusDialog();
                    showCommandResultDialog(getString(R.string.write_note), getString(R.string.write_note_success));
                    mOp = Otk.Operation.OTK_OP_NONE;
                    mIsOpInProcessing = false;
                    mOtk.cancelOperation();
                    if (mSelectedFragment instanceof FragmentOtk) {
                        ((FragmentOtk) mSelectedFragment).updateOperation(mOp);
                    }
                    Intent intent = new Intent(getApplicationContext(), ActivityOpenturnkeyInfo.class);
                    intent.putExtra(KEY_OTK_DATA, event.getData());
                    startActivity(intent);
                } else if (type == OtkEvent.Type.WRITE_NOTE_FAIL) {
                    hideStatusDialog();
                    mOp = Otk.Operation.OTK_OP_NONE;
                    mIsOpInProcessing = false;
                    mOtk.cancelOperation();
                    if (mSelectedFragment instanceof FragmentOtk) {
                        ((FragmentOtk) mSelectedFragment).updateOperation(mOp);
                    }
                    showStatusDialog(getString(R.string.write_note_fail), parseFailureReason(event.getFailureReason()));
                } else if (type == OtkEvent.Type.SET_PIN_SUCCESS) {
                    hideStatusDialog();
                    showCommandResultDialog(getString(R.string.set_pin_code), getString(R.string.set_pin_success));
                    mOp = Otk.Operation.OTK_OP_NONE;
                    mIsOpInProcessing = false;
                    mOtk.cancelOperation();
                    if (mSelectedFragment instanceof FragmentOtk) {
                        ((FragmentOtk) mSelectedFragment).updateOperation(mOp);
                    }
                    Intent intent = new Intent(getApplicationContext(), ActivityOpenturnkeyInfo.class);
                    intent.putExtra(KEY_OTK_DATA, event.getData());
                    startActivity(intent);
                } else if (type == OtkEvent.Type.SET_PIN_FAIL) {
                    hideStatusDialog();
                    mOp = Otk.Operation.OTK_OP_NONE;
                    mIsOpInProcessing = false;
                    mOtk.cancelOperation();
                    if (mSelectedFragment instanceof FragmentOtk) {
                        ((FragmentOtk) mSelectedFragment).updateOperation(mOp);
                    }
                    showStatusDialog(getString(R.string.set_pin_fail), parseFailureReason(event.getFailureReason()));
                } else if (type == OtkEvent.Type.CHOOSE_KEY_SUCCESS) {
                    hideStatusDialog();
                    showCommandResultDialog(getString(R.string.choose_key), getString(R.string.choose_key_success));
                    mOp = Otk.Operation.OTK_OP_READ_GENERAL_INFO;
                    mIsOpInProcessing = false;
                    mOtk.cancelOperation();
                    if (mSelectedFragment instanceof FragmentOtk) {
                        ((FragmentOtk) mSelectedFragment).updateOperation(mOp);
                    }
                } else if (type == OtkEvent.Type.CHOOSE_KEY_FAIL) {
                    hideStatusDialog();
                    mIsOpInProcessing = false;
                    mOtk.cancelOperation();
                    mOp = Otk.Operation.OTK_OP_READ_GENERAL_INFO;
                    if (mSelectedFragment instanceof FragmentOtk) {
                        ((FragmentOtk) mSelectedFragment).updateOperation(mOp);
                    }
                    showStatusDialog(getString(R.string.choose_key_fail), parseFailureReason(event.getFailureReason()));
                } else if ((type == OtkEvent.Type.GET_KEY_SUCCESS) || (type == OtkEvent.Type.GET_KEY_FAIL)) {
                    processGetKeyEvent(event);
                } else if (type == OtkEvent.Type.SIGN_MESSAGE_SUCCESS) {
                    hideStatusDialog();
                    mOp = Otk.Operation.OTK_OP_READ_GENERAL_INFO;
                    mIsOpInProcessing = false;
                    mOtk.cancelOperation();
                    if (mSelectedFragment instanceof FragmentOtk) {
                        ((FragmentOtk) mSelectedFragment).updateOperation(mOp);
                    }
                    Intent intent = new Intent(getApplicationContext(), ActivitySignValidateMessage.class);
                    intent.putExtra(KEY_OTK_DATA, event.getData());
                    intent.putExtra(KEY_MESSAGE_TO_SIGN, event.getMessageToSign());
                    intent.putExtra(KEY_USING_MASTER_KEY, event.getUsingMasterKey());
                    startActivity(intent);
//                    startActivityForResult(intent, MainActivity.REQUEST_CODE_SIGN_MESSAGE);
                } else if (type == OtkEvent.Type.SIGN_MESSAGE_FAIL) {
                    hideStatusDialog();
                    mIsOpInProcessing = false;
                    mOtk.cancelOperation();
                    mOp = Otk.Operation.OTK_OP_READ_GENERAL_INFO;
                    if (mSelectedFragment instanceof FragmentOtk) {
                        ((FragmentOtk) mSelectedFragment).updateOperation(mOp);
                    }
                    showStatusDialog(getString(R.string.sign_message_fail), parseFailureReason(event.getFailureReason()));
                } else if (type == OtkEvent.Type.OTK_PIN_UNSET) {
                    /* Clear current OTK op */
                    mIsOpInProcessing = false;
                    mOtk.cancelOperation();
                    /* Go to set PIN page */
                    mOp = Otk.Operation.OTK_OP_READ_GENERAL_INFO;
                    if (mSelectedFragment instanceof FragmentOtk) {
                        ((FragmentOtk) mSelectedFragment).updateOperation(mOp);
                    }
                    showStatusDialog(getString(R.string.pin_unset), getString(R.string.pin_unset_msg));
                } else if ((type == OtkEvent.Type.RESET_SUCCESS) || (type == OtkEvent.Type.RESET_FAIL)) {
                    processResetOtkEvent(event);
                } else if ((type == OtkEvent.Type.EXPORT_WIF_KEY_SUCCESS) ||
                        (type == OtkEvent.Type.EXPORT_WIF_KEY_FAIL)) {
                    processExportWifKeyEvent(event);
                    /* Show Private key WIF format */
                } else if ((type == OtkEvent.Type.SESSION_TIMED_OUT) ||
                        (type == OtkEvent.Type.READ_RESPONSE_TIMED_OUT)) {
                    // Dismiss dialogs
                    hideProgressDialog();
                    hideDialogConfirmOperationAndWaitResult();
                    hideConfirmPaymentDialog();
                    hideStatusDialog();
                    hideDialogAuthByPin();
                    /* TODO update error description */
                    if (type == OtkEvent.Type.SESSION_TIMED_OUT) {
                        showStatusDialog(getString(R.string.operation_timeout), getString(R.string.session_timeout));
                    } else {
                        showStatusDialog(getString(R.string.operation_timeout), getString(R.string.read_response_timeout));
                    }
                    /* Clear current OTK op */
                    mIsOpInProcessing = false;
                    mOtk.cancelOperation();
                    /* Go to set PIN page */
                    mOp = Otk.Operation.OTK_OP_READ_GENERAL_INFO;
                    if (mSelectedFragment instanceof FragmentOtk) {
                        ((FragmentOtk) mSelectedFragment).updateOperation(mOp);
                    }
                } else {
                    logger.debug("Unhandled event:{}", type.name());
                }
            }
        });
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        /*  For the incoming intents, we only care about NFC event,
            specifically, the NDEF message event which is used by OpenTurnKey.
            We will ignore other intents since this app is designed to deal with
            OpenTurnKey only.
         */

        /*
         1. If this is not the currentActivity, dispatch the intent to the currentActivity
         2. Keep NFC read result processed in each activity to avoid cross-activity operations
            and data passing.
         */
        /* Note:
            The foreground dispatch system does not work and the other activities will not
            receive NFC intent as expected, so we implement this instaed.
         */
        if (!getClass().getName().equals(currentActivity)) {
            Class cls = null;

            try {
                cls = Class.forName(currentActivity);
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }

            if (cls != null) {
                Intent newIntent = new Intent(this, cls);

                if (mEnableReadOtk) {
                    newIntent.setAction(intent.getAction())
                        .putExtra(NfcAdapter.EXTRA_TAG,
                                intent.getParcelableExtra(NfcAdapter.EXTRA_TAG))
                        .putExtra(NfcAdapter.EXTRA_NDEF_MESSAGES,
                                intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES))
                        .setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                }
                startActivity(newIntent);
            }
        }

        if (!mEnableReadOtk || !getClass().getName().equals(currentActivity)) {
            return;
        }

        String action = intent.getAction();

        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action)) {
            logger.info("Found NFC tag!!");
            OtkData otkData = NfcHandler.parseIntent(intent);

            if (otkData != null) {
                logger.debug("Found OpenTurnKey:\nSession#{} : {}{}",
                        otkData.getSessionData().getSessionId(),
                        otkData.getSessionData().getAddress(),
                        otkData.getOtkState().toString()
                );

                // disableReadOtk to avoid new intent breaking unfinished processing
                disableReadOtk();

                /*
                 If the otkRequestQueue is not empty, there is a request pending.
                 Check if public key and session id matched current session.
                 */
                if (hasRequest()) {
                    OtkRequest request = peekRequest();

                    /*
                     If a request has a session Id and otk address, the request
                     must have been delivered to an openturnkey and expecting a request result.
                     */
                    if (request.getSessionId().length() > 0) {
                        logger.info("Waiting for request result");

                        // Sanity check on the otkData
                        if (otkData.getSessionData().getSessionId().equals(request.getSessionId()) &&
                                otkData.getSessionData().getAddress().equals(request.getOtkAddress())) {
                            /*
                             Request has been delivered, intent should contain request result.
                             Either success or fail, the request is made, remove it from the
                             otkRequestQueue.
                             */
                        }
                        else {
                            /*
                             Sanity check failed, error occurs, should quit request to avoid
                             suspicious hack.
                             */
                            logger.error("Invalid request result.");
                            otkData = null;
                            // handleRequestResult(request, otkData)

                            AlertPrompt.info(this, getString(R.string.not_openturnkey));
                        }
                    }
                    else {
                        // Pending request has not been delivered, prepare to send.

                        // Check if OpenTurnKey is locked to accept authentication.
                        if (otkData.getOtkState().getLockState() == OtkState.LockState.UNLOCKED) {
                            // OpenTurnKey is not locked, request cannot be made
                            AlertPrompt.info(this, getString(R.string.otk_is_unlocked));
                            return;
                        }
//                        else if (otkData.getOtkState().getLockState() != OtkState.LockState.AUTHORIZED &&
//                        request.getPin().length() != 8) {
//                            dialogAuthByPin();
//                            return;
//                        }

                        /*
                         Set the session Id and otk address with the otkData we just parsed.
                         */
                        request.setSessionId(otkData.getSessionData().getSessionId());
                        request.setOtkAddress(otkData.getSessionData().getAddress());
                        String sessId = NfcHandler.sendRequest(intent, request);

                        if (!otkData.getSessionData().getSessionId().equals(sessId)) {
                            /*
                             Send request failed, most likely a communication error occurs.
                             Remove the session id and otk address and keep the request as a fresh one.
                             */
                            request.setSessionId("");
                            request.setOtkAddress("");
                            logger.info("Something wrong, request is not sent.");
                        }
                        else {
                            /*
                             Request delivered, the current otkData is not useful result.
                             Set otkData to null and waiting for process request result in
                             the next intent.
                             */
                            otkData = null;
                            enableReadOtk();
                        }
                    }
                }

                // process valid OpenTurnKey data
                if (otkData != null) {
                    logger.info(otkData.toString());

                    // Notify the DialogReadOtk of the parsing result and close the dialog.
                    DialogReadOtk.updateReadOtkStatus(DialogReadOtk.READ_SUCCESS);

                    /* There is an otk show result delay in DialogReadOtk,
                        to start to a new activity, wait until the dialog closed.
                     */
                    final OtkData _otkData = otkData;
                    new CountDownTimer(DialogReadOtk.SHOW_RESULT_DELAY +
                            DialogReadOtk.DISMISS_ANIMATION_TIME, 1000) {
                        public void onTick(long millisUntilFinished) {
                        }

                        public void onFinish() {
                            // show otkData in ActivityOpenturnkeyInfo
                            mSelectedFragment.postOtkData(_otkData);
                        }
                    }.start();
                }
            }
            else {
                logger.info("Not a valid OpenTurnKey");
                // Notify the DialogReadOtk of the parsing result and close the dialog.
                DialogReadOtk.updateReadOtkStatus(DialogReadOtk.NOT_OPENTURNKEY);
                AlertPrompt.info(this, getString(R.string.not_openturnkey));
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent;
        int optionId = item.getItemId();

        switch (optionId) {
            case R.id.menu_openturnkey_unlock:
                pushRequest(new OtkRequest(menuOptionToOtkCommand(optionId)));
                dialogAuthByPin();
                return true;
            case R.id.menu_openturnkey_set_note:
                pushRequest(new OtkRequest(menuOptionToOtkCommand(optionId)));
                dialogAddNote();
                return true;
            case R.id.menu_openturnkey_set_pin:
                if (dialogConfirmOperationAndWaitResult(getString(R.string.warning),
                        getString(R.string.pin_code_warning_message),
                        getString(R.string.understood))) {
                    // show set pin dialog
                    pushRequest(new OtkRequest(menuOptionToOtkCommand(optionId)));
                    dialogSetPIN();
                    return true;
                }
            case R.id.menu_openturnkey_choose_key:
                // show confirm dialog
                if (dialogConfirmOperationAndWaitResult(getString(R.string.warning),
                        getString(R.string.choose_key_warning_message),
                        getString(R.string.understood))) {
                    logger.debug("Choose key Confirmed.");
                    pushRequest(new OtkRequest(menuOptionToOtkCommand(optionId)));
                    intent = new Intent(this, ActivityChooseKey.class);
//                    startActivity(intent);
                    startActivityForResult(intent, MainActivity.REQUEST_CODE_CHOOSE_KEY);
                    return true;
                }
            case R.id.menu_openturnkey_sign_message:
                intent = new Intent(this, ActivitySignValidateMessage.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(intent);
//                startActivityForResult(intent, MainActivity.REQUEST_CODE_SIGN_MESSAGE);
                return true;
            case R.id.menu_openturnkey_get_key:
                if (dialogConfirmOperationAndWaitResult(getString(R.string.warning),
                        getString(R.string.full_pubkey_info_warning),
                        getString(R.string.understood))) {
                    pushRequest(new OtkRequest(menuOptionToOtkCommand(optionId)));
                    dialogAuthByPin();
                    return true;
                } else {
                    mOp = Otk.Operation.OTK_OP_READ_GENERAL_INFO;
                    ((FragmentOtk) mSelectedFragment).updateOperation(mOp);
                    logger.debug("Get key information cancelled!");
                    return false;
                }
            case R.id.menu_openturnkey_export_wif_key:
                if (dialogConfirmOperationAndWaitResult(getString(R.string.warning),
                        getString(R.string.export_wif_warning_message),
                        getString(R.string.understood))) {
                    pushRequest(new OtkRequest(menuOptionToOtkCommand(optionId)));

//                    exportWifKey();
                    return true;
                }
            case R.id.menu_openturnkey_reset:
                if (dialogConfirmOperationAndWaitResult(getString(R.string.warning),
                        getString(R.string.reset_warning_message),
                        getString(R.string.understood))) {
                    pushRequest(new OtkRequest(menuOptionToOtkCommand(optionId)));

//                    resetOtk();
                    return true;
                }
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        intent.addCategory(Intent.CATEGORY_HOME);
        startActivity(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();

        setCurrentActivity(getClass().getName());

        logger.debug("Current Fragment={}", getNavIndex(bottomNav.getSelectedItemId()));

        // Synchronize navigation bar with with fragment
        navToFragment(bottomNav.getSelectedItemId());

        // Refresh current fragment
        getSupportFragmentManager().beginTransaction().replace(
                R.id.frame_main, mSelectedFragment).commit();

        getSupportFragmentManager().beginTransaction()
                .detach(mSelectedFragment)
                .attach(mSelectedFragment)
                .commit();
    }


    public void addNote(String note) {
        logger.debug("note:" + note);
        OtkRequest request = otkRequestQueue.peek();

        if (request != null && request.getCommand().equals(Command.SET_NOTE.toString())) {
            request.setData(note);
            dialogAuthByPin();
        }
    }

    public void cancelAddNote() {
        mOtk.cancelOperation();
        mOp = Otk.Operation.OTK_OP_NONE;
        mIsOpInProcessing = false;
        ((FragmentOtk) mSelectedFragment).updateOperation(mOp);
    }

    public void setPIN(String pin) {
        logger.info("pin:" + pin);
        OtkRequest request = otkRequestQueue.peek();
        if (request != null && request.getCommand().equals(Command.SET_PIN.toString())) {
            request.setData(pin);
            dialogAuthByPin();
        }
    }

    public void cancelSetPIN() {
        mOtk.cancelOperation();
        mOp = Otk.Operation.OTK_OP_NONE;
        mIsOpInProcessing = false;
        ((FragmentOtk) mSelectedFragment).updateOperation(mOp);
    }

    public static void clearRequest() {
        while (otkRequestQueue.size() > 0) otkRequestQueue.poll();
    }

    public void authByPin(String pin) {
        logger.debug("pin:" + pin);
        OtkRequest request = peekRequest();
        if (request != null) request.setPin(pin);
    }

    public void cancelAuthByPin() {
        mOtk.cancelOperation();
        mOp = Otk.Operation.OTK_OP_NONE;
        mIsOpInProcessing = false;
        navToFragment(FRAGMENT_PAY);
    }

    public void onSignPaymentButtonClick(String to, double amount, String btcAmount, String lcAmount, boolean isAllFundsChecked) {
        logger.debug("Make payment:\nTo: " + to +
                "\nBTC = " + btcAmount + "\nLocal currency = " + lcAmount +
                "\nmAmount:" + amount + "\nUse all funds: " + isAllFundsChecked);

        mOp = Otk.Operation.OTK_OP_SIGN_PAYMENT;
        long txFees = BtcUtils.getTxFeeInSatoshi();

        double payAmount = isAllFundsChecked ? -1 : amount;
        mOtk.setOperation(Otk.Operation.OTK_OP_SIGN_PAYMENT, to, payAmount, includeFee, txFees);

        navToFragment(FRAGMENT_OTK);

        mIsOpInProcessing = true;

        // Cache recipient address and amount
        mRecipientAddress = to;
    }

    public static void navToFragment(int fragmentId) {
        bottomNav.setSelectedItemId(fragmentId);
    }

    public void onCancelButtonClick() {
        AlertPrompt.info(this, getString(R.string.operation_cancelled));
        if (mOp != Otk.Operation.OTK_OP_SIGN_PAYMENT) {
            mOp = Otk.Operation.OTK_OP_READ_GENERAL_INFO;
            mIsOpInProcessing = false;
            if (mSelectedFragment instanceof FragmentOtk) {
                ((FragmentOtk) mSelectedFragment).updateOperation(mOp);
            }
            mOtk.cancelOperation();
            return;
        }

        // For sign payment
        hideConfirmPaymentDialog();
        hideProgressDialog();
        hideDialogConfirmOperationAndWaitResult();
        hideStatusDialog();
        mOtk.cancelOperation();
        mOp = Otk.Operation.OTK_OP_NONE;
        mIsOpInProcessing = false;
        if (mWaitingAddressFromAddrEditor) {
            backToAddressEditorActivity(mAddressEditorTempAlias, mAddressEditorTempAddress);
        } else {
            navToFragment(FRAGMENT_PAY);
        }
    }

    public void onCancelTimeout() {
        AlertPrompt.info(this, getString(R.string.operation_timeout));
        mOtk.cancelOperation();
        mOp = Otk.Operation.OTK_OP_NONE;
        mIsOpInProcessing = false;
    }

    private void clearCachedPayFragmentData() {
        mRecipientAddress = "";
    }

    private void addTxToDb(Tx tx) {
        if (tx == null) {
            logger.error("addTxToDb(): tx is null");
            return;
        }
        logger.info("addTxToDb() tx:\n{}", tx.toString());

        // Get timezone offset
        Calendar mCalendar = new GregorianCalendar();
        TimeZone mTimeZone = mCalendar.getTimeZone();
        int mGMTOffset = mTimeZone.getRawOffset();

        // Add transaction to database.
        DBTransItem dbTrans = new DBTransItem(0,
                BtcUtils.convertDateTimeStringToLong(tx.getTime()) + mGMTOffset,
                tx.getHash(), tx.getFrom(), tx.getTo(), tx.getAmount(), tx.getFee(),
                tx.getStatus().toInt(), tx.getDesc(), tx.getRaw(), tx.getConfirmations());
        OpenturnkeyDB otkDB = new OpenturnkeyDB(getApplicationContext());
        otkDB.addTransaction(dbTrans);
        logger.info("DB tx count:{}", otkDB.getTransactionCount());
    }

    private void backToAddressEditorActivity(String alias, String address) {
        mWaitingAddressFromAddrEditor = false;

        Intent intent = new Intent(getApplicationContext(), ActivityAddressEditor.class);

        intent.putExtra(ActivityAddressEditor.KEY_EDITOR_CONTACT_DB_ID, mAddressEditorDBId);
        intent.putExtra(ActivityAddressEditor.KEY_EDITOR_CONTACT_ALIAS, alias);
        intent.putExtra(ActivityAddressEditor.KEY_EDITOR_CONTACT_ADDR, address);
        startActivity(intent);
    }

    private void processGetKeyEvent(OtkEvent event) {
        Intent intent;

        logger.debug("processGetKeyEvent");
        hideStatusDialog();
        mIsOpInProcessing = false;
        mOtk.cancelOperation();
        mOp = Otk.Operation.OTK_OP_READ_GENERAL_INFO;
        if (mSelectedFragment instanceof FragmentOtk) {
            ((FragmentOtk) mSelectedFragment).updateOperation(mOp);
        }

        if (OtkEvent.Type.GET_KEY_SUCCESS == event.getType()) {
            intent = new Intent(this, ActivityKeyInformation.class);
            intent.putExtra(KEY_OTK_DATA, event.getData());
            startActivity(intent);
        } else if (OtkEvent.Type.GET_KEY_FAIL == event.getType()) {
            showStatusDialog(getString(R.string.get_key_fail), parseFailureReason(event.getFailureReason()));
        }
    }

    private void resetOtk() {
        logger.info("Reset OTK");
        mOp = Otk.Operation.OTK_OP_RESET;
        if (mSelectedFragment instanceof FragmentOtk) {
            ((FragmentOtk) mSelectedFragment).updateOperation(mOp);
        }
        mIsOpInProcessing = true;
        mOtk.setOperation(mOp);
    }

    private void processResetOtkEvent(OtkEvent event) {
        logger.info("processResetOtkEvent");
        hideStatusDialog();
        mIsOpInProcessing = false;
        mOtk.cancelOperation();
        mOp = Otk.Operation.OTK_OP_READ_GENERAL_INFO;
        if (mSelectedFragment instanceof FragmentOtk) {
            ((FragmentOtk) mSelectedFragment).updateOperation(mOp);
        }

        if (OtkEvent.Type.RESET_FAIL == event.getType()) {
            showStatusDialog(getString(R.string.reset_fail), parseFailureReason(event.getFailureReason()));
        } else if (OtkEvent.Type.RESET_SUCCESS == event.getType()) {
            showStatusDialog(getString(R.string.reset_success), getString(R.string.reset_step_intro));
        }
    }

    private void exportWifKey() {
        logger.info("exportWifKey");
        mOp = Otk.Operation.OTK_OP_EXPORT_WIF_KEY;
        mIsOpInProcessing = true;
        if (mSelectedFragment instanceof FragmentOtk) {
            ((FragmentOtk) mSelectedFragment).updateOperation(mOp);
        }
        mOtk.setOperation(mOp);
    }

    private void processExportWifKeyEvent(OtkEvent event) {
        logger.info("processExportWifKeyEvent");
        hideStatusDialog();
        mIsOpInProcessing = false;
        mOtk.cancelOperation();
        mOp = Otk.Operation.OTK_OP_READ_GENERAL_INFO;
        if (mSelectedFragment instanceof FragmentOtk) {
            ((FragmentOtk) mSelectedFragment).updateOperation(mOp);
        }

        if (OtkEvent.Type.EXPORT_WIF_KEY_FAIL == event.getType()) {
            showStatusDialog(getString(R.string.export_private_key_wif_fail), getString(R.string.need_fp_preauth));
        } else if (OtkEvent.Type.EXPORT_WIF_KEY_SUCCESS == event.getType()) {
            /* Show Private key which is WIF format */
            final String keyInfo = event.getData().getSessionData().getWIFKey();
            final View v = View.inflate(this, R.layout.dialog_private_key_wif, null);
            TextView tvKeyString = v.findViewById(R.id.textView_export_key_string);
            ImageView ivQRCode = v.findViewById(R.id.imageView_export_key_qrcode);

            logger.info("Show private key in dialog. key: {}", keyInfo);

            tvKeyString.setText(keyInfo);
            Bitmap bitmap = QRCodeUtils.encodeAsBitmap(keyInfo,
                    ivQRCode.getDrawable().getIntrinsicWidth(),
                    ivQRCode.getDrawable().getIntrinsicHeight());
            ivQRCode.setImageBitmap(bitmap);
            // copy button
            ImageView copy = v.findViewById(R.id.wif_key_copy);
            copy.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Context context = view.getContext();
                    ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                    ClipData clip = ClipData.newPlainText("wif_key", keyInfo);
                    if (clipboard != null) {
                        clipboard.setPrimaryClip(clip);
                    }
                    AlertPrompt.info(context, getString(R.string.copy));
                }
            });

            new AlertDialog.Builder(MainActivity.this)
                    .setTitle(getString(R.string.export_private_key))
                    .setView(v)
                    .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    })
                    .show();
        }
    }

    private void dialogAddNote() {
        DialogAddNote dialog = new DialogAddNote();
        dialog.show(getSupportFragmentManager(), "dialog");
    }

    private void dialogSetPIN() {
        DialogSetPIN dialog = new DialogSetPIN();
        dialog.show(getSupportFragmentManager(), "dialog");
    }

    public void dialogAuthByPin() {
        mDialogAuthByPin = new DialogAuthByPin();
        mDialogAuthByPin.setCancelable(false);
        mDialogAuthByPin.show(getSupportFragmentManager(), "dialog");
    }

    public void hideDialogAuthByPin() {
        if ((mDialogAuthByPin != null) && mDialogAuthByPin.isVisible()) {
            mDialogAuthByPin.dismiss();
        }
    }

    private void showProgressDialog(String title) {
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            return;
        }
        mProgressDialog = mProgressDialogBuilder.setTitle(title)
                .setView(R.layout.dialog_progress_circle)
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        logger.debug("onCancel()");
                        onCancelButtonClick();
                    }
                })
                .setCancelable(true)
                .show();
    }

    private void hideProgressDialog() {
        if (mProgressDialog != null) {
            mProgressDialog.dismiss();
        }
    }

    private void showStatusDialog(String title, String message) {
        if (mStatusDialog != null && mStatusDialog.isShowing()) {
            // Update title and message
            mStatusDialog.setTitle(title);
            mStatusDialog.setMessage(message);
            return;
        }
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(MainActivity.this);

        mStatusDialog = alertDialogBuilder.setTitle(title)
                .setMessage(message)
                .setPositiveButton(R.string.ok, null)
                .setCancelable(false)
                .show();
    }

    private void hideStatusDialog() {
        if (mStatusDialog != null) {
            mStatusDialog.dismiss();
        }
    }

    boolean showConfirmPaymentDialog(UnsignedTx utx) {
        @SuppressLint("HandlerLeak") final Handler handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                throw new RuntimeException();
            }
        };

        if (mConfirmPaymentDialog != null && mConfirmPaymentDialog.isShowing()) {
            logger.error("Confirm dialog is shown, should be some error!");
        }

        // From:
        // To:
        // Amount:
        // Fee:
        // Estimated time to be confirmed:

        long estBlocks = BtcUtils.getEstimatedTime(utx.getFee());
        String estTime = (estBlocks == 1) ? " 5~15" : ((estBlocks > 3) ? " 40~60+" : " 15~35");
        double txFees = BtcUtils.satoshiToBtc(utx.getFee());
        double payAmount = utx.getAmount() + txFees;

        LocalCurrency lc = Preferences.getLocalCurrency();
        String strBtcAmount = String.format(Locale.ENGLISH, "%.8f", payAmount);
        String strFiatAmount = String.format(Locale.ENGLISH, "%.3f", BtcUtils.btcToLocalCurrency(mCurrencyExRate, lc, payAmount));
        String strBtcFees = String.format(Locale.ENGLISH, "%.8f",txFees);
        String strFiatFess = String.format(Locale.ENGLISH, "%.3f", BtcUtils.btcToLocalCurrency(mCurrencyExRate, lc, txFees));

        String msg = getString(R.string.subject_sender) + "\n" + utx.getFrom() + "\n" +
                getString(R.string.subject_recipient) + "\n" + utx.getTo() + "\n\n" +
                getString(R.string.amount_fees_included) + ":\n"+
                strBtcAmount + " / " + strFiatAmount + " (" + getString(R.string._unit_btc) + "/" + lc.toString() + ")\n\n" +
                getString(R.string.transaction_fee) + ":\n" +
                strBtcFees + " / " + strFiatFess + " (" + getString(R.string._unit_btc) + "/" + lc.toString() + ")\n\n" +
                getString(R.string.subject_text_estimated_time) + estTime;
        mConfirmPaymentDialog = mConfirmPaymentDialogBuilder.setTitle(R.string.confirm_payment)
                .setMessage(msg)
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        logger.debug("onCancel()");
                        mConfirmPaymentDialogResultValue = false;
                        handler.sendMessage(handler.obtainMessage());
                        onCancelButtonClick();
                    }
                })
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        logger.debug("onOk()");
                        hideConfirmPaymentDialog();
                        mConfirmPaymentDialogResultValue = true;
                        handler.sendMessage(handler.obtainMessage());
                    }
                })
                .setCancelable(true)
                .show();
        try {
            Looper.loop();
        } catch (RuntimeException e) {
            logger.debug("Exit showConfirmPaymentDialog");
        }
        return mConfirmPaymentDialogResultValue;
    }

    void showCommandResultDialog(String title, String message) {
        @SuppressLint("HandlerLeak") final Handler handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                throw new RuntimeException();
            }
        };

        if (mCommandResultDialog != null && mCommandResultDialog.isShowing()) {
            logger.error("Command result dialog is shown, should be some error!");
        }
        mCommandResultDialog = mConfirmPaymentDialogBuilder.setTitle(title)
                .setMessage(message)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        logger.debug("onOk()");
                        mCommandResultDialog.dismiss();
                        handler.sendMessage(handler.obtainMessage());
                    }
                })
                .setCancelable(false)
                .show();
        try {
            Looper.loop();
        } catch (RuntimeException e) {
            logger.debug("Exit showCommandResultDialog");
        }
    }

    private void hideConfirmPaymentDialog() {
        if (mConfirmPaymentDialog != null) {
            mConfirmPaymentDialog.dismiss();
        }
    }

    public void dialogBtcSent(Tx tx) {
        DialogSendBtcResult dialog = new DialogSendBtcResult();
        Bundle bundle = new Bundle();
        // result string id
        bundle.putInt("sendBtcResult", R.string.transaction_receipt);
        bundle.putString("from", tx.getFrom());
        bundle.putString("to", tx.getTo());
        bundle.putString("hash", tx.getHash());
        bundle.putDouble("amount", tx.getAmount());
        bundle.putDouble("fee", tx.getFee());
        bundle.putString("time", tx.getTime());
        bundle.putString("raw", tx.getRaw());

        dialog.setArguments(bundle);
        dialog.show(getSupportFragmentManager(), "dialog");
    }

    public void dialogSentBtcFailed(String reason) {
        DialogSendBtcResult dialog = new DialogSendBtcResult();
        Bundle bundle = new Bundle();
        // result string id
        bundle.putInt("sendBtcResult", R.string.transaction_not_executed);
        bundle.putString("failureReason", reason);
        /* Here we want to replace the reason from the web service to
         * out own strings. Such as "Not enough balance, transaction canceled"
         * or "Transaction cannot be completed at the moment, try again later"
         */
        //bundle.putString("failureReason", getString(R.string.try_later));
        dialog.setArguments(bundle);
        dialog.show(getSupportFragmentManager(), "dialog");
    }

    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (requestCode == MainActivity.REQUEST_CODE_QR_CODE) {
            if (resultCode == RESULT_OK) {
                qrScanText = intent.getStringExtra(KEY_QR_CODE);
                logger.debug("qr scan result: {}", qrScanText);
            }
        }
        else if (requestCode == MainActivity.REQUEST_CODE_CHOOSE_KEY) {
            if (intent == null) return;
            String keyPath = intent.getStringExtra(KEY_CHOOSE_KEY);
            if (keyPath != null && keyPath.length() > 0) {
                String[] strList = keyPath.split(",");
                if (strList.length == 5) {
                    logger.debug("Set Key Path: {}", keyPath);
                    OtkRequest request = otkRequestQueue.peek();

                    if (request != null && request.getCommand().equals(Command.SET_KEY.toString())) {
                        request.setData(keyPath);
                        dialogAuthByPin();
                    }
                }
            }
        }
    }

    boolean dialogConfirmOperationAndWaitResult(String title, String message, String positiveButtonString) {
        @SuppressLint("HandlerLeak") final Handler handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                throw new RuntimeException();
            }
        };

        if (mConfirmOpDialog != null && mConfirmOpDialog.isShowing()) {
            logger.debug("Confirm dialog is shown, should be some error!");
        }

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(MainActivity.this);
        alertDialogBuilder.setTitle(title);
        alertDialogBuilder.setMessage(message);
        alertDialogBuilder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                logger.debug("Cancel operation");
                mOperationConfirmed = false;
                handler.sendMessage(handler.obtainMessage());
                hideDialogConfirmOperationAndWaitResult();
            }
        });
        alertDialogBuilder.setPositiveButton(positiveButtonString, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                logger.debug("Continue operation");
                hideDialogConfirmOperationAndWaitResult();
                mOperationConfirmed = true;
                // Terminate current op
                mOtk.cancelOperation();
                mIsOpInProcessing = false;
                mWaitingAddressFromAddrEditor = false;
                clearCachedPayFragmentData();
                mOp = Otk.Operation.OTK_OP_NONE;
                if (mSelectedFragment instanceof FragmentOtk) {
                    ((FragmentOtk) mSelectedFragment).updateOperation(mOp);
                }
                handler.sendMessage(handler.obtainMessage());
            }
        });
        alertDialogBuilder.setCancelable(true);
        mConfirmOpDialog = alertDialogBuilder
                .show();
        try {
            Looper.loop();
        } catch (RuntimeException e) {
            logger.debug("Exit dialogConfirmOperationAndWaitResult");
        }
        return mOperationConfirmed;
    }

    private void hideDialogConfirmOperationAndWaitResult() {
        if (mConfirmOpDialog != null) {
            mConfirmOpDialog.dismiss();
        }
    }

//    /* Returning MainActivity running state for ActivitySplash to check */
//    static public boolean isRunning() {
//        return (mOtk != null);
//    }

    public String parseFailureReason(String desc) {
        if (desc == null || desc.equals("")) {
            return getString(R.string.unknown_reason);
        }
        switch (desc) {
            case "C0":
                return getString(R.string.session_timeout);
            case "C1":
                return getString(R.string.auth_failed);
            case "C3":
                return getString(R.string.invalid_params);
            case "C4":
                return getString(R.string.missing_params);
            case "C7":
                return getString(R.string.pin_unset);
            case "00":
            case "C2":
            case "FF":
                return getString(R.string.invalid_command);
            default:
                return desc;
        }
    }

    private String menuOptionToOtkCommand(int opt) {
        switch (opt) {
            case R.id.menu_openturnkey_unlock:
                return OtkCommand.CMD_UNLOCK;
            case R.id.menu_openturnkey_set_note:
                return OtkCommand.CMD_SETNOTE;
            case R.id.menu_openturnkey_set_pin:
                return OtkCommand.CMD_SETPIN;
            case R.id.menu_openturnkey_choose_key:
                return OtkCommand.CMD_SETKEY;
            case R.id.menu_openturnkey_sign_message:
                return OtkCommand.CMD_SIGN;
            case R.id.menu_openturnkey_get_key:
                return OtkCommand.CMD_GETKEY;
            case R.id.menu_openturnkey_export_wif_key:
                return OtkCommand.CMD_EXPORTWIF;
            case R.id.menu_openturnkey_reset:
                return OtkCommand.CMD_RESET;
            default:
                return null;
        }
    }
    
    public static void pushRequest(OtkRequest request) {
        clearRequest();
        otkRequestQueue.add(request);
        logger.debug("Push a request({}) of {} requests", request.getCommand(), otkRequestQueue.size());
    }
    
    public static OtkRequest pollRequest() {
        OtkRequest request = otkRequestQueue.poll();
        if (request != null) {
            logger.debug("Poll a request({}) of {} requests", request.getCommand(), otkRequestQueue.size());
        }
        return request;
    }
    
    public static OtkRequest peekRequest() {
        return otkRequestQueue.peek();
    }
    
    public static boolean hasRequest() {
        return otkRequestQueue.size() > 0;
    }

    public static Queue<OtkRequest> makeRequest(String command, String[] signatures,
                    String data, String pin, String options, boolean more, boolean masterKey) {
        Queue<OtkRequest> requests = new LinkedList<>();

        if (signatures.length > 0) {
            StringBuilder sigStr = new StringBuilder();
            int sigCount = 0;

            for (int i = 0; i < signatures.length; i++) {
                sigStr.append(signatures[i]);
                if (i + 1  < signatures.length) {
                    sigStr.append(",");
                }
                sigCount++;

                if (sigCount == 10) {
                    OtkRequest request = new OtkRequest(command);
                    request.setOption(sigStr.toString());

                    if (options.length() > 0)
                        request.setOption(options);

                    request.setPin(pin);

                    if (more) request.setMore();

                    if (masterKey) request.setPublicKey();

                    requests.add(request);

                    sigStr = new StringBuilder();
                    sigCount = 0;
                }
            }

            if (sigCount > 0) {
                OtkRequest request = new OtkRequest(command);
                request.setOption(sigStr.toString());

                if (options.length() > 0)
                    request.setOption(options);

                request.setPin(pin);

                if (more) request.setMore();

                if (masterKey) request.setPublicKey();

                requests.add(request);
            }
        }
        else {
            OtkRequest request = new OtkRequest(command);
            if (data.length() > 0)
                request.setData(data);

            if (options.length() > 0)
                request.setOption(options);

            request.setPin(pin);

            if (more) request.setMore();

            if (masterKey) request.setPublicKey();

            requests.add(request);
        }

        return requests;
    }

    public static void enableReadOtk() {
        mEnableReadOtk = true;
    }

    public static void disableReadOtk() {
        mEnableReadOtk = false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        logger.info("touched");
        switch(event.getAction())
        {
            case MotionEvent.ACTION_DOWN:
                x1 = event.getX();
                break;
            case MotionEvent.ACTION_UP:
                float x2 = event.getX();
                float deltaX = x2 - x1;
                if (Math.abs(deltaX) > MIN_DISTANCE)
                {
                    int leftItem = getNavIndex(bottomNav.getSelectedItemId()) - 1;
                    leftItem = leftItem < 0 ? 0 : leftItem;
                    int rightItem = getNavIndex(bottomNav.getSelectedItemId()) + 1;
                    rightItem = rightItem > navItems.length - 1 ? navItems.length - 1 : rightItem;
                    if (x2 > x1) {
                        navToFragment(navItems[leftItem]);
                    }
                    else {
                        navToFragment(navItems[rightItem]);
                    }
                }
                break;
        }
        return super.onTouchEvent(event);
    }

    private int getNavIndex(int resourceId) {
        for (int i = 0; i < navItems.length; i++) {
            if (navItems[i] == resourceId) return i;
        }
        return FRAGMENT_PAY;
    }

    public static String getPayToAddress() {
        return payToAddress;
    }

    public static void setPayToAddress(String payToAddress) {
        MainActivity.payToAddress = payToAddress;
    }

    public static String getQrScanText() {
        return qrScanText;
    }

    public static void setQrScanText(String qrScanText) {
        MainActivity.qrScanText = qrScanText;
    }

    public static boolean isAddressValid(String addr) {
        if (addr.contains(":")) {
            // contents might be a uri
            String[] strLists = addr.split(":");

            if (strLists.length > 1 && strLists[0].contentEquals("bitcoin")) {
                addr = strLists[1];
            }
        }

        if (BtcUtils.validateAddress(true, addr) || BtcUtils.validateAddress(false, addr)) {
            return true;
        }

        return false;
    }

    public static void readOtk() {
        DialogReadOtk dialogReadOtk = new DialogReadOtk();
        DialogReadOtk.dialogReadOtkListener listener = new DialogReadOtk.dialogReadOtkListener() {
            @Override
            public void onCancel() {
                pollRequest();
            }
        };

        dialogReadOtk.setOnCanelListener(listener);
        dialogReadOtk.show(fm, "ReadOtk");
    }
}

