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

    public void setSessionId(String sessionId) {
        // sessionId can only be set once
        if (this.sessionId.length() == 0)
            this.sessionId = sessionId;
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

    public void setData(String data) {
        this.data = data;
        if (this.data == null)
            this.data = "";

        logger.debug("Update request ({}) data: {}", this.command, data);
    }

    public String getOption() {
        return option;
    }

    public void setOption(String option) {
        this.option = option;
        logger.debug("Update request ({}) option: {}", this.command, option);
    }

    public String getOtkAddress() { return otkAddress; }

    public void setOtkAddress(String otkAddress) {
        // the OtkAddress can only be set once
        if (this.otkAddress.length() == 0)
            this.otkAddress = otkAddress;
        logger.debug("Set request ({}) OtkAddress: {}", this.command, otkAddress);
    }

    public String getPin() {
        String pin = "";
        String[] strList = this.option.split(",");

        for (int i = 0; i < strList.length; i++) {
            if (strList[i].contains("pin=")) {
                pin = strList[i].substring(4);
                return pin;
            }
        }
        return pin;
    }

    public void setPin(String pin) {
        setOption(getOption() + ",pin=" + pin);
    }

    public void setMore() {
        setOption(getOption() + ",more=1");
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
