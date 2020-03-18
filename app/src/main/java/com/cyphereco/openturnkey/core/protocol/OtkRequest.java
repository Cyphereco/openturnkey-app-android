package com.cyphereco.openturnkey.core.protocol;

import com.cyphereco.openturnkey.utils.Log4jHelper;

import org.slf4j.Logger;

import java.io.Serializable;
import java.util.Locale;
import java.util.Random;

public class OtkRequest implements Serializable {
    private static final String TAG = "OtkRequest";
    Logger logger = Log4jHelper.getLogger(TAG);

    private String sessionId = "";   // default empty, set after request sent
    private String requestId;   // auto-generated in all constructors
    private String command;     // mandatory require parameter in all constructors
    private String data = "";        // optional
    private String option = "";      // optional

    private String pin = "";
    private String otkAddress = "";

    public OtkRequest(String command) {
        this.requestId = getId();
        this.command = command;
        this.data = "";
    }

    public OtkRequest(String command, String data) {
        this.requestId = getId();
        this.command = command;
        this.data = data;
        if (this.data == null)
            this.data = "";
    }

    public OtkRequest(String command, String data, String option) {
        this.requestId = getId();
        this.command = command;
        this.data = data;
        this.option = option;
        if (this.data == null)
            this.data = "";
    }

    public String getSessionId() {
        return sessionId;
    }

    public OtkRequest setSessionId(String sessionId) {
        // sessionId can only be set once
        if (this.sessionId.length() == 0)
            this.sessionId = sessionId;
        return this;
    }

    public String getRequestId() {
        return requestId;
    }

    public String getCommand() {
        return command;
    }

    public String getData() {
        return data;
    }

    public OtkRequest setData(String data) {
        this.data = data;
        if (this.data == null)
            this.data = "";

        logger.debug("Update request ({}) data: {}", this.command, data);
        return this;
    }

    public String getOption() {
        return option;
    }

    public OtkRequest setOption(String option) {
        this.option = option;
        logger.debug("Update request ({}) option: {}", this.command, option);
        return this;
    }

    public String getOtkAddress() { return otkAddress; }

    public OtkRequest setOtkAddress(String otkAddress) {
        // the OtkAddress can only be set once
        if (this.otkAddress.length() == 0)
            this.otkAddress = otkAddress;
        logger.debug("Set request ({}) OtkAddress: {}", this.command, otkAddress);
        return this;
    }

    public String getPin() {
        String pin = "";
        String[] strList = this.option.split(",");

        for (String s : strList) {
            if (s.contains("pin=")) {
                pin = s.substring(4);
                return pin;
            }
        }
        return pin;
    }

    public OtkRequest setPin(String pin) {
        setOption(getOption() + ",pin=" + pin);
        return this;
    }

    public OtkRequest setMore() {
        if (!hasMore()) {
            setOption(getOption() + ",more=1");
        }
        return this;
    }

    public OtkRequest setMore(boolean more) {
        if (!hasMore() && more) {
            setOption(getOption() + ",more=1");
        }
        return this;
    }

    public boolean hasMore() {
        return this.option.contains("more=1");
    }

    public void setMasterKey() {
        setOption(getOption() + ",key=1");
    }

    @Override
    public String toString() {
        return "OtkRequest{" +
                "sessionId='" + sessionId + '\'' +
                ", requestId='" + requestId + '\'' +
                ", command='" + command + '\'' +
                ", data='" + data + '\'' +
                ", option='" + option + '\'' +
                ", otkAddress='" + otkAddress + '\'' +
                '}';
    }

    private String getId() {
        Random r = new Random();
        int id = r.nextInt();
        while (id <= 0) {
            id = r.nextInt();
        }

        return String.format(Locale.ENGLISH, "%d", id);
    }
}
