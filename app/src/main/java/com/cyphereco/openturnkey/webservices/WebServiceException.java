package com.cyphereco.openturnkey.webservices;

import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

public class WebServiceException extends Exception {
    public WebServiceException(WebTarget wt, Response resp) {

    }
}
