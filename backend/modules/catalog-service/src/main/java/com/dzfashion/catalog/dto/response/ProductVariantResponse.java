package com.dzfashion.catalog.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

public record ProductVariantResponse(
        UUID id,
        String sku,
        String size,
        String colour,
        BigDecimal priceOverride,
        BigDecimal effectivePrice,
        boolean active
) {
}
