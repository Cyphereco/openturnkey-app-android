package com.cyphereco.openturnkey.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import com.cyphereco.openturnkey.utils.Log4jHelper;
import com.cyphereco.openturnkey.core.Configurations;
import com.cyphereco.openturnkey.utils.LocalCurrency;
import com.cyphereco.openturnkey.utils.TxFee;
import org.slf4j.Logger;

import java.util.Locale;

public class Preferences {
    public static final String TAG = Preferences.class.getSimpleName();
    private static Logger logger = Log4jHelper.getLogger(TAG);

    private static SharedPreferences prefs;

    private static final String LOCAL_CURRENCY = "LOCAL_CURRENCY";
    private static final String TX_FEE_TYPE = "TX_FEE_TYPE";
    private static final String TX_FEE_LOW = "TX_FEE_LOW";
    private static final String TX_FEE_MID = "TX_FEE_MID";
    private static final String TX_FEE_HIGH = "TX_FEE_HIGH";
    private static final String CUSTOMIZED_TX_FEE = "CUSTOMIZED_TX_FEE";
    private static final String NETWORK = "NETWORK";
    private static final String FEE_INCLUDED = "FEE_INCLUDED";
    private static final String USE_FIX_ADDRESS_CHECKED = "USE_FIX_ADDRESS_CHECKED";
    private static final String USE_FIX_ADDRESS_ADDR_STR = "USE_FIX_ADDRESS_ADDR_STR";

    Preferences(Context ctx) {
        prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
    }

    static void setLocalCurrency(LocalCurrency lc) {
        prefs.edit().putString(LOCAL_CURRENCY, lc.name()).apply();

    }

    static LocalCurrency getLocalCurrency() {
        String s = prefs.getString(LOCAL_CURRENCY, "");

        if (s.length() == 0) {
            if (Locale.getDefault().getCountry().equals(Locale.TAIWAN.getCountry())) {
                s = LocalCurrency.LOCAL_CURRENCY_TWD.name();
            }
            else if (Locale.getDefault().getCountry().equals(Locale.CHINA.getCountry())) {
                s = LocalCurrency.LOCAL_CURRENCY_CNY.name();
            }
            else if (Locale.getDefault().getCountry().equals(Locale.JAPAN.getCountry())) {
                s = LocalCurrency.LOCAL_CURRENCY_JPY.name();
            }
            else if (Locale.getDefault().getCountry().equals(Locale.GERMAN.getCountry()) ||
                    Locale.getDefault().getCountry().equals(Locale.FRANCE.getCountry()) ||
                    Locale.getDefault().getCountry().equals(Locale.ITALY.getCountry()) ||
                    Locale.getDefault().getCountry().equals(Locale.UK.getCountry())) {
                s = LocalCurrency.LOCAL_CURRENCY_EUR.name();
            }
            else {
                s = LocalCurrency.LOCAL_CURRENCY_USD.name();
            }
            logger.debug("Auto-select Currency by locale: {}", s);
        }

        if (s.equals(LocalCurrency.LOCAL_CURRENCY_CNY.name())) {
            return LocalCurrency.LOCAL_CURRENCY_CNY;
        }
        if (s.equals(LocalCurrency.LOCAL_CURRENCY_EUR.name())) {
            return LocalCurrency.LOCAL_CURRENCY_EUR;
        }
        if (s.equals(LocalCurrency.LOCAL_CURRENCY_TWD.name())) {
            return LocalCurrency.LOCAL_CURRENCY_TWD;
        }
        if (s.equals(LocalCurrency.LOCAL_CURRENCY_JPY.name())) {
            return LocalCurrency.LOCAL_CURRENCY_JPY;
        }

        return LocalCurrency.LOCAL_CURRENCY_USD;
    }

    static void setTxFeeType(Configurations.TxFeeType txFeeType) {
        prefs.edit().putString(TX_FEE_TYPE, txFeeType.name()).apply();
    }

    public static Configurations.TxFeeType getTxFeeType() {
        String s = prefs.getString(TX_FEE_TYPE, "");
        if (s.equals(Configurations.TxFeeType.HIGH.name())) {
            return Configurations.TxFeeType.HIGH;
        } else if (s.equals(Configurations.TxFeeType.MID.name())) {
            return Configurations.TxFeeType.MID;
        } else if (s.equals(Configurations.TxFeeType.LOW.name())) {
            return Configurations.TxFeeType.LOW;
        } else if (s.equals(Configurations.TxFeeType.CUSTOMIZED.name())) {
            return Configurations.TxFeeType.CUSTOMIZED;
        }
        // Default
        return Configurations.TxFeeType.LOW;
    }

    static void setTxFee(TxFee txFee) {
        logger.debug("low:" + txFee.getLow() + " mid:" + txFee.getMid() + " high:" + txFee.getHigh());
        prefs.edit().putLong(TX_FEE_LOW, txFee.getLow()).apply();
        prefs.edit().putLong(TX_FEE_MID, txFee.getMid()).apply();
        prefs.edit().putLong(TX_FEE_HIGH, txFee.getHigh()).apply();
    }

    public static TxFee getTxFee() {
        return new TxFee(prefs.getLong(TX_FEE_LOW, Configurations.txFeeLow),
                prefs.getLong(TX_FEE_MID, Configurations.txFeeMid),
                prefs.getLong(TX_FEE_HIGH, Configurations.txFeeHigh));
    }

    static void setCustomizedTxFee(long satoshi) {
        prefs.edit().putLong(CUSTOMIZED_TX_FEE, satoshi).apply();
    }

    public static long getCustomizedTxFee() {
        return prefs.getLong(CUSTOMIZED_TX_FEE, 1000);
    }

    static void setFeeIncluded(boolean isChecked) {
        prefs.edit().putBoolean(FEE_INCLUDED, isChecked).apply();
    }

    static boolean getFeeIncluded() {
        return prefs.getBoolean(FEE_INCLUDED, false);
    }

    static void setUseFixAddress(boolean isChecked, String address) {
        prefs.edit().putBoolean(USE_FIX_ADDRESS_CHECKED, isChecked).apply();
        prefs.edit().putString(USE_FIX_ADDRESS_ADDR_STR, address).apply();
    }

    static boolean getUseFixAddressChecked() {
        return prefs.getBoolean(USE_FIX_ADDRESS_CHECKED, false);
    }

    static String getUseFixAddressAddrString() {
        return prefs.getString(USE_FIX_ADDRESS_ADDR_STR, "");
    }

    static void setNetwork(Configurations.Network network) {
        prefs.edit().putString(NETWORK, network.name()).apply();
        // Update configuration
        Configurations.setNetwork(network);
    }

    private static Configurations.Network getNetwork() {
        String s = prefs.getString(NETWORK, Configurations.network.name());
        if (s.equals(Configurations.Network.MAINNET.name())) {
            return Configurations.Network.MAINNET;
        }
        return Configurations.Network.TESTNET;
    }

    public static boolean isTestnet() {
        return (getNetwork() == Configurations.Network.TESTNET);
    }

    public interface PreferenceInterce {
        void onPreferenceChanged();
    }
}