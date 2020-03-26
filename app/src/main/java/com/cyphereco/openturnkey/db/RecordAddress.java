package com.cyphereco.openturnkey.db;

public class RecordAddress implements java.io.Serializable {

    private long id;
    private String address;
    private String alias;

    RecordAddress() {
        id = 0;
        address = "";
        alias = "";
    }

    public RecordAddress(String addr, String alias) {
        this.address = addr;
        this.alias = alias;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String addr) {
        this.address = addr;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }
}
