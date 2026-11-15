package com.dzfashion.inventory.dto.response;

import com.dzfashion.inventory.enums.StockMovementReason;

import java.time.Instant;
import java.util.UUID;

public record StockMovementResponse(
        UUID id,
        UUID variantId,
        int changeQty,
        int balanceAfter,
        StockMovementReason reason,
        String referenceType,
        String referenceId,
        UUID actorId,
        Instant createdAt
) {
}
