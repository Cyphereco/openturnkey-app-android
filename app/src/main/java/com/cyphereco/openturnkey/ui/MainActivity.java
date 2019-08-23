package com.cyphereco.openturnkey.ui;

import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NfcAdapter;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.cyphereco.openturnkey.R;
import com.cyphereco.openturnkey.core.Otk;
import com.cyphereco.openturnkey.core.OtkEvent;
import com.cyphereco.openturnkey.core.Tx;
import com.cyphereco.openturnkey.core.protocol.OtkState;
import com.cyphereco.openturnkey.utils.CurrencyExchangeRate;
import com.cyphereco.openturnkey.utils.LocalCurrency;

public class MainActivity extends AppCompatActivity
        implements DialogLocalCurrency.DialogLocalCurrecyListener,
        DialogTransactionFee.DialogTransactionFeeListener,
        DialogAuthByPin.DialogAuthByPinListener,
        FragmentPay.FragmentPayListener,
        FragmentOtk.FragmentOtkListener {

    public static final String TAG = MainActivity.class.getSimpleName();

    public static final int REQUEST_CODE_QR_CODE = 0;
    public static final int REQUEST_CODE_PRE_AUTH = 1;
    public static final int REQUEST_CODE_SET_PIN = 2;
    public static final int REQUEST_CODE_SET_KEY = 3;
    public static final String KEY_QR_CODE = "KEY_QR_CODE";
    public static final String KEY_PRE_AUTH_PIN_CODE = "KEY_PRE_AUTH_PIN_CODE";
    public static final String KEY_SET_PIN_CODE = "KEY_SET_PIN_CODE";
    public static final String KEY_SET_KEY = "KEY_SET_KEY";

    private static final String BEGIN_BITCOIN_SIGNED_MESSAGE = "-----BEGIN BITCOIN SIGNED MESSAGE-----";
    private static final String AMOUNT_EQUAL_TO = "amount=";

    private Menu toolbarMenu = null;

    private int localCurrency = R.id.radio_twd;
    private int transactionFee = R.id.radio_low;
    private double customTransactionFee = 0.00001;
    private boolean includeFee = false;
    private boolean useFixAddr = false;
    private NfcAdapter mNfcAdapter = null;
    private Otk mOtk = null;
    private Fragment mSelectedFragment = null;
    private CurrencyExchangeRate mCurrencyExRate;
    private boolean mIsOperationOnGoing = false;
    AlertDialog.Builder mProgressDialogBuilder = null;
    AlertDialog mProgressDialog = null;


    /**
     * Process activity result
     * @param requestCode
     * @param resultCode
     * @param intent
     */
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        Log.d(TAG, "onActivityResult:" + requestCode + " resultCode:" + resultCode);
        if (requestCode == MainActivity.REQUEST_CODE_QR_CODE) {
            if (resultCode == RESULT_OK) {
                // Handle successful scan
                String contents = intent.getStringExtra(KEY_QR_CODE);
                if (contents.contains(BEGIN_BITCOIN_SIGNED_MESSAGE)) {
                    // TODO
                    // updateFormattedSignedMessage(contents);
                }
                else {
                    String addr = "";
                    String amount = "0.0";
                    boolean notBTC = false;

                    if (contents.contains(":")) {
                        // contents might be a uri
                        String uriArray[] = contents.split(":");

                        if (uriArray.length > 1) {
                            if (uriArray[0].contentEquals("bitcoin")) {
                                contents = uriArray[1];
                                addr = contents;
                            }
                            else {
                                notBTC = true;
                                Toast.makeText(this, "Sorry! " + uriArray[0] +
                                        " is not supported at this moment.",Toast.LENGTH_LONG).show();
                                contents = "";
                            }
                        }
                        else {
                            // incorrect uri format
                        }
                    }

                    if (!notBTC && contents.contains("?")) {
                        // contents might contains query tag
                        String queryArray[] = contents.split("\\?");

                        if (queryArray.length > 1) {
                            addr = queryArray[0];

                            String queryTagArray[] = queryArray[1].split("&");

                            for (String s : queryTagArray) {
                                if (s.toLowerCase().contains(AMOUNT_EQUAL_TO)) {
                                    String amountArray[] = s.split("=");
                                    if (amountArray.length > 1) {
                                        Toast.makeText(this, "Amount: " + amountArray[1],Toast.LENGTH_LONG).show();
                                        amount = amountArray[1];
                                    }
                                }
                            }
                        }
                        else {
                            // incorrect uri
                        }
                    }
                    else {
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
                Toast.makeText(this, getString(R.string.qr_code_scan_cancelled),Toast.LENGTH_LONG).show();
            }
        }
    }

    /* declarations */
    private BottomNavigationView.OnNavigationItemSelectedListener navListener =
            new BottomNavigationView.OnNavigationItemSelectedListener() {
                @Override
                public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
                    Toolbar toolbar = findViewById(R.id.toolbar);
                    Menu menu = toolbar.getMenu();
                    menu.clear();

                    switch (menuItem.getItemId()) {
                        case R.id.nav_menu_history:
                            getSupportActionBar().setTitle(getString(R.string.history));
                            getMenuInflater().inflate(R.menu.menu_history, menu);
                            mSelectedFragment = new FragmentHistory();
                            break;
                        case R.id.nav_menu_addresses:
                            getSupportActionBar().setTitle(getString(R.string.addresses));
                            getMenuInflater().inflate(R.menu.menu_addresses, menu);
                            mSelectedFragment = new FragmentAddrbook();
                            break;
                        case R.id.nav_menu_openturnkey:
                            getSupportActionBar().setTitle(getString(R.string._openturnkey));
                            getMenuInflater().inflate(R.menu.menu_openturnkey, menu);
                            mSelectedFragment = new FragmentOtk();
                            break;
                        default:
                            getSupportActionBar().setTitle(getString(R.string.pay));
                            getMenuInflater().inflate(R.menu.menu_pay, menu);
                            updatePayConfig(menu);
                            mSelectedFragment = new FragmentPay();
                            ((FragmentPay) mSelectedFragment).updateCurrencyExchangeRate(mCurrencyExRate);
                            ((FragmentPay) mSelectedFragment).updateRecipientAddress("mhifA1DwiMPHTjSJM8FFSL8ibrzWaBCkVT");

                            break;
                    }

                    getSupportFragmentManager().beginTransaction().replace(R.id.frame_main,
                            mSelectedFragment).commit();

                    return true;
                }
            };

    /* class functions */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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

        mOtk = Otk.getInstance();
        mOtk.setEventListener(new Otk.OtkEventListener() {
            @Override
            public void onOtkEvent(OtkEvent event) {
                OtkEvent.Type type = event.getType();
                Log.d(TAG, "onOtkEvent:" + type.toString());
                // TODO: process event
                if (type == OtkEvent.Type.CURRENCY_EXCHANGE_RATE_UPDATE) {
                    // Cache rate
                    mCurrencyExRate = event.mCurrencyExRate;
                    if (mSelectedFragment instanceof FragmentPay) {
                        // Update rate
                        ((FragmentPay) mSelectedFragment).updateCurrencyExchangeRate(event.mCurrencyExRate);
                    }
                }
                else if (type == OtkEvent.Type.APPROACH_OTK) {
                    // TODO
                }
                else if (type == OtkEvent.Type.OPERATION_IN_PROCESSING) {
                    // Show progress spin circle
                    showProgressDialog(getString(R.string.processing));
                }
                else if (type == OtkEvent.Type.GENERAL_INFORMATION) {
                    // TODO
                }
                else if (type == OtkEvent.Type.SEND_BITCOIN_SUCCESS) {
                    hideProgressDialog();
                    Tx tx = event.getTx();
                    Log.d(TAG, "From:" + tx.getFrom());
                    Log.d(TAG, "To:" + tx.getTo());
                    Log.d(TAG, "Hash:" + tx.getHash());
                    Log.d(TAG, "Fee:" + tx.getFee());
                    Log.d(TAG, "Amount:" + tx.getAmount());
                    Log.d(TAG, "Time:" + tx.getTime());

                    // Make sure we are in FragmentOtk
                    if (mSelectedFragment instanceof FragmentOtk) {
                        // Go back to pay fragment
                        mIsOperationOnGoing = false;
                        backToPreviousFragment();
                    }

                    // Show tx
                    dialogBtcSent(tx);

                }
                else if ((type == OtkEvent.Type.SEND_BITCOIN_FAIL) ||
                        (type == OtkEvent.Type.COMMAND_EXECUTION_FAILED)) {
                    // Hide progress
                    hideProgressDialog();
                    // Show error in doalog
                    dialogSentBtcFailed(event.getFailureReason());

                    // Make sure we are in FragmentOtk
                    if (mSelectedFragment instanceof FragmentOtk) {
                        // Go back to pay fragment
                        mIsOperationOnGoing = false;
                        backToPreviousFragment();

                    }
                }
                else if (type == OtkEvent.Type.RECIPIENT_ADDRESS) {
                    // Make sure we are in FragmentOtk
                    if (mSelectedFragment instanceof FragmentOtk) {
                        // Go back to pay fragment
                        mIsOperationOnGoing = false;
                        backToPreviousFragment();
                        // Set recipient address
                        ((FragmentPay) mSelectedFragment).updateRecipientAddress(event.getRecipientAddress());
                    }
                }
                else if (type == OtkEvent.Type.OTK_UNAUTHORIZED) {
                    // Dismiss progress dialog
                    hideProgressDialog();
                    // Show pre-auth with pin dialog
                    dialogAuthByPin();
                }
                else {

                }
            }
        });
    }


    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        String action = intent.getAction();

        if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(action) ||
                NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action)) {
            int ret = mOtk.processIntent(intent);
            if (ret != Otk.OTK_RETURN_OK) {
                Log.d(TAG, "process intent failed:" + ret);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        IntentFilter tagDetected = new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED);
        IntentFilter ndefDetected = new IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED);
        IntentFilter techDetected = new IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED);
        IntentFilter[] nfcIntentFilter = new IntentFilter[]{techDetected,tagDetected,ndefDetected};

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);

        if (mNfcAdapter != null) {
            mNfcAdapter.enableForegroundDispatch(this, pendingIntent, nfcIntentFilter, null);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(mNfcAdapter!= null)
            mNfcAdapter.disableForegroundDispatch(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_pay, menu);
        updatePayConfig(menu);
        toolbarMenu = menu;

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        TextView tv;

        switch (item.getItemId()) {
            case R.id.menu_history_clear_history:
                dialogClearHistory();
                return true;
            case R.id.menu_openturnkey_get_key:
                tv = findViewById(R.id.text_nfc_comm_type);
                tv.setText(R.string.disclose_key_information);
                return true;
            case R.id.menu_openturnkey_unlock:
                tv = findViewById(R.id.text_nfc_comm_type);
                tv.setText(R.string.unlock_openturnkey);
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
                startActivity(new Intent(this, AddAddressActivity.class));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void setLocalCurrency(int localCurrency) {
        this.localCurrency = localCurrency;
        updatePayConfig(toolbarMenu);
        if (mSelectedFragment instanceof FragmentPay) {
            // Update local currency
            ((FragmentPay) mSelectedFragment).updateLocalCurrency(currencyToLocalCurrency(localCurrency));
        }
    }

    public void authByPin(String pin) {
        Log.d(TAG, "pin:" + pin);
        mOtk.setPinForOperation(pin);
    }

    public void cancelAuthByPin() {
        mOtk.cancelOperation();
        mIsOperationOnGoing = false;
        backToPreviousFragment();
    }

    public void setTransactionFee(int transactionFee) {
        this.transactionFee = transactionFee;
        updatePayConfig(toolbarMenu);
    }

    /* Local functions */
    public void dialogLocalCurrency() {
        DialogLocalCurrency dialog = new DialogLocalCurrency();
        Bundle bundle = new Bundle();
        bundle.putInt("localCurrency", localCurrency);
        dialog.setArguments(bundle);
        dialog.show(getSupportFragmentManager(), "dialog");
    }

    public void dialogTransactionFee() {
        DialogTransactionFee dialog = new DialogTransactionFee();
        Bundle bundle = new Bundle();
        bundle.putInt("transactionFee", transactionFee);
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
        bundle.putInt("sendBtcResult", R.string.btc_sent);
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


    public String currencyToString(int currency) {
        switch (currency) {
            case R.id.radio_cny:
                return getString(R.string._currency_cny);
            case R.id.radio_eur:
                return getString(R.string._currency_eur);
            case R.id.radio_jpy:
                return getString(R.string._currency_jpy);
            case R.id.radio_twd:
                return getString(R.string._currency_twd);
            default:
                return getString(R.string._currency_usd);
        }
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

    public String transactionFeeToString(int transactionFee) {
        switch (transactionFee) {
            case R.id.radio_custom:
                return getString(R.string.custom);
            case R.id.radio_high:
                return getString(R.string.high);
            case R.id.radio_mid:
                return getString(R.string.mid);
            default:
                return getString(R.string.low);
        }
    }

    public void updatePayConfig(Menu menu) {
        String str;
        MenuItem menuItem;

        menuItem = menu.findItem(R.id.menu_pay_local_curreny);
        if (menuItem != null) {
            str = getString(R.string.local_currency) +
                    " (" + currencyToString(localCurrency) + ")";
            menuItem.setTitle(str);
        }

        menuItem = menu.findItem(R.id.menu_pay_transaction_fee);
        if (menuItem != null) {
            str = getString(R.string.transaction_fee) +
                    " (" + transactionFeeToString(transactionFee) + ")";
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

    public void onSignPaymentButtonClick(String to, double amount) {
        // Show FragmentOtk
        Toolbar toolbar = findViewById(R.id.toolbar);
        Menu menu = toolbar.getMenu();
        // Dont show menu
        menu.clear();
        getSupportActionBar().setTitle(getString(R.string._openturnkey));
        mSelectedFragment = FragmentOtk.newInstance(Otk.Operation.OTK_OP_SIGN_PAYMENT);

        getSupportFragmentManager().beginTransaction().replace(R.id.frame_main,
                mSelectedFragment).addToBackStack(null).commit();

        // TODO convert txFee log, mid high
        mOtk.setOperation(Otk.Operation.OTK_OP_SIGN_PAYMENT, to, amount, includeFee, 0);

        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setVisibility(View.INVISIBLE);

        mIsOperationOnGoing = true;
    }

    public void onGetRecipientAddressByReadNfcButtonClick() {
        // Show FragmentOtk
        Toolbar toolbar = findViewById(R.id.toolbar);
        Menu menu = toolbar.getMenu();
        // Dont show menu
        menu.clear();
        getSupportActionBar().setTitle(getString(R.string._openturnkey));
        mSelectedFragment = FragmentOtk.newInstance(Otk.Operation.OTK_OP_GET_RECIPIENT_ADDRESS);

        getSupportFragmentManager().beginTransaction().replace(R.id.frame_main,
                mSelectedFragment).addToBackStack(null).commit();

        mOtk.setOperation(Otk.Operation.OTK_OP_GET_RECIPIENT_ADDRESS);

        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setVisibility(View.INVISIBLE);
        mIsOperationOnGoing = true;

    }

    public void backToPreviousFragment() {
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setVisibility(View.VISIBLE);
        // Add menu back
        Toolbar toolbar = findViewById(R.id.toolbar);
        Menu menu = toolbar.getMenu();
        getMenuInflater().inflate(R.menu.menu_pay, menu);
        updatePayConfig(menu);
        super.onBackPressed();
        getActiveFragment();
        // Update rate in case it's not updated
        if (mSelectedFragment instanceof FragmentPay) {
            ((FragmentPay) mSelectedFragment).updateCurrencyExchangeRate(mCurrencyExRate);
        }
    }

    public void onCancelButtonClick() {
        Toast.makeText(this, getString(R.string.operation_cancelled), Toast.LENGTH_LONG).show();
        mOtk.cancelOperation();
        mIsOperationOnGoing = false;
        backToPreviousFragment();
    }

    private void getActiveFragment() {
        // Get active fragment
        try {
            for (Fragment fragment : getSupportFragmentManager().getFragments()) {
                if (fragment != null && fragment.isVisible()) {
                    mSelectedFragment = fragment;
                }
            }
        }
        catch (Exception e) {
            // Do nothing
        }
    }

    @Override
    public void onBackPressed() {
        // Do nothing
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
                        Log.d(TAG, "onCancel()");
                        onCancelButtonClick();
                        hideProgressDialog();
                    }})
                .setCancelable(true)
                .show();
    }

    private void hideProgressDialog() {
        if (mProgressDialog != null) {
            mProgressDialog.dismiss();
        }
    }
}

