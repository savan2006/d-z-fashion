package com.dzfashion.inventory.dto.response;

import java.time.Instant;
import java.util.UUID;

public record StockResponse(
        UUID variantId,
        String sku,
        int physicalQty,
        int reservedQty,
        int availableQty,
        int quarantinedQty,
        int lowStockThreshold,
        Instant updatedAt
) {
}
