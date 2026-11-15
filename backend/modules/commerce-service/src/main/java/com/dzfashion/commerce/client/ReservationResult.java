package com.dzfashion.commerce.client;

import java.util.UUID;

public record ReservationResult(
        UUID reservationId,
        UUID variantId,
        int quantity,
        boolean success,
        String message
) {
    public static ReservationResult success(UUID reservationId, UUID variantId, int quantity) {
        return new ReservationResult(reservationId, variantId, quantity, true, "Reserved successfully");
    }

    public static ReservationResult failure(UUID variantId, int quantity, String message) {
        return new ReservationResult(null, variantId, quantity, false, message);
    }
}
