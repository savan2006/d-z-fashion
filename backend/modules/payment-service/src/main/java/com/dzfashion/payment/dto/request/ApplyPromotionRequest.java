package com.dzfashion.payment.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record ApplyPromotionRequest(
        String couponCode,

        @NotNull(message = "Subtotal is required")
        @DecimalMin(value = "0.00", message = "Subtotal must be non-negative")
        BigDecimal subtotal
) {
}
