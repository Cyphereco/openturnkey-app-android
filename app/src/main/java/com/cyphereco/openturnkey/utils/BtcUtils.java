package com.cyphereco.openturnkey.utils;

import com.cyphereco.openturnkey.bitcoin.ECDSASignature;
import com.cyphereco.openturnkey.bitcoin.ECException;
import com.cyphereco.openturnkey.bitcoin.ECKey;
import com.cyphereco.openturnkey.bitcoin.Utils;
import com.cyphereco.openturnkey.bitcoin.VarInt;
import com.cyphereco.openturnkey.core.Configurations;
import com.cyphereco.openturnkey.ui.Preferences;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import org.bouncycastle.util.encoders.Base64;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;

public class BtcUtils {
    public static final String TAG = BtcUtils.class.getSimpleName();
    private static Logger logger = Log4jHelper.getLogger(TAG);

    private static final BigInteger HALF_CURVE_ORDER = new BigInteger("7fffffffffffffffffffffffffffffff5d576e7357a4501ddfe92f46681b20a0", 16);
    private static final BigInteger CURVE_ORDER = new BigInteger("fffffffffffffffffffffffffffffffebaaedce6af48a03bbfd25e8cd0364141", 16);

    /**
     * Signed message header
     */
    private static final String BITCOIN_SIGNED_MESSAGE_HEADER = "Bitcoin Signed Message:\n";

    private BtcUtils() {
    }

    private static final char[] ALPHABET = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz".toCharArray();
    private static final int[] INDEXES = new int[128];

    static {
        Arrays.fill(INDEXES, -1);
        for (int i = 0; i < ALPHABET.length; i++) {
            INDEXES[ALPHABET[i]] = i;
        }
    }

