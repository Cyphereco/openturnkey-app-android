package com.cyphereco.openturnkey.core;

import com.cyphereco.openturnkey.bitcoin.NetParams;

import java.io.IOException;
import java.math.BigDecimal;

public class Configurations {

    public enum Network {
        MAINNET,
        TESTNET,
    }

    // Set this for main net or test net
    static Network mNetwork = Network.TESTNET;
    //static Network mNetwork = Network.MAINNET;
    // Tx fees level in satoshis/byte. Value TBD.
    static long txFeesLow = 100;
    static long txFeesMid = 250;
    static long txFeesHigh = 500;

    static public boolean isTestnet() {
        return (mNetwork == Network.TESTNET);
    }
}
