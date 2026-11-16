package com.dzfashion.fulfillment.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record ReturnEligibilityResponse(
        UUID orderId,
        String orderNumber,
        boolean eligible,
        Instant eligibleUntil,
        LocalDate deliveryDate,
        String reasonIfNotEligible,
        List<ReturnEligibleItem> items
) {
    public record ReturnEligibleItem(
            UUID orderItemId,
            String sku,
            String productName,
            int deliveredQuantity,
            int activeReturnQuantity,
            int eligibleQuantity,
            BigDecimal unitPrice
    ) {}
}
