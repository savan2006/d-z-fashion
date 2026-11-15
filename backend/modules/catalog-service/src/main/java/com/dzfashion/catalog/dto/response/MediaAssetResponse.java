package com.dzfashion.catalog.dto.response;

import java.util.UUID;

public record MediaAssetResponse(
        UUID id,
        UUID variantId,
        String cloudinaryPublicId,
        String secureUrl,
        String format,
        Integer width,
        Integer height,
        String altText,
        int displayOrder,
        boolean primary
) {
}
