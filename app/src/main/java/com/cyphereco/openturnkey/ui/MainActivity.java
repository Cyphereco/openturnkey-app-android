package com.cyphereco.openturnkey.ui;

import android.app.AlertDialog;
import android.content.Intent;
import android.nfc.NfcAdapter;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.support.v7.widget.Toolbar;
import android.view.inputmethod.InputMethodManager;
import android.widget.SearchView;

import com.cyphereco.openturnkey.R;
import com.cyphereco.openturnkey.core.Configurations;
import com.cyphereco.openturnkey.core.Tx;
import com.cyphereco.openturnkey.db.DBTransItem;
import com.cyphereco.openturnkey.db.OpenturnkeyDB;
import com.cyphereco.openturnkey.utils.AlertPrompt;
import com.cyphereco.openturnkey.utils.BtcUtils;
import com.cyphereco.openturnkey.utils.ExchangeRate;
import com.cyphereco.openturnkey.utils.Log4jHelper;
import com.cyphereco.openturnkey.utils.TxFee;

import org.slf4j.Logger;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Objects;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {

    public static final String TAG = MainActivity.class.getSimpleName();
    static Logger logger = Log4jHelper.getLogger(TAG);

    public static Preferences pref;

    private static String payToAddress = "";
    private static String qrScanText = "";

    public static final int REQUEST_CODE_QR_CODE = 0;
    public static final int REQUEST_CODE_CHOOSE_KEY = 3;
    public static final int REQUEST_CODE_TRANSACTION_INFO = 5;
    public static final String KEY_QR_CODE = "KEY_QR_CODE";
    public static final String KEY_OTK_DATA = "KEY_OTK_DATA";
    public static final int REQUEST_RESULT_CODE_REPAY = 1000;
    
    AlertDialog.Builder mProgressDialogBuilder = null;
    AlertDialog.Builder mConfirmTerminateOpDialogBuilder = null;
    AlertDialog.Builder mConfirmPaymentDialogBuilder = null;
    AlertDialog.Builder mCommandResultDialogBuilder = null;

    private static ExchangeRate exchangeRate;
    private static TxFee txFee;

    public static final int FRAGMENT_PAY = R.id.nav_menu_pay;
    public static final int FRAGMENT_OTK = R.id.nav_menu_openturnkey;
    public static final int FRAGMENT_HISTORY = R.id.nav_menu_history;
    public static final int FRAGMENT_ADDRBOOK = R.id.nav_menu_addresses;

    private BottomNavigationView bottomNav;

    private int[] navItems = {
            FRAGMENT_PAY,
            FRAGMENT_OTK,
            FRAGMENT_HISTORY,
            FRAGMENT_ADDRBOOK
    };

    private static boolean mEnableReadOtk = false;
    private static FragmentExtendOtkViewPage selectedFragment = null;
    private static String currentActivity = "";

    private static OnlineDataUpdateListener onlineDataUpdateListner;

    public static void setCurrentActivity(String activityName) {
        currentActivity = activityName;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // if system configuration changed, i.e. language changed,
        // finish the activity and start from the splash for a complete UI refreshment
        if (selectedFragment != null) {
            selectedFragment = null;
            startActivity(new Intent(this, ActivitySplash.class));
            finish();
        }

        // Initialize Database
        OpenturnkeyDB.init(getApplicationContext());

        updateXchangeRateAndFeesTask();

        Tx tx = new Tx(Tx.Status.STATUS_SUCCESS, "5dc7bee70b2d4d486d2e9ca997354e6909769049b2d971dc4034e2c03df909c7", "");
        tx.setFrom("1QEma6prBJscNqw7s3t8EGFcx3zF7mzWab");
        tx.setTo("1QEma6prBJscNqw7s3t8EGFcx3zF7mzWab");
        tx.setAmount(0.00191967);
        tx.setTime("2020-01-11T23:00:09.584902078Z");
        tx.setRaw("01000000030781f8fa7f6a30621c29ac47a2d5bb81bef88973839680f8f5de0d879c6417f9000000006a47304402204688a19b3ebe5bb05ff3fa05177f6f4889016c29a12ee9abdf325c5e1f32fe1e02205be7f1afa0df30c8165e4de6743f1b034d9de2cf7571f9500d7ce81c2bd9a55d01210323c012252f1f00996c6f05b074b99f516c1a9a0c8966cb645f9a01a11b9fc229fffffffff585248874c9b15176863d579379a5cc2c01453926395973da9264022ec3ed39010000006b483045022100baa9783aedc0b9860e0f486c09bdfbad92f71a5d383019cfcac35ea8ed59f282022030bde9f23990d434e1544e5e1bf227c66a0a2c1aed39e55bb8129385f127e70501210323c012252f1f00996c6f05b074b99f516c1a9a0c8966cb645f9a01a11b9fc229fffffffff585248874c9b15176863d579379a5cc2c01453926395973da9264022ec3ed39000000006a47304402201d6aa95358825ef7319c4a5f1ee89c864c3b71b45926610a9fc95574ca39bff20220459a21b1321687237bc9f37a17df6a0290a0920979882108e816cbd246ac679f01210323c012252f1f00996c6f05b074b99f516c1a9a0c8966cb645f9a01a11b9fc229ffffffff01dfed0200000000001976a914fee5819b32e8618699ad07a17b3df5a77346261788ac00000000");
        addTxToDb(tx);

        pref = new Preferences(getApplication());

        setContentView(R.layout.activity_main);

        final ViewPager viewPager = findViewById(R.id.view_pager_main);
        final MainPagerAdapter pagerAdapter = new MainPagerAdapter(getSupportFragmentManager());
        viewPager.setAdapter(pagerAdapter);

        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int i, float v, int i1) {

            }

            @Override
            public void onPageSelected(int i) {
                bottomNav.setSelectedItemId(navItems[i]);
                if (selectedFragment == null) {
                    setSelectedFragment((FragmentExtendOtkViewPage) pagerAdapter.getItem(i));
                } else {
                    selectedFragment.onPageUnselected();
                    setSelectedFragment((FragmentExtendOtkViewPage) pagerAdapter.getItem(i));
                }
                selectedFragment.onPageSelected();
            }

            @Override
            public void onPageScrollStateChanged(int i) {

            }
        });

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setTitle(getString(R.string.pay));

        bottomNav = findViewById(R.id.bottom_navigation);

        /*
         Declaration for bottom navigation bar.
         */
        BottomNavigationView.OnNavigationItemSelectedListener navListener =
                new BottomNavigationView.OnNavigationItemSelectedListener() {
                    @Override
                    public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
                        int page;
                        switch (menuItem.getItemId()) {
                            case FRAGMENT_ADDRBOOK:
                                Objects.requireNonNull(getSupportActionBar()).setTitle(getString(R.string.addresses));
                                page = MainPagerAdapter.PAGE_ADDRBOOK;
                                break;
                            case FRAGMENT_HISTORY:
                                Objects.requireNonNull(getSupportActionBar()).setTitle(getString(R.string.history));
                                page = MainPagerAdapter.PAGE_HISTORY;
                                break;
                            case FRAGMENT_OTK:
                                Objects.requireNonNull(getSupportActionBar()).setTitle(getString(R.string._openturnkey));
                                page = MainPagerAdapter.PAGE_OTK;
                                break;
                            default:
                                Objects.requireNonNull(getSupportActionBar()).setTitle(getString(R.string.pay));
                                page = MainPagerAdapter.PAGE_PAY;
                                break;
                        }

                        // navigate to page if triggered by navigation bar,
                        // call viewPager to scroll the page, otherwise (triggered by scroll page)
                        // do nothing
                        if (navItems[viewPager.getCurrentItem()] != menuItem.getItemId()) {
                            viewPager.setCurrentItem(page, true);
                        }

                        // if soft-keyboard is opened, close it!
                        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                        if (imm.isAcceptingText()) { // verify if the soft keyboard is open
                            imm.hideSoftInputFromWindow(Objects.requireNonNull(getCurrentFocus()).getWindowToken(), 0);
                        }

                        return true;
                    }
                };

        bottomNav.setOnNavigationItemSelectedListener(navListener);

        /* init NFC. */
        NfcAdapter mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if (mNfcAdapter == null) {
            AlertPrompt.alert(this, getString(R.string.nfc_unavailable));
        }

        mProgressDialogBuilder = new AlertDialog.Builder(MainActivity.this);
        mConfirmTerminateOpDialogBuilder = new AlertDialog.Builder(MainActivity.this);
        mConfirmPaymentDialogBuilder = new AlertDialog.Builder(MainActivity.this);
        mCommandResultDialogBuilder = new AlertDialog.Builder(MainActivity.this);

