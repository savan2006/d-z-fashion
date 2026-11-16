package com.dzfashion.catalog.dto.response;

import java.util.Map;
import java.util.UUID;

public record ProductReviewSummaryResponse(
        UUID productId,
        double averageRating,
        long totalReviews,
        Map<Integer, Long> ratingCounts
) {
}
