package com.cyphereco.openturnkey.core;

public class OtkEvent {
    /** Event type enum */
    public enum Type {
        INVALID,
        GENERAL_INFORMATION,
    }

    public Type mType;
    public OtkData mData;

    /**
     * Constructor for general info event
     */
    public OtkEvent(OtkData.GeneralInfo data) {
        mType = Type.GENERAL_INFORMATION;
        mData = data;
    }
}
