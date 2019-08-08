package com.cyphereco.openturnkey.webservices;

import android.system.ErrnoException;

import java.math.BigDecimal;
import java.net.ConnectException;

public abstract class BtcBase {
    // TODO ...
    protected abstract BigDecimal getBalance(String address);
}
