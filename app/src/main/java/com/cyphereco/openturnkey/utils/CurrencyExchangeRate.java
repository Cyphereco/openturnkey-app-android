package com.cyphereco.openturnkey.utils;

public class CurrencyExchangeRate {
    double mTWD; // 1 BTC to mTWD TWD
    double mUSD;
    double mJPY;
    double mEUR;
    double mCNY;
    public CurrencyExchangeRate(double twd, double usd, double jpy, double eur, double cny) {
        mTWD = twd;
        mUSD = usd;
        mJPY = jpy;
        mEUR = eur;
        mCNY = cny;
    }

    public double getTWD() {return mTWD;}
    public double getUSD() {return mUSD;}
    public double getJPY() {return mJPY;}
    public double getEUR() {return mEUR;}
    public double getCNY() {return mCNY;}

}
