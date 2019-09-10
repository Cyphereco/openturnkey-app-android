package com.cyphereco.openturnkey.core.protocol;

import java.io.Serializable;

/**
 * Example:
 *     Key Mint: Cyphereco OU
 *     Mint Date: 2019/06/09
 *     H/W Version: 1.2
 *     F/W Version: 1.1.10
 *     Serial No.: b7022ddc25
 *     Battery Level: 10% / 620 mV
 *     Note: 
 */
public class MintInfo implements Serializable {
    private static final String TAG = MintInfo.class.getSimpleName();

    private static final String CRLF = "\r\n";
    private static final String MINT_INFO_KEY_MINT = "Key Mint: ";
    private static final String MINT_INFO_MINT_DATE = "Mint Date: ";
    private static final String MINT_INFO_HW_VERSION = "H/W Version: ";
    private static final String MINT_INFO_FW_VERSION = "F/W Version: ";
    private static final String MINT_INFO_SERIAL_NO = "Serial No.: ";
    private static final String MINT_INFO_BATTERY_LEVEL = "Battery Level: ";
    private static final String MINT_INFO_NOTE = "Note: \r\n";

    String mKeyMint;
    String mMintDate;
    String mHwVersion;
    String mFwVersion;
    String mSeialNo;
    String mBatteryLevel;
    int mBatteryPercentage;
    String mNote;

    public MintInfo(String info) {
        int lableStart;
        // Key mint
        lableStart = info.indexOf(MINT_INFO_KEY_MINT);
        if (lableStart != -1) {
            int dataStart = lableStart + MINT_INFO_KEY_MINT.length();
            int crlf = info.indexOf(CRLF, dataStart);
            mKeyMint = info.substring(dataStart, crlf);
        }

        // Mint data
        lableStart = info.indexOf(MINT_INFO_MINT_DATE);
        if (lableStart != -1) {
            int dataStart = lableStart + MINT_INFO_MINT_DATE.length();
            int crlf = info.indexOf(CRLF, dataStart);
            mMintDate = info.substring(dataStart, crlf);
        }

        // HW Ver
        lableStart = info.indexOf(MINT_INFO_HW_VERSION);
        if (lableStart != -1) {
            int dataStart = lableStart + MINT_INFO_HW_VERSION.length();
            int crlf = info.indexOf(CRLF, dataStart);
            mHwVersion = info.substring(dataStart, crlf);
        }

        // FW Ver
        lableStart = info.indexOf(MINT_INFO_FW_VERSION);
        if (lableStart != -1) {
            int dataStart = lableStart + MINT_INFO_FW_VERSION.length();
            int crlf = info.indexOf(CRLF, dataStart);
            mFwVersion = info.substring(dataStart, crlf);
        }

        // Serial No.
        lableStart = info.indexOf(MINT_INFO_SERIAL_NO);
        if (lableStart != -1) {
            int dataStart = lableStart + MINT_INFO_SERIAL_NO.length();
            int crlf = info.indexOf(CRLF, dataStart);
            mSeialNo = info.substring(dataStart, crlf);
        }

        // Battery Level
        lableStart = info.indexOf(MINT_INFO_BATTERY_LEVEL);
        if (lableStart != -1) {
            int dataStart = lableStart + MINT_INFO_BATTERY_LEVEL.length();
            int percent = info.indexOf("%", dataStart);
            int crlf = info.indexOf(CRLF, dataStart);
            mBatteryLevel = info.substring(dataStart, crlf);
            mBatteryPercentage = Integer.valueOf(info.substring(dataStart, percent));
        }

        // Note
        lableStart = info.indexOf(MINT_INFO_NOTE);
        if (lableStart != -1) {
            int dataStart = lableStart + MINT_INFO_NOTE.length();
            mNote = info.substring(dataStart);
        }
    }

    public String toString() {
        String s =  "Key Mint: " + mKeyMint + "\n" +
                    "Mint Date: " + mMintDate + "\n" +
                    "H/W Version: " + mHwVersion + "\n" +
                    "F/W Version: "+ mFwVersion + "\n" +
                    "Serial No.: " + mSeialNo + "\n" +
                    "Battery Level: " + mBatteryLevel + "\n" +
                    "Note: " + mNote;
        return s;
    }

    public int getBatteryLevel() { return mBatteryPercentage;}
    public String getNote() { return mNote;}
}
