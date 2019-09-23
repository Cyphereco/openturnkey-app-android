package com.cyphereco.openturnkey.webservices;


import android.content.Context;
import android.util.Log;

import com.blockcypher.context.BlockCypherContext;
import com.blockcypher.exception.BlockCypherException;
import com.blockcypher.model.address.Address;
import com.blockcypher.model.transaction.Transaction;
import com.blockcypher.model.transaction.intermediary.IntermediaryTransaction;
import com.blockcypher.model.transaction.output.Output;
import com.cyphereco.openturnkey.core.Configurations;
import com.cyphereco.openturnkey.core.UnsignedTx;
import com.cyphereco.openturnkey.ui.Preferences;
import com.cyphereco.openturnkey.utils.BtcUtils;
import com.cyphereco.openturnkey.utils.Log4jHelper;

import org.slf4j.Logger;
import org.spongycastle.asn1.DERInteger;
import org.spongycastle.asn1.DERSequenceGenerator;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class BlockCypher extends BtcBase {
    public static final String TAG = BlockCypher.class.getSimpleName();
    static Logger logger = Log4jHelper.getLogger(TAG);

    private static BlockCypher mBc = null;
    private static BlockCypherContext mBcCtx;
    IntermediaryTransaction mCachedUnsignedTx = null;

    private BlockCypher(Context ctx) {
        String network;
        if (Preferences.isTestnet(ctx)) {
            network = "test3";
        }
        else {
            network = "main";
        }
        logger.info("BlockCypher:{}", network);
        mBcCtx = new BlockCypherContext("v1", "btc", network, "7744d177ce1e4ef48c7431fcb55531b9");
    }

    /**
     * Singleton retrieval of the BlockCypher.
     *
     * @return The singleton.
     */
    public static synchronized BlockCypher getInstance(Context ctx) {
        if (null == mBc) {
            mBc = new BlockCypher(ctx);
        }
        return mBc;
    }

    public static BlockCypher reInit(Context ctx) {
        logger.info("reInit");
        mBcCtx = null;
        mBc = null;
        return getInstance(ctx);
    }

    public BigDecimal getBalance(String address) {
        logger.info("getBalance");
        try {
            Address a = mBcCtx.getAddressService().getAddress(address);
            logger.info("address:" + address + " balance:" + a.getBalance() + " final:" + a.getFinalBalance());
            BigDecimal d = a.getFinalBalance();
            return d;
        } catch (Exception e) {
            logger.error("e:" + e.toString());
        }
        return BigDecimal.ZERO;
    }

    /**
     * Send bitcoin.
     * @param from From address
     * @param to To address
     * @param amount Amount to send in satoshis
     * @param feeIncluded
     * @param txFees
     * @return List of unsigned tx
     * @throws BlockCypherException
     */
    public UnsignedTx sendBitcoin(String from, String to, long amount, long txFees, boolean feeIncluded) throws BlockCypherException {
        logger.info("sendBitcoin() from:{} to:{} amount:{} fee:{} feeIncluded:{}", from, to, amount, txFees, feeIncluded);
        try {
            if (feeIncluded) {
                amount -= txFees;
                logger.info("Fee included. Amount:{}, fee:{}", amount, txFees);
            }
            IntermediaryTransaction unsignedTx = mBcCtx.getTransactionService().newTransaction(
                    new ArrayList<String>(Arrays.asList(from)), new ArrayList<String>(Arrays.asList(to)), amount, txFees);
            if ((unsignedTx == null) || unsignedTx.getTosign().size() == 0) {
                logger.info("unsignedTx is null or toSign is empty");
                return null;
            }
            // Cache unsignedTx
            logger.info("unsignedTx:" + unsignedTx.toString());
            mCachedUnsignedTx = unsignedTx;
            double a = 0.0;
            Transaction trans = unsignedTx.getTx();
            // Find amount from outputs
            for (int i = 0; i < trans.getOutputs().size(); i++) {
                Output o = trans.getOutputs().get(i);
                if (o.getAddresses().get(0).equals(to)) {
                    a = BtcUtils.satoshiToBtc(o.getValue().longValue());
                    break;
                }
            }
            if (a == 0.0) {
                logger.error("Amount is zero");
                // Should not be here
                a = amount;
            }
            UnsignedTx utx = new UnsignedTx(from, to, a, txFees, unsignedTx.getTosign());
            return utx;
//            ArrayList al = new ArrayList<String>();
//            return unsignedTx.getTosign();
        }
        catch (BlockCypherException e) {
            logger.error("e:" + e.toString());
            throw e;
        }
        catch (Exception e) {
            logger.error("e:" + e.toString());
            throw e;
        }
    }

    public static String parseError(String error) {
        // Error{error='xxx'}
        // Temporarily skip first few leters and last two letters
        int len = error.length();
        return error.substring(13, len - 2);
    }

    public static String bytesToHexString(byte[] bytes) {
        StringBuffer buf = new StringBuffer(bytes.length * 2);
        for (byte b : bytes) {
            String sT = Integer.toString(0xFF & b, 16);
            if (sT.length() < 2)
                buf.append('0');
            buf.append(sT);
        }
        return buf.toString();
    }

    private static byte[] toDER(BigInteger r, BigInteger s) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream(72);
        DERSequenceGenerator seq = null;
        byte[] res = new byte[0];
        try {
            seq = new DERSequenceGenerator(bos);
            seq.addObject(new DERInteger(r));
            seq.addObject(new DERInteger(s));
            seq.close();
            res = bos.toByteArray();
            return res;
        } catch (IOException e) {
            logger.info("Exception while toDER()" + e.toString());
        }
        return null;
    }

    public Transaction completeSendBitcoin(String publicKey, List<String> sigList) throws BlockCypherException, Exception {
        if (mCachedUnsignedTx == null) {
            logger.info("mCachedUnsignedTx is null");
            return null;
        }

        if (mCachedUnsignedTx.getTosign().size() != sigList.size()) {
            logger.info("toSign number:" + mCachedUnsignedTx.getTosign().size() + " sig number:" + sigList.size());
            mCachedUnsignedTx = null;
            return null;
        }
        for (int i = 0; i < mCachedUnsignedTx.getTosign().size(); i++) {
            String sig = sigList.get(i);
            // Pushing Pub key for input
            mCachedUnsignedTx.addPubKeys(publicKey);
            BigInteger r = new BigInteger(sig.substring(0, 64),16);
            BigInteger s = new BigInteger(sig.substring(64, 128),16);
            // To ensure low S values for BIP 62
            s = BtcUtils.lowSValue(s);
            String signedString = bytesToHexString(toDER(r, s));
            mCachedUnsignedTx.addSignature(signedString);
        }

        try {
            Transaction trans = mBcCtx.getTransactionService().sendTransaction(mCachedUnsignedTx);
            logger.info("TX Sent: " + trans.toString());
            mCachedUnsignedTx = null;
            return trans;
        }
        catch (BlockCypherException e) {
            logger.info("e:" + e.toString());
            mCachedUnsignedTx = null;
            throw e;
        }
        catch (Exception e) {
            logger.info("e:" + e.toString());
            mCachedUnsignedTx = null;
            throw e;
        }
    }
}
