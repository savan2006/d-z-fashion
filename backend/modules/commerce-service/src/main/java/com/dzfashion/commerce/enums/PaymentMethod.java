package com.dzfashion.commerce.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum PaymentMethod {
    SIMULATED_ONLINE,
    COD;

    @JsonCreator
    public static PaymentMethod fromString(String value) {
        if (value == null) return null;
        String normalized = value.trim().toUpperCase();
        if ("CASH_ON_DELIVERY".equals(normalized) || "COD".equals(normalized)) {
            return COD;
        }
        if ("SIMULATED_ONLINE".equals(normalized) || "ONLINE".equals(normalized)) {
            return SIMULATED_ONLINE;
        }
        return PaymentMethod.valueOf(normalized);
    }
}
