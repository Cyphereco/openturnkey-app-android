package com.cyphereco.openturnkey.core;

import android.util.Log;

import com.cyphereco.openturnkey.core.protocol.MintInfo;
import com.cyphereco.openturnkey.core.protocol.OtkState;
import com.cyphereco.openturnkey.core.protocol.SessionData;
import com.cyphereco.openturnkey.utils.Log4jHelper;

import org.slf4j.Logger;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamException;
import java.io.Serializable;

public class OtkData implements Serializable {
    private static final String TAG = OtkData.class.getSimpleName();
    static Logger logger = Log4jHelper.getLogger(TAG);

    /* Data type. */
    public enum Type {
        OTK_DATA_TYPE_GENERAL_INFO,
        OTK_DATA_TYPE_SIGNATURE,
        OTK_DATA_TYPE_KEY_INFO,
        OTK_DATA_TYPE_COMMAND_EXEC_FAILURE,
        OTK_DATA_TYPE_COMMAND_EXEC_SUCCESS,
    }
    private Type mType;
    String mAppUri;
    MintInfo mMintInfo;

    OtkState mOtkState;
    String mPublicKey;
    private SessionData mSessionData;
    String mFailureReason;

    public Type getType() {return mType;}
    public SessionData getSessionData() {return mSessionData;}
    public OtkState getOtkState() { return mOtkState;}
    public String getPublicKey() { return mPublicKey;}
    public String getFailureReason() { return mFailureReason;}
    public MintInfo getMintInfo() { return mMintInfo;}

    /**
     * Constructor
     * @param mintInfo
     * @param otkState
     * @param sd
     */
    public OtkData(String mintInfo, OtkState otkState, String pubKey, SessionData sd) {
        Log.d(TAG, "mintInfo:" + mintInfo + "\notkState:" + otkState.toString() + "\nSessionData:" + sd.toString());
        mMintInfo = new MintInfo(mintInfo);
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
        else if (otkState.getFailureReason() != OtkState.FailureReason.NFC_REASON_INVALID) {
            mType = Type.OTK_DATA_TYPE_COMMAND_EXEC_FAILURE;
            mFailureReason = otkState.getFailureReason().getReasonString();
        }
        else if (otkState.getExecutionState() == OtkState.ExecutionState.NFC_CMD_EXEC_SUCCESS) {
            mType = Type.OTK_DATA_TYPE_COMMAND_EXEC_SUCCESS;
        }
        else {
            mType = Type.OTK_DATA_TYPE_GENERAL_INFO;
        }
    }
}
