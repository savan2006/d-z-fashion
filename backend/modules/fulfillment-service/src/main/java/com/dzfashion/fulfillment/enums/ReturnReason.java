package com.dzfashion.fulfillment.enums;

public enum ReturnReason {
    DAMAGED,
    DEFECTIVE,
    INCORRECT_PRODUCT;

    public static boolean isValidReason(String reasonStr) {
        if (reasonStr == null) return false;
        try {
            valueOf(reasonStr.trim().toUpperCase());
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
