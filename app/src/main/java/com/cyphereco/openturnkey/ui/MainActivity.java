package com.cyphereco.openturnkey.ui;

import android.content.Intent;
import android.nfc.NfcAdapter;
import android.os.Handler;
import android.os.Message;
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
import com.cyphereco.openturnkey.db.OpenturnkeyDB;
import com.cyphereco.openturnkey.utils.AlertPrompt;
import com.cyphereco.openturnkey.utils.BtcUtils;
import com.cyphereco.openturnkey.utils.BtcExchangeRates;
import com.cyphereco.openturnkey.utils.Log4jHelper;
import com.cyphereco.openturnkey.utils.TxFee;
import com.cyphereco.openturnkey.webservices.BlockChainInfo;

import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
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

    public enum PAGE {PAY, OTK, HISTORY, ADDRBOOK}

    private static int blockHeight = 0;
    private static BtcExchangeRates btcExchangeRates;
    private static TxFee txFee;
    private static Handler pageSwitchHandler;

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

    private static List<OnlineDataUpdateListener> listOnlineDataUpdateListener = new ArrayList<>();

    public static String getCurrentActivity() {
        return currentActivity;
    }

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
            finish();
            startActivity(new Intent(this, MainActivity.class));
        }

        // Initialize Database
        OpenturnkeyDB.init(getApplicationContext());

        updateOnlineData();

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
                        if (getCurrentFocus() != null && imm.isAcceptingText()) { // verify if the soft keyboard is open
                            imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
                        }

                        return true;
                    }
                };

        bottomNav.setOnNavigationItemSelectedListener(navListener);

        pageSwitchHandler = new Handler(new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {
                viewPager.setCurrentItem(msg.arg1);
                return false;
            }
        });

        /* init NFC. */
        NfcAdapter mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if (mNfcAdapter == null) {
            AlertPrompt.alert(this, getString(R.string.nfc_unavailable));
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        logger.debug("destroyed");
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        /*
         For the incoming intents, we only care about NFC event, (at the moment)
         specifically, NDEF message event, which is used by OpenTurnKey. We will
         ignore other intents since this app is designed to deal with OpenTurnKey only.
         */

        /*
         All the NFC new intent will be post here as the entry point, then dispatch
         to its activity or fragment.

         Note:
            The foreground dispatch system does not work as said in the manual.
            None of the activities except the registered one  will receive NFC intent.
            So we make our own customized dispatch implementation instead.
         */


        if (getClass().getName().equals(currentActivity)) {
            logger.debug("MainActivity is current");
            if (mEnableReadOtk && selectedFragment != null) {
                // MainActivity is the current activity, dispatch intent to current fragment.
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
                                    intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES));
                }
                startActivity(newIntent);
            }
        }
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.setFlags(Intent.FLAG_ACTIVITY_TASK_ON_HOME);
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (getClass().getName().equals(currentActivity) && selectedFragment != null) {
            selectedFragment.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            // when focus is on address search view in history page, collapse view first
            SearchView v = findViewById(R.id.menu_addresses_search);
            if (v != null && v.hasFocus()) {
                v.clearFocus();
                v.onActionViewCollapsed();
                return false;
            }
        }
        return super.onKeyDown(keyCode, event);
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

    public static BtcExchangeRates getBtcExchangeRates() {
        return btcExchangeRates;
    }

    public void setExchangeRate(BtcExchangeRates btcExchangeRates) {
        if (MainActivity.btcExchangeRates == null) MainActivity.btcExchangeRates = btcExchangeRates;
        for (OnlineDataUpdateListener listener : listOnlineDataUpdateListener
             ) {
            listener.onExchangeRateUpdated(btcExchangeRates);
        }
    }

    public void setTxFee(TxFee txFee) {
        logger.debug("Update TxFee: {}", txFee.toString());
        if (MainActivity.txFee != null) MainActivity.txFee = txFee;
        for (OnlineDataUpdateListener listener : listOnlineDataUpdateListener
        ) {
            listener.onTxFeeUpdated(txFee);
        }
    }

    public static int getBlockHeight() {
        return blockHeight;
    }

    public static void setBlockHeight(int blockHeight) {
        logger.debug("Current block height: {}", blockHeight);
        MainActivity.blockHeight = blockHeight;
        for (OnlineDataUpdateListener listener : listOnlineDataUpdateListener
        ) {
            listener.onBlockHeightUpdated(blockHeight);
        }
    }

    public static FragmentExtendOtkViewPage getSelectedFragment() {
        return selectedFragment;
    }

    public static void setSelectedFragment(FragmentExtendOtkViewPage selectedFragment) {
        MainActivity.selectedFragment = selectedFragment;
    }

    public static void switchToPage(int page) {
        Message msg = new Message();
        msg.arg1 = page;
        pageSwitchHandler.sendMessage(msg);
    }

    private void updateOnlineData() {
        final Timer t = new Timer();
        t.schedule(new TimerTask() {
            @Override
            public void run() {
                setBlockHeight(BlockChainInfo.getLatestBlockHeight());

                BtcExchangeRates r = BtcUtils.getCurrencyExchangeRate();
                if (r == null) {
                    t.cancel();
                    t.purge();
                    updateOnlineData();
                } else {
                    setExchangeRate(r);
                    setTxFee(BtcUtils.getTxFee());
                }
            }
        }, 1000, 1000 * 60 * Configurations.INTERVAL_EXCHANGE_RATE_UPDATE);
    }

    public static void addToListOnlineDataUpdateListener(OnlineDataUpdateListener listener) {
        if (listener != null) {
            MainActivity.listOnlineDataUpdateListener.add(listener);
        }
    }

    public interface OnlineDataUpdateListener {
        void onExchangeRateUpdated(BtcExchangeRates xrate);

        void onTxFeeUpdated(TxFee txFee);

        void onBlockHeightUpdated(int height);
    }
}

