package com.cyphereco.openturnkey.utils;

import org.jetbrains.annotations.NotNull;

public class TxFee {
    // in satoshi/byte
    private long mLow;
    private long mMid;
    private long mHigh;

    public TxFee(long low, long mid, long high) {
        mLow = low;
        mMid = mid;
        mHigh = high;
    }

    public long getLow() {
        return mLow;
    }

    public long getMid() {
        return mMid;
    }

    public long getHigh() {
        return mHigh;
    }

    @NotNull
    @Override
    public String toString() {
        return "TxFee{" +
                "mLow=" + mLow +
                ", mMid=" + mMid +
                ", mHigh=" + mHigh +
                '}';
    }
}
