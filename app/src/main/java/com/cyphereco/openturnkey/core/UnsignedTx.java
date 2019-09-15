package com.cyphereco.openturnkey.core;

import java.util.List;

public class UnsignedTx {
    private String mFrom;
    private String mTo;
    private double mAmount; // in BTC
    private long mFee; // in satoshi
    List<String> mToSign;

    public UnsignedTx(String from, String to, double amount, long fee, List<String> toSign) {
        mFrom = from;
        mTo = to;
        mAmount = amount;
        mFee = fee;
        mToSign = toSign;
    }

    public String getFrom() { return mFrom;}
    public String getTo() { return mTo;}
    public List<String> getToSign() { return mToSign;}
    public double getAmount() { return mAmount;}
    public long getFee() { return mFee;}
}
