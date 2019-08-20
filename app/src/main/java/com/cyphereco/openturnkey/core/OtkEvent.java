package com.cyphereco.openturnkey.core;

import com.blockcypher.model.transaction.Transaction;
import com.cyphereco.openturnkey.utils.CurrencyExchangeRate;

import java.util.concurrent.TransferQueue;

public class OtkEvent {

    /** Event type enum */
    public enum Type {
        INVALID,
        GENERAL_INFORMATION,
        RECIPIENT_ADDRESS,
        SEND_BITCOIN_SUCCESS,
        SEND_BITCOIN_FAIL,
        APPROACH_OTK,
        CURRENCY_EXCHANGE_RATE_UPDATE,
        OPERATION_IN_PROCESSING,
        SIGN_FAILED,
        OTK_UNAUTHORIZED,
    }

    public Type mType;
    public OtkData mData;
    public String mDesc;
    public CurrencyExchangeRate mCurrencyExRate = null;
    private Tx mTx;

    /**
     * Constructor for general info event
     */
    public OtkEvent(Type eventType, OtkData data) {
        mType = eventType;
        mData = data;
    }

    public OtkEvent(Type eventType, String desc) {
        mType = eventType;
        mDesc = desc;
    }

    public OtkEvent(Tx tx) {
        mType = Type.SEND_BITCOIN_SUCCESS;
        mTx = tx;
    }

    public OtkEvent(Type eventType) {
        mType = eventType;
    }
    public OtkEvent(Type eventType, CurrencyExchangeRate currencyExRate) {
        mType = eventType;
        mCurrencyExRate = currencyExRate;
    }

    public Type getType() {
        return mType;
    }
    public String getRecipientAddress() { return mDesc;}
    public String getFailureReason() { return mDesc;}
    public Tx getTx() { return mTx;}

}
