package com.cyphereco.openturnkey.utils;

import android.util.Log;

//import com.google.bitcoin.core.utils.Utils;
import com.cyphereco.openturnkey.bitcoin.ECDSASignature;
import com.cyphereco.openturnkey.bitcoin.ECException;
import com.cyphereco.openturnkey.bitcoin.ECKey;
import com.cyphereco.openturnkey.bitcoin.Utils;
import com.cyphereco.openturnkey.bitcoin.VarInt;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.util.Arrays;

import org.bouncycastle.util.encoders.Base64;

public class BtcUtils {
    public static final String TAG = BtcUtils.class.getSimpleName();

    /** Signed message header */
    private static final String BITCOIN_SIGNED_MESSAGE_HEADER = "Bitcoin Signed Message:\n";

    private BtcUtils() {}

    public static byte[] generateMessageToSign(String message) {
        byte[] contents;
        try (ByteArrayOutputStream outStream = new ByteArrayOutputStream(message.length()*2)) {
            byte[] headerBytes = BITCOIN_SIGNED_MESSAGE_HEADER.getBytes("UTF-8");
            outStream.write(VarInt.encode(headerBytes.length));
            outStream.write(headerBytes);
            byte[] messageBytes = message.getBytes("UTF-8");
            outStream.write(VarInt.encode(messageBytes.length));
            outStream.write(messageBytes);
            contents = outStream.toByteArray();
        } catch (IOException exc) {
            return null;
        }
        // dobule sha256
        return doubleSha256(contents);
    }

    /**
     * Converts a BigInteger to a fixed-length byte array.
     *
     * The regular BigInteger method isn't quite what we often need: it appends a
     * leading zero to indicate that the number is positive and it may need padding.
     *
     * @param       bigInteger          Integer to format into a byte array
     * @param       numBytes            Desired size of the resulting byte array
     * @return                          Byte array of the desired length
     */
    public static byte[] bigIntegerToBytes(BigInteger bigInteger, int numBytes) {
        if (bigInteger == null)
            return null;
        byte[] bigBytes = bigInteger.toByteArray();
        byte[] bytes = new byte[numBytes];
        int start = (bigBytes.length==numBytes+1) ? 1 : 0;
        int length = Math.min(bigBytes.length, numBytes);
        System.arraycopy(bigBytes, start, bytes, numBytes-length, length);
        return bytes;
    }

    public static boolean isKeyCompressed(byte[] key) {
        if (key.length == 33) {
            return true;
        }
        return false;
    }

    public static String processSignedMessage(byte[] encodedMessageToSign, byte[] publicKey, byte[] signedMessage) {
        String encodedSignature;
        try {
            ECDSASignature sig = new ECDSASignature(bytesToHexString(signedMessage));
            //
            // Get the RecID used to recover the public key from the signature
            //
            BigInteger e = new BigInteger(1, encodedMessageToSign);
            int recID = -1;
            for (int i=0; i<4; i++) {
                ECKey k = ECKey.recoverFromSignature(i, sig, e, isKeyCompressed(publicKey));
                if (k != null && Arrays.equals(k.getPubKey(), publicKey)) {
                    recID = i;
                    break;
                }
            }
            if (recID == -1)
                throw new ECException("Unable to recover public key from signature");
            //
            // The message signature consists of a header byte followed by the R and S values
            //
            int headerByte = recID + 27 + (isKeyCompressed(publicKey) ? 4 : 0);
            byte[] sigData = new byte[65];
            sigData[0] = (byte)headerByte;
            System.arraycopy(bigIntegerToBytes(sig.getR(), 32), 0, sigData, 1, 32);
            System.arraycopy(bigIntegerToBytes(sig.getS(), 32), 0, sigData, 33, 32);
            //
            // Create a Base-64 encoded string for the message signature
            //
            encodedSignature = new String(Base64.encode(sigData), "UTF-8");
        } catch (IOException | ECException exc) {
            throw new IllegalStateException("Unexpected IOException", exc);
        }
        return encodedSignature;
    }

    public static String keyToAddress(String key) {
        ECKey k = new ECKey(hexStringToBytes(key), null, true);
        return k.toAddress().toString();
    }

    public static String bytesToHexString(byte[] bytes) {
        StringBuilder buf = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            String s = Integer.toString(0xFF&b, 16);
            if (s.length() < 2)
                buf.append('0');
            buf.append(s);
        }
        return buf.toString();
    }

    public static byte[] hexStringToBytes(String s) {
        Log.d(TAG, "hexStringToBytes:" + s);
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }

    static byte[] doubleSha256(byte[] message) {
        return Utils.doubleDigest(message);
    }

    static boolean verifySignature(String address, String message, String signature) {
        try {
            return ECKey.verifyMessage(address, message, signature);
        }
        catch (VarInt.SignatureException e){
            return false;
        }
    }


}