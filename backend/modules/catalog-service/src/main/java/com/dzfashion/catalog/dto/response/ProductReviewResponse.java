package com.dzfashion.catalog.dto.response;

import com.dzfashion.catalog.enums.ReviewStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ProductReviewResponse(
        UUID id,
        UUID productId,
        String productName,
        UUID customerId,
        UUID orderId,
        UUID orderItemId,
        int rating,
        String title,
        String comment,
        List<String> photoUrls,
        boolean verifiedPurchase,
        ReviewStatus status,
        String moderationReason,
        Instant createdAt,
        Instant updatedAt
) {
}
