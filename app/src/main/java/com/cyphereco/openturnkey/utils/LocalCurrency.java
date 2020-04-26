package com.cyphereco.openturnkey.utils;

import org.jetbrains.annotations.NotNull;

public enum LocalCurrency {
    LOCAL_CURRENCY_CNY("CNY"),
    LOCAL_CURRENCY_EUR("EUR"),
    LOCAL_CURRENCY_JPY("JPY"),
    LOCAL_CURRENCY_TWD("TWD"),
    LOCAL_CURRENCY_USD("USD");

    private final String value;

    LocalCurrency(String s) {
        value = s;
    }

    @NotNull
    public String toString() {
        return value;
    }
}
