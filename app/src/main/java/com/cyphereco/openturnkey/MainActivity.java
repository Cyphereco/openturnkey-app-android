package com.cyphereco.openturnkey;

import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity
        implements DialogLocalCurrency.DialogLocalCurrecyListener,
        DialogTransactionFee.DialogTransactionFeeListener {

    private Menu toolbarMenu = null;

    private int localCurrency = R.id.radio_twd;
    private int transactionFee = R.id.radio_low;
    private double customTransactionFee = 0.00001;
    private boolean includeFee = false;
    private boolean useFixAddr = false;

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

