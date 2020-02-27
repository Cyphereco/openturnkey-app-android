package com.cyphereco.openturnkey.core;

import com.cyphereco.openturnkey.utils.CurrencyExchangeRate;
import com.cyphereco.openturnkey.utils.TxFee;

import java.math.BigDecimal;

public class OtkEvent {

    /**
     * Event type enum
     */
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
        WRITE_NOTE_SUCCESS,
        WRITE_NOTE_FAIL,
        SET_PIN_SUCCESS,
        SET_PIN_FAIL,
        CHOOSE_KEY_SUCCESS,
        CHOOSE_KEY_FAIL,
        FIND_UTXO,
        GET_KEY_SUCCESS,
        GET_KEY_FAIL,
        SIGN_MESSAGE_SUCCESS,
        SIGN_MESSAGE_FAIL,
        OTK_PIN_UNSET,
        RESET_SUCCESS,
        RESET_FAIL,
        EXPORT_WIF_KEY_SUCCESS,
        EXPORT_WIF_KEY_FAIL,
        SESSION_TIMED_OUT,
        READ_RESPONSE_TIMED_OUT,
        CHECKING_BALANCE_FOR_PAYMENT,
        SESSION_ID_MISMATCH,
    }

    private Type mType;
    private OtkData mData;
    private String mDesc;
    private CurrencyExchangeRate mCurrencyExRate = null;
    private Tx mTx;
    private UnsignedTx mUnsignedTx;
    private BigDecimal mBalance;
    private TxFee mTxFee;
    private boolean mUsingMasterKey;

    /**
     * Constructor for general info event
     */
    OtkEvent(Type eventType, OtkData data) {
        mType = eventType;
        mData = data;
    }

    OtkEvent(Type eventType, OtkData data, String msgToSign, boolean usingMasterKey) {
        mType = eventType;
        mData = data;
        mDesc = msgToSign;
        mUsingMasterKey = usingMasterKey;
    }

    OtkEvent(Type eventType, String desc) {
        mType = eventType;
        mDesc = desc;
    }

    OtkEvent(Type eventType, String address, BigDecimal b, CurrencyExchangeRate rate) {
        mType = eventType;
        mBalance = b;
        mDesc = address;
        mCurrencyExRate = rate;
    }

    OtkEvent(Type eventType, Tx tx) {
        mType = eventType;
        mTx = tx;
    }

    OtkEvent(UnsignedTx tx) {
        mType = Type.UNSIGNED_TX;
        mUnsignedTx = tx;
    }

    OtkEvent(Type eventType) {
        mType = eventType;
    }

    OtkEvent(Type eventType, CurrencyExchangeRate currencyExRate) {
        mType = eventType;
        mCurrencyExRate = currencyExRate;
    }

    OtkEvent(Type eventType, TxFee txFee) {
        mType = eventType;
        mTxFee = txFee;
    }

    public Type getType() {
        return mType;
    }

    public String getRecipientAddress() {
        return mDesc;
    }

    public String getAddress() {
        return mDesc;
    }

    public String getFailureReason() {
        return mDesc;
    }

    public String getMessageToSign() {
        return mDesc;
    }

    public boolean getUsingMasterKey() {
        return mUsingMasterKey;
    }

    public Tx getTx() {
        return mTx;
    }

    public UnsignedTx getUnsignedTx() {
        return mUnsignedTx;
    }

    public OtkData getData() {
        return mData;
    }

    public CurrencyExchangeRate getCurrencyExRate() {
        return mCurrencyExRate;
    }

    public BigDecimal getBalance() {
        return mBalance;
    }

    public TxFee getTxFee() {
        return mTxFee;
    }

}
