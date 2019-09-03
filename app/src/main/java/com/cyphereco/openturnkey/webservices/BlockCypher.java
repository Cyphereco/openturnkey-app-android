package com.cyphereco.openturnkey.webservices;


import android.util.Log;

import com.blockcypher.context.BlockCypherContext;
import com.blockcypher.exception.BlockCypherException;
import com.blockcypher.model.address.Address;
import com.blockcypher.model.transaction.Transaction;
import com.blockcypher.model.transaction.intermediary.IntermediaryTransaction;
import com.cyphereco.openturnkey.core.Configurations;
import com.cyphereco.openturnkey.core.Otk;

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
    private static BlockCypher mBc = null;
    BlockCypherContext mBcCtx;
    IntermediaryTransaction mCachedUnsignedTx = null;

    private BlockCypher() {
        String network;
        if (Configurations.isTestnet()) {
            network = "test3";
        }
        else {
            network = "main";
        }
        mBcCtx = new BlockCypherContext("v1", "btc", network, "7744d177ce1e4ef48c7431fcb55531b9");
    }

    /**
     * Singleton retrieval of the BlockCypher.
     *
     * @return The singleton.
     */
    public static synchronized BlockCypher getInstance() {
        Log.d(TAG, "getInstance()");
        if (null == mBc) {
            mBc = new BlockCypher();
        }
        return mBc;
    }

    public BigDecimal getBalance(String address) {
        Log.d(TAG, "getBalance");
        try {
            Address a = mBcCtx.getAddressService().getAddress(address);
            Log.d(TAG, "address:" + address + " balance:" + a.getBalance() + " final:" + a.getFinalBalance());
            BigDecimal d = a.getFinalBalance();
            return d;
        } catch (Exception e) {
            Log.d(TAG, "e:" + e.toString());
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
    public List<String> sendBitcoin(String from, String to, long amount, boolean feeIncluded, long txFees) throws BlockCypherException {
        try {
            IntermediaryTransaction unsignedTx = mBcCtx.getTransactionService().newTransaction(
                    new ArrayList<String>(Arrays.asList(from)), new ArrayList<String>(Arrays.asList(to)), amount, txFees);
            if ((unsignedTx == null) || unsignedTx.getTosign().size() == 0) {
                Log.d(TAG, "unsignedTx is null or toSign is empty");
                return null;
            }
            // Cache unsignedTx
            Log.d(TAG, "unsignedTx:" + unsignedTx.toString());
            mCachedUnsignedTx = unsignedTx;
            ArrayList al = new ArrayList<String>();
            return unsignedTx.getTosign();
        }
        catch (BlockCypherException e) {
            Log.d(TAG, "e:" + e.toString());
            throw e;
        }
        catch (Exception e) {
            Log.d(TAG, "e:" + e.toString());
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
            Log.d(TAG, "Exception while toDER()" + e.toString());
        }
        return null;
    }

    public Transaction completeSendBitcoin(String publicKey, List<String> sigList) {
        if (mCachedUnsignedTx == null) {
            Log.d(TAG, "mCachedUnsignedTx is null");
            return null;
        }

        if (mCachedUnsignedTx.getTosign().size() != sigList.size()) {
            Log.d(TAG, "toSign number:" + mCachedUnsignedTx.getTosign().size() + " sig number:" + sigList.size());
            mCachedUnsignedTx = null;
            return null;
        }
        for (int i = 0; i < mCachedUnsignedTx.getTosign().size(); i++) {
            String sig = sigList.get(i);
            // Pushing Pub key for input
            mCachedUnsignedTx.addPubKeys(publicKey);
            BigInteger r = new BigInteger(sig.substring(0, 64),16);
            BigInteger s = new BigInteger(sig.substring(64, 128),16);
            String signedString = bytesToHexString(toDER(r, s));
            mCachedUnsignedTx.addSignature(signedString);
        }

        try {
            Transaction tx = mBcCtx.getTransactionService().sendTransaction(mCachedUnsignedTx);

            mCachedUnsignedTx = null;
            return tx;
        }
        catch (BlockCypherException e) {
            Log.d(TAG, "e:" + e.toString());
            mCachedUnsignedTx = null;
            return null;
        }
        catch (Exception e) {
            Log.d(TAG, "e:" + e.toString());
            return null;
        }
    }
}
