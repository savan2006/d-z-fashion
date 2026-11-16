package com.dzfashion.fulfillment.dto.document;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record PackingSlipDocument(
        String orderNumber,
        String warehouseLocation,
        Instant generatedAt,
        String recipientName,
        String maskedPhone,
        String shippingAddress,
        String city,
        String state,
        String pincode,
        boolean cod,
        BigDecimal codAmount,
        List<PackingSlipItem> items
) {
    public record PackingSlipItem(
            String sku,
            String productName,
            String size,
            String color,
            int quantity
    ) {}
}
