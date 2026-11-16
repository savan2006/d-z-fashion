package com.dzfashion.payment.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ReversePaymentRequest(
        @NotNull(message = "Order ID is required")
        UUID orderId,

        @NotBlank(message = "Reason is required")
        String reason
) {
}
