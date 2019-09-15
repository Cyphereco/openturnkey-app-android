package com.cyphereco.openturnkey.db;

public class DBTransItem implements java.io.Serializable {
    private long id;
    private long datetime;
    private String payeeAddr;
    private String payerAddr;
    private double amount;
    private double fee;
    private int status;
    private String comment;
    private String rawData;

    public DBTransItem() {
        this.id = 0;
        this.datetime = 0;
        this.payerAddr = "";
        this.payeeAddr = "";
        this.amount = 0.0;
        this.fee = 0.0;
        this.status = 0;
        this.comment = "";
        this.rawData = "";
    }

    public DBTransItem(long id, long datetime, String payerAddr, String payeeAddr,
                       double cryptoCurrencyAmount, double fee, int status, String comment, String rawData) {
        this.id = id;
        this.datetime = datetime;
        this.payerAddr = payerAddr;
        this.payeeAddr = payeeAddr;
        this.amount = cryptoCurrencyAmount;
        this.fee = fee;
        this.status = status;
        this.comment = comment;
        this.rawData = rawData;
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

    public void setStatus(int status) {
        this.status = status;
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
}
