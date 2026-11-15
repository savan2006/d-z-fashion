package com.dzfashion.catalog.dto.response;

import com.dzfashion.catalog.enums.ProductStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ProductResponse(
        UUID id,
        UUID categoryId,
        String categoryName,
        String categorySlug,
        String name,
        String slug,
        String description,
        String fabric,
        String occasion,
        ProductStatus status,
        BigDecimal defaultPrice,
        List<ProductVariantResponse> variants,
        List<MediaAssetResponse> media,
        Instant createdAt,
        Instant updatedAt
) {
}
