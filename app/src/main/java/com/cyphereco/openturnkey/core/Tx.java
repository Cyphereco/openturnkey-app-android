package com.cyphereco.openturnkey.core;

import com.blockcypher.model.transaction.Transaction;
import com.blockcypher.model.transaction.output.Output;
import com.cyphereco.openturnkey.utils.BtcUtils;
import com.cyphereco.openturnkey.utils.Log4jHelper;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

public class Tx {
    private static final String TAG = Tx.class.getSimpleName();
    private Logger logger = Log4jHelper.getLogger(TAG);

    private String mFrom;
    private String mTo;
    private String mTime;
    private double mAmount;
    private double mFee;
    private String mRaw;
    private String mHash;
    private Status mStatus;
    private String mDesc;
    private int mConfirmations;

    public enum Status {
        STATUS_SUCCESS(0),
        STATUS_NETWORK_FAILURE(1),
        STATUS_UNKNOWN_FAILURE(2),
        STATUS_SIGNATURE_MISMATCH(3);

        private final int value;

        Status(int s) {
            value = s;
        }

        public int toInt() {
            return value;
        }
    }

    public Tx(Status status, String hash, String errorDesc) {
        mHash = hash;
        mStatus = status;
        mDesc = errorDesc;
    }

    public Tx(String from, String to, Transaction trans, Status status) {
        this(from, to, trans, status, "");
    }

    public Tx(Transaction trans, Status status, String desc) {
        this("", "", trans, status, desc);
    }

    public Tx(String from, String to, Transaction trans, Status status, String errorDesc) {
        mFrom = from;
        mTo = to;
        double amount = 0.0;
        // Find amount from outputs
        for (int i = 0; i < trans.getOutputs().size(); i++) {
            Output o = trans.getOutputs().get(i);
            if (o.getAddresses().get(0).equals(mTo)) {
                amount = BtcUtils.satoshiToBtc(o.getValue().longValue());
                break;
            }
        }
        mRaw = trans.getHex();
        mHash = trans.getHash();
        mAmount = amount;
        mFee = BtcUtils.satoshiToBtc(trans.getFees().longValue());
        mTime = trans.getReceived();
        mStatus = status;
        mDesc = errorDesc;
        mConfirmations = trans.getConfirmations().intValue();
    }

    public Tx(String hash, String from, String to, double amount, double fee, String time, String raw, Status status, String errorDesc) {
        mHash = hash;
        mFrom = from;
        mTo = to;
        mAmount = amount;
        mFee = fee;
        mTime = time;
        mRaw = raw;
        mStatus = status;
        mDesc = errorDesc;
    }

    public String getFrom() {
        return mFrom;
    }

    public String getTo() {
        return mTo;
    }

    public String getHash() {
        return mHash;
    }

    public double getAmount() {
        return mAmount;
    }

    public double getFee() {
        return mFee;
    }

    public String getTime() {
        return mTime;
    }

    public String getRaw() {
        return mRaw;
    }

    public String getDesc() {
        return mDesc;
    }

    public Status getStatus() {
        return mStatus;
    }

    public void setFrom(String from) {
        mFrom = from;
    }

    public void setTo(String to) {
        mTo = to;
    }

    public void setRaw(String raw) {
        mRaw = raw;
    }

    public void setAmount(double amount) {
        mAmount = amount;
    }

    public void setFee(double fee) {
        mFee = fee;
    }

    public void setDesc(String desc) {
        mDesc = desc;
    }

    public void setTime(String time) {
        mTime = time;
    }

    public int getConfirmations() {
        return mConfirmations;
    }

    @NotNull
    public String toString() {
        return "Raw:" + mRaw +
                "\nFrom:" + mFrom +
                "\nTo:" + mTo +
                "\nHash:" + mHash +
                "\nAmount:" + mAmount +
                "\nFee:" + mFee +
                "\nTime:" + mTime +
                "\nStatus:" + mStatus +
                "\nConfirmations:" + mConfirmations +
                "\nError desc:" + mDesc;
    }
}
