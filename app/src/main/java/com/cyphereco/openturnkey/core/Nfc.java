package com.cyphereco.openturnkey.core;

import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.util.Log;

import com.cyphereco.openturnkey.bitcoin.ECException;
import com.cyphereco.openturnkey.bitcoin.ECKey;
import com.cyphereco.openturnkey.core.protocol.Command;
import com.cyphereco.openturnkey.core.protocol.OtkState;
import com.cyphereco.openturnkey.core.protocol.SessionData;
import com.cyphereco.openturnkey.utils.BtcUtils;
import com.cyphereco.openturnkey.utils.Log4jHelper;

import org.slf4j.Logger;

import java.io.IOException;
import java.util.List;
import java.util.Random;

public class Nfc {
    public static final String TAG = Nfc.class.getSimpleName();
    private static Logger logger = Log4jHelper.getLogger(TAG);
    
    private static final int OTK_NFC_RECORD_MAX = 10;
    private static final String OTK_NFC_DATA_APP_PKG_URI = "com.cyphereco.openturnkey";
    private static final String OTK_REQUEST_DATA_DELIM = "\n";

    static private String mCommandId = "";
    static private Command mIssuedCommand = Command.INVALID;

    /**
     *
     * @param tag
     * @return
     */
    static OtkData read(Tag tag) {
        logger.info("read tag");
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
        logger.info("read NdefMessage[] length:" + ndefMessages.length);
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
        logger.info("read NdefMessage");

        OtkData ret = null;
        Command issuedCommand = mIssuedCommand;
        mIssuedCommand = Command.INVALID;

        int recordLen = ndefMessage.getRecords().length;
        if (recordLen <= 0 || recordLen > OTK_NFC_RECORD_MAX) {
            logger.error("Invalid record length:" + recordLen);
            return ret;
        }
        for (int i = 0; i < recordLen; i++) {
            logger.info("record[" + i + "]:" + new String(ndefMessage.getRecords()[i].getPayload()));
        }

        if (recordLen != 6) {
            logger.info("record number is invalid:" + recordLen);
            return ret;
        }

        // First record indicate what's the records for
        int i = 0;
        String message = new String(ndefMessage.getRecords()[i].getPayload());
        logger.info("read NFC record[" + i + "]:" + message);

        if (!message.equals(OTK_NFC_DATA_APP_PKG_URI)) {
            logger.error("Invalid NFC record. First one is not Appliction Packet URI.");
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
            logger.error("Session data verify failed.");
            return ret;
        }

        // Record 1 is mint data
        i = 1;
        String mintInfo = new String(ndefMessage.getRecords()[i].getPayload()).substring(3);

        // Record 2 is lock state
        i = 2;

        message = new String(ndefMessage.getRecords()[i].getPayload()).substring(3);
        OtkState otkState = new OtkState(message);

        // Parse address from session data
        SessionData sd = new SessionData(sessData);

        return new OtkData(mintInfo, otkState, pubKey, sd);
    }

    static private boolean verifySessionData(String publicKey, String message, String signature) {
        byte[] pubKeyByte = BtcUtils.hexStringToBytes(publicKey);
        try {
            ECKey ecKey = new ECKey(pubKeyByte, null, true);
            return ecKey.verifySignature(message.getBytes(), signature);
        }
        catch (ECException e){
            logger.error("ECException:" + e.toString());
            return false;
        }
    }

    static int writeCommand(Tag tag, Command cmd, String sessId, String pin, List<String> args, boolean isMore, boolean usingMasterKey) {
        logger.info("write Command:" + cmd.toString());
        mIssuedCommand = Command.INVALID;
        if (tag == null) {
            logger.error("NFC tag is null");
            return Otk.OTK_RETURN_ERROR;
        }
        Ndef ndef = Ndef.get(tag);
        int recordNum = 3;

        if (args != null && args.size() > 0) {
            logger.info("args:" + args.toString());
        }
        // request data is required
        recordNum += 1;

        if (pin != null && pin.length() > 0) {
            recordNum += 1;
        }

        if (recordNum > OTK_NFC_RECORD_MAX) {
            logger.error("Too many record numbers " + recordNum);
            return Otk.OTK_RETURN_ERROR;
        }
        NdefRecord record[] = new NdefRecord[recordNum];
        Random r = new Random();
        int rdn = r.nextInt();
        if (rdn < 0) {
            rdn = -rdn;
        }
        mCommandId = String.valueOf(rdn);
        logger.info("mCommandId:" + mCommandId);
        if (ndef == null) {
            return Otk.OTK_RETURN_ERROR;
        }
        try {
            ndef.connect();
            // 1:session id
            record[0] = NdefRecord.createTextRecord("en", sessId);
            // 2: command id
            record[1] = NdefRecord.createTextRecord("en", String.valueOf(mCommandId));
            // 3: command
            record[2] = NdefRecord.createTextRecord("en", cmd.toString());

            int idx = 3;
            // 4: request data
            String requestData = "";

            if (args.size() > 0) {
                // Separate by '\n'
                for (int i = 0; i < args.size(); i++) {
                    if (i == 0) {
                        requestData += args.get(i);
                    }
                    else {
                        requestData += OTK_REQUEST_DATA_DELIM + args.get(i);
                    }
                }
                logger.info("requestData:" + requestData);
                record[idx] = NdefRecord.createTextRecord("en", requestData);
                idx++;
            }
            else {
                requestData = "OpenTurnKey";
                logger.info("requestData:" + requestData);
                record[idx] = NdefRecord.createTextRecord("en", requestData);
                idx++;
            }

            // 5 Options
            String options = "";
            // using master key
            if (usingMasterKey) {
                logger.info("usgin master key");
                options += "key=1" + OTK_REQUEST_DATA_DELIM;
            }
            // more
            if (isMore) {
                logger.info("more data to write");
                options += "more=1" + OTK_REQUEST_DATA_DELIM;

            }
            // pin
            if (pin != null && pin.length() > 0) {
                logger.info("pin=" + pin);
                options += "pin=" + pin + OTK_REQUEST_DATA_DELIM;
            }

            if (options.length() > 0) {
                // remove last delimiter
                options.substring(0, options.length() - 1);
                record[idx] = NdefRecord.createTextRecord("en", options);
            }

            ndef.writeNdefMessage(new NdefMessage(record));
            mIssuedCommand = cmd;
            ndef.close();
        } catch (IOException | FormatException | IllegalStateException e) {
            logger.error("Write command exception:" + e);
            return Otk.OTK_RETURN_ERROR;
        }
        return Otk.OTK_RETURN_OK;
    }
}
