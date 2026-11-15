package com.dzfashion.inventory.dto.response;

import com.dzfashion.inventory.enums.ReservationStatus;

import java.time.Instant;
import java.util.UUID;

public record ReservationResponse(
        UUID reservationId,
        UUID orderId,
        UUID variantId,
        int quantity,
        ReservationStatus status,
        String idempotencyKey,
        Instant createdAt
) {
}
