package com.dzfashion.inventory.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record InitializeStockRequest(
        @NotNull(message = "Variant ID is required")
        UUID variantId,

        @NotBlank(message = "SKU is required")
        @Size(max = 60, message = "SKU cannot exceed 60 characters")
        String sku,

        @PositiveOrZero(message = "Initial quantity cannot be negative")
        int initialQuantity,

        @PositiveOrZero(message = "Threshold cannot be negative")
        int lowStockThreshold
) {
}
