package com.cyphereco.openturnkey.core.protocol;

import java.io.Serializable;
import java.util.Locale;
import java.util.Random;

public class OtkRequest implements Serializable {

    private String sessionId = "";   // default empty, set after request sent
    private String requestId;   // auto-generated in all constructors
    private String command;     // mandatory require parameter in all constructors
    private String data = "";        // optional
    private String option = "";      // optional

    private String otkAddress = "";
    private String pin = "";

    public OtkRequest(String command) {
        this.requestId = getId();
        this.command = command;
        this.data = "nil";
    }

    public OtkRequest(String command, String data) {
        this.requestId = getId();
        this.command = command;
        this.data = data;
        if (this.data.length() == 0)
            this.data = "nil";
    }

    public OtkRequest(String command, String data, String option) {
        this.requestId = getId();
        this.command = command;
        this.data = data;
        this.option = option;
        if (this.data.length() == 0)
            this.data = "nil";
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
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
        if (this.data.length() == 0)
            this.data = "nil";
    }

    public String getOption() {
        return option;
    }

    public void setOption(String option) {
        this.option = option;
    }

    public String getOtkAddress() {
        return otkAddress;
    }

    public void setOtkAddress(String otkAddress) {
        if (this.otkAddress.length() == 0)
            this.otkAddress = otkAddress;
    }

    public String getPin() {
        return pin;
    }

    public void setPin(String pin) {
        this.pin = pin;
        this.option += ",pin=" + pin;
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
