package com.cyphereco.openturnkey.bitcoin;

/**
 * An ECException is thrown if an error occurs in an elliptic curve cryptographic
 * function
 */
public class ECException extends Exception {

    /**
     * Creates a new exception with a detail message
     *
     * @param       msg             Detail message
     */
    public ECException(String msg) {
        super(msg);
    }

    /**
     * Creates a new exception with a detail message and cause
     *
     * @param       msg             Detail message
     * @param       t               Caught exception
     */
    public ECException(String msg, Throwable t) {
        super(msg, t);
    }
}
