package com.cyphereco.openturnkey.core.protocol;

public enum Command {
    INVALID("0"),
    LOCK("160"),
    UNLOCK("161"),
    SHOW_KEY("162"),
    SIGN("163"),
    SET_KEY("164"),
    SET_PIN("165"),
    PRE_AUTH_WITH_PIN("166");

    private final String value;
    private Command(String s) {
        value = s;
    }

    public String toString(){
        return value;
    }
}
