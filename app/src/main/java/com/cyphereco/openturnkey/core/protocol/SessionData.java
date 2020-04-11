package com.cyphereco.openturnkey.core.protocol;

import com.cyphereco.openturnkey.utils.Log4jHelper;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class SessionData implements Serializable {
    private static final String TAG = SessionData.class.getSimpleName();
    private static Logger logger = Log4jHelper.getLogger(TAG);

    private static final String CRLF = "\r\n";
    private static final String OTK_LABEL_BITCOIN_ADDR = "<BTC_Addr>\r\n";
    private static final String OTK_LABEL_SESSION_ID = "<Session_ID>\r\n";
    private static final String OTK_LABEL_REQUEST_SIG = "<Request_Signature>\r\n";
    private static final String OTK_LABEL_REQUEST_ID = "<Request_ID>\r\n";
    private static final String OTK_LABEL_PUBLIC_KEY = "<Public_Key>\r\n";
    private static final String OTK_LABEL_MASTER_EXT_KEY = "<Master_Extended_Key>\r\n";
    private static final String OTK_LABEL_DERIVATIVE_EXT_KEY = "<Derivative_Extended_Key>\r\n";
    private static final String OTK_LABEL_DERIVATIVE_EXT_KEY_OLD = "<Derivative_Exteded_Key>\r\n"; // there was a typo which has been fixed, but to keep thing working with the old firmware
    private static final String OTK_LABEL_DERIVATIVE_PATH = "<Derivative_Path>\r\n";
    private static final String OTK_LABEL_WIF_KEY = "<WIF_Key>\r\n";
    private static final String OTK_REQUEST_SIGNATURE_DELIM = "\n";


    private String sessionId = null;
    private String address = null;
    private String requestId = null;
    private String masterExtKey = null;
    private String derivativeExtKey = null;
    private String derivativePath = null;
    private String publicKey = null;
    private String wifKey = null;
    private List<String> sigList = new ArrayList<>();

    public SessionData(String sessData) {
        logger.debug("DEBUG SessionData: {}", sessData);
        // Session id
        int labelSsessIdStart = sessData.indexOf(OTK_LABEL_SESSION_ID);
        if (labelSsessIdStart != -1) {
            int sessIdStart = labelSsessIdStart + OTK_LABEL_SESSION_ID.length();
            int crlf = sessData.indexOf(CRLF, sessIdStart);
            sessionId = sessData.substring(sessIdStart, crlf);
        }
        // Address
        int labelAddrStart = sessData.indexOf(OTK_LABEL_BITCOIN_ADDR);
        if (labelAddrStart != -1) {
            int addrStart = labelAddrStart + OTK_LABEL_BITCOIN_ADDR.length();
            int crlf = sessData.indexOf(CRLF, addrStart);
            address = sessData.substring(addrStart, crlf);
        }
        // Request signature
        int labelReqSigStart = sessData.indexOf(OTK_LABEL_REQUEST_SIG);
        if (labelReqSigStart != -1) {
            int reqSigStart = labelReqSigStart + OTK_LABEL_REQUEST_SIG.length();
            int crlf = sessData.indexOf(CRLF, reqSigStart);
            String requestSig = sessData.substring(reqSigStart, crlf);
            while (true) {
//                logger.debug("requestSig:" + requestSig);
                // Find delim that separate signatures
                int delim = requestSig.indexOf(OTK_REQUEST_SIGNATURE_DELIM);
                String sig;
                if (delim > 0) {
                    sig = requestSig.substring(0, delim);
                } else {
                    sig = requestSig;
                }
                // Double check if the sig size if correct.
                if (sig.length() != 128) {
                    logger.debug("Invalid signature:" + sig);
                    break;
                }
                logger.debug("sig:" + sig);
                sigList.add(sig);
                if (delim < 0) {
                    break;
                }
                // Advance reqSigStart
                requestSig = requestSig.substring(delim + OTK_REQUEST_SIGNATURE_DELIM.length());
            }
        }

        // Request id
        int labelReqIdStart = sessData.indexOf(OTK_LABEL_REQUEST_ID);
        if (labelReqIdStart != -1) {
            int reqIdStart = labelReqIdStart + OTK_LABEL_REQUEST_ID.length();
            int crlf = sessData.indexOf(CRLF, reqIdStart);
            requestId = sessData.substring(reqIdStart, crlf);
        }

        // Public Key
        int labelPubKeyStart = sessData.indexOf(OTK_LABEL_PUBLIC_KEY);
        if (labelPubKeyStart != -1) {
            int pubKeyStart = labelPubKeyStart + OTK_LABEL_PUBLIC_KEY.length();
            int crlf = sessData.indexOf(CRLF, pubKeyStart);
            publicKey = sessData.substring(pubKeyStart, crlf);
        }

        // Master ext key
        int labelMasterExtKeyStart = sessData.indexOf(OTK_LABEL_MASTER_EXT_KEY);
        if (labelMasterExtKeyStart != -1) {
            int masterExtKeyStart = labelMasterExtKeyStart + OTK_LABEL_MASTER_EXT_KEY.length();
            int crlf = sessData.indexOf(CRLF, masterExtKeyStart);
            masterExtKey = sessData.substring(masterExtKeyStart, crlf);
        }

        // Derivative ext key
        int labelDerivativeExtKeyStart = sessData.indexOf(OTK_LABEL_DERIVATIVE_EXT_KEY);
        if (labelDerivativeExtKeyStart != -1) {
            int derivativeExtKeyStart = labelDerivativeExtKeyStart + OTK_LABEL_DERIVATIVE_EXT_KEY.length();
            int crlf = sessData.indexOf(CRLF, derivativeExtKeyStart);
            derivativeExtKey = sessData.substring(derivativeExtKeyStart, crlf);
        }

        // for backward compatible
        int labelDerivativeExtKeyStartOld = sessData.indexOf(OTK_LABEL_DERIVATIVE_EXT_KEY_OLD);
        if (labelDerivativeExtKeyStartOld != -1) {
            int derivativeExtKeyStart = labelDerivativeExtKeyStart + OTK_LABEL_DERIVATIVE_EXT_KEY_OLD.length();
            int crlf = sessData.indexOf(CRLF, derivativeExtKeyStart);
            derivativeExtKey = sessData.substring(derivativeExtKeyStart, crlf);
        }

        // Derivative path
        int labelDerivativePathStart = sessData.indexOf(OTK_LABEL_DERIVATIVE_PATH);
        if (labelDerivativePathStart != -1) {
            int derivativePathStart = labelDerivativePathStart + OTK_LABEL_DERIVATIVE_PATH.length();
            int crlf = sessData.indexOf(CRLF, derivativePathStart);
            derivativePath = sessData.substring(derivativePathStart, crlf);
        }

        // WIF_Key
        int labelWIFKeyStart = sessData.indexOf(OTK_LABEL_WIF_KEY);
        if (labelWIFKeyStart != -1) {
            int wifKeyStart = labelWIFKeyStart + OTK_LABEL_WIF_KEY.length();
            int crlf = sessData.indexOf(CRLF, wifKeyStart);
            wifKey = sessData.substring(wifKeyStart, crlf);
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

    public String getPublicKey() {
        return publicKey;
    }

    public String getDerivativeExtKey() {
        return derivativeExtKey;
    }

    public String getDerivativePath() {
        return derivativePath;
    }

    public String getWIFKey() {
        return wifKey;
    }

    public List<String> getRequestSigList() {
        return sigList;
    }

    @NotNull
    @Override
    public String toString() {
        return "SessionData{" +
                "sessionId='" + sessionId + '\'' +
                ", address='" + address + '\'' +
                ", requestId='" + requestId + '\'' +
                ", masterExtKey='" + masterExtKey + '\'' +
                ", derivativeExtKey='" + derivativeExtKey + '\'' +
                ", derivativePath='" + derivativePath + '\'' +
                ", publicKey='" + publicKey + '\'' +
                ", wifKey='" + wifKey + '\'' +
                '}';
    }
}

