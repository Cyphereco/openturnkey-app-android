package com.cyphereco.openturnkey.ui;

import android.app.PendingIntent;
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
import android.widget.TextView;
import android.widget.Toast;

import com.cyphereco.openturnkey.R;
import com.cyphereco.openturnkey.core.Otk;
import com.cyphereco.openturnkey.ui.DialogAbout;
import com.cyphereco.openturnkey.ui.DialogClearHistory;
import com.cyphereco.openturnkey.ui.DialogLocalCurrency;
import com.cyphereco.openturnkey.ui.DialogTransactionFee;
import com.cyphereco.openturnkey.ui.FragmentAddrbook;
import com.cyphereco.openturnkey.ui.FragmentHistory;
import com.cyphereco.openturnkey.ui.FragmentOtk;
import com.cyphereco.openturnkey.ui.FragmentPay;

public class MainActivity extends AppCompatActivity
        implements DialogLocalCurrency.DialogLocalCurrecyListener,
        DialogTransactionFee.DialogTransactionFeeListener {

    public static final String TAG = MainActivity.class.getSimpleName();
    private Menu toolbarMenu = null;

    private int localCurrency = R.id.radio_twd;
    private int transactionFee = R.id.radio_low;
    private double customTransactionFee = 0.00001;
    private boolean includeFee = false;
    private boolean useFixAddr = false;
    private NfcAdapter mNfcAdapter = null;
    private Otk mOtk = null;

    /* declarations */
    private BottomNavigationView.OnNavigationItemSelectedListener navListener =
            new BottomNavigationView.OnNavigationItemSelectedListener() {
                @Override
                public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
                    Fragment selectedFragment = null;
                    Toolbar toolbar = findViewById(R.id.toolbar);
                    Menu menu = toolbar.getMenu();
                    menu.clear();

                    switch (menuItem.getItemId()) {
                        case R.id.nav_menu_history:
                            getSupportActionBar().setTitle(getString(R.string.history));
                            getMenuInflater().inflate(R.menu.menu_history, menu);
                            selectedFragment = new FragmentHistory();
                            break;
                        case R.id.nav_menu_addresses:
                            getSupportActionBar().setTitle(getString(R.string.addresses));
                            getMenuInflater().inflate(R.menu.menu_addresses, menu);
                            selectedFragment = new FragmentAddrbook();
                            break;
                        case R.id.nav_menu_openturnkey:
                            getSupportActionBar().setTitle(getString(R.string._openturnkey));
                            getMenuInflater().inflate(R.menu.menu_openturnkey, menu);
                            selectedFragment = new FragmentOtk();
                            break;
                        default:
                            getSupportActionBar().setTitle(getString(R.string.pay));
                            getMenuInflater().inflate(R.menu.menu_pay, menu);
                            updatePayConfig(menu);
                            selectedFragment = new FragmentPay();
                            break;
                    }

                    getSupportFragmentManager().beginTransaction().replace(R.id.frame_main,
                            selectedFragment).commit();

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
            finish();
        }

        mOtk = Otk.getInstance();
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
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void setLocalCurrency(int localCurrency) {
        this.localCurrency = localCurrency;
        updatePayConfig(toolbarMenu);
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

    public void dialogAbout() {
        DialogAbout dialog = new DialogAbout();
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

}

