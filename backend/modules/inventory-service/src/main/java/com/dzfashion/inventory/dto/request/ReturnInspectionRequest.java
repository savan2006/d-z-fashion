package com.dzfashion.inventory.dto.request;

import com.dzfashion.inventory.enums.ItemCondition;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record ReturnInspectionRequest(
        @NotNull(message = "Variant ID is required")
        UUID variantId,

        @Positive(message = "Quantity must be greater than zero")
        int quantity,

        @NotNull(message = "Item condition is required (RESELLABLE or DAMAGED)")
        ItemCondition condition,

        @Size(max = 100)
        String returnOrderId,

        @Size(max = 255)
        String notes
) {
}
