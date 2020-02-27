package com.cyphereco.openturnkey.core;

import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.Tag;
import android.nfc.tech.Ndef;

import com.cyphereco.openturnkey.utils.Log4jHelper;

import org.slf4j.Logger;

import com.cyphereco.openturnkey.bitcoin.ECException;
import com.cyphereco.openturnkey.bitcoin.ECKey;
import com.cyphereco.openturnkey.core.protocol.Command;
import com.cyphereco.openturnkey.core.protocol.OtkState;
import com.cyphereco.openturnkey.core.protocol.SessionData;
import com.cyphereco.openturnkey.utils.BtcUtils;


import java.io.IOException;
import java.util.List;
import java.util.Random;

public class Nfc {
    public static final String TAG = Nfc.class.getSimpleName();
    private static Logger logger = Log4jHelper.getLogger(TAG);

    private static final String OTK_NFC_DATA_APP_PKG_URI = "com.cyphereco.openturnkey";
    private static final String OTK_REQUEST_DATA_DELIM = "\n";

    static private int mSessionId = 0;
    static private int mRequestId = 0;

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


    static OtkData makeRequest(Tag tag, Command cmd, String pin, List<String> args, boolean hasMore, boolean useMaster) {
        logger.debug("NFC make reqeust");
        OtkData otkData = null;

        if (tag != null) {
            Ndef ndef = Ndef.get(tag);
            if (ndef != null) {
                try {
                    ndef.connect();
                    NdefMessage ndefMessage = ndef.getNdefMessage();
                    otkData = parseOtkData(ndefMessage);

                    if (otkData == null) {
                        logger.info("Not a valid OpenTurnKey");
                        return null;
                    }

                    OtkState otkState = otkData.getOtkState();
                    String sessionId = otkData.getSessionData().getSessionId();
                    String requestId = otkData.getSessionData().getRequestId();

                    if (otkState.getExecutionState() != OtkState.ExecutionState.NFC_CMD_EXEC_NA) {
                        logger.debug("Received a execution response, run sanity check.");

                        if (Integer.parseInt(sessionId) != mSessionId) {
                            logger.info("");
                        }

                        mRequestId = 0;
                        mSessionId = 0;
                        return otkData;
                    }
                    int command = Integer.parseInt(cmd.toString());
                    if (command < 161 || command > 169) {
                        logger.debug("No request command, return read data.");
                        return otkData;
                    }



                    ndef.close();
                } catch (IOException | FormatException | NullPointerException | IllegalStateException e) {
                    e.printStackTrace();
                    return null;
                }
            }
        }

        return null;
    }

    static OtkData read(Tag tag) {
        logger.debug("NFC read tag");
        if (tag != null) {
            Ndef ndef = Ndef.get(tag);
            if (ndef != null) {
                try {
                    ndef.connect();
                    NdefMessage ndefMessage = ndef.getNdefMessage();
                    ndef.close();
                    return parseOtkData(ndefMessage);
                } catch (IOException | FormatException | NullPointerException | IllegalStateException e) {
                    e.printStackTrace();
                    return null;
                }
            }
        }
        logger.error("Null tag");
        return null;
    }

    static OtkData read(NdefMessage[] ndefMessages) {
        logger.error("We don't process NdefMessage read for now. NdefMessage: " + ndefMessages);
        return null;
//        logger.debug("read NdefMessage[] length:" + ndefMessages.length);
//        // Get first item for now
//        NdefMessage ndefMessage = ndefMessages[0];
//        return parseOtkData(ndefMessage);
    }

    private static OtkData parseOtkData(NdefMessage ndefMessage) {
        logger.debug("parse NdefMessage");

        if (ndefMessage.getRecords().length != NUM_OF_OTK_RECORDS) {
            logger.debug("Not a valid OpenTurnKey - incorrect records too less or more");
            return null;
        }

        for (int i = 0; i < NUM_OF_OTK_RECORDS; i++) {
            logger.debug("NDEF Record[" + i + "]:\n" + new String(ndefMessage.getRecords()[i].getPayload()).substring(3));
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

    static private boolean verifySessionData(String publicKey, String message, String signature) {
        byte[] pubKeyByte = BtcUtils.hexStringToBytes(publicKey);
        try {
            ECKey ecKey = new ECKey(pubKeyByte, null, true);
            return ecKey.verifySignature(message.getBytes(), signature);
        } catch (ECException e) {
            logger.error("ECException:" + e.toString());
            return false;
        }
    }

    static int writeCommand(Tag tag, Command cmd, String sessId, String pin, List<String> args, boolean isMore, boolean usingMasterKey) {
        logger.debug("NFC writeCommand (" + cmd.toString() + ")");
        if (tag != null) {
            Ndef ndef = Ndef.get(tag);
            if (ndef != null) {
                NdefRecord[] record = new NdefRecord[NUM_OF_REQUET_RECORDS];

                Random r = new Random();
                mRequestId = r.nextInt();
                while (mRequestId <= 0) {
                    mRequestId = r.nextInt();
                }

                record[RECORD_REQUET_TYPE_SESSION_ID] = NdefRecord.createTextRecord("en", sessId);
                record[RECORD_REQUET_TYPE_REQUEST_ID] = NdefRecord.createTextRecord("en", String.valueOf(mRequestId));
                record[RECORD_REQUET_TYPE_COMMAND] = NdefRecord.createTextRecord("en", cmd.toString());

                String requestData = "";
                for (int i = 0; i < args.size(); i++) {
                    if (i == 0) {
                        requestData += args.get(i);
                    } else {
                        requestData += OTK_REQUEST_DATA_DELIM + args.get(i);
                    }
                }
                record[RECORD_REQUET_TYPE_DATA] = NdefRecord.createTextRecord("en", requestData);

                String options = "pin=" + pin + OTK_REQUEST_DATA_DELIM;
                options = usingMasterKey ? options + "key=1" + OTK_REQUEST_DATA_DELIM : options;
                options = isMore ? options + "more=1" : options;
                record[RECORD_REQUET_TYPE_OPTION] = NdefRecord.createTextRecord("en", options);

                for (int i = 0; i < NUM_OF_REQUET_RECORDS; i++) {
                    logger.debug("Request Record[" + i + "]:\n" + new String(record[i].getPayload()).substring(3));
                }

                try {
                    ndef.connect();
                    ndef.writeNdefMessage(new NdefMessage(record));
                    ndef.close();

                    return Otk.OTK_RETURN_OK;
                } catch (IOException | FormatException | IllegalStateException e) {
                    logger.error("Write command exception:" + e);
                    return Otk.OTK_RETURN_ERROR;
                }
            }
        }
        return Otk.OTK_RETURN_ERROR;
    }
}
