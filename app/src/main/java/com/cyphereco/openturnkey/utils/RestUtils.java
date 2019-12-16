package com.cyphereco.openturnkey.utils;

import com.blockcypher.utils.gson.GsonFactory;
import com.cyphereco.openturnkey.webservices.BlockCypher;
import com.cyphereco.openturnkey.webservices.WebServiceException;

import org.slf4j.Logger;

import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

public final class RestUtils {
    public static final String TAG = RestUtils.class.getSimpleName();
    static Logger logger = Log4jHelper.getLogger(TAG);

    private static <T> T get(WebTarget webTarget, Class<T> clazz) throws WebServiceException {
        Response response = null;
        try {
            response = webTarget.request(MediaType.APPLICATION_JSON).get();
            T returnedObject = GsonFactory.getGson().fromJson(response.readEntity(String.class), clazz);
            if (response.getStatus() != 200) {
                throw new WebServiceException(webTarget, response);
            } else {
                return returnedObject;
            }
        } catch (IllegalStateException ex) {
            throw new WebServiceException(webTarget, response);
        }
    }
}
