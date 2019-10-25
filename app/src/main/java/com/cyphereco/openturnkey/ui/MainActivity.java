package com.cyphereco.openturnkey.ui;

import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NfcAdapter;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.os.Handler;
import android.os.Message;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.support.v7.widget.Toolbar;
import android.widget.TextView;
import android.widget.Toast;

import com.cyphereco.openturnkey.R;
import com.cyphereco.openturnkey.core.Configurations;
import com.cyphereco.openturnkey.core.Otk;
import com.cyphereco.openturnkey.core.OtkEvent;
import com.cyphereco.openturnkey.core.Tx;
import com.cyphereco.openturnkey.core.UnsignedTx;
import com.cyphereco.openturnkey.db.DBTransItem;
import com.cyphereco.openturnkey.db.OpenturnkeyDB;
import com.cyphereco.openturnkey.utils.BtcUtils;
import com.cyphereco.openturnkey.utils.CurrencyExchangeRate;
import com.cyphereco.openturnkey.utils.LocalCurrency;
import com.cyphereco.openturnkey.utils.Log4jHelper;

import org.slf4j.Logger;

public class MainActivity extends AppCompatActivity
        implements DialogLocalCurrency.DialogLocalCurrecyListener,
        DialogTransactionFee.DialogTransactionFeeListener,
        DialogAuthByPin.DialogAuthByPinListener,
        DialogAddNote.DialogAddNoteListener,
        DialogClearHistory.DialogClearHistoryListener,
        FragmentPay.FragmentPayListener,
        FragmentOtk.FragmentOtkListener,
        FragmentAddrbook.FragmentAddrbookListener {

    public static final String TAG = MainActivity.class.getSimpleName();
    Logger logger = Log4jHelper.getLogger(TAG);

    public static final int REQUEST_CODE_QR_CODE = 0;
    public static final int REQUEST_CODE_PRE_AUTH = 1;
    public static final int REQUEST_CODE_SET_PIN = 2;
    public static final int REQUEST_CODE_SET_KEY = 3;
    public static final int REQUEST_CODE_ADDRESS_EDIT = 4;
    public static final int REQUEST_CODE_TRANSACTION_INFO = 5;
    public static final String KEY_QR_CODE = "KEY_QR_CODE";
    public static final String KEY_PRE_AUTH_PIN_CODE = "KEY_PRE_AUTH_PIN_CODE";
    public static final String KEY_SET_PIN_CODE = "KEY_SET_PIN_CODE";
    public static final String KEY_SET_KEY = "KEY_SET_KEY";
    public static final String KEY_OTK_DATA = "KEY_OTK_DATA";
    public static final String KEY_ADDRESS_EDITOR_TEMP_ALIAS = "KEY_ADDRESS_EDITOR_TMEP_ALIAS";
    public static final String KEY_ADDRESS_EDITOR_TEMP_ADDR = "KEY_ADDRESS_EDITOR_TMEP_ADDR";
    public static final int REQUEST_RESULT_CODE_REPAY = 1000;
    public static final int REQUEST_RESULT_CODE_READ_NFC = 1001;

    private static final String BEGIN_BITCOIN_SIGNED_MESSAGE = "-----BEGIN BITCOIN SIGNED MESSAGE-----";
    private static final String AMOUNT_EQUAL_TO = "amount=";

    private Menu toolbarMenu = null;

    private double customTransactionFee = 0.00001;
    private boolean includeFee = false;
    private boolean useFixAddr = false;
    private NfcAdapter mNfcAdapter = null;
    static private Otk mOtk = null;
    private Fragment mSelectedFragment = null;
    private CurrencyExchangeRate mCurrencyExRate;
    AlertDialog.Builder mProgressDialogBuilder = null;
    AlertDialog mProgressDialog = null;
    AlertDialog mStatusDialog = null;
    AlertDialog.Builder mStatusDialogBuilder = null;
    AlertDialog.Builder mConfirmTerminateOpDialogBuilder = null;
    AlertDialog.Builder mConfirmPaymentDialogBuilder = null;
    AlertDialog mConfirmTerminateOpDialog = null;
    AlertDialog mConfirmPaymentDialog = null;
    AlertDialog.Builder mCommandResultDialogBuilder = null;
    AlertDialog mCommandResultDialog = null;
    private boolean mConfirmDialogResultValue;
    private boolean mConfirmPaymentDialogResultValue;

    private Otk.Operation mOp = Otk.Operation.OTK_OP_NONE;
    private boolean mIsOpInProcessing = false;
    // Cached pay fragment data
    private String mRecipientAddress = "";
    private String mBtcAmount = "";
    private String mLcAmount = "";
    private boolean mIsUseAllFundsChecked = false;

    private boolean mSwitchToOTKFragment = false;
    private boolean mWaitingAddressFromAddrEditor = false;
    private String mAddressEditorTempAlias = "";
    private String mAddressEditorTempAddress = "";
    private boolean mSwitchToAddressBookFragment = false;

    /**
     * Process activity result
     *
     * @param requestCode
     * @param resultCode
     * @param intent
     */
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        logger.info("onActivityResult:" + requestCode + " resultCode:" + resultCode);
        if (requestCode == MainActivity.REQUEST_CODE_QR_CODE) {
            if (resultCode == RESULT_OK) {
                // Handle successful scan
                String contents = intent.getStringExtra(KEY_QR_CODE);
                if (contents.contains(BEGIN_BITCOIN_SIGNED_MESSAGE)) {
                    // TODO
                    // updateFormattedSignedMessage(contents);
                } else {
                    String addr = "";
                    String amount = "0.0";
                    boolean notBTC = false;

                    if (contents.contains(":")) {
                        // contents might be a uri
                        String[] uriArray = contents.split(":");

                        if (uriArray.length > 1) {
                            if (uriArray[0].contentEquals("bitcoin")) {
                                contents = uriArray[1];
                                addr = contents;
                            } else {
                                notBTC = true;
                                Toast.makeText(this, uriArray[0] +
                                        getString(R.string.not_supported), Toast.LENGTH_LONG).show();
                                contents = "";
                            }
                        } else {
                            // incorrect uri format
                        }
                    }

                    if (!notBTC && contents.contains("?")) {
                        // contents might contains query tag
                        String[] queryArray = contents.split("\\?");

                        if (queryArray.length > 1) {
                            addr = queryArray[0];

                            String[] queryTagArray = queryArray[1].split("&");

                            for (String s : queryTagArray) {
                                if (s.toLowerCase().contains(AMOUNT_EQUAL_TO)) {
                                    String[] amountArray = s.split("=");
                                    if (amountArray.length > 1) {
                                        Toast.makeText(this, getString(R.string.subject_amount) + amountArray[1], Toast.LENGTH_LONG).show();
                                        amount = amountArray[1];
                                    }
                                }
                            }
                        } else {
                            // incorrect uri
                        }
                    } else {
                        addr = contents;
                    }

                    if (addr.length() > 0) {
                        if (mSelectedFragment instanceof FragmentPay) {
                            // Update local currency
                            ((FragmentPay) mSelectedFragment).updateRecipientAddress(addr);
                        }
                    }

                    if (Float.parseFloat(amount) > 0) {
                        if (mSelectedFragment instanceof FragmentPay) {
                            // Update local currency
                            ((FragmentPay) mSelectedFragment).updateAmount(amount);
                        }
                    }
                }
            } else if (resultCode == RESULT_CANCELED) {
                //Handle cancel
                Toast.makeText(this, getString(R.string.qr_code_scan_cancelled), Toast.LENGTH_LONG).show();
            }
        }
        else if (requestCode == REQUEST_CODE_ADDRESS_EDIT) {
            if (resultCode == RESULT_OK) {
                if (mSelectedFragment instanceof FragmentAddrbook) {
                    // Refresh address book
                    ((FragmentAddrbook) mSelectedFragment).refresh();
                }
                else {
                    // switch to address book
                    mSwitchToAddressBookFragment = true;
                }
            }
            else if (resultCode == RESULT_CANCELED) {
                mSwitchToAddressBookFragment = true;
            }
            else if (resultCode == REQUEST_RESULT_CODE_READ_NFC) {
                // Switch to OTK fragment
                mSwitchToOTKFragment = true;
                mWaitingAddressFromAddrEditor = true;
                mAddressEditorTempAlias = intent.getStringExtra(KEY_ADDRESS_EDITOR_TEMP_ALIAS);
                mAddressEditorTempAddress = intent.getStringExtra(KEY_ADDRESS_EDITOR_TEMP_ADDR);
            }
        }
        else if (requestCode == REQUEST_CODE_TRANSACTION_INFO) {
            if (resultCode == RESULT_OK) {
                if (mSelectedFragment instanceof FragmentHistory) {
                    ((FragmentHistory) mSelectedFragment).refresh();
                }
            }
            else if (resultCode == REQUEST_RESULT_CODE_REPAY) {
                mRecipientAddress = intent.getStringExtra("REPAY_ADDRESS");
                mBtcAmount = intent.getStringExtra("REPAY_AMOUNT");
                BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
                bottomNav.setSelectedItemId(R.id.nav_menu_pay);
            }
        }
    }

    /* declarations */
    private BottomNavigationView.OnNavigationItemSelectedListener navListener =
            new BottomNavigationView.OnNavigationItemSelectedListener() {
                @Override
                public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
                    BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
                    // mSelectedFragment null for the first show
                    if (bottomNav.getSelectedItemId() == menuItem.getItemId() && mSelectedFragment != null) {
                        return true;
                    }

                    if (mIsOpInProcessing == true) {
                        // Cache selected item
                        if (false == showConfirmDialogAndWaitResult(mOp, false)) {
                            logger.info("Confirmation cancelled!");
                            return false;
                        }
                        logger.info("Terminate Confirmed.");
                    }

                    Toolbar toolbar = findViewById(R.id.toolbar);
                    Menu menu = toolbar.getMenu();
                    menu.clear();

                    switch (menuItem.getItemId()) {
                        case R.id.nav_menu_history:
                            getSupportActionBar().setTitle(getString(R.string.history));
                            getMenuInflater().inflate(R.menu.menu_history, menu);
                            mSelectedFragment = new FragmentHistory();
                            clearCachedPayFragmentData();
                            mOp = Otk.Operation.OTK_OP_NONE;
                            break;
                        case R.id.nav_menu_addresses:
                            getSupportActionBar().setTitle(getString(R.string.addresses));
                            getMenuInflater().inflate(R.menu.menu_addresses, menu);
                            mSelectedFragment = new FragmentAddrbook();
                            clearCachedPayFragmentData();
                            mOp = Otk.Operation.OTK_OP_NONE;
                            break;
                        case R.id.nav_menu_openturnkey:
                            getSupportActionBar().setTitle(getString(R.string._openturnkey));
                            getMenuInflater().inflate(R.menu.menu_openturnkey, menu);
                            if (mOp == Otk.Operation.OTK_OP_NONE) {
                                mOp = Otk.Operation.OTK_OP_READ_GENERAL_INFO;
                            }
                            mSelectedFragment = FragmentOtk.newInstance(mOp);
                            mOtk.setOperation(mOp);
                            clearCachedPayFragmentData();
                            break;
                        default:
                            // Pay fragment
                            getSupportActionBar().setTitle(getString(R.string.pay));
                            getMenuInflater().inflate(R.menu.menu_pay, menu);
                            updatePayConfig(menu);
                            // Restore cached data
                            mSelectedFragment = FragmentPay.newInstance(mRecipientAddress, mBtcAmount, mLcAmount, mIsUseAllFundsChecked);
                            ((FragmentPay) mSelectedFragment).updateCurrencyExchangeRate(mCurrencyExRate);
                            mOp = Otk.Operation.OTK_OP_NONE;
                            break;
                    }
                    getSupportFragmentManager().beginTransaction().replace(
                            R.id.frame_main, mSelectedFragment).commit();
                    return true;
                }
            };

    /* class functions */
    static public boolean isRunning() {
        return (mOtk != null);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        logger.info("onCreate");
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setOnNavigationItemSelectedListener(navListener);
        bottomNav.setSelectedItemId(R.id.nav_menu_pay);

        /* init NFC. */
        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if (mNfcAdapter == null) {
            Toast.makeText(this, R.string.nfc_unavailable, Toast.LENGTH_LONG).show();
        }

        mProgressDialogBuilder = new AlertDialog.Builder(MainActivity.this);
        mStatusDialogBuilder = new AlertDialog.Builder(MainActivity.this);
        mConfirmTerminateOpDialogBuilder = new AlertDialog.Builder(MainActivity.this);
        mConfirmPaymentDialogBuilder = new AlertDialog.Builder(MainActivity.this);
        mCommandResultDialogBuilder = new AlertDialog.Builder(MainActivity.this);

        mOtk = Otk.getInstance(getApplicationContext());
        mOtk.setEventListener(new Otk.OtkEventListener() {
            @Override
            public void onOtkEvent(OtkEvent event) {
                OtkEvent.Type type = event.getType();
                logger.info("onOtkEvent:" + type.toString());
                // TODO: process event
                if (type == OtkEvent.Type.CURRENCY_EXCHANGE_RATE_UPDATE) {
                    // Cache rate
                    mCurrencyExRate = event.getCurrencyExRate();
                    if (mSelectedFragment instanceof FragmentPay) {
                        // Update rate
                        ((FragmentPay) mSelectedFragment).updateCurrencyExchangeRate(mCurrencyExRate);
                    }
                } else if (type == OtkEvent.Type.TX_FEE_UPDATE) {
                    // Store to preference
                    Preferences.setTxFee(getApplicationContext(), event.getTxFee());
                } else if (type == OtkEvent.Type.APPROACH_OTK) {
                    hideProgressDialog();
                    // Show dialog to indicate user not to remove OTK
                    showStatusDialog(getString(R.string.in_operation), getString(R.string.do_not_remove_otk));
                } else if (type == OtkEvent.Type.OPERATION_IN_PROCESSING) {
                    // Stop cancel timer
                    if (mSelectedFragment instanceof FragmentOtk) {
                        // Update rate
                        ((FragmentOtk) mSelectedFragment).stopCancelTimer();
                    }
                    // Hide status dialog
                    hideStatusDialog();
                    // Show progress spin circle
                    showProgressDialog(getString(R.string.processing));
                } else if (type == OtkEvent.Type.GENERAL_INFORMATION) {
                    Intent intent = new Intent(getApplicationContext() , OpenturnkeyInfoActivity.class);
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
                        // TODO: Go to history page and show the tx
                        goToHistoryFragment();
                        // Show tx
                        dialogBtcSent(tx);
                    }

                } else if ((type == OtkEvent.Type.SEND_BITCOIN_FAIL) ||
                        (type == OtkEvent.Type.COMMAND_EXECUTION_FAILED)) {
                    // Hide progress
                    hideProgressDialog();
                    // Show error in doalog
                    dialogSentBtcFailed(event.getFailureReason());

                    mOp = Otk.Operation.OTK_OP_NONE;
                    mIsOpInProcessing = false;
                    mOtk.cancelOperation();
                    // Make sure we are in FragmentOtk
                    if (mSelectedFragment instanceof FragmentOtk) {
                        // Go back to pay fragment
                        backToPayFragment();
                    }
                } else if (type == OtkEvent.Type.COMPLETE_PAYMENT_FAIL) {
                    // Hide progress
                    hideProgressDialog();
                    mOp = Otk.Operation.OTK_OP_NONE;
                    mIsOpInProcessing = false;
                    mOtk.cancelOperation();
                    // Make sure we are in FragmentOtk
                    if (mSelectedFragment instanceof FragmentOtk) {
                        // Add tx to db
                        addTxToDb(event.getTx());
                        // TODO: Go to history page and show the tx
                        goToHistoryFragment();
                        // Show error in doalog
                        dialogSentBtcFailed(event.getTx().getDesc());
                    }

                } else if (type == OtkEvent.Type.RECIPIENT_ADDRESS) {
                    // Make sure we are in FragmentOtk
                    if (mSelectedFragment instanceof FragmentOtk) {
                        // Go back to pay fragment
                        mOp = Otk.Operation.OTK_OP_NONE;
                        mIsOpInProcessing = false;
                        if (mWaitingAddressFromAddrEditor) {
                            // Back to address editor activity
                            backToAddressEditorActivity(mAddressEditorTempAlias,
                                    event.getRecipientAddress());
                        }
                        else {
                            mRecipientAddress = event.getRecipientAddress();
                            backToPayFragment();
                        }
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
                        if (true == showConfirmPaymentDialog(utx)) {
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
                    setNfcCommTypeText(R.id.menu_openturnkey_read_generalinformation);
                    mIsOpInProcessing = false;
                    mOtk.cancelOperation();
                    Intent intent = new Intent(getApplicationContext(), OpenturnkeyInfoActivity.class);
                    intent.putExtra(KEY_OTK_DATA, event.getData());
                    startActivity(intent);

                } else if (type == OtkEvent.Type.UNLOCK_SUCCESS) {
                    hideStatusDialog();
                    showCommandResultDialog(getString(R.string.unlock_success), getString(R.string.otk_is_unlocked));
                    setNfcCommTypeText(R.id.menu_openturnkey_read_generalinformation);
                    mOp = Otk.Operation.OTK_OP_NONE;
                    mIsOpInProcessing = false;
                    mOtk.cancelOperation();
                    Intent intent = new Intent(getApplicationContext(), OpenturnkeyInfoActivity.class);
                    intent.putExtra(KEY_OTK_DATA, event.getData());
                    startActivity(intent);
                } else if (type == OtkEvent.Type.UNLOCK_FAIL) {
                    hideStatusDialog();
                    mOp = Otk.Operation.OTK_OP_NONE;
                    setNfcCommTypeText(R.id.menu_openturnkey_read_generalinformation);
                    mIsOpInProcessing = false;
                    mOtk.cancelOperation();
                    showStatusDialog(getString(R.string.unlock_failed), "");
                }  else if (type == OtkEvent.Type.WRITE_NOTE_SUCCESS) {
                    hideStatusDialog();
                    showCommandResultDialog(getString(R.string.write_note_success), getString(R.string.note_is_wrote));
                    setNfcCommTypeText(R.id.menu_openturnkey_read_generalinformation);
                    mOp = Otk.Operation.OTK_OP_NONE;
                    mIsOpInProcessing = false;
                    mOtk.cancelOperation();
                    ((FragmentOtk) mSelectedFragment).hideCancelButton();
                    Intent intent = new Intent(getApplicationContext(), OpenturnkeyInfoActivity.class);
                    intent.putExtra(KEY_OTK_DATA, event.getData());
                    startActivity(intent);
                } else if (type == OtkEvent.Type.WRITE_NOTE_FAIL) {
                    hideStatusDialog();
                    mOp = Otk.Operation.OTK_OP_NONE;
                    setNfcCommTypeText(R.id.menu_openturnkey_read_generalinformation);
                    mIsOpInProcessing = false;
                    mOtk.cancelOperation();
                    ((FragmentOtk) mSelectedFragment).hideCancelButton();
                    showStatusDialog(getString(R.string.write_note_fail), "");
                }
                else {
                }
            }
        });
    }


    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        logger.info("onNewIntent");
        String action = intent.getAction();

        if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(action) ||
                NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action)) {
            int ret = mOtk.processIntent(intent, null);
            if (ret != Otk.OTK_RETURN_OK) {
                logger.info("process intent failed:" + ret);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        logger.info("onResume");
        IntentFilter tagDetected = new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED);
        IntentFilter ndefDetected = new IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED);
        IntentFilter techDetected = new IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED);
        IntentFilter[] nfcIntentFilter = new IntentFilter[]{techDetected, tagDetected, ndefDetected};

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);

        if (mNfcAdapter != null) {
            mNfcAdapter.enableForegroundDispatch(this, pendingIntent, nfcIntentFilter, null);
        }

        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        if (mSwitchToOTKFragment) {
            mSwitchToOTKFragment = false;
            // Cache op
            mOp = Otk.Operation.OTK_OP_GET_RECIPIENT_ADDRESS;
            bottomNav.setSelectedItemId(R.id.nav_menu_openturnkey);
            mIsOpInProcessing = true;
        }
        else if (mSwitchToAddressBookFragment) {
            mSwitchToAddressBookFragment = false;
            bottomNav.setSelectedItemId(R.id.nav_menu_addresses);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mNfcAdapter != null)
            mNfcAdapter.disableForegroundDispatch(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_pay, menu);
        updatePayConfig(menu);
        toolbarMenu = menu;

        return super.onCreateOptionsMenu(menu);
    }

    private void setNfcCommTypeText(int item) {
        TextView tv;
        tv = findViewById(R.id.text_nfc_comm_type);
        try {
            switch (item) {
                case R.id.menu_openturnkey_read_generalinformation:
                    tv.setText(R.string.read_general_information);
                    return;
                case R.id.menu_openturnkey_authenticity_check:
                    tv.setText(R.string.authenticity_check);
                    return;
                case R.id.menu_openturnkey_get_key:
                    tv.setText(R.string.read_key_information);
                    return;
                case R.id.menu_openturnkey_unlock:
                    tv.setText(R.string.unlock);
                    return;
                case R.id.menu_openturnkey_set_note:
                    tv.setText(R.string.write_memo);
                    return;
                case R.id.menu_openturnkey_choose_key:
                    tv.setText(R.string.set_key);
                    return;
                case R.id.menu_openturnkey_set_pin:
                    tv.setText(R.string.set_pin_code);
                    return;
                case R.id.menu_openturnkey_sign_message:
                    tv.setText(R.string.sign_message);
                    return;
            }
        } catch (NullPointerException e) {
            // Do nothing
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        TextView tv;

        if (mIsOpInProcessing) {
            if (false == showConfirmDialogAndWaitResult(mOp, true)) {
                return false;
            }
        }

        switch (item.getItemId()) {
            case R.id.menu_history_clear_history:
                dialogClearHistory();
                return true;
            case R.id.menu_pay_local_curreny:
                dialogLocalCurrency();
                return true;
            case R.id.menu_pay_transaction_fee:
                dialogTransactionFee();
                return true;
            case R.id.menu_pay_fee_included:
                includeFee = item.setChecked(!item.isChecked()).isChecked();
                return true;
            case R.id.menu_pay_use_fix_address:
                useFixAddr = item.setChecked(!item.isChecked()).isChecked();
                return true;
            case R.id.menu_pay_about:
                dialogAbout();
                return true;
            case R.id.menu_addresses_add:
                Intent intent = new Intent(this, ActivityAddressEditor.class);
                intent.putExtra(ActivityAddressEditor.KEY_EDITOR_TYPE,
                        ActivityAddressEditor.EDITOR_TYPE_ADD);
                startActivityForResult(intent, MainActivity.REQUEST_CODE_ADDRESS_EDIT);
                return true;
            case R.id.menu_openturnkey_unlock:
                setNfcCommTypeText(item.getItemId());
                mOp = Otk.Operation.OTK_OP_UNLOCK;
                mOtk.setOperation(mOp);
                return true;
            case R.id.menu_openturnkey_read_generalinformation:
                setNfcCommTypeText(item.getItemId());
                mOp = Otk.Operation.OTK_OP_NONE;
                mOtk.cancelOperation();
                return true;
            case R.id.menu_openturnkey_set_note:
                // show add note dialog
                dialogAddNote();
                return true;
            case R.id.menu_openturnkey_authenticity_check:
            case R.id.menu_openturnkey_get_key:
            case R.id.menu_openturnkey_choose_key:
            case R.id.menu_openturnkey_set_pin:
            case R.id.menu_openturnkey_sign_message:
                setNfcCommTypeText(item.getItemId());
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void setLocalCurrency(int localCurrency) {
        LocalCurrency lc = currencyToLocalCurrency(localCurrency);
        Preferences.setLocalCurrency(getApplicationContext(), lc);
        if (mSelectedFragment instanceof FragmentPay) {
            // Update local currency
            ((FragmentPay) mSelectedFragment).updateLocalCurrency(lc);
        }
        // Update currency in option menu
        updatePayConfig(toolbarMenu);
    }

    public void addNote(String note) {
        logger.info("note:" + note);
        mOp = Otk.Operation.OTK_OP_WRITE_NOTE;
        setNfcCommTypeText(R.id.menu_openturnkey_set_note);
        ((FragmentOtk) mSelectedFragment).showCancelButton();
        mOtk.setNote(note);
    }

    public void cancelAddNote() {
        mOtk.cancelOperation();
        mOp = Otk.Operation.OTK_OP_NONE;
        mIsOpInProcessing = false;
    }

    public void authByPin(String pin) {
        logger.info("pin:" + pin);
        mOtk.setPinForOperation(pin);
    }

    public void cancelAuthByPin() {
        mOtk.cancelOperation();
        mOp = Otk.Operation.OTK_OP_NONE;
        mIsOpInProcessing = false;
        backToPayFragment();
    }

    Configurations.TxFeeType toTxFee(int transactionFee) {
        switch (transactionFee) {
            case R.id.radio_custom:
                return Configurations.TxFeeType.CUSTOMIZED;
            case R.id.radio_high:
                return Configurations.TxFeeType.HIGH;
            case R.id.radio_mid:
                return Configurations.TxFeeType.MID;
            default:
                return Configurations.TxFeeType.LOW;
        }
    }

    public void setTransactionFee(int transactionFee) {
        // Store to preference
        Preferences.setTxFeeType(getApplicationContext(), toTxFee(transactionFee));
        updatePayConfig(toolbarMenu);
    }

    public void setCustomizedTxFee(double txFee) {
        // Store to preference
        logger.info("Customized fee:" + txFee);
        Preferences.setCustomizedTxFee(getApplicationContext(), BtcUtils.btcToSatoshi(txFee));
        updatePayConfig(toolbarMenu);
    }

    private void dialogAddNote() {
        DialogAddNote dialog = new DialogAddNote();
        dialog.show(getSupportFragmentManager(), "dialog");
    }

    /* Local functions */
    public void dialogLocalCurrency() {
        DialogLocalCurrency dialog = new DialogLocalCurrency();
        int localCurrencyId;

        LocalCurrency lc = Preferences.getLocalCurrency(getApplicationContext());
        if (lc == LocalCurrency.LOCAL_CURRENCY_CNY) {
            localCurrencyId = R.id.radio_cny;
        } else if (lc == LocalCurrency.LOCAL_CURRENCY_EUR) {
            localCurrencyId = R.id.radio_eur;
        } else if (lc == LocalCurrency.LOCAL_CURRENCY_JPY) {
            localCurrencyId = R.id.radio_jpy;
        } else if (lc == LocalCurrency.LOCAL_CURRENCY_USD) {
            localCurrencyId = R.id.radio_usd;
        } else {
            localCurrencyId = R.id.radio_twd;
        }
        Bundle bundle = new Bundle();
        bundle.putInt("localCurrency", localCurrencyId);
        dialog.setArguments(bundle);
        dialog.show(getSupportFragmentManager(), "dialog");
    }

    public void dialogTransactionFee() {
        DialogTransactionFee dialog = new DialogTransactionFee();

        Configurations.TxFeeType txFeeType = Preferences.getTxFeeType(getApplicationContext());
        int transactionFeeId;
        if (txFeeType == Configurations.TxFeeType.CUSTOMIZED) {
            transactionFeeId = R.id.radio_custom;
        }
        else if (txFeeType == Configurations.TxFeeType.HIGH) {
            transactionFeeId = R.id.radio_high;
        }
        else if (txFeeType == Configurations.TxFeeType.MID) {
            transactionFeeId = R.id.radio_mid;
        }
        else {
            transactionFeeId = R.id.radio_low;
        }

        Bundle bundle = new Bundle();
        bundle.putInt("transactionFee", transactionFeeId);
        bundle.putLong("customizedFee", Preferences.getCustomizedTxFee(getApplicationContext()));
        dialog.setArguments(bundle);
        dialog.show(getSupportFragmentManager(), "dialog");
    }

    public void dialogAuthByPin() {
        DialogAuthByPin dialog = new DialogAuthByPin();
        dialog.setCancelable(false);
        dialog.show(getSupportFragmentManager(), "dialog");
    }

    public void dialogAbout() {
        DialogAbout dialog = new DialogAbout();
        dialog.show(getSupportFragmentManager(), "dialog");
    }

    public void dialogSentBtcFailed(String reason) {
        DialogSendBtcResult dialog = new DialogSendBtcResult();
        Bundle bundle = new Bundle();
        // result string id
        bundle.putInt("sendBtcResult", R.string.send_btc_failed);
        bundle.putString("failureReason", reason);
        dialog.setArguments(bundle);
        dialog.show(getSupportFragmentManager(), "dialog");
    }

    public void dialogBtcSent(Tx tx) {
        DialogSendBtcResult dialog = new DialogSendBtcResult();
        Bundle bundle = new Bundle();
        // result string id
        bundle.putInt("sendBtcResult", R.string.transaction_sent);
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


    public void dialogClearHistory() {
        DialogClearHistory dialog = new DialogClearHistory();
        dialog.show(getSupportFragmentManager(), "dialog");
    }

    public LocalCurrency currencyToLocalCurrency(int currency) {
        switch (currency) {
            case R.id.radio_cny:
                return LocalCurrency.LOCAL_CURRENCY_CNY;
            case R.id.radio_eur:
                return LocalCurrency.LOCAL_CURRENCY_EUR;
            case R.id.radio_jpy:
                return LocalCurrency.LOCAL_CURRENCY_JPY;
            case R.id.radio_twd:
                return LocalCurrency.LOCAL_CURRENCY_TWD;
            default:
                return LocalCurrency.LOCAL_CURRENCY_USD;
        }
    }

    public String transactionFeeToString(Configurations.TxFeeType txFeeType) {
        if (txFeeType == Configurations.TxFeeType.CUSTOMIZED) {
            return getString(R.string.customized_fees);
        }
        if (txFeeType == Configurations.TxFeeType.HIGH) {
            return getString(R.string.high_fees);
        }
        if (txFeeType == Configurations.TxFeeType.MID) {
            return getString(R.string.mid_fees);
        }
        return getString(R.string.low_fees);
    }

    public void updatePayConfig(Menu menu) {
        String str;
        MenuItem menuItem;

        menuItem = menu.findItem(R.id.menu_pay_local_curreny);
        LocalCurrency lc = Preferences.getLocalCurrency(getApplicationContext());
        if (menuItem != null) {
            str = getString(R.string.local_currency) +
                    " (" + lc.toString() + ")";
            menuItem.setTitle(str);
        }

        menuItem = menu.findItem(R.id.menu_pay_transaction_fee);
        Configurations.TxFeeType txFeeType = Preferences.getTxFeeType(getApplicationContext());
        if (menuItem != null) {
            str = getString(R.string.transaction_fee) +
                    " (" + transactionFeeToString(txFeeType) + ")";
            menuItem.setTitle(str);
        }

        menuItem = menu.findItem(R.id.menu_pay_fee_included);
        if (menuItem != null) {
            menuItem.setChecked(includeFee);
        }

        menuItem = menu.findItem(R.id.menu_pay_use_fix_address);
        if (menuItem != null) {
            menuItem.setChecked(useFixAddr);
        }
    }

    public void onSignPaymentButtonClick(String to, double amount, String btcAmount, String lcAmount, boolean isAllFundsChecked) {
        mOp = Otk.Operation.OTK_OP_SIGN_PAYMENT;
        long txFees = BtcUtils.getTxFeeInSatoshi(getApplicationContext());

        mOtk.setOperation(Otk.Operation.OTK_OP_SIGN_PAYMENT, to, amount, includeFee, txFees);

        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setSelectedItemId(R.id.nav_menu_openturnkey);

        mIsOpInProcessing = true;

        // Cache recipient address and amount
        mRecipientAddress = to;
        mBtcAmount = btcAmount;
        mLcAmount = lcAmount;
        mIsUseAllFundsChecked = isAllFundsChecked;
    }

    public void onGetRecipientAddressByReadNfcButtonClick(String address, String btcAmount, String lcAmount, boolean isAllFundsChecked) {
        // Cache op
        mOp = Otk.Operation.OTK_OP_GET_RECIPIENT_ADDRESS;

        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setSelectedItemId(R.id.nav_menu_openturnkey);

        mIsOpInProcessing = true;

        // Cache recipient address and amount
        mRecipientAddress = address;
        mBtcAmount = btcAmount;
        mLcAmount = lcAmount;
        mIsUseAllFundsChecked = isAllFundsChecked;
    }

    public void backToPayFragment() {
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);

        bottomNav.setSelectedItemId(R.id.nav_menu_pay);
        // Update rate in case it's not updated
        if (mSelectedFragment instanceof FragmentPay) {
            ((FragmentPay) mSelectedFragment).updateCurrencyExchangeRate(mCurrencyExRate);
        }
    }

    public void goToHistoryFragment() {
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);

        bottomNav.setSelectedItemId(R.id.nav_menu_history);
    }

    public void onCancelButtonClick() {
        Toast.makeText(this, getString(R.string.operation_cancelled), Toast.LENGTH_LONG).show();
        if (mOp == Otk.Operation.OTK_OP_WRITE_NOTE) {
            setNfcCommTypeText(R.id.menu_openturnkey_read_generalinformation);
            ((FragmentOtk) mSelectedFragment).hideCancelButton();
            mOtk.cancelOperation();
            mOp = Otk.Operation.OTK_OP_NONE;
            mIsOpInProcessing = false;
            return;
        }

        // For sign payment
        hideConfirmPaymentDialog();
        hideProgressDialog();
        hideConfirmTerminateOpDialog();
        hideStatusDialog();
        mOtk.cancelOperation();
        mOp = Otk.Operation.OTK_OP_NONE;
        mIsOpInProcessing = false;
        if (mWaitingAddressFromAddrEditor) {
            backToAddressEditorActivity(mAddressEditorTempAlias, mAddressEditorTempAddress);
        }
        else {
            backToPayFragment();
        }
    }

    public void onCancelTimeout() {
        Toast.makeText(this, getString(R.string.operation_timeout), Toast.LENGTH_LONG).show();
        mOtk.cancelOperation();
        mOp = Otk.Operation.OTK_OP_NONE;
        mIsOpInProcessing = false;
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addCategory(Intent.CATEGORY_HOME);
        startActivity(intent);
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
                        logger.info("onCancel()");
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
        mStatusDialog = mStatusDialogBuilder.setTitle(title)
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

    boolean showConfirmDialogAndWaitResult(Otk.Operation op, final boolean isOtkOptionsSet) {
        final Handler handler = new Handler() {
            @Override
            public void handleMessage(Message mesg) {
                logger.info("msg:" + mesg.toString());
                throw new RuntimeException();
            }
        };

        if (mConfirmTerminateOpDialog != null && mConfirmTerminateOpDialog.isShowing()) {
            logger.info("Confirm dialog is shown, should be some error!");
        }

        mConfirmTerminateOpDialog = mConfirmTerminateOpDialogBuilder.setTitle(R.string.terminate_op)
                .setMessage(String.format(getString(R.string.confirm_terminate_op), op.toString()))
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        logger.info("onCancel()");
                        mConfirmDialogResultValue = false;
                        handler.sendMessage(handler.obtainMessage());
                        hideConfirmTerminateOpDialog();
                    }
                })
                .setPositiveButton(R.string.terminate, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        logger.info("onTerminate()");
                        hideConfirmTerminateOpDialog();
                        mConfirmDialogResultValue = true;
                        // Terminate current op
                        mOtk.cancelOperation();
                        mIsOpInProcessing = false;
                        mWaitingAddressFromAddrEditor = false;
                        clearCachedPayFragmentData();
                        if (isOtkOptionsSet) {
                            ((FragmentOtk) mSelectedFragment).hideCancelButton();
                        }
                        ((FragmentOtk) mSelectedFragment).stopCancelTimer();
                        mOp = Otk.Operation.OTK_OP_NONE;
                        handler.sendMessage(handler.obtainMessage());
                    }
                })
                .setCancelable(true)
                .show();
        try {
            Looper.loop();
        } catch (RuntimeException e) {
        }
        return mConfirmDialogResultValue;
    }

    private void hideConfirmTerminateOpDialog() {
        if (mConfirmTerminateOpDialog != null) {
            mConfirmTerminateOpDialog.dismiss();
        }
    }

    boolean showConfirmPaymentDialog(UnsignedTx utx) {
        final Handler handler = new Handler() {
            @Override
            public void handleMessage(Message mesg) {
                logger.info("msg:" + mesg.toString());
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

        String msg = getString(R.string.subject_sender) + utx.getFrom() + "\n" +
                getString(R.string.subject_recipient) + utx.getTo() + "\n" +
                getString(R.string.subject_amount) + String.format("%.8f", utx.getAmount()) + "\n" +
                getString(R.string.subject_fees_in_satoshi) +  utx.getFee() + "\n" +
                getString(R.string.subject_text_estimated_time) + BtcUtils.getEstimatedTime(getApplicationContext(), BtcUtils.btcToSatoshi(utx.getFee()));
        mConfirmPaymentDialog = mConfirmPaymentDialogBuilder.setTitle(R.string.confirm_payment)
                .setMessage(msg)
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        logger.info("onCancel()");
                        mConfirmPaymentDialogResultValue = false;
                        handler.sendMessage(handler.obtainMessage());
                        onCancelButtonClick();
                    }
                })
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        logger.info("onOk()");
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
        }
        return mConfirmPaymentDialogResultValue;
    }

    boolean showCommandResultDialog(String title, String message) {
        final Handler handler = new Handler() {
            @Override
            public void handleMessage(Message mesg) {
                logger.info("msg:" + mesg.toString());
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
                        logger.info("onOk()");
                        mCommandResultDialog.dismiss();
                        handler.sendMessage(handler.obtainMessage());
                    }
                })
                .setCancelable(false)
                .show();
        try {
            Looper.loop();
        } catch (RuntimeException e) {
        }
        return true;
    }

    private void hideConfirmPaymentDialog() {
        if (mConfirmPaymentDialog != null) {
            mConfirmPaymentDialog.dismiss();
        }
    }

    private void clearCachedPayFragmentData() {
        mRecipientAddress = "";
        mBtcAmount = "";
        mLcAmount = "";
        mIsUseAllFundsChecked = false;
    }

    @Override
    public void onAddressbookPayingButtonClick(String address) {
        mRecipientAddress = address;
        backToPayFragment();
    }

    private void addTxToDb(Tx tx) {
        if (tx == null) {
            logger.error("addTxToDb(): tx is null");
            return;
        }
        logger.debug("addTxToDb() tx:\n{}", tx.toString());
        // Add transaction to database.
        DBTransItem dbTrans = new DBTransItem(0,
                BtcUtils.convertDateTimeStringToLong(tx.getTime()),
                tx.getHash(), tx.getFrom(), tx.getTo(), tx.getAmount(), tx.getFee(),
                tx.getStatus().toInt(), tx.getDesc(), tx.getRaw());
        OpenturnkeyDB otkDB = new OpenturnkeyDB(getApplicationContext());
        otkDB.addTransaction(dbTrans);
        logger.debug("DB tx count:{}", otkDB.getTransactionCount());
    }

    @Override
    public void onClearHistorySuccess() {
        // Reload history fragment
        if (mSelectedFragment instanceof FragmentHistory) {
            ((FragmentHistory) mSelectedFragment).refresh();
        }
    }

    private void backToAddressEditorActivity(String alias, String address) {
        mWaitingAddressFromAddrEditor = false;

        Intent intent = new Intent(getApplicationContext(), ActivityAddressEditor.class);

        intent.putExtra(ActivityAddressEditor.KEY_EDITOR_TYPE,
                ActivityAddressEditor.EDITOR_TYPE_EDIT);
        intent.putExtra(ActivityAddressEditor.KEY_EDITOR_CONTACT_ALIAS, alias);
        intent.putExtra(ActivityAddressEditor.KEY_EDITOR_CONTACT_ADDR, address);
        startActivityForResult(intent, MainActivity.REQUEST_CODE_ADDRESS_EDIT);
    }
}

