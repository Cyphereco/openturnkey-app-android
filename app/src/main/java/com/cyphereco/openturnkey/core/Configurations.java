package com.cyphereco.openturnkey.core;

import com.cyphereco.openturnkey.bitcoin.NetParams;

import java.io.IOException;
import java.math.BigDecimal;

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
    static Network mNetwork = Network.TESTNET;
    //static Network mNetwork = Network.MAINNET;

    // This value should be define according to OTK's definition
    static public int maxSignaturesPerCommand = 10;

    static public long txFeeLow = 2;
    static public long txFeeMid = 14;
    static public long txFeeHigh = 16;
    static public boolean isTestnet() {
        return (mNetwork == Network.TESTNET);
    }
}
