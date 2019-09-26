package com.cyphereco.openturnkey.utils;

public class AddressUtils {
    private static final int ALIAS_SHORT_MAX_LENGTH = 10;
    private static final int ADDRESS_SHORT_MAX_LENGTH = 8;
    private static final int ADDRESS_SHORT_HEAD_LENGTH = 4;
    private static final int ADDRESS_SHORT_TIAL_LENGTH = 4;
    private static final String ADDRESS_SHORT_MASK_STR = "****";

    private AddressUtils() {

    }

    public static String getShortAddress(String srcAddress) {
        String retAddress = "--";

        if ((null != srcAddress) && (0 < srcAddress.length())) {
            retAddress = srcAddress;
            if (srcAddress.length() > ADDRESS_SHORT_MAX_LENGTH) {
                retAddress = srcAddress.substring(0, ADDRESS_SHORT_HEAD_LENGTH) +
                        ADDRESS_SHORT_MASK_STR +
                        srcAddress.substring((srcAddress.length() - ADDRESS_SHORT_TIAL_LENGTH));
            }
        }
        return retAddress;
    }

    public static String getShortAlias(String srcAlias) {
        String retAlias = "--";
        if ((null != srcAlias) && (0 < srcAlias.length())) {
            retAlias = srcAlias;
            if (srcAlias.length() > ALIAS_SHORT_MAX_LENGTH) {
                retAlias = srcAlias.substring(0, ALIAS_SHORT_MAX_LENGTH) + "...";
            }
        }
        return retAlias;
    }

}
