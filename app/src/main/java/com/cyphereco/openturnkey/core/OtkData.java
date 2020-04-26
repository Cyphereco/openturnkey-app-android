package com.cyphereco.openturnkey.core;

import com.cyphereco.openturnkey.core.protocol.MintInfo;
import com.cyphereco.openturnkey.core.protocol.OtkState;
import com.cyphereco.openturnkey.core.protocol.SessionData;

import org.jetbrains.annotations.NotNull;

import java.io.Serializable;

public class OtkData implements Serializable {

    private MintInfo mMintInfo;
    private OtkState mOtkState;
    private String mPublicKey;
    private SessionData mSessionData;

    public SessionData getSessionData() {
        return mSessionData;
    }

    public OtkState getOtkState() {
        return mOtkState;
    }

    public String getPublicKey() {
        return mPublicKey;
    }

    public MintInfo getMintInfo() {
        return mMintInfo;
    }

    public OtkData(String mintInfo, OtkState otkState, String pubKey, SessionData sd) {
        mMintInfo = new MintInfo(mintInfo);
        mOtkState = otkState;
        mSessionData = sd;
        mPublicKey = pubKey;
    }

    @NotNull
    @Override
    public String toString() {
        return "OtkData{" +
                "mMintInfo=" + mMintInfo +
                ", mOtkState=" + mOtkState +
                ", mPublicKey='" + mPublicKey + '\'' +
                ", mSessionData=" + mSessionData +
                '}';
    }
}
