package com.cyphereco.openturnkey.core;

public class Configurations {

    public enum Network {
        MAINNET,
        TESTNET,
    }

    public enum TxFeeType {
        HIGH,
        MID,
        LOW,
        CUSTOMIZED
    }

    // Set this for main net or test net
    static public Network network = Network.MAINNET;
    //static Network mNetwork = Network.TESTNET;

    // This value should be define according to OTK's definition
    static int maxSignaturesPerCommand = 10;

    static public long txFeeLow = 2;
    static public long txFeeMid = 14;
    static public long txFeeHigh = 16;
    static public boolean writeLogToFile = true;

    /* Background update task intervals, Unit = minute */
    static final int INTERVAL_EXCHANGE_RATE_UPDATE = 5;
    static final int INTERVAL_TX_FEE_UPDATE = 10;
    static final int INTERVAL_DB_UPDATE = 10;

    static public void setNetwork(Network net) {
        network = net;
    }
}
