package com.cyphereco.openturnkey.core;

import com.cyphereco.openturnkey.core.protocol.MintInfo;
import com.cyphereco.openturnkey.core.protocol.OtkState;
import com.cyphereco.openturnkey.core.protocol.SessionData;

import org.jetbrains.annotations.NotNull;

import java.io.Serializable;

public class OtkData implements Serializable {
    /* Data type. */
    public enum Type {
        OTK_DATA_TYPE_GENERAL_INFO,
        OTK_DATA_TYPE_SIGNATURE,
        OTK_DATA_TYPE_KEY_INFO,
        OTK_DATA_TYPE_COMMAND_EXEC_FAILURE,
        OTK_DATA_TYPE_COMMAND_EXEC_SUCCESS,
    }

    private Type mType;
    private MintInfo mMintInfo;

    private OtkState mOtkState;
    private String mPublicKey;
    private SessionData mSessionData;
    private String mFailureReason = "";

    Type getType() {
        return mType;
    }

    public SessionData getSessionData() {
        return mSessionData;
    }

    public OtkState getOtkState() {
        return mOtkState;
    }

    String getPublicKey() {
        return mPublicKey;
    }

    public String getFailureReason() {
        return mFailureReason;
    }

    public MintInfo getMintInfo() {
        return mMintInfo;
    }

    public OtkData(String mintInfo, OtkState otkState, String pubKey, SessionData sd) {
        mMintInfo = new MintInfo(mintInfo);
        mOtkState = otkState;
        mSessionData = sd;
        mPublicKey = pubKey;
        // Determine data type
        if (sd.getRequestSigList() != null && sd.getRequestSigList().size() != 0) {
            mType = Type.OTK_DATA_TYPE_SIGNATURE;
        } else if (sd.getMasterExtKey() != null && !sd.getMasterExtKey().equals("")) {
            mType = Type.OTK_DATA_TYPE_KEY_INFO;
        } else if (otkState.getFailureReason() != OtkState.FailureReason.NFC_REASON_INVALID) {
            mType = Type.OTK_DATA_TYPE_COMMAND_EXEC_FAILURE;
            mFailureReason = otkState.getFailureReason().getValue();
        } else if (otkState.getExecutionState() == OtkState.ExecutionState.NFC_CMD_EXEC_SUCCESS) {
            mType = Type.OTK_DATA_TYPE_COMMAND_EXEC_SUCCESS;
        } else {
            mType = Type.OTK_DATA_TYPE_GENERAL_INFO;
        }
    }

    @NotNull
    @Override
    public String toString() {
        return "OtkData{" +
                "mType=" + mType +
                ", mMintInfo=" + mMintInfo +
                ", mOtkState=" + mOtkState +
                ", mPublicKey='" + mPublicKey + '\'' +
                ", mSessionData=" + mSessionData +
                ", mFailureReason='" + mFailureReason + '\'' +
                '}';
    }
}
