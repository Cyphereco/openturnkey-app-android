package com.cyphereco.openturnkey.core.protocol;

public enum Command {
    INVALID("00"),
    LOCK("160"),
    UNLOCK("161"),
    SHOW_KEY("162"),
    SIGN("163"),
    SET_KEY("164"),
    SET_PIN("165"),
    SET_NOTE("166"),
    CANCEL("167"),
    EXPORT_WIF_KEY("169");

    private final String value;
    private Command(String s) {
        value = s;
    }

    public String toString(){
        return value;
    }
}
