package com.dzfashion.inventory.dto.request;

import com.dzfashion.inventory.enums.StockMovementReason;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record StockAdjustmentRequest(
        @NotNull(message = "Variant ID is required")
        UUID variantId,

        int quantityDelta,

        @NotNull(message = "Reason is required")
        StockMovementReason reason,

        @NotBlank(message = "Notes explaining the stock adjustment are mandatory")
        @Size(max = 255)
        String notes
) {
}