    public static byte[] generateMessageToSign(String message) {
        byte[] contents;
        try (ByteArrayOutputStream outStream = new ByteArrayOutputStream(message.length() * 2)) {
            byte[] headerBytes = BITCOIN_SIGNED_MESSAGE_HEADER.getBytes(StandardCharsets.UTF_8);
            outStream.write(VarInt.encode(headerBytes.length));
            outStream.write(headerBytes);
            byte[] messageBytes = message.getBytes(StandardCharsets.UTF_8);
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
     * <p>
     * The regular BigInteger method isn't quite what we often need: it appends a
     * leading zero to indicate that the number is positive and it may need padding.
     *
     * @param bigInteger Integer to format into a byte array
     * @return Byte array of the desired length
     */
    private static byte[] bigIntegerToBytes(BigInteger bigInteger) {
        if (bigInteger == null)
            return null;
        byte[] bigBytes = bigInteger.toByteArray();
        byte[] bytes = new byte[32];
        int start = (bigBytes.length == 32 + 1) ? 1 : 0;
        int length = Math.min(bigBytes.length, 32);
        System.arraycopy(bigBytes, start, bytes, 32 - length, length);
        return bytes;
    }

    private static boolean isKeyCompressed(byte[] key) {
        return key.length == 33;
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
            for (int i = 0; i < 4; i++) {
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
            sigData[0] = (byte) headerByte;
            System.arraycopy(bigIntegerToBytes(sig.getR()), 0, sigData, 1, 32);
            System.arraycopy(bigIntegerToBytes(sig.getS()), 0, sigData, 33, 32);
            //
            // Create a Base-64 encoded string for the message signature
            //
            encodedSignature = new String(Base64.encode(sigData), StandardCharsets.UTF_8);
        } catch (ECException exc) {
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
            String s = Integer.toString(0xFF & b, 16);
            if (s.length() < 2)
                buf.append('0');
            buf.append(s);
        }
        return buf.toString();
    }

    public static byte[] hexStringToBytes(String s) {
//        logger.debug("hexStringToBytes:" + s);
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }

    private static byte[] doubleSha256(byte[] message) {
        return Utils.doubleDigest(message);
    }

    static public boolean validateSignature(String address, String message, String signature) {
        try {
            return ECKey.verifyMessage(address, message, signature);
        } catch (VarInt.SignatureException e) {
            return false;
        }
    }

    static public TxFee getTxFee() {
        TxFee txFee = null;
        HttpURLConnection httpConn = null;

        // Read text input stream.
        InputStreamReader isReader = null;

        // Read text into buffer.
        BufferedReader bufReader = null;

        // Save server response text.
        StringBuilder readTextBuf = new StringBuilder();

        try {
            // Create a URL object use page url.
            URL url = new URL("https://bitcoinfees.earn.com/api/v1/fees/recommended");

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
            String low = reader.getString("hourFee");
            //String mid = reader.getString("halfHourFee");
            String high = reader.getString("fastestFee");
            /* Since min and high fees are often the same, we will take an
             * average of hign and low fees.
             */
            String mid = "" + (Integer.parseInt(low) + Integer.parseInt(high)) / 2;

            txFee = new TxFee(Integer.parseInt(low), Integer.parseInt(mid), Integer.parseInt(high));
        } catch (IOException | JSONException | NumberFormatException e) {
            e.printStackTrace();
        } finally {
            try {
                if (bufReader != null) {
                    bufReader.close();
                }

                if (isReader != null) {
                    isReader.close();
                }

                if (httpConn != null) {
                    httpConn.disconnect();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return txFee;
    }

    static public BtcExchangeRates getCurrencyExchangeRate() {
        BtcExchangeRates ret = null;
        HttpURLConnection httpConn = null;

        // Read text input stream.
        InputStreamReader isReader = null;

        // Read text into buffer.
        BufferedReader bufReader = null;

        // Save server response text.
        StringBuilder readTextBuf = new StringBuilder();

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
            String cny = reader.getJSONObject("CNY").getString("last");
            String eur = reader.getJSONObject("EUR").getString("last");
            String jpy = reader.getJSONObject("JPY").getString("last");
            String twd = reader.getJSONObject("TWD").getString("last");
            String usd = reader.getJSONObject("USD").getString("last");

            ret = new BtcExchangeRates(Double.parseDouble(cny), Double.parseDouble(eur), Double.parseDouble(jpy),
                    Double.parseDouble(twd), Double.parseDouble(usd));


        } catch (IOException | JSONException | NumberFormatException e) {
            e.printStackTrace();
        } finally {
            try {
                if (bufReader != null) {
                    bufReader.close();
                }

                if (isReader != null) {
                    isReader.close();
                }

                if (httpConn != null) {
                    httpConn.disconnect();
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

        return ((double) satoshi / 100000000.0);
    }

    static public double localCurrencyToBtc(BtcExchangeRates rate, LocalCurrency lc, double amount) {
        if (rate == null) {
            return 0;
        }
        switch (lc) {
            case LOCAL_CURRENCY_CNY:
                return amount / rate.getRate_cny();
            case LOCAL_CURRENCY_EUR:
                return amount / rate.getRate_eur();
            case LOCAL_CURRENCY_JPY:
                return amount / rate.getRate_jpy();
            case LOCAL_CURRENCY_TWD:
                return amount / rate.getRate_twd();
            case LOCAL_CURRENCY_USD:
                return amount / rate.getRate_usd();
        }
        return 0;
    }

    static public double btcToLocalCurrency(BtcExchangeRates rate, LocalCurrency lc, double amount) {
        if (rate == null) {
            return 0;
        }
        switch (lc) {
            case LOCAL_CURRENCY_CNY:
                return rate.getRate_cny() * amount;
            case LOCAL_CURRENCY_EUR:
                return rate.getRate_eur() * amount;
            case LOCAL_CURRENCY_JPY:
                return rate.getRate_jpy() * amount;
            case LOCAL_CURRENCY_TWD:
                return rate.getRate_twd() * amount;
            case LOCAL_CURRENCY_USD:
                return rate.getRate_usd() * amount;
        }
        return 0;
    }

    static public long getTxFeeInSatoshi() {
        Configurations.TxFeeType type = Preferences.getTxFeeType();
        TxFee txFee = Preferences.getTxFee();
        long txFees;
        if (type == Configurations.TxFeeType.HIGH) {
            // Now we estimate tx size is about 200 bytes
            txFees = txFee.getHigh() * 200;
        } else if (type == Configurations.TxFeeType.MID) {
            // Now we estimate tx size is about 200 bytes
            txFees = txFee.getMid() * 200;
        } else if (type == Configurations.TxFeeType.LOW) {
            // Now we estimate tx size is about 200 bytes
            txFees = txFee.getLow() * 200;
        } else {
            txFees = Preferences.getCustomizedTxFee();
        }
        return txFees;
    }

    static public long getEstimatedTime(long fee) {
        TxFee txFee = Preferences.getTxFee();
        if (fee >= txFee.getHigh() * 200) {
            // within 1 block
            return 1;
        }
        if (fee >= txFee.getMid() * 200) {
            // within 3 blocks
            return 3;
        }

        // over 6 blocks
        return 6;
    }

    static public long convertDateTimeStringToLong(String dateTime) {
        if (dateTime == null) return 0;

        logger.debug("Time string to convert: {}", dateTime);
        String regexDateFormat = "\\b[0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9]{2}:[0-9]{2}:[0-9]{2}.*Z\\b";
        if (!dateTime.matches(regexDateFormat)) return 0;

        // remove milliseconds string if exists
        String regexReplacement = "\\..*Z";
        String timeFormatted = dateTime.replaceAll(regexReplacement, "Z");
        try {
            //2019-09-15T16:49:49.584902078Z
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.ENGLISH);
            format.setTimeZone(TimeZone.getTimeZone("UTC"));
            Date d = format.parse(timeFormatted);
            return d.getTime();
        } catch (ParseException e) {
            logger.error("Exception: {}", e);
            return 0;
        }
    }

    static public String convertDateTimeStringFromLong(long time) {
        Date date = new Date(time);
        //2019-09-15T16:49:49Z
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ'", Locale.getDefault());
        return format.format(date);
    }

    static public boolean isSegWitAddress(boolean isMainNet, String address) {
        try {
            if (isMainNet) {
                if (address.substring(0, 3).equals("bc1")) {
                    return true;
                }
            } else {
                // testnet
                if (address.substring(0, 3).equals("tb1")) {
                    return true;
                }
            }
        } catch (Exception e) {
            logger.error("isSegWitAddress() ex:" + e);
        }
        return false;
    }

    static public boolean validateAddress(boolean isMainNet, String address) {
        logger.info("Validate Address for MainNet={}, Address={}", isMainNet, address);
        // check prefix
        try {
            char[] addr = address.toCharArray();
            if (isMainNet) {
                if (addr[0] != '1' && addr[0] != '3') {
//                    logger.error("Invalid prefix:{}", addr[0]);
                    return false;
                }
            } else {
                // Testnet
                if (addr[0] != 'm' && addr[0] != 'n' && addr[0] != '2') {
//                    logger.error("Invalid prefix:{}", addr[0]);
                    return false;
                }
            }
            // Check length
            if (addr.length < 26 || addr.length > 35) {
                logger.error("Invalid length:{}", addr.length);
                return false;
            }
            // Check is all are valid character
            for (int i = 1; i < addr.length - 1; i++) {
                int digit58 = -1;
                char c = addr[i];
                if (c < 128) {
                    digit58 = INDEXES[c];
                }
                if (digit58 < 0) {
                    logger.error("Invalid address:{} at {}", address, i);
                    return false;
                }
            }
            return true;
        } catch (Exception e) {
            logger.error("validateAddress() ex:" + e);
        }
        return false;
    }

    static public BigInteger lowSValue(BigInteger s) {
        if (s.compareTo(HALF_CURVE_ORDER) <= 0) {
            return s;
        }
        logger.debug("s > half curve order");
        return CURVE_ORDER.subtract(s);
    }
}