package com.dzfashion.fulfillment.dto.request;

import jakarta.validation.constraints.NotBlank;

public record RejectRefundRequest(
        @NotBlank(message = "Rejection reason is required")
        String reason
) {
}
