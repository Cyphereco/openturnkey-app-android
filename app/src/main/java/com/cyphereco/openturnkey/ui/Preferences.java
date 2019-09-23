package com.cyphereco.openturnkey.ui;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.cyphereco.openturnkey.core.Configurations;
import com.cyphereco.openturnkey.utils.LocalCurrency;
import com.cyphereco.openturnkey.utils.TxFee;

public class Preferences {
    public static final String TAG = Preferences.class.getSimpleName();
    private static final String LOCAL_CURRENCY = "LOCAL_CURRENCY";
    private static final String TX_FEE_TYPE = "TX_FEE_TYPE";
    private static final String TX_FEE_LOW = "TX_FEE_LOW";
    private static final String TX_FEE_MID = "TX_FEE_MID";
    private static final String TX_FEE_HIGH = "TX_FEE_HIGH";
    private static final String CUSTOMIZED_TX_FEE = "CUSTOMIZED_TX_FEE";
    private static final String NETWORK = "NETWORK";


    static public LocalCurrency getLocalCurrency(Context ctx) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
        String s = prefs.getString(LOCAL_CURRENCY, "");
        if (s.equals(LocalCurrency.LOCAL_CURRENCY_CNY.name())) {
            return LocalCurrency.LOCAL_CURRENCY_CNY;
        }
        if (s.equals(LocalCurrency.LOCAL_CURRENCY_EUR.name())) {
            return LocalCurrency.LOCAL_CURRENCY_EUR;
        }
        if (s.equals(LocalCurrency.LOCAL_CURRENCY_TWD.name())) {
            return LocalCurrency.LOCAL_CURRENCY_TWD;
        }
        if (s.equals(LocalCurrency.LOCAL_CURRENCY_USD.name())) {
            return LocalCurrency.LOCAL_CURRENCY_USD;
        }
        if (s.equals(LocalCurrency.LOCAL_CURRENCY_JPY.name())) {
            return LocalCurrency.LOCAL_CURRENCY_JPY;
        }

        return LocalCurrency.LOCAL_CURRENCY_TWD;
    }

    static public void setLocalCurrency(Context ctx, LocalCurrency lc) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
        prefs.edit().putString(LOCAL_CURRENCY, lc.name()).commit();
    }

    static public void setTxFeeType(Context ctx, Configurations.TxFeeType txFeeType) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
        prefs.edit().putString(TX_FEE_TYPE, txFeeType.name()).commit();
    }

    static public Configurations.TxFeeType getTxFeeType(Context ctx) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
        String s = prefs.getString(TX_FEE_TYPE, "");
        if (s.equals(Configurations.TxFeeType.HIGH.name())) {
            return Configurations.TxFeeType.HIGH;
        }
        else if (s.equals(Configurations.TxFeeType.MID.name())) {
            return Configurations.TxFeeType.MID;
        }
        else if (s.equals(Configurations.TxFeeType.LOW.name())) {
            return Configurations.TxFeeType.LOW;
        }
        else if (s.equals(Configurations.TxFeeType.CUSTOMIZED.name())) {
            return Configurations.TxFeeType.CUSTOMIZED;
        }
        // Default
        return Configurations.TxFeeType.LOW;
    }
    static public void setNetwork(Context ctx, Configurations.Network network) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
        prefs.edit().putString(NETWORK, network.name()).commit();
        // Update configuration
        Configurations.setNetwork(network);
    }

    static public Configurations.Network getNetwork(Context ctx) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
        String s = prefs.getString(NETWORK, Configurations.network.name());
        if (s.equals(Configurations.Network.MAINNET.name())) {
            return Configurations.Network.MAINNET;
        }
        return Configurations.Network.TESTNET;
    }

    static public boolean isTestnet(Context ctx) {
        return (getNetwork(ctx) == Configurations.Network.TESTNET);
    }

    static public void setTxFee(Context ctx, TxFee txFee) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
        Log.d(TAG, "low:" + txFee.getLow() + " mid:" + txFee.getMid() + " high:" + txFee.getHigh());
        prefs.edit().putLong(TX_FEE_LOW, txFee.getLow()).commit();
        prefs.edit().putLong(TX_FEE_MID, txFee.getMid()).commit();
        prefs.edit().putLong(TX_FEE_HIGH, txFee.getHigh()).commit();
    }

    static public TxFee getTxFee(Context ctx) {
        Configurations.TxFeeType type = getTxFeeType(ctx);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
        TxFee txFee = new TxFee(prefs.getLong(TX_FEE_LOW, Configurations.txFeeLow),
                prefs.getLong(TX_FEE_MID, Configurations.txFeeMid),
                prefs.getLong(TX_FEE_HIGH, Configurations.txFeeHigh));
        return txFee;
    }

    static public void setCustomizedTxFee(Context ctx, long satoshi) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
        prefs.edit().putLong(CUSTOMIZED_TX_FEE, satoshi).commit();
    }

    static public long getCustomizedTxFee(Context ctx) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
        return prefs.getLong(CUSTOMIZED_TX_FEE, 1000);
    }
}