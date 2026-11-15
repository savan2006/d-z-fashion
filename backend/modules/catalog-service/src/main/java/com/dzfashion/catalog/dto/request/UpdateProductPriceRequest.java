package com.dzfashion.catalog.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record UpdateProductPriceRequest(
        @NotNull(message = "Default price is required")
        @PositiveOrZero(message = "Default price must be non-negative")
        BigDecimal defaultPrice
) {
}
