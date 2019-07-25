package com.cyphereco.openturnkey.core;

import android.content.Intent;
import android.nfc.NdefMessage;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;

/**
 * Otk is the main processor of openturnkey.
 */
public class Otk {
    public static final String TAG = Otk.class.getSimpleName();

    /** Return value. */
    public static final int OTK_RETURN_OK = 0;
    public static final int OTK_RETURN_ERROR = 1;

    /** The place holder for the single object. */
    private static Otk mOtk = null;

    /** Event listener. */
    OtkEventListener mEventListener = null;
    private Tag mCachedNfcTag = null;

    /**
     * Singleton retrieval of the OtkCoin.
     *
     * @return The singleton.
     */
    public static synchronized Otk getInstance() {
        Log.d(TAG, "getInstance()");
        if (null == mOtk) {
            mOtk = new Otk();
        }
        return mOtk;
    }

    /**
     * Method to process all NFC intents
     */
    public int processIntent(Intent intent) {
        Log.d(TAG, "process intent");
        NdefMessage[] msgs;

        Parcelable[] rawMsgs = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
        if (rawMsgs != null) {
            msgs = new NdefMessage[rawMsgs.length];
            for (int i = 0; i < rawMsgs.length; i++) {
                msgs[i] = (NdefMessage) rawMsgs[i];
            }
            OtkData data = Nfc.read(msgs);
            return OTK_RETURN_OK;
        }

        Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        if (tag != null) {
            OtkData data = Nfc.read(tag);
            mCachedNfcTag = tag;
            return OTK_RETURN_OK;
        }

        return OTK_RETURN_ERROR;
    }

    /**
     * Event listener interface
     */
    public interface OtkEventListener {
        void onOtkEvent(OtkEvent event);
    }

    /**
     * Event listener.
     */
    public int setEventListener(OtkEventListener eventListener) {
        mEventListener = eventListener;
        return OTK_RETURN_OK;
    }
}
