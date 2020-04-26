package com.cyphereco.openturnkey.webservices;

import com.blockcypher.model.transaction.Transaction;
import com.cyphereco.openturnkey.ui.Preferences;
import com.cyphereco.openturnkey.utils.Log4jHelper;

import org.slf4j.Logger;

import java.math.BigDecimal;
import java.util.Objects;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Response;

import org.json.*;

import static com.cyphereco.openturnkey.utils.BtcUtils.convertDateTimeStringToLong;

/*
 * References:
 *
 * -To get balance for those tx confirmation >= 6
 * https://blockchain.info/balance?active=13GNvHaSjhs8dLbEWrRP7Lvbb8ZBsKUU4P
 * {"13GNvHaSjhs8dLbEWrRP7Lvbb8ZBsKUU4P":{"final_balance":4991000,"n_tx":7,"total_received":5000000}}
 *
 * -Get latest block
 * https://blockchain.info/latestblock
 * {
 *   "hash":"0000000000000538200a48202ca6340e983646ca088c7618ae82d68e0c76ef5a",
 *   "time":1325794737,
 *   "block_index":841841,
 *   "height":160778,
 *   "txIndexes":[13950369,13950510,13951472]
 * }
 *
 * -Get tx:
 * https://blockchain.info/rawtx/86d258468e4a3fe4d7c3eca7b52565871165431a20cbeb1963b3a9a422bc8be3
 *
 * -Get raw tx:
 * https://blockchain.info/rawtx/74d350ca44c324f4643274b98801f9a023b2b8b72e8e895879fd9070a68f7f1f?format=hex
 *
 * -Get confirmations for a tx:
 * Current block height - "block_height" for the tx + 1
 *
 *
 */
public class BlockChainInfo {
    public static final String TAG = BlockChainInfo.class.getSimpleName();
    private static Logger logger = Log4jHelper.getLogger(TAG);

    private static String URI = "https://blockchain.info/";
    private static String PATH_RAWTX = "rawtx";
    private static long latestBlockHeight = -1;

    public static BigDecimal getBalance(String address) {
        BigDecimal ret = new BigDecimal(0);
        String PATH_ADDRESS_BALANCE = "balance";
        String PARAMETER_ACTIVE = "active";

        try {
            Client webClient = ClientBuilder.newClient();
            Response response = webClient.target(URI).path(PATH_ADDRESS_BALANCE).queryParam(PARAMETER_ACTIVE, address)
                    .request().get();
            JSONObject json = new JSONObject(response.readEntity(String.class));
            String finalBalance = json.getJSONObject(address).getString("final_balance");
            ret = new BigDecimal(finalBalance);
            webClient.close();
        } catch (Exception e) {
            logger.error("e:{}", e.toString());
        }
        logger.debug("getBalance ({}): {}", address, ret);
        return ret;
    }

    public static void getBalance(final String address, final WebResultHandler handler) {
        if (handler != null) {
            new Thread() {
                @Override
                public void run() {
                    synchronized (this) {
                        if (Preferences.isTestnet()) {
                            handler.onBalanceUpdated(BlockCypher.getBalance(address));
                        }
                        else {
                            handler.onBalanceUpdated(getBalance(address));
                        }
                    }
                }
            }.start();
        }
    }

    public static long getLatestBlockHeight() {
        String PATH_LATESTBLOCK = "latestblock";
        if (Preferences.isTestnet()) {
            return BlockCypher.getBlockHeight();
        }
        try {
            Client webClient = ClientBuilder.newClient();
            Response response = webClient.target(URI)
                    .path(PATH_LATESTBLOCK)
                    .request()
                    .get();
            JSONObject json = new JSONObject(response.readEntity(String.class));
            String height = json.getString("height");
            latestBlockHeight = Long.parseLong(height);
            webClient.close();
        } catch (Exception e) {
            logger.error("e:{}", e.toString());
        }
        logger.debug("Update latest block height: {}", latestBlockHeight);
        return latestBlockHeight;
    }

    public static void getLatestBlockHeight(WebResultHandler handler) {
        if (handler != null) {
            handler.onBlockHeightUpdated(getLatestBlockHeight());
        }
    }

    public static long getTxTime(String txHash) {
        long ret = 0;
        if (Preferences.isTestnet()) {
            return convertDateTimeStringToLong(Objects.requireNonNull(BlockCypher.getTransaction(txHash, false)).getReceived());
        }
        try {
            Client webClient = ClientBuilder.newClient();
            Response response = webClient.target(URI).path(PATH_RAWTX).path(txHash)
                    .request().get();
            String body = response.readEntity(String.class);
            JSONObject json = new JSONObject(body);
            String time = json.getString("time");
            ret = Long.parseLong(time);
            webClient.close();
        } catch (Exception e) {
            logger.error("e:{}", e.toString());
        }
//        logger.info("getTxTime ({}): {}", txHash, ret);
        return ret;
    }

    public static void getTxTime(String txHash, WebResultHandler handler) {
        if (handler != null) {
            handler.onTxTimeUpdated(getTxTime(txHash));
        }
    }

    public static long getTxBlockHeight(String txHash) {
        long ret = 0;
        if (Preferences.isTestnet()) {
            Transaction tx = BlockCypher.getTransaction(txHash, false);
            return (tx == null) ? -1 : tx.getBlockHeight();
        }
        try {
            Client webClient = ClientBuilder.newClient();
            Response response = webClient.target(URI).path(PATH_RAWTX).path(txHash)
                    .request().get();
            String body = response.readEntity(String.class);
            JSONObject json = new JSONObject(body);
            String height = json.getString("block_height");
            ret = Long.parseLong(height);
            webClient.close();
        } catch (Exception e) {
            logger.error("e:{}", e.toString());
        }
//        logger.info("getTxBlockHeight ({}): {}", txHash, ret);
        return ret;
    }

    public static void getTxBlockHeight(String txHash, WebResultHandler handler) {
        if (handler != null) {
            handler.onTxBlockHeightUpdated(getTxBlockHeight(txHash));
        }
    }

    public static String getRawTx(String txHash) {
        String rawTx = null;
        String PARAMETER_FORMAT = "format";
        String PARAMETER_VALUE_HEX = "hex";
        try {
            Client webClient = ClientBuilder.newClient();
            Response response = webClient.target(URI).path(PATH_RAWTX).path(txHash).queryParam(PARAMETER_FORMAT, PARAMETER_VALUE_HEX)
                    .request().get();
            rawTx = response.readEntity(String.class);
            webClient.close();
        } catch (Exception e) {
            logger.error("e:{}", e.toString());
        }
//        logger.info("getRawTx ({}): {}", txHash, rawTx);
        return rawTx;
    }

    public static void getRawTx(String txHash, WebResultHandler handler) {
        if (handler != null) {
            handler.onRawTxUpdated(getRawTx(txHash));
        }
    }

    public static long getConfirmations(String tx) {
        return (latestBlockHeight < 0) ? latestBlockHeight : latestBlockHeight - getTxBlockHeight(tx) + 1;
    }

    public static void getConfirmations(String tx, WebResultHandler handler) {
        if (handler != null) {
            handler.onConfirmationsUpdated(getConfirmations(tx));
        }
    }

    public interface WebResultHandler {
        void onBalanceUpdated(BigDecimal balance);

        void onBlockHeightUpdated(long height);

        void onTxTimeUpdated(long time);

        void onTxBlockHeightUpdated(long height);

        void onRawTxUpdated(String rawTx);

        void onConfirmationsUpdated(long confirmations);
    }
}
