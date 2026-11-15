package com.dzfashion.catalog.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CreateVariantRequest(
        @NotBlank(message = "Size is required")
        @Size(max = 20, message = "Size cannot exceed 20 characters")
        String size,

        @NotBlank(message = "Colour is required")
        @Size(max = 50, message = "Colour cannot exceed 50 characters")
        String colour,

        @PositiveOrZero(message = "Price override must be non-negative")
        BigDecimal priceOverride
) {
}
