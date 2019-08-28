package com.cyphereco.openturnkey.core;

import com.blockcypher.model.transaction.Transaction;
import com.cyphereco.openturnkey.core.protocol.OtkState;
import com.cyphereco.openturnkey.utils.CurrencyExchangeRate;

import java.math.BigDecimal;
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
        COMMAND_EXECUTION_FAILED,
        BALANCE_UPDATE,
    }

    private Type mType;
    private OtkData mData;
    private String mDesc;
    private CurrencyExchangeRate mCurrencyExRate = null;
    private Tx mTx;
    private BigDecimal mBalance;

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

    public OtkEvent(Type eventType, BigDecimal b, CurrencyExchangeRate rate) {
        mType = eventType;
        mBalance = b;
        mCurrencyExRate = rate;
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
    public OtkData getData() {return mData;}
    public CurrencyExchangeRate getCurrencyExRate() {return mCurrencyExRate;}
    public BigDecimal getBalance() {return mBalance;}

}
