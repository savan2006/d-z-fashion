package com.dzfashion.fulfillment.dto.document;

import java.time.Instant;
import java.util.List;

public record DailyPickingListDocument(
        String warehouseName,
        Instant generatedAt,
        int totalOrders,
        int totalUnitsToPick,
        List<SkuPickSummary> items
) {
    public record SkuPickSummary(
            String sku,
            String productName,
            String size,
            String color,
            String binLocation,
            int totalQuantity,
            List<OrderAllocation> allocations
    ) {}

    public record OrderAllocation(
            String orderNumber,
            int quantity
    ) {}
}
