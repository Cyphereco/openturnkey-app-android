package com.cyphereco.openturnkey.ui;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.cyphereco.openturnkey.core.Configurations;
import com.cyphereco.openturnkey.utils.LocalCurrency;

import java.math.BigDecimal;

public class Preferences {
    public static final String TAG = Preferences.class.getSimpleName();
    private static final String LOCAL_CURRENCY = "LOCAL_CURRENCY";


    static LocalCurrency getLocalCurrency(Context ctx) {
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

    static void setLocalCurrency(Context ctx, LocalCurrency lc) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
        prefs.edit().putString(LOCAL_CURRENCY, lc.name()).commit();
    }

    static BigDecimal txFees(Context ctx){
        long txFees;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
        String txFeesSelection = prefs.getString("tx_fees_list", "Mid");
        Log.d(TAG, "txFeesSelection:" + txFeesSelection);
        if (txFeesSelection.equals("Low")) {
            txFees = Configurations.txFeesLow;
        }
        else if (txFeesSelection.equals("Mid")) {
            txFees = Configurations.txFeesMid;
        }
        else if (txFeesSelection.equals("High")) {
            txFees = Configurations.txFeesHigh;
        }
        else {
            String txFeesCustomizedStr = prefs.getString("tx_fees", "");
            if (txFeesCustomizedStr != null) {
                long txFeesCustomized = Long.parseLong(txFeesCustomizedStr);
                txFees = txFeesCustomized;
            }
            else {
                txFees = Configurations.txFeesHigh;
            }
        }
        Log.d(TAG, "Preference tx fees:" + txFees);

        return new BigDecimal(txFees);
    }
}