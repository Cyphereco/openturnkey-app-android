package com.cyphereco.openturnkey.webservices;

import android.system.ErrnoException;

import com.cyphereco.openturnkey.core.Tx;

import java.math.BigDecimal;
import java.net.ConnectException;

public abstract class BtcBase {
    // TODO ...
    protected abstract BigDecimal getBalance(String address);
    protected abstract Tx getTx(String hash);

}
