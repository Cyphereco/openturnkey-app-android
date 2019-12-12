package com.cyphereco.openturnkey.db;

public class DBTransItem implements java.io.Serializable {
    public static final String AMOUNT_UNIT_STRING_DEFAULT = "BTC";

    private long id;
    private long datetime;
    private String payeeAddr;
    private String payerAddr;
    private double amount;
    private String amountUnitString;
    private double fee;
    private String feeUnitString;
    private int status;
    private int confirmations;
    private String hash;
    private String comment;
    private String rawData;

    public DBTransItem() {
        this.id = 0;
        this.datetime = 0;
        this.hash = "";
        this.payerAddr = "";
        this.payeeAddr = "";
        this.amount = 0.0;
        this.fee = 0.0;
        this.status = 0;
        this.comment = "";
        this.rawData = "";
        this.confirmations = 0;
    }

    public DBTransItem(long id, long datetime, String hash, String payerAddr, String payeeAddr,
                       double cryptoCurrencyAmount, double fee, int status,
                       String comment, String rawData, int confirmations) {
        this.id = id;
        this.datetime = datetime;
        this.hash = hash;
        this.payerAddr = payerAddr;
        this.payeeAddr = payeeAddr;
        this.amount = cryptoCurrencyAmount;
        this.fee = fee;
        this.status = status;
        this.comment = comment;
        this.rawData = rawData;
        this.confirmations = confirmations;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getDatetime() {
        return datetime;
    }

    public void setDatetime(long datetime) {
        this.datetime = datetime;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public String getPayeeAddr() {
        return payeeAddr;
    }

    public void setPayeeAddr(String payeeAddr) {
        this.payeeAddr = payeeAddr;
    }

    public String getPayerAddr() {
        return payerAddr;
    }

    public void setPayerAddr(String payerAddr) {
        this.payerAddr = payerAddr;
    }

    public double getAmount() {
        return this.amount;
    }

    public double getFee() {
        return this.fee;
    }

    public void setAmount(double cryptoCurrencyAmount) {
        this.amount = cryptoCurrencyAmount;
    }

    public void setFee(double fee) {
        this.fee = fee;
    }

    public int getStatus() {
        return status;
    }
    public int getConfirmations() {
        return confirmations;
    }

    public void setStatus(int status) {
        this.status = status;
    }
    public void setConfrimations(int confirmations) {
        this.confirmations = confirmations;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getRawData() {
        return rawData;
    }

    public void setRawData(String rawData) {
        this.rawData = rawData;
    }

    public void setAmountUnitString(String unitString) {
        if ((null ==unitString) || ("" == unitString)) {
            unitString = AMOUNT_UNIT_STRING_DEFAULT;
        }
        this.amountUnitString = unitString;
    }

    public String getAmountUnitString() {
        if ((null == this.amountUnitString) || ("" == this.amountUnitString)) {
            return AMOUNT_UNIT_STRING_DEFAULT;
        }
        return this.amountUnitString;
    }

    public void setFeeUnitString(String unitString) {
        if ((null ==unitString) || ("" == unitString)) {
            unitString = AMOUNT_UNIT_STRING_DEFAULT;
        }
        this.feeUnitString = unitString;
    }

    public String getFeeUnitString() {
        return this.feeUnitString;
    }
}
