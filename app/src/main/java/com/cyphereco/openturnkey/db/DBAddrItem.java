package com.cyphereco.openturnkey.db;

public class DBAddrItem implements java.io.Serializable {
    private String address;
    private String name;

    public DBAddrItem() {
        address = "";
        name = "";
    }

    public DBAddrItem(String addr, String name) {
        this.address = addr;
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String addr) {
        this.address = addr;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
