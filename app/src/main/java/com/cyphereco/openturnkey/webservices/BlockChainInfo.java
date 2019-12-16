package com.cyphereco.openturnkey.webservices;

import android.content.Context;

import com.blockcypher.model.address.Address;
import com.blockcypher.utils.gson.GsonFactory;
import com.cyphereco.openturnkey.core.Tx;
import com.cyphereco.openturnkey.utils.Log4jHelper;
import com.google.gson.JsonObject;

import org.glassfish.jersey.client.ClientConfig;
import org.slf4j.Logger;

import java.math.BigDecimal;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.json.*;

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
 * Current block hight - "block_height" for the tx + 1
 *
 *
 */
public class BlockChainInfo extends BtcBase {
    public static final String TAG = BlockChainInfo.class.getSimpleName();
    static Logger logger = Log4jHelper.getLogger(TAG);
    private static BlockChainInfo mBci = null;

    private Client webClient = ClientBuilder.newClient();

    private String URI = "https://blockchain.info/";
    private String PATH_ADDRESS_BALANCE = "balance";
    private String PARAMETER_ACTIVE = "active";
    private String PATH_RAWTX = "rawtx";
    private String PATH_LATESTBLOCK = "latestblock";
    private String PARAMETER_RAWTX_HEX = "?format=hex";

    private class Address {

    }

    public BlockChainInfo () {

    }

    /**
     * Singleton retrieval of the BlockCypher.
     *
     * @return The singleton.
     */
    public static synchronized BlockChainInfo getInstance(Context ctx) {
        if (null == mBci) {
            mBci = new BlockChainInfo();
        }
        return mBci;
    }
    /**
     * Get balance in satoshi
     * @param address
     * @return
     */
     public BigDecimal getBalance(String address) {
        logger.info("getBalance:{}", address);
        try {
            Response response  = webClient.target(URI).path(PATH_ADDRESS_BALANCE).queryParam(PARAMETER_ACTIVE, address)
                    .request().get();
            JSONObject json = new JSONObject(response.readEntity(String.class));
            String finalBalance = json.getJSONObject(address).getString("final_balance");
            return new BigDecimal(finalBalance);
        }
        catch (Exception e ) {
            logger.error("e:{}", e.toString());
        }
        return BigDecimal.valueOf(-1);
    }

    public int getLatestBlochHight() {
         int ret = -1;

        logger.debug("getLatestBlochHight");
        Response response  = webClient.target(URI).path(PATH_LATESTBLOCK)
                .request().get();
        try {
            JSONObject json = new JSONObject(response.readEntity(String.class));
            String hight = json.getString("height");
            return Integer.parseInt(hight);
        }
        catch (Exception e ) {
            logger.error("e:{}", e.toString());
        }
        return ret;
    }

    public int getTxBlockHight(String tx) {
        int ret = -1;

        logger.debug("getTxBlockHight:{}", tx);
        Response response  = webClient.target(URI).path(PATH_RAWTX).path(tx)
                .request().get();
        try {
            JSONObject json = new JSONObject(response.readEntity(String.class));
            String hight = json.getString("block_height");
            return Integer.parseInt(hight);
        }
        catch (Exception e ) {
            logger.error("e:{}", e.toString());
        }
        return ret;
    }

    public int getConfirmations(String tx) {
         return getLatestBlochHight() - getTxBlockHight(tx) + 1;
    }

    public Tx getTx(String hash) {
        return null;
    }
}