//        mOtk = Otk.getInstance(getApplicationContext());
//        mOtk.setEventListener(new Otk.OtkEventListener() {
//            @Override
//            public void onOtkEvent(OtkEvent event) {
//                OtkEvent.Type type = event.getType();
//                logger.debug("onOtkEvent:" + type.toString());
//                /* TODO: process event */
//                 if (type == OtkEvent.Type.TX_FEE_UPDATE) {
//                    // Store to preference
//                    Preferences.setTxFee(event.getTxFee());
//                } else if (type == OtkEvent.Type.APPROACH_OTK) {
//                    hideProgressDialog();
//                } else if (type == OtkEvent.Type.FIND_UTXO || type == OtkEvent.Type.CHECKING_BALANCE_FOR_PAYMENT) {
//                    // Hide status dialog
//                    hideStatusDialog();
//                    // Show progress spin circle
//                    showProgressDialog(getString(R.string.check_balance));
//                } else if (type == OtkEvent.Type.OPERATION_IN_PROCESSING) {
//                    // Hide status dialog
//                    hideStatusDialog();
//                    // Show progress spin circle
//                    showProgressDialog(getString(R.string.processing));
//                } else if (type == OtkEvent.Type.GENERAL_INFORMATION) {
//                    Intent intent = new Intent(getApplicationContext(), ActivityOpenturnkeyInfo.class);
//                    intent.putExtra(KEY_OTK_DATA, event.getData());
//                    startActivity(intent);
//                } else if (type == OtkEvent.Type.SEND_BITCOIN_SUCCESS) {
//                    hideProgressDialog();
//                    Tx tx = event.getTx();
//                    // Add transaction to database.
//                    addTxToDb(tx);
//                    // Make sure we are in FragmentOtk
//                    if (selectedFragment instanceof FragmentOtk) {
//                        // Go back to pay fragment
//                        mOp = Otk.Operation.OTK_OP_NONE;
//                        /* TODO: Go to history page and show the tx */
//                        // Show tx
//                        dialogBtcSent(tx);
//                    }
//
//                } else if ((type == OtkEvent.Type.SEND_BITCOIN_FAIL) ||
//                        (type == OtkEvent.Type.COMMAND_EXECUTION_FAILED) ||
//                        (type == OtkEvent.Type.SESSION_ID_MISMATCH)) {
//                    // Hide progress
//                    hideProgressDialog();
//                    // Show error in dialog
//                    String s;
//                    if (type == OtkEvent.Type.SESSION_ID_MISMATCH) {
//                        s = getString(R.string.communication_error);
//                    } else {
//                        s = getString(R.string.try_later) + "\n\n" +
//                                "{" + event.getFailureReason() + "}";
//                    }
//
//                    dialogSentBtcFailed(parseFailureReason(s));
//
//                    mOp = Otk.Operation.OTK_OP_NONE;
//                    mOtk.cancelOperation();
//                    // Make sure we are in FragmentOtk
//                    if (selectedFragment instanceof FragmentOtk) {
//                        // Go back to pay fragment
//                    }
//                } else if (type == OtkEvent.Type.COMPLETE_PAYMENT_FAIL) {
//                    // Hide progress
//                    hideProgressDialog();
//                    mOp = Otk.Operation.OTK_OP_NONE;
//                    mOtk.cancelOperation();
//                    // Make sure we are in FragmentOtk
//                    if (selectedFragment instanceof FragmentOtk) {
//                        /* TODO: Go to history page and show the tx */
//                        // Show error in dialog
//                        String reason;
//                        if (event.getTx() != null) {
//                            // Add tx to db
//                            addTxToDb(event.getTx());
//                            reason = event.getTx().getDesc();
//                        } else {
//                            reason = parseFailureReason(event.getFailureReason());
//                        }
//                        String s = getString(R.string.try_later) + "\n\n" +
//                                "{" + reason + "}";
//                        dialogSentBtcFailed(s);
//                    }
//
//                } else if (type == OtkEvent.Type.OTK_UNAUTHORIZED) {
//                    // Dismiss progress dialog
//                    hideProgressDialog();
//                    // Show pre-auth with pin dialog
//                } else if (type == OtkEvent.Type.UNSIGNED_TX) {
//                    if (mOp != Otk.Operation.OTK_OP_SIGN_PAYMENT) {
//                        logger.error("Got unsigned tx but sign payment is already terminated");
//                    } else {
//                        // Show dialog for user to confirm the payment
//                        UnsignedTx utx = event.getUnsignedTx();
//                        if (showConfirmPaymentDialog(utx)) {
//                            // Confirm payment
//                            mOtk.confirmPayment();
//                        } else {
//                            // Cancel payment
//                            onCancelButtonClick();
//                        }
//                    }
//                } else if (type == OtkEvent.Type.OTK_IS_NOT_LOCKED) {
//                    hideStatusDialog();
//                    showCommandResultDialog(getString(R.string.unlock_failed), getString(R.string.otk_is_not_locked));
//                    mOp = Otk.Operation.OTK_OP_NONE;
//                    mOtk.cancelOperation();
//                    Intent intent = new Intent(getApplicationContext(), ActivityOpenturnkeyInfo.class);
//                    intent.putExtra(KEY_OTK_DATA, event.getData());
//                    startActivity(intent);
//
//                } else if (type == OtkEvent.Type.UNLOCK_SUCCESS) {
//                    hideStatusDialog();
//                    showCommandResultDialog(getString(R.string.unlock_success), getString(R.string.otk_is_unlocked));
//                    mOp = Otk.Operation.OTK_OP_NONE;
//                    mOtk.cancelOperation();
//                    Intent intent = new Intent(getApplicationContext(), ActivityOpenturnkeyInfo.class);
//                    intent.putExtra(KEY_OTK_DATA, event.getData());
//                    startActivity(intent);
//                } else if (type == OtkEvent.Type.UNLOCK_FAIL) {
//                    hideStatusDialog();
//                    mOp = Otk.Operation.OTK_OP_NONE;
//                    mOtk.cancelOperation();
//                    showStatusDialog(getString(R.string.unlock_failed), parseFailureReason(event.getFailureReason()));
//                } else if (type == OtkEvent.Type.WRITE_NOTE_SUCCESS) {
//                    hideStatusDialog();
//                    showCommandResultDialog(getString(R.string.write_note), getString(R.string.write_note_success));
//                    mOp = Otk.Operation.OTK_OP_NONE;
//                    mOtk.cancelOperation();
//                    Intent intent = new Intent(getApplicationContext(), ActivityOpenturnkeyInfo.class);
//                    intent.putExtra(KEY_OTK_DATA, event.getData());
//                    startActivity(intent);
//                } else if (type == OtkEvent.Type.WRITE_NOTE_FAIL) {
//                    hideStatusDialog();
//                    mOp = Otk.Operation.OTK_OP_NONE;
//                    mOtk.cancelOperation();
//                    showStatusDialog(getString(R.string.write_note_fail), parseFailureReason(event.getFailureReason()));
//                } else if (type == OtkEvent.Type.SET_PIN_SUCCESS) {
//                    hideStatusDialog();
//                    showCommandResultDialog(getString(R.string.set_pin_code), getString(R.string.set_pin_success));
//                    mOp = Otk.Operation.OTK_OP_NONE;
//                    mOtk.cancelOperation();
//                    Intent intent = new Intent(getApplicationContext(), ActivityOpenturnkeyInfo.class);
//                    intent.putExtra(KEY_OTK_DATA, event.getData());
//                    startActivity(intent);
//                } else if (type == OtkEvent.Type.SET_PIN_FAIL) {
//                    hideStatusDialog();
//                    mOp = Otk.Operation.OTK_OP_NONE;
//                    mOtk.cancelOperation();
//                    showStatusDialog(getString(R.string.set_pin_fail), parseFailureReason(event.getFailureReason()));
//                } else if (type == OtkEvent.Type.CHOOSE_KEY_SUCCESS) {
//                    hideStatusDialog();
//                    showCommandResultDialog(getString(R.string.choose_key), getString(R.string.choose_key_success));
//                    mOp = Otk.Operation.OTK_OP_READ_GENERAL_INFO;
//                    mOtk.cancelOperation();
//                } else if (type == OtkEvent.Type.CHOOSE_KEY_FAIL) {
//                    hideStatusDialog();
//                    mOtk.cancelOperation();
//                    mOp = Otk.Operation.OTK_OP_READ_GENERAL_INFO;
//                    showStatusDialog(getString(R.string.choose_key_fail), parseFailureReason(event.getFailureReason()));
//                } else if (type == OtkEvent.Type.SIGN_MESSAGE_SUCCESS) {
//                    hideStatusDialog();
//                    mOp = Otk.Operation.OTK_OP_READ_GENERAL_INFO;
//                    mOtk.cancelOperation();
//                    Intent intent = new Intent(getApplicationContext(), ActivitySignValidateMessage.class);
//                    intent.putExtra(KEY_OTK_DATA, event.getData());
//                    intent.putExtra(KEY_MESSAGE_TO_SIGN, event.getMessageToSign());
//                    intent.putExtra(KEY_USING_MASTER_KEY, event.getUsingMasterKey());
//                    startActivity(intent);
////                    startActivityForResult(intent, MainActivity.REQUEST_CODE_SIGN_MESSAGE);
//                } else if (type == OtkEvent.Type.SIGN_MESSAGE_FAIL) {
//                    hideStatusDialog();
//                    mOtk.cancelOperation();
//                    mOp = Otk.Operation.OTK_OP_READ_GENERAL_INFO;
//                    showStatusDialog(getString(R.string.sign_message_fail), parseFailureReason(event.getFailureReason()));
//                } else if (type == OtkEvent.Type.OTK_PIN_UNSET) {
//                    /* Clear current OTK op */
//                    mOtk.cancelOperation();
//                    /* Go to set PIN page */
//                    mOp = Otk.Operation.OTK_OP_READ_GENERAL_INFO;
//                    showStatusDialog(getString(R.string.pin_unset), getString(R.string.pin_unset_msg));
//                } else if ((type == OtkEvent.Type.SESSION_TIMED_OUT) ||
//                        (type == OtkEvent.Type.READ_RESPONSE_TIMED_OUT)) {
//                    // Dismiss dialogs
//                    hideProgressDialog();
//                    hideDialogConfirmOperationAndWaitResult();
//                    hideConfirmPaymentDialog();
//                    hideStatusDialog();
//                    /* TODO update error description */
//                    if (type == OtkEvent.Type.SESSION_TIMED_OUT) {
//                        showStatusDialog(getString(R.string.operation_timeout), getString(R.string.session_timeout));
//                    } else {
//                        showStatusDialog(getString(R.string.operation_timeout), getString(R.string.read_response_timeout));
//                    }
//                    /* Clear current OTK op */
//                    mOtk.cancelOperation();
//                    /* Go to set PIN page */
//                    mOp = Otk.Operation.OTK_OP_READ_GENERAL_INFO;
//                } else {
//                    logger.debug("Unhandled event:{}", type.name());
//                }
//            }
//        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        logger.debug("destroyed");
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        logger.debug("new intent");
        /*
         For the incoming intents, we only care about NFC event, (at the moment)
         specifically, NDEF message event, which is used by OpenTurnKey. We will
         ignore other intents since this app is designed to deal with OpenTurnKey only.
         */

        /*
         All the NFC new intent will be post here as the entry point, then dispatch
         to its activiy or fragment.

         Note:
            The foreground dispatch system does not work as said in the manual.
            None of the activities except the registered one  will receive NFC intent.
            So we make our own customized dispatch implementation instead.
         */


        if (getClass().getName().equals(currentActivity)) {
            logger.debug("MainActivity is current");
            if (mEnableReadOtk && selectedFragment != null) {
                // MainAcitivy is the current activty, dispatch intent to current fragment.
                logger.debug("dispatch intent to fragment");
                selectedFragment.onNewIntent(intent);
            }
        } else {
            logger.debug("MainActivity is not current");
            // MainActivity is not the current activity, dispatch intent to the current activity.
            Class cls = null;

            try {
                cls = Class.forName(currentActivity);
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }

            if (cls != null) {
                logger.debug("dispatch intent to current activity class");
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
        logger.debug("resumed");

        setCurrentActivity(getClass().getName());

        if (selectedFragment != null) {
            selectedFragment.onPageSelected();
        }
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
        OpenturnkeyDB.addTransaction(dbTrans);
        logger.info("DB tx count:{}", OpenturnkeyDB.getTransactionCount());
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
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (getClass().getName().equals(currentActivity) && selectedFragment != null) {
            selectedFragment.onActivityResult(requestCode, resultCode, data);
        }
    }
    
    public static void enableReadOtk() {
        mEnableReadOtk = true;
    }

    public static void disableReadOtk() {
        mEnableReadOtk = false;
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

        return BtcUtils.validateAddress(true, addr) || BtcUtils.validateAddress(false, addr);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            SearchView v = findViewById(R.id.menu_addresses_search);
            if (v != null && v.hasFocus()) {
                v.clearFocus();
                v.onActionViewCollapsed();
                return false;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    private void updateXchangeRateAndFeesTask() {
        final Timer t = new Timer();
        t.schedule(new TimerTask() {
            @Override
            public void run() {
                ExchangeRate r = BtcUtils.getCurrencyExchangeRate();
                if (r == null) {
                    t.cancel();
                    t.purge();
                    updateXchangeRateAndFeesTask();
                } else {
                    setExchangeRate(r);
                    setTxFee(BtcUtils.getTxFee());
                }
            }
        }, 1000, 1000 * 60 * Configurations.INTERVAL_EXCHANGE_RATE_UPDATE);
    }

    public static ExchangeRate getExchangeRate() {
        return exchangeRate;
    }

    public void setExchangeRate(ExchangeRate exchangeRate) {
        logger.debug("Update ExchangeRate: {}", exchangeRate.toString());
        if (MainActivity.exchangeRate == null) MainActivity.exchangeRate = exchangeRate;
        if (onlineDataUpdateListner != null)
            onlineDataUpdateListner.onExchangeRateUpdated(exchangeRate);
    }

    public static TxFee getTxFee() {
        return txFee;
    }

    public void setTxFee(TxFee txFee) {
        logger.debug("Update TxFee: {}", txFee.toString());
        if (MainActivity.txFee != null) MainActivity.txFee = txFee;
        if (onlineDataUpdateListner != null) onlineDataUpdateListner.onTxFeeUpdated(txFee);
    }

    public static FragmentExtendOtkViewPage getSelectedFragment() {
        return selectedFragment;
    }

    public static void setSelectedFragment(FragmentExtendOtkViewPage selectedFragment) {
        MainActivity.selectedFragment = selectedFragment;
    }

    public static void setOnlineDataUpdateListner(OnlineDataUpdateListener listner) {
        MainActivity.onlineDataUpdateListner = listner;
    }

    public interface OnlineDataUpdateListener {
        void onExchangeRateUpdated(ExchangeRate xrate);
        void onTxFeeUpdated(TxFee txFee);
    }
}

