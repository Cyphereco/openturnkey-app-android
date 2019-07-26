package com.cyphereco.openturnkey.core.protocol;

public enum LockState {
    INVALID("-1"),
    UNLOCKED("0"),
    LOCKED("1");

    private final String value;
    private LockState(String s) {
        value = s;
    }
    public String toString(){
        return value;
    }
}
