package com.cyphereco.openturnkey.db;

public class DBAddrItem implements java.io.Serializable {

    private long dbId;
    private String address;
    private String name;

    DBAddrItem() {
        dbId = 0;
        address = "";
        name = "";
    }

    public DBAddrItem(String addr, String name) {
        this.address = addr;
        this.name = name;
    }

    public long getDbId() {
        return dbId;
    }

    public void setDbId(long id) {
        dbId = id;
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
