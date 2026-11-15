package com.dzfashion.inventory.dto.request;

import jakarta.validation.constraints.PositiveOrZero;

public record UpdateThresholdRequest(
        @PositiveOrZero(message = "Low stock threshold cannot be negative")
        int lowStockThreshold
) {
}
