package com.cyphereco.openturnkey.db;

public class DBTransItem implements java.io.Serializable {
    private long id;
    private long datetime;
    private String payeeAddr;
    private int cryptoCurrency;
    private int cryptoCurrencyAmount;
    private int localCurrency;
    private int localCurrencyAmount;
    private int status;
    private String comment;
    private String rawData;

    public DBTransItem() {
        this.id = 0;
        this.datetime = 0;
        this.payeeAddr = "";
        this.cryptoCurrency = 0;
        this.cryptoCurrencyAmount = 0;
        this.localCurrency = 0;
        this.localCurrencyAmount = 0;
        this.status = 0;
        this.comment = "";
        this.rawData = "";
    }

    public DBTransItem(long id, long datetime, String payeeAddr, int cryptoCurrency,
                       int cryptoCurrencyAmount, int localCurrency, int localCurrencyAmount,
                       int status, String comment, String rawData) {
        this.id = id;
        this.datetime = datetime;
        this.payeeAddr = payeeAddr;
        this.cryptoCurrency = cryptoCurrency;
        this.cryptoCurrencyAmount = cryptoCurrencyAmount;
        this.localCurrency = localCurrency;
        this.localCurrencyAmount = localCurrencyAmount;
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

    public int getCryptoCurrency() {
        return cryptoCurrency;
    }

    public void setCryptoCurrency(int cryptoCurrency) {
        this.cryptoCurrency = cryptoCurrency;
    }

    public int getCryptoCurrencyAmount() {
        return cryptoCurrencyAmount;
    }

    public void setCryptoCurrencyAmount(int cryptoCurrencyAmount) {
        this.cryptoCurrencyAmount = cryptoCurrencyAmount;
    }

    public int getLocalCurrency() {
        return localCurrency;
    }

    public void setLocalCurrency(int localCurrency) {
        this.localCurrency = localCurrency;
    }

    public int getLocalCurrencyAmount() {
        return localCurrencyAmount;
    }

    public void setLocalCurrencyAmount(int localCurrencyAmount) {
        this.localCurrencyAmount = localCurrencyAmount;
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
