package com.cyphereco.openturnkey.utils;

public enum LocalCurrency {
    LOCAL_CURRENCY_TWD("TWD"),
    LOCAL_CURRENCY_USD("USD"),
    LOCAL_CURRENCY_JPY("JPY"),
    LOCAL_CURRENCY_EUR("EUR"),
    LOCAL_CURRENCY_CNY("CNY");

    private final String value;
    private LocalCurrency(String s) {
        value = s;
    }

    public String toString(){
        return value;
    }
}
