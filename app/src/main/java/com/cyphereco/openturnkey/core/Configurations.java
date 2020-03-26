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

    public static long txFeeLow = 2;
    public static long txFeeMid = 4;
    public static long txFeeHigh = 5;
    public static boolean writeLogToFile = true;

    /* Background update task intervals, Unit = minute */
    public static final int INTERVAL_EXCHANGE_RATE_UPDATE = 5;
    public static final int INTERVAL_TX_FEE_UPDATE = 10;
    public static final int INTERVAL_DB_UPDATE = 10;

    public static void setNetwork(Network net) {
        network = net;
    }
}
