package com.cyphereco.openturnkey.utils;

import java.util.List;
import java.util.regex.Pattern;

public class BtcExchangeRates {
    private double rate_cny;
    private double rate_eur;
    private double rate_jpy;
    private double rate_twd;
    private double rate_usd;

    public BtcExchangeRates(double rate_cny, double rate_eur, double rate_jpy, double rate_twd, double rate_usd) {
        this.rate_cny = rate_cny;
        this.rate_eur = rate_eur;
        this.rate_jpy = rate_jpy;
        this.rate_twd = rate_twd;
        this.rate_usd = rate_usd;
    }

    public BtcExchangeRates(String strBtcExchangeRate) {
        Pattern pattern = Pattern.compile("BtcExchangeRates[{]rate_cny=.*, rate_eur=.*, rate_jpy=.*, rate_twd=.*, rate_usd=.*[}]");
        if (pattern.matcher(strBtcExchangeRate).matches()) {
            String[] listExchagneRates = strBtcExchangeRate.replace("BtcExchangeRates{", "")
                    .replace("}", "")
                    .replace(" ", "").split(",");

            this.rate_cny = Double.parseDouble(listExchagneRates[0].split("=")[1]);
            this.rate_eur = Double.parseDouble(listExchagneRates[1].split("=")[1]);
            this.rate_jpy = Double.parseDouble(listExchagneRates[2].split("=")[1]);
            this.rate_twd = Double.parseDouble(listExchagneRates[3].split("=")[1]);
            this.rate_usd = Double.parseDouble(listExchagneRates[4].split("=")[1]);
        }
        else {
            new BtcExchangeRates(0, 0, 0, 0, 0);
        }
    }

    public double getRate_cny() {
        return rate_cny;
    }

    public void setRate_cny(double rate_cny) {
        this.rate_cny = rate_cny;
    }

    public double getRate_eur() {
        return rate_eur;
    }

    public void setRate_eur(double rate_eur) {
        this.rate_eur = rate_eur;
    }

    public double getRate_jpy() {
        return rate_jpy;
    }

    public void setRate_jpy(double rate_jpy) {
        this.rate_jpy = rate_jpy;
    }

    public double getRate_twd() {
        return rate_twd;
    }

    public void setRate_twd(double rate_twd) {
        this.rate_twd = rate_twd;
    }

    public double getRate_usd() {
        return rate_usd;
    }

    public void setRate_usd(double rate_usd) {
        this.rate_usd = rate_usd;
    }

    @Override
    public String toString() {
        return "BtcExchangeRates{" +
                "rate_cny=" + rate_cny +
                ", rate_eur=" + rate_eur +
                ", rate_jpy=" + rate_jpy +
                ", rate_twd=" + rate_twd +
                ", rate_usd=" + rate_usd +
                '}';
    }
}
