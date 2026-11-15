package com.dzfashion.commerce.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CancelOrderRequest(
        @NotBlank(message = "Cancellation reason is required")
        String reason
) {
}
