package com.dzfashion.inventory.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record ReserveStockRequest(
        @NotNull(message = "Order ID is required")
        UUID orderId,

        @NotNull(message = "Variant ID is required")
        UUID variantId,

        @Positive(message = "Reserved quantity must be greater than zero")
        int quantity,

        @NotBlank(message = "Idempotency key is required")
        @Size(max = 100)
        String idempotencyKey
) {
}
