package com.dzfashion.catalog.dto.request;

import com.dzfashion.catalog.enums.ReviewStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ModerateReviewRequest(
        @NotNull(message = "Review status is required")
        ReviewStatus status,

        @NotBlank(message = "Moderation reason is required")
        @Size(min = 5, max = 500, message = "Moderation reason must be between 5 and 500 characters")
        String reason
) {
}
