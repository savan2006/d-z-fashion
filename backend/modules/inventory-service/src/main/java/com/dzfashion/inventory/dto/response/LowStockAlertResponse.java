package com.dzfashion.inventory.dto.response;

import java.time.Instant;
import java.util.UUID;

public record LowStockAlertResponse(
        UUID id,
        UUID variantId,
        String sku,
        int availableQty,
        int threshold,
        Instant alertSentAt,
        boolean isResolved
) {
}
