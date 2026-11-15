package com.dzfashion.catalog.dto.response;

import com.dzfashion.catalog.enums.ProductStatus;

import java.math.BigDecimal;
import java.util.UUID;

public record ProductSummaryResponse(
        UUID id,
        String categoryName,
        String name,
        String slug,
        String fabric,
        String occasion,
        BigDecimal defaultPrice,
        String primaryImageUrl,
        ProductStatus status
) {
}
