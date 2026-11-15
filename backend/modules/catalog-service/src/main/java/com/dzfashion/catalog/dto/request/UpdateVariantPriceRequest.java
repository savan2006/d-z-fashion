package com.dzfashion.catalog.dto.request;

import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record UpdateVariantPriceRequest(
        @PositiveOrZero(message = "Price override must be non-negative")
        BigDecimal priceOverride
) {
}
