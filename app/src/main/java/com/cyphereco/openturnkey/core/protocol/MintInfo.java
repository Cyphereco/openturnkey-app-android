package com.cyphereco.openturnkey.core.protocol;

import org.jetbrains.annotations.NotNull;

import java.io.Serializable;

/**
 * Example:
 * Key Mint: Cyphereco OU
 * Mint Date: 2019/06/09
 * H/W Version: 1.2
 * F/W Version: 1.1.10
 * Serial No.: b7022ddc25
 * Battery Level: 10% / 620 mV
 * Note:
 */
public class MintInfo implements Serializable {

    private static final String CRLF = "\r\n";
    private static final String MINT_INFO_KEY_MINT = "Key Mint: ";
    private static final String MINT_INFO_MINT_DATE = "Mint Date: ";
    private static final String MINT_INFO_HW_VERSION = "H/W Version: ";
    private static final String MINT_INFO_FW_VERSION = "F/W Version: ";
    private static final String MINT_INFO_SERIAL_NO = "Serial No.: ";
    private static final String MINT_INFO_BATTERY_LEVEL = "Battery Level: ";
    private static final String MINT_INFO_NOTE = "Note: \r\n";

    private String mKeyMint;
    private String mMintDate;
    private String mHwVersion;
    private String mFwVersion;
    private String mSerialNo;
    private String mBatteryLevel;
    private int mBatteryPercentage;
    private String mNote;

    public MintInfo(String info) {
        int labelStart;
        // Key mint
        labelStart = info.indexOf(MINT_INFO_KEY_MINT);
        if (labelStart != -1) {
            int dataStart = labelStart + MINT_INFO_KEY_MINT.length();
            int crlf = info.indexOf(CRLF, dataStart);
            mKeyMint = info.substring(dataStart, crlf);
        }

        // Mint data
        labelStart = info.indexOf(MINT_INFO_MINT_DATE);
        if (labelStart != -1) {
            int dataStart = labelStart + MINT_INFO_MINT_DATE.length();
            int crlf = info.indexOf(CRLF, dataStart);
            mMintDate = info.substring(dataStart, crlf);
        }

        // HW Ver
        labelStart = info.indexOf(MINT_INFO_HW_VERSION);
        if (labelStart != -1) {
            int dataStart = labelStart + MINT_INFO_HW_VERSION.length();
            int crlf = info.indexOf(CRLF, dataStart);
            mHwVersion = info.substring(dataStart, crlf);
        }

        // FW Ver
        labelStart = info.indexOf(MINT_INFO_FW_VERSION);
        if (labelStart != -1) {
            int dataStart = labelStart + MINT_INFO_FW_VERSION.length();
            int crlf = info.indexOf(CRLF, dataStart);
            mFwVersion = info.substring(dataStart, crlf);
        }

        // Serial No.
        labelStart = info.indexOf(MINT_INFO_SERIAL_NO);
        if (labelStart != -1) {
            int dataStart = labelStart + MINT_INFO_SERIAL_NO.length();
            int crlf = info.indexOf(CRLF, dataStart);
            mSerialNo = info.substring(dataStart, crlf);
        }

        // Battery Level
        labelStart = info.indexOf(MINT_INFO_BATTERY_LEVEL);
        if (labelStart != -1) {
            int dataStart = labelStart + MINT_INFO_BATTERY_LEVEL.length();
            int percent = info.indexOf("%", dataStart);
            int crlf = info.indexOf(CRLF, dataStart);
            mBatteryLevel = info.substring(dataStart, crlf);
            mBatteryPercentage = Integer.parseInt(info.substring(dataStart, percent));
        }

        // Note
        labelStart = info.indexOf(MINT_INFO_NOTE);
        if (labelStart != -1) {
            int dataStart = labelStart + MINT_INFO_NOTE.length();
            mNote = info.substring(dataStart);
        }
    }

    @NotNull
    public String toString() {
        return "Key Mint: " + mKeyMint + "\n" +
                "Mint Date: " + mMintDate + "\n" +
                "H/W Version: " + mHwVersion + "\n" +
                "F/W Version: " + mFwVersion + "\n" +
                "Serial No.: " + mSerialNo + "\n" +
                "Battery Level: " + mBatteryLevel + "\n" +
                "Note: " + mNote;
    }

    public int getBatteryLevel() {
        return mBatteryPercentage;
    }

    public String getNote() {
        return mNote;
    }
}
