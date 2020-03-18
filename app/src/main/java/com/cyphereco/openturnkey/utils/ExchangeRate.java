package com.cyphereco.openturnkey.utils;

import org.jetbrains.annotations.NotNull;

public class ExchangeRate {
    private double mTWD; // 1 BTC to mTWD TWD
    private double mUSD;
    private double mJPY;
    private double mEUR;
    private double mCNY;

    ExchangeRate(double twd, double usd, double jpy, double eur, double cny) {
        mTWD = twd;
        mUSD = usd;
        mJPY = jpy;
        mEUR = eur;
        mCNY = cny;
    }

    public double getTWD() {
        return mTWD;
    }

    public double getUSD() {
        return mUSD;
    }

    public double getJPY() {
        return mJPY;
    }

    public double getEUR() {
        return mEUR;
    }

    public double getCNY() {
        return mCNY;
    }

    public void setmTWD(double mTWD) {
        this.mTWD = mTWD;
    }

    public void setmUSD(double mUSD) {
        this.mUSD = mUSD;
    }

    public void setmJPY(double mJPY) {
        this.mJPY = mJPY;
    }

    public void setmEUR(double mEUR) {
        this.mEUR = mEUR;
    }

    public void setmCNY(double mCNY) {
        this.mCNY = mCNY;
    }

    @NotNull
    @Override
    public String toString() {
        return "ExchangeRate{" +
                "mTWD=" + mTWD +
                ", mUSD=" + mUSD +
                ", mJPY=" + mJPY +
                ", mEUR=" + mEUR +
                ", mCNY=" + mCNY +
                '}';
    }
}
