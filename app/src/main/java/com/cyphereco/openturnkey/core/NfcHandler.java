package com.cyphereco.openturnkey.core;

import android.content.Intent;
import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.os.Parcelable;

import com.cyphereco.openturnkey.bitcoin.ECException;
import com.cyphereco.openturnkey.bitcoin.ECKey;
import com.cyphereco.openturnkey.core.protocol.OtkRequest;
import com.cyphereco.openturnkey.core.protocol.OtkState;
import com.cyphereco.openturnkey.core.protocol.SessionData;
import com.cyphereco.openturnkey.utils.BtcUtils;
import com.cyphereco.openturnkey.utils.Log4jHelper;

import org.slf4j.Logger;

import java.io.IOException;

public class NfcHandler {
    public static final String TAG = NfcHandler.class.getSimpleName();
    private static Logger logger = Log4jHelper.getLogger(TAG);

    private static final String OTK_NFC_DATA_APP_PKG_URI = "com.cyphereco.openturnkey";

    private static final int RECORD_TYPE_APP_URI = 0;
    private static final int RECORD_TYPE_MINT_INFO = 1;
    private static final int RECORD_TYPE_OTK_STATE = 2;
    private static final int RECORD_TYPE_PUBLIC_KEY = 3;
    private static final int RECORD_TYPE_SESSION_DATA = 4;
    private static final int RECORD_TYPE_SESSION_HASH = 5;
    private static final int NUM_OF_OTK_RECORDS = 6;

    private static final int RECORD_REQUET_TYPE_SESSION_ID = 0;
    private static final int RECORD_REQUET_TYPE_REQUEST_ID = 1;
    private static final int RECORD_REQUET_TYPE_COMMAND = 2;
    private static final int RECORD_REQUET_TYPE_DATA = 3;
    private static final int RECORD_REQUET_TYPE_OPTION = 4;
    private static final int NUM_OF_REQUET_RECORDS = 5;

    public static String sendRequest(Intent intent, OtkRequest request) {
        logger.info("\n" + request.toString());
        Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);

        if (tag != null) {
            Ndef ndef = Ndef.get(tag);
            if (ndef != null) {

                NdefRecord[] records = new NdefRecord[NUM_OF_REQUET_RECORDS];

                records[RECORD_REQUET_TYPE_SESSION_ID] = NdefRecord.createTextRecord("en", request.getSessionId());
                records[RECORD_REQUET_TYPE_REQUEST_ID] = NdefRecord.createTextRecord("en", request.getRequestId());
                records[RECORD_REQUET_TYPE_COMMAND] = NdefRecord.createTextRecord("en", request.getCommand());
                records[RECORD_REQUET_TYPE_DATA] = NdefRecord.createTextRecord("en", request.getData());
                records[RECORD_REQUET_TYPE_OPTION] = NdefRecord.createTextRecord("en", request.getOption());
//                for (int i = 0; i < NUM_OF_REQUET_RECORDS; i++) {
//                    logger.debug("Request Record[" + i + "]:\n" + new String(records[i].getPayload()).substring(3));
//                }
                try {
                    ndef.connect();
                    ndef.writeNdefMessage(new NdefMessage(records));
                    ndef.close();

                    logger.debug("OTK request sent.");
                    return request.getSessionId();
                } catch (IOException | FormatException | IllegalStateException e) {
                    logger.error("Write command exception:" + e);
                    return "0";
                }
            }
        }

        return "0";
    }

    public static OtkData parseIntent(Intent intent) {

        Parcelable[] ndefMessages = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);

        if (ndefMessages.length == 1) {
            return parseNdefMessage((NdefMessage) ndefMessages[0]);
        }

        return null;
    }

    private static OtkData parseNdefMessage(NdefMessage ndefMessage) {
        logger.debug("parse NdefMessage");

        if (ndefMessage.getRecords().length != NUM_OF_OTK_RECORDS) {
            logger.error("Not a valid OpenTurnKey - incorrect records too less or more");
            return null;
        }

        String uri = new String(ndefMessage.getRecords()[RECORD_TYPE_APP_URI].getPayload());
        if (!uri.equals(OTK_NFC_DATA_APP_PKG_URI)) {
            logger.error("Incorrect app URI, not a valid OpenTurnKey.");
            return null;
        }

        String mintInfo = new String(ndefMessage.getRecords()[RECORD_TYPE_MINT_INFO].getPayload()).substring(3);
        OtkState otkState = new OtkState(new String(ndefMessage.getRecords()[RECORD_TYPE_OTK_STATE].getPayload()).substring(3));
        String pubKey = new String(ndefMessage.getRecords()[RECORD_TYPE_PUBLIC_KEY].getPayload()).substring(3);

        String sessData = new String(ndefMessage.getRecords()[RECORD_TYPE_SESSION_DATA].getPayload()).substring(3);
        String hash = new String(ndefMessage.getRecords()[RECORD_TYPE_SESSION_HASH].getPayload()).substring(3);

        if (!verifySessionData(pubKey, sessData, hash)) {
            logger.error("Session hash validated failed");
            return null;
        }

        SessionData sessionData = new SessionData(sessData);

        return new OtkData(mintInfo, otkState, pubKey, sessionData);
    }

    private static boolean verifySessionData(String publicKey, String message, String signature) {
        byte[] pubKeyByte = BtcUtils.hexStringToBytes(publicKey);
        try {
            ECKey ecKey = new ECKey(pubKeyByte, null, true);
            return ecKey.verifySignature(message.getBytes(), signature);
        } catch (ECException e) {
            logger.error("ECException:" + e.toString());
            return false;
        }
    }
}
