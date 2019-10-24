package com.cyphereco.openturnkey.core;

import com.cyphereco.openturnkey.utils.CurrencyExchangeRate;
import com.cyphereco.openturnkey.utils.TxFee;

import java.math.BigDecimal;

public class OtkEvent {

    /** Event type enum */
    public enum Type {
        INVALID,
        GENERAL_INFORMATION,
        RECIPIENT_ADDRESS,
        SEND_BITCOIN_SUCCESS,
        SEND_BITCOIN_FAIL,
        COMPLETE_PAYMENT_FAIL,
        APPROACH_OTK,
        CURRENCY_EXCHANGE_RATE_UPDATE,
        TX_FEE_UPDATE,
        OPERATION_IN_PROCESSING,
        SIGN_FAILED,
        OTK_UNAUTHORIZED,
        COMMAND_EXECUTION_FAILED,
        BALANCE_UPDATE,
        UNSIGNED_TX,
        UNLOCK_SUCCESS,
        UNLOCK_FAIL,
        OTK_IS_NOT_LOCKED,
    }

    private Type mType;
    private OtkData mData;
    private String mDesc;
    private CurrencyExchangeRate mCurrencyExRate = null;
    private Tx mTx;
    private UnsignedTx mUnsignedTx;
    private BigDecimal mBalance;
    private TxFee mTxFee;

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

    public OtkEvent(Type eventType, String address, BigDecimal b, CurrencyExchangeRate rate) {
        mType = eventType;
        mBalance = b;
        mDesc = address;
        mCurrencyExRate = rate;
    }

    public OtkEvent(Type eventType, Tx tx) {
        mType = eventType;
        mTx = tx;
    }

    public OtkEvent(UnsignedTx tx) {
        mType = Type.UNSIGNED_TX;
        mUnsignedTx = tx;
    }

    public OtkEvent(Type eventType) {
        mType = eventType;
    }
    public OtkEvent(Type eventType, CurrencyExchangeRate currencyExRate) {
        mType = eventType;
        mCurrencyExRate = currencyExRate;
    }

    public OtkEvent(Type eventType, TxFee txFee) {
        mType = eventType;
        mTxFee = txFee;
    }

    public Type getType() {
        return mType;
    }
    public String getRecipientAddress() { return mDesc;}
    public String getAddress() { return mDesc;}
    public String getFailureReason() { return mDesc;}
    public Tx getTx() { return mTx;}
    public UnsignedTx getUnsignedTx() { return mUnsignedTx;}
    public OtkData getData() {return mData;}
    public CurrencyExchangeRate getCurrencyExRate() {return mCurrencyExRate;}
    public BigDecimal getBalance() {return mBalance;}
    public TxFee getTxFee() { return mTxFee;}

}
