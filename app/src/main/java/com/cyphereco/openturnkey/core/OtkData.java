package com.cyphereco.openturnkey.core;

import android.util.Log;

import com.cyphereco.openturnkey.core.protocol.OtkState;
import com.cyphereco.openturnkey.core.protocol.SessionData;

public class OtkData {
    private static final String TAG = OtkData.class.getSimpleName();

    /* Data type. */
    public enum Type {
        OTK_DATA_TYPE_GENERAL_INFO,
        OTK_DATA_TYPE_SIGNATURE,
        OTK_DATA_TYPE_KEY_INFO,
    }
    private Type mType;
    String mAppUri;
    String mMintInfo;

    OtkState mOtkState;
    String mPublicKey;
    private SessionData mSessionData;

    public Type getType() {return mType;}
    public SessionData getSessionData() {return mSessionData;}
    public OtkState getOtkState() { return mOtkState;}

    /**
     * Constructor
     * @param mintInfo
     * @param otkState
     * @param sd
     */
    public OtkData(String mintInfo, OtkState otkState, String pubKey, SessionData sd) {
        Log.d(TAG, "mintInfo:" + mintInfo + "\notkState:" + otkState.toString() + "\nSessionData:" + sd.toString());
        mMintInfo = mintInfo;
        mOtkState = otkState;
        mSessionData = sd;
        mPublicKey = pubKey;
        // Determine data type
        if (sd.getRequestSigList() != null && sd.getRequestSigList().size() != 0) {
            mType = Type.OTK_DATA_TYPE_SIGNATURE;
        }
        else if (sd.getMasterExtKey() != null && sd.getMasterExtKey() != "") {
            mType = Type.OTK_DATA_TYPE_KEY_INFO;
        }
        else {
            mType = Type.OTK_DATA_TYPE_GENERAL_INFO;
        }
    }
}
