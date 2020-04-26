package com.cyphereco.openturnkey.db;

import com.blockcypher.model.transaction.Transaction;

import org.jetbrains.annotations.NotNull;

import static com.cyphereco.openturnkey.utils.BtcUtils.convertDateTimeStringToLong;

public class RecordTransaction implements java.io.Serializable {
    private long id;
    private long timestamp;
    private String hash;
    private String payer;
    private String payee;
    private double amountSent;
    private double amountRecv;
    private String rawData;
    private long blockHeight;
    private String exchangeRate;

    public RecordTransaction() {
        id = 0;
        timestamp = 0;
        hash = "";
        payer = "";
        payee = "";
        amountSent = 0;
        amountRecv = 0;
        rawData = "";
        blockHeight = -1;
        exchangeRate = null;
    }

    public RecordTransaction(Transaction transaction) {
        id = 0;
        timestamp = convertDateTimeStringToLong(transaction.getReceived());
        hash = transaction.getHash();
        payer = transaction.getAddresses().get(0);
        payee = (transaction.getAddresses().size() > 1) ? transaction.getAddresses().get(1) : transaction.getAddresses().get(0);
        amountSent = (transaction.getTotal().longValue() + transaction.getFees().longValue()) / 100000000d;
        amountRecv = transaction.getTotal().longValue() / 100000000d;
        rawData = transaction.getHex();
        blockHeight = transaction.getBlockHeight();
        exchangeRate = null;
    }

    public RecordTransaction(long timestamp,
                             String hash,
                             String payer,
                             String payee,
                             double amountSent,
                             double amountRecv,
                             String rawData,
                             long blockHeight,
                             String exchangeRate) {
        this.timestamp = timestamp;
        this.hash = hash;
        this.payer = payer;
        this.payee = payee;
        this.amountSent = amountSent;
        this.amountRecv = amountRecv;
        this.rawData = rawData;
        this.blockHeight = blockHeight;
        this.exchangeRate = exchangeRate;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        int curTimeLength = String.valueOf(System.currentTimeMillis()).length();
        int inTimeLength = String.valueOf(timestamp).length();
        if (inTimeLength - curTimeLength == 3) timestamp /= 1000;
        if (curTimeLength - inTimeLength == 3) timestamp *= 1000;
        this.timestamp = timestamp;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public String getPayer() {
        return payer;
    }

    public void setPayer(String payer) {
        this.payer = payer;
    }

    public String getPayee() {
        return payee;
    }

    public void setPayee(String payee) {
        this.payee = payee;
    }

    public double getAmountSent() {
        return amountSent;
    }

    public void setAmountSent(double amountSent) {
        this.amountSent = amountSent;
    }

    public double getAmountRecv() {
        return amountRecv;
    }

    public void setAmountRecv(double amountRecv) {
        this.amountRecv = amountRecv;
    }

    public String getRawData() {
        return rawData;
    }

    public void setRawData(String rawData) {
        this.rawData = rawData;
    }

    public long getBlockHeight() {
        return blockHeight;
    }

    public void setBlockHeight(long blockHeight) {
        this.blockHeight = blockHeight;
    }

    public String getExchangeRate() {
        return exchangeRate;
    }

    public void setExchangeRate(String exchangeRate) {
        this.exchangeRate = exchangeRate;
    }

    @NotNull
    @Override
    public String toString() {
        return "RecordTransaction{" +
                "id=" + id +
                ", timestamp=" + timestamp +
                ", hash='" + hash + '\'' +
                ", payeeAddr='" + payee + '\'' +
                ", payerAddr='" + payer + '\'' +
                ", amountSent=" + amountSent +
                ", amountRecv=" + amountRecv +
                ", rawData='" + rawData + '\'' +
                ", blockHeight=" + blockHeight +
                ", exchangeRate='" + exchangeRate + '\'' +
                '}';
    }
}
