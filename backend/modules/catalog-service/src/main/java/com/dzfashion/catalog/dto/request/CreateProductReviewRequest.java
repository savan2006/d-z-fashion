package com.dzfashion.catalog.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record CreateProductReviewRequest(
        @NotNull(message = "orderId is required")
        UUID orderId,

        @NotNull(message = "orderItemId is required")
        UUID orderItemId,

        @Min(value = 1, message = "Rating must be at least 1 star")
        @Max(value = 5, message = "Rating cannot exceed 5 stars")
        int rating,

        @Size(max = 150, message = "Review title cannot exceed 150 characters")
        String title,

        @NotBlank(message = "Review comment is required")
        @Size(min = 10, max = 2000, message = "Review comment must be between 10 and 2000 characters")
        String comment,

        @Size(max = 5, message = "Cannot attach more than 5 photos")
        List<String> photoUrls
) {
}
