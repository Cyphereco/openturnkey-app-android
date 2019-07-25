package com.cyphereco.openturnkey.core;

import android.nfc.NdefMessage;
import android.nfc.Tag;
import android.util.Log;

public class Nfc {
    public static final String TAG = Nfc.class.getSimpleName();
    private static final int OTK_NFC_RECORD_MAX = 10;


    public class OtkCommand {

    }
    static OtkData read(Tag tag) {
        Log.d(TAG, "read tag");
        if (tag == null) {
            return null;
        }
        return null;
    }

    static OtkData read(NdefMessage[] ndefMessages) {
        Log.d(TAG, "read NdefMessage[] length:" + ndefMessages.length);

        // Get first item for now
        NdefMessage ndefMessage = ndefMessages[0];
        int recordLen = ndefMessage.getRecords().length;
        if (recordLen <= 0 || recordLen > OTK_NFC_RECORD_MAX) {
            Log.d(TAG, "Invalid record length:" + recordLen);
            return null;
        }
        for (int i = 0; i < recordLen; i++) {
            Log.d(TAG, "record[" + i + "]:" + new String(ndefMessage.getRecords()[i].getPayload()));
        }

        if (recordLen != 6) {
            Log.d(TAG, "record number is invalid:" + recordLen);
            return null;
        }

        return null;
    }

    static int writeCommand(OtkCommand cmd) {
        return Otk.OTK_RETURN_OK;
    }
}
