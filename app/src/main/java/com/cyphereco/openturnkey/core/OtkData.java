package com.cyphereco.openturnkey.core;

import android.util.Log;

import com.cyphereco.openturnkey.core.protocol.LockState;
import com.cyphereco.openturnkey.core.protocol.SessionData;

public class OtkData {
    private static final String TAG = OtkData.class.getSimpleName();

    String mAppUri;
    String mMintInfo;
    LockState mLockState;
    SessionData mSessionData;

    /**
     * Constructor
     * @param mintInfo
     * @param lockState
     * @param sd
     */
    public OtkData(String mintInfo, LockState lockState, SessionData sd) {
        Log.d(TAG, "mintInfo:" + mintInfo + " lockState:" + lockState.toString() + " SessionData:" + sd.toString());
        mMintInfo = mintInfo;
        mLockState = lockState;
        mSessionData = sd;
    }
}
