package com.cyphereco.openturnkey.core;

public class Tx {
    private String mFrom;
    private String mTo;
    private String mTime;
    private double mAmount;
    private double mFee;
    private String mRaw;
    private String mHash;

    public Tx(String hash, String from, String to, double amount, double fee, String time, String raw) {
        mHash = hash;
        mFrom = from;
        mTo = to;
        mAmount = amount;
        mFee = fee;
        mTime = time;
        mRaw = raw;
    }

    public String getFrom() { return mFrom;}
    public String getTo() { return mTo;}
    public String getHash() { return mHash;}
    public double getAmount() { return mAmount;}
    public double getFee() { return mFee;}
    public String getTime() { return mTime;}
    public String getRaw() { return mRaw;}
}
