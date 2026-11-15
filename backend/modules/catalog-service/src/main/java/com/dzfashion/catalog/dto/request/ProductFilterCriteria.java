package com.dzfashion.catalog.dto.request;

import com.dzfashion.catalog.enums.ProductStatus;

import java.math.BigDecimal;
import java.util.UUID;

public record ProductFilterCriteria(
        UUID categoryId,
        String categorySlug,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        String size,
        String colour,
        String fabric,
        String occasion,
        String search,
        ProductStatus status
) {
}
