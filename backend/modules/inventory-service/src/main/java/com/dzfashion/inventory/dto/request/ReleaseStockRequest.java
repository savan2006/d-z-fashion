package com.dzfashion.inventory.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record ReleaseStockRequest(
        @NotNull(message = "Order ID is required")
        UUID orderId,

        @NotNull(message = "Variant ID is required")
        UUID variantId,

        @Size(max = 100)
        String idempotencyKey
) {
}
