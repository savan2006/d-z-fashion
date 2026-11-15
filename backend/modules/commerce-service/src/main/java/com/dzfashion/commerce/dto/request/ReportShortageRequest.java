package com.dzfashion.commerce.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ReportShortageRequest(
        @NotNull(message = "Variant ID is required")
        UUID variantId,

        @Min(value = 1, message = "Expected quantity must be at least 1")
        int expectedQuantity,

        @Min(value = 0, message = "Actual quantity must be at least 0")
        int actualQuantity,

        @NotBlank(message = "Notes are required describing the physical shortage")
        String notes
) {
}
