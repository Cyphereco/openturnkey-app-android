package com.cyphereco.openturnkey.utils;

import android.util.Log;

//import com.google.bitcoin.core.utils.Utils;
import com.cyphereco.openturnkey.bitcoin.ECDSASignature;
import com.cyphereco.openturnkey.bitcoin.ECException;
import com.cyphereco.openturnkey.bitcoin.ECKey;
import com.cyphereco.openturnkey.bitcoin.Utils;
import com.cyphereco.openturnkey.bitcoin.VarInt;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;

import org.bouncycastle.util.encoders.Base64;
import org.json.JSONException;
import org.json.JSONObject;

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

    static public CurrencyExchangeRate getCurrencyExchangeRate() {
        CurrencyExchangeRate ret = null;
        HttpURLConnection httpConn = null;

        // Read text input stream.
        InputStreamReader isReader = null;

        // Read text into buffer.
        BufferedReader bufReader = null;

        // Save server response text.
        StringBuffer readTextBuf = new StringBuffer();

        try {
            // Create a URL object use page url.
            URL url = new URL("https://blockchain.info/ticker");

            // Open http connection to web server.
            httpConn = (HttpURLConnection) url.openConnection();

            // Set http request method to get.
            httpConn.setRequestMethod("GET");

            // Set connection timeout and read timeout value.
            httpConn.setConnectTimeout(5000);
            httpConn.setReadTimeout(5000);

            // Get input stream from web url connection.
            InputStream inputStream = httpConn.getInputStream();

            // Create input stream reader based on url connection input stream.
            isReader = new InputStreamReader(inputStream);

            // Create buffered reader.
            bufReader = new BufferedReader(isReader);

            // Read line of text from server response.
            String line = bufReader.readLine();

            // Loop while return line is not null.
            while (line != null) {
                // Append the text to string buffer.
                readTextBuf.append(line);

                // Continue to read text line.
                line = bufReader.readLine();
            }

            String in = readTextBuf.toString();
            JSONObject reader = new JSONObject(in);
            String twd = reader.getJSONObject("TWD").getString("last");
            String usd = reader.getJSONObject("USD").getString("last");
            String cny = reader.getJSONObject("CNY").getString("last");
            String jpy = reader.getJSONObject("JPY").getString("last");
            String eur = reader.getJSONObject("EUR").getString("last");

            ret = new CurrencyExchangeRate(Double.parseDouble(twd), Double.parseDouble(usd), Double.parseDouble(jpy),
                    Double.parseDouble(eur), Double.parseDouble(cny));


        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (NumberFormatException e) {
            e.printStackTrace();
        } finally {
            try {
                if (bufReader != null) {
                    bufReader.close();
                    bufReader = null;
                }

                if (isReader != null) {
                    isReader.close();
                    isReader = null;
                }

                if (httpConn != null) {
                    httpConn.disconnect();
                    httpConn = null;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return ret;
    }

    static public long btcToSatoshi(double btc) {
        if (btc == -1) {
            // For use all funds
            return -1;
        }
        return (long) (btc * 100000000);
    }

    static public double satoshiToBtc(long satoshi) {

        return ((double)satoshi / 100000000.0);
    }

    static public double localCurrencyToBtc(CurrencyExchangeRate rate, LocalCurrency lc, double amount) {
        if (rate == null) {
            return 0;
        }
        switch (lc) {
            case LOCAL_CURRENCY_TWD:
                return Double.valueOf(amount / rate.getTWD());
            case LOCAL_CURRENCY_USD:
                return Double.valueOf(amount / rate.getUSD());
            case LOCAL_CURRENCY_CNY:
                return Double.valueOf(amount / rate.getCNY());
            case LOCAL_CURRENCY_EUR:
                return Double.valueOf(amount / rate.getEUR());
            case LOCAL_CURRENCY_JPY:
                return Double.valueOf(amount / rate.getJPY());
        }
        return 0;
    }

    static public double btcToLocalCurrency(CurrencyExchangeRate rate, LocalCurrency lc, double amount) {
        if (rate == null) {
            return 0;
        }
        switch (lc) {
            case LOCAL_CURRENCY_TWD:
                return Double.valueOf(rate.getTWD() * amount);
            case LOCAL_CURRENCY_USD:
                return Double.valueOf(rate.getUSD() * amount);
            case LOCAL_CURRENCY_CNY:
                return Double.valueOf(rate.getCNY() * amount);
            case LOCAL_CURRENCY_EUR:
                return Double.valueOf(rate.getEUR() * amount);
            case LOCAL_CURRENCY_JPY:
                return Double.valueOf(rate.getJPY() * amount);
        }
        return 0;
    }
}