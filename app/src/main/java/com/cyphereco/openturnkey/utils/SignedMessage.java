package com.cyphereco.openturnkey.utils;

public class SignedMessage {
    private static final String BEGIN_BITCOIN_SIGNED_MESSAGE = "-----BEGIN BITCOIN SIGNED MESSAGE-----\n";
    private static final String BEGIN_SIGNATURE = "\n-----BEGIN SIGNATURE-----\n";
    private static final String END_BITCOIN_SIGNED_MESSAGE = "\n-----END BITCOIN SIGNED MESSAGE-----";

    private String mAddress;
    private String mSignature;
    private String mMessage;

    public String getAddress() {
        return mAddress;
    }

    public String getMessage() {
        return mMessage;
    }

    public String getSignature() {
        return mSignature;
    }

    public SignedMessage(String address, String signature, String message) {
        mAddress = address;
        mSignature = signature;
        mMessage = message;
    }

    static public SignedMessage parseSignedMessage(String formattedMessage) {
        SignedMessage sm;
        try {
            int beginSignedMessageIdx = formattedMessage.indexOf(BEGIN_BITCOIN_SIGNED_MESSAGE);
            int beginSignatureIdx = formattedMessage.indexOf(BEGIN_SIGNATURE);
            int endSignedMessageIdx = formattedMessage.indexOf(END_BITCOIN_SIGNED_MESSAGE);
            String message = formattedMessage.substring(beginSignedMessageIdx + BEGIN_BITCOIN_SIGNED_MESSAGE.length(), beginSignatureIdx);
            int nlOfAddress = formattedMessage.substring(beginSignatureIdx + BEGIN_SIGNATURE.length()).indexOf('\n') + beginSignatureIdx + BEGIN_SIGNATURE.length();
            String address = formattedMessage.substring(beginSignatureIdx + BEGIN_SIGNATURE.length(), nlOfAddress);
            String signature = formattedMessage.substring(nlOfAddress + 1, endSignedMessageIdx);

            if (message.length() == 0 || address.length() == 0 || signature.length() == 0) {
                return null;
            }
            sm = new SignedMessage(address, signature, message);
        } catch (Exception e) {
            return null;
        }
        return sm;
    }

    public String getFormattedMessage() {
        String s = "";
        s += BEGIN_BITCOIN_SIGNED_MESSAGE;
        s += mMessage;
        s += BEGIN_SIGNATURE;
        s += mAddress + "\n";
        s += mSignature;
        s += END_BITCOIN_SIGNED_MESSAGE;
        return s;
    }
}
