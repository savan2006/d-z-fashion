package com.dzfashion.commerce.client;

import java.math.BigDecimal;
import java.util.UUID;

public record VariantSnapshot(
        UUID variantId,
        UUID productId,
        String productName,
        String sku,
        String size,
        String colour,
        BigDecimal price,
        boolean active,
        boolean published
) {
}
