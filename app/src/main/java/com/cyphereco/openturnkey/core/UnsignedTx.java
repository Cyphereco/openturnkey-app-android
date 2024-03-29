package com.cyphereco.openturnkey.core;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public class UnsignedTx {
    private String mFrom;
    private String mTo;
    private double mAmount; // in BTC
    private long mFee; // in satoshi
    private List<String> mToSign;

    public UnsignedTx(String from, String to, double amount, long fee, List<String> toSign) {
        mFrom = from;
        mTo = to;
        mAmount = amount;
        mFee = fee;
        mToSign = toSign;
    }

    public String getFrom() {
        return mFrom;
    }

    public String getTo() {
        return mTo;
    }

    public List<String> getToSign() {
        return mToSign;
    }

    public double getAmount() {
        return mAmount;
    }

    public long getFee() {
        return mFee;
    }

    @NotNull
    @Override
    public String toString() {
        return "UnsignedTx{" +
                "mFrom='" + mFrom + '\'' +
                ", mTo='" + mTo + '\'' +
                ", mAmount=" + mAmount +
                ", mFee=" + mFee +
                ", mToSign=" + mToSign +
                '}';
    }
}
