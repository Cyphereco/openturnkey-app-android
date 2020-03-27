package com.cyphereco.openturnkey.webservices;

import com.blockcypher.context.BlockCypherContext;
import com.blockcypher.exception.BlockCypherException;
import com.blockcypher.model.address.Address;
import com.blockcypher.model.transaction.Transaction;
import com.blockcypher.model.transaction.intermediary.IntermediaryTransaction;
import com.blockcypher.model.transaction.output.Output;
import com.cyphereco.openturnkey.core.UnsignedTx;
import com.cyphereco.openturnkey.db.RecordTransaction;
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
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Semaphore;

public class BlockCypher {
    public static final String TAG = BlockCypher.class.getSimpleName();
    private static Logger logger = Log4jHelper.getLogger(TAG);

    private static final String TOKEN = "7744d177ce1e4ef48c7431fcb55531b9";
    private static BlockCypherContext mBcCtx;
    private static IntermediaryTransaction mCachedUnsignedTx = null;
    private static boolean useToken = false;

    private static final int MAX_AVAILABLE = 2;
    private static final Semaphore semaphoreWebRequest = new Semaphore(MAX_AVAILABLE, true);

    private static void newBlockCypherContext() {
        String network = Preferences.isTestnet() ? "test3" : "main";

        // BlockCypherContext with token
        String token = useToken ? TOKEN : "";
        mBcCtx = new BlockCypherContext("v1", "btc", network, token);
        logger.debug("Re-Init BlcokCypher ({}) with Token: {}", network, useToken);
    }

    public static void reInit() {
        mBcCtx = null;
        newBlockCypherContext();
    }

    public static BigDecimal getBalance(String address) {
        if (mBcCtx == null) newBlockCypherContext();

        logger.debug("getBalance");
        try {
            semaphoreWebRequest.acquire();
            Address a = mBcCtx.getAddressService().getAddress(address);
            semaphoreWebRequest.release();
            logger.debug("address:" + address + " balance:" + a.getBalance() + " final:" + a.getFinalBalance());
            return a.getFinalBalance();
        }
        catch (Exception e) {
            semaphoreWebRequest.release();
            if (e != null && e.toString().contains("429")) {
                useToken = !(useToken);
                mBcCtx = null;
            }
        }
        return BigDecimal.valueOf(-1);
    }

    public static long getBlockHeight() {
        if (mBcCtx == null) newBlockCypherContext();

        try{
            semaphoreWebRequest.acquire();
            long height = mBcCtx.getInfoService().getInfo().getHeight();
            semaphoreWebRequest.release();

            return height;
        }
        catch (Exception e) {
            semaphoreWebRequest.release();
            if (e != null && e.toString().contains("429")) {
                useToken = !(useToken);
                mBcCtx = null;
            }
        }
        return 0;
    }

    public static Transaction getTransaction(String hash, boolean includeHex) {
        if (mBcCtx == null) newBlockCypherContext();

        try {
            semaphoreWebRequest.acquire();
            Transaction tx = mBcCtx.getTransactionService().getTransaction(hash, includeHex);
            semaphoreWebRequest.release();
            return tx;
        }
        catch (Exception e) {
            semaphoreWebRequest.release();
            if (e != null && e.toString().contains("429")) {
                useToken = !(useToken);
                mBcCtx = null;
            }
        }
        return null;
    }

    public static UnsignedTx newTransaction(String from, String to, long amount, long txFees) throws BlockCypherException, InterruptedException {
        if (mBcCtx == null) newBlockCypherContext();

//        logger.debug("sendBitcoin() from:{} to:{} amount:{} fee:{} feeIncluded:{}", from, to, amount, txFees, feeIncluded);
        try {
            IntermediaryTransaction unsignedTx;
            semaphoreWebRequest.acquire();
            if (txFees == 0) {
                // set "prefrence":"zero"
                unsignedTx = mBcCtx.getTransactionService().newTransaction(
                        new ArrayList<>(Collections.singletonList(from)), new ArrayList<>(Collections.singletonList(to)), amount, "zero");
            } else {
                unsignedTx = mBcCtx.getTransactionService().newTransaction(
                        new ArrayList<>(Collections.singletonList(from)), new ArrayList<>(Collections.singletonList(to)), amount, txFees);
            }
            semaphoreWebRequest.release();;
            if ((unsignedTx == null) || unsignedTx.getTosign().size() == 0) {
                logger.debug("unsignedTx is null or toSign is empty");
                return null;
            }
            // Cache unsignedTx
//            logger.debug("unsignedTx:" + unsignedTx.toString());
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
            return new UnsignedTx(from, to, a, txFees, unsignedTx.getTosign());
        }
        catch (Exception e) {
            semaphoreWebRequest.release();
            if (e != null && e.toString().contains("429")) {
                useToken = !(useToken);
                mBcCtx = null;
            }
            throw e;
        }
    }

    public static String parseError(String error) {
        // Error{error='xxx'}
        // Temporarily skip first few leters and last two letters
        int len = error.length();
        return error.substring(13, len - 2);
    }

    private static String bytesToHexString(byte[] bytes) {
        StringBuilder buf = new StringBuilder(bytes.length * 2);
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
        DERSequenceGenerator seq;
        byte[] res;
        try {
            seq = new DERSequenceGenerator(bos);
            seq.addObject(new DERInteger(r));
            seq.addObject(new DERInteger(s));
            seq.close();
            res = bos.toByteArray();
            return res;
        } catch (IOException e) {
            logger.debug("Exception while toDER()" + e.toString());
        }
        return null;
    }

    public static RecordTransaction completeSendBitcoin(String publicKey,
                List<String> sigList, String to) throws BlockCypherException, Exception {
        if (mBcCtx == null) newBlockCypherContext();

        if (mCachedUnsignedTx == null) {
            logger.debug("mCachedUnsignedTx is null");
            return null;
        }

        if (mCachedUnsignedTx.getTosign().size() != sigList.size()) {
            logger.debug("toSign number:" + mCachedUnsignedTx.getTosign().size() + " sig number:" + sigList.size());
            mCachedUnsignedTx = null;
            return null;
        }

        for (int i = 0; i < mCachedUnsignedTx.getTosign().size(); i++) {
            String sig = sigList.get(i);
            // Pushing Pub key for input
            mCachedUnsignedTx.addPubKeys(publicKey);
            BigInteger r = new BigInteger(sig.substring(0, 64), 16);
            BigInteger s = new BigInteger(sig.substring(64, 128), 16);
            // To ensure low S values for BIP 62
            s = BtcUtils.lowSValue(s);
            String signedString = bytesToHexString(toDER(r, s));
            mCachedUnsignedTx.addSignature(signedString);
        }

        Transaction trans;

        try {
            semaphoreWebRequest.acquire();
            trans = mBcCtx.getTransactionService().sendTransaction(mCachedUnsignedTx);
            semaphoreWebRequest.release();
            logger.debug("TX Sent, Hash({}) ", trans.getHash());
            RecordTransaction recordTransaction = new RecordTransaction(trans);
            return recordTransaction;
        }
        catch (Exception e) {
            semaphoreWebRequest.release();
            if (e != null && e.toString().contains("429")) {
                useToken = !(useToken);
                mBcCtx = null;
            }
            throw e;
        }
    }
}
