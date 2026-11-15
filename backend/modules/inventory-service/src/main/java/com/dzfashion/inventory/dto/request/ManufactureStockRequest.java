package com.dzfashion.inventory.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record ManufactureStockRequest(
        @NotNull(message = "Variant ID is required")
        UUID variantId,

        @Positive(message = "Manufactured quantity must be greater than zero")
        int quantity,

        @Size(max = 255)
        String notes
) {
}
