package com.cyphereco.openturnkey.core;

import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.util.Log;

import com.cyphereco.openturnkey.bitcoin.ECException;
import com.cyphereco.openturnkey.bitcoin.ECKey;
import com.cyphereco.openturnkey.core.protocol.Command;
import com.cyphereco.openturnkey.core.protocol.LockState;
import com.cyphereco.openturnkey.core.protocol.SessionData;
import com.cyphereco.openturnkey.utils.BtcUtils;

import java.io.IOException;

public class Nfc {
    public static final String TAG = Nfc.class.getSimpleName();

    private static final int OTK_NFC_RECORD_MAX = 10;
    private static final String OTK_NFC_DATA_APP_PKG_URI = "com.cyphereco.openturnkey";

    static private String mCommandId = "";
    static private Command mIssuedCommand = Command.INVALID;

    /**
     *
     * @param tag
     * @return
     */
    static OtkData read(Tag tag) {
        Log.d(TAG, "read tag");
        OtkData ret = null;
        if (tag == null) {
            return null;
        }
        Ndef ndef = Ndef.get(tag);
        try {
            ndef.connect();
            NdefMessage ndefMessage = ndef.getNdefMessage();
            ret = read(ndefMessage);
            ndef.close();
        }
        catch (IOException | FormatException | NullPointerException | IllegalStateException e) {
            e.printStackTrace();
            return ret;
        }

        return ret;
    }

    /**
     *
     * @param ndefMessages
     * @return
     */
    static OtkData read(NdefMessage[] ndefMessages) {
        Log.d(TAG, "read NdefMessage[] length:" + ndefMessages.length);
        // Get first item for now
        NdefMessage ndefMessage = ndefMessages[0];
        return read(ndefMessage);
    }

    /**
     * read and parse NdefMessage
     * @param ndefMessage
     * @return
     */
    static OtkData read(NdefMessage ndefMessage) {
        Log.d(TAG, "read NdefMessage");

        OtkData ret = null;
        Command issuedCommand = mIssuedCommand;
        mIssuedCommand = Command.INVALID;

        int recordLen = ndefMessage.getRecords().length;
        if (recordLen <= 0 || recordLen > OTK_NFC_RECORD_MAX) {
            Log.d(TAG, "Invalid record length:" + recordLen);
            return ret;
        }
        for (int i = 0; i < recordLen; i++) {
            Log.d(TAG, "record[" + i + "]:" + new String(ndefMessage.getRecords()[i].getPayload()));
        }

        if (recordLen != 6) {
            Log.d(TAG, "record number is invalid:" + recordLen);
            return ret;
        }

        // First record indicate what's the records for
        int i = 0;
        String message = new String(ndefMessage.getRecords()[i].getPayload());
        Log.d(TAG, "read NFC record[" + i + "]:" + message);

        if (!message.equals(OTK_NFC_DATA_APP_PKG_URI)) {
            Log.d(TAG, "Invalid NFC record. First one is not Appliction Packet URI.");
            return ret;
        }

        //Record 3 is the public key
        i = 3;
        message = new String(ndefMessage.getRecords()[i].getPayload()).substring(3);
        // Skip "en" for now
        String pubKey = message;

        //Record 4 is  session data
        // Example:
        //<Session_ID>
        //506518935
        //<BTC_Addr>
        //mzn6xGu2SpVmoziGnK5rtpQsZN5UPp34L2
        //...

        i = 4;
        message = new String(ndefMessage.getRecords()[i].getPayload()).substring(3);
        String sessData = message;

        //Record 5 is  signature
        i = 5;
        message = new String(ndefMessage.getRecords()[i].getPayload()).substring(3);
        if (false == verifySessionData(pubKey, sessData, message)) {
            Log.d(TAG, "Session data verify failed.");
            return ret;
        }

        // Record 1 is mint data
        i = 1;
        String mintInfo = new String(ndefMessage.getRecords()[i].getPayload()).substring(3);

        // Record 2 is lock state
        i = 2;
        LockState lockState;
        message = new String(ndefMessage.getRecords()[i].getPayload()).substring(3);
        if (message.equals(LockState.UNLOCKED.toString())) {
            lockState = LockState.UNLOCKED;
        }
        else if (message.equals(LockState.LOCKED.toString())) {
            lockState = LockState.LOCKED;
        }
        else {
            /* Should not be here. */
            Log.d(TAG, "Lock state is invalid.");
            lockState = LockState.INVALID;
        }

        // Parse address from session data
        SessionData sd = new SessionData(sessData);

        return new OtkData(mintInfo, lockState, sd);
    }

    static private boolean verifySessionData(String publicKey, String message, String signature) {
        byte[] pubKeyByte = BtcUtils.hexStringToBytes(publicKey);
        try {
            ECKey ecKey = new ECKey(pubKeyByte, null, true);
            return ecKey.verifySignature(message.getBytes(), signature);
        }
        catch (ECException e){
            Log.d(TAG, "ECException:" + e.toString());
            return false;
        }
    }

    static int writeCommand(Command cmd) {
        return Otk.OTK_RETURN_OK;
    }
}
