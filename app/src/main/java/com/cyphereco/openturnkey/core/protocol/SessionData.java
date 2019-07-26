package com.cyphereco.openturnkey.core.protocol;

import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class SessionData {
    private static final String TAG = SessionData.class.getSimpleName();

    private static final String CRLF = "\r\n";
    private static final String OTK_LABEL_BITCOIN_ADDR = "<BTC_Addr>\r\n";
    private static final String OTK_LABEL_SESSION_ID = "<Session_ID>\r\n";
    private static final String OTK_LABEL_REQUEST_SIG = "<Request_Signature>\r\n";
    private static final String OTK_LABEL_REQUEST_ID = "<Request_ID>\r\n";
    private static final String OTK_LABEL_MASTER_EXT_KEY = "<Master_Extended_Key>\r\n";
    private static final String OTK_LABEL_DERIVATIVE_EXT_KEY = "<Derivative_Exteded_Key>\r\n";
    private static final String OTK_LABEL_DERIVATIVE_PATH = "<Derivative_Path>\r\n";
    private static final String OTK_LABEL_SECURE_PIN = "<Secure_Pin>\r\n";
    private static final String OTK_REQUEST_DATA_DELIM = "\n";
    private static final String OTK_REQUEST_SIGNATURE_DELIM = "\n";


    private String sessionId = null;
    private String address = null;
    private String requestId = null;
    private String masterExtKey = null;
    private String derivativeExtKey = null;
    private String derivativePath = null;
    private String securePIN = null;
    List<String> sigList = new ArrayList<String>();

    public SessionData(String sessData) {
        // Session id
        int labelSsessIdStart = sessData.indexOf(OTK_LABEL_SESSION_ID);
        if (labelSsessIdStart != -1) {
            int sessIdStart = labelSsessIdStart + OTK_LABEL_SESSION_ID.length();
            int crlf = sessData.indexOf(CRLF, sessIdStart);
            sessionId = sessData.substring(sessIdStart, crlf);
        }
        // Address
        int lableAddrStart = sessData.indexOf(OTK_LABEL_BITCOIN_ADDR);
        if (lableAddrStart != -1) {
            int addrStart = lableAddrStart + OTK_LABEL_BITCOIN_ADDR.length();
            int crlf = sessData.indexOf(CRLF, addrStart);
            address = sessData.substring(addrStart, crlf);
        }
        // Request signature
        int lableReqSigStart = sessData.indexOf(OTK_LABEL_REQUEST_SIG);
        if (lableReqSigStart != -1) {
            int reqSigStart = lableReqSigStart + OTK_LABEL_REQUEST_SIG.length();
            int crlf = sessData.indexOf(CRLF, reqSigStart);
            String requestSig = sessData.substring(reqSigStart, crlf);
            while (true) {
                Log.d(TAG, "requestSig:" + requestSig);
                // Find delim that separate signatures
                int delim = requestSig.indexOf(OTK_REQUEST_SIGNATURE_DELIM);
                String sig = null;
                if (delim > 0) {
                    sig = requestSig.substring(0, delim);
                } else {
                    sig = requestSig;
                }
                // Double check if the sig size if correct.
                if (sig.length() != 128) {
                    Log.d(TAG, "Invalid signature:" + sig);
                    break;
                }
                Log.d(TAG, "sig:" + sig);
                sigList.add(sig);
                if (delim < 0) {
                    break;
                }
                // Advance reqSigStart
                requestSig = requestSig.substring(delim + OTK_REQUEST_SIGNATURE_DELIM.length());
            }
        }

        // Request id
        int lableReqIdStart = sessData.indexOf(OTK_LABEL_REQUEST_ID);
        if (lableReqIdStart != -1) {
            int reqIdStart = lableReqIdStart + OTK_LABEL_REQUEST_ID.length();
            int crlf = sessData.indexOf(CRLF, reqIdStart);
            requestId = sessData.substring(reqIdStart, crlf);
        }

        // Master ext key
        int lableMasterExtKeyStart = sessData.indexOf(OTK_LABEL_MASTER_EXT_KEY);
        if (lableMasterExtKeyStart != -1) {
            int masterExtKeyStart = lableMasterExtKeyStart + OTK_LABEL_MASTER_EXT_KEY.length();
            int crlf = sessData.indexOf(CRLF, masterExtKeyStart);
            masterExtKey = sessData.substring(masterExtKeyStart, crlf);
        }

        // Derivative ext key
        int lableDerivativeExtKeyStart = sessData.indexOf(OTK_LABEL_DERIVATIVE_EXT_KEY);
        if (lableDerivativeExtKeyStart != -1) {
            int derivativeExtKeyStart = lableMasterExtKeyStart + OTK_LABEL_DERIVATIVE_EXT_KEY.length();
            int crlf = sessData.indexOf(CRLF, derivativeExtKeyStart);
            derivativeExtKey = sessData.substring(derivativeExtKeyStart, crlf);
        }

        // Derivative path
        int lableDerivativePathStart = sessData.indexOf(OTK_LABEL_DERIVATIVE_PATH);
        if (lableDerivativePathStart != -1) {
            int derivativePathStart = lableDerivativePathStart + OTK_LABEL_DERIVATIVE_PATH.length();
            int crlf = sessData.indexOf(CRLF, derivativePathStart);
            derivativePath = sessData.substring(derivativePathStart, crlf);
        }

        // PIN
        int lablePINStart = sessData.indexOf(OTK_LABEL_SECURE_PIN);
        if (lablePINStart != -1) {
            int pinStart = lablePINStart + OTK_LABEL_SECURE_PIN.length();
            int crlf = sessData.indexOf(CRLF, pinStart);
            securePIN = sessData.substring(pinStart, crlf);
        }

    }

    public String getAddress() {
        return address;
    }

    public String getSessionId() {
        return sessionId;
    }

    public String getRequestId() {
        return requestId;
    }

    public String getMasterExtKey() {
        return masterExtKey;
    }

    public String getDerivativeExtKey() {
        return derivativeExtKey;
    }

    public String getDerivativePath() {
        return derivativePath;
    }

    public String getSecurePIN() {
        return securePIN;
    }

    public List<String> getRequestSigList() {
        return sigList;
    }
}

