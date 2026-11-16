package com.dzfashion.payment.dto.request;

import com.dzfashion.payment.enums.PromotionType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;

public record UpdatePromotionRequest(
        @NotBlank(message = "Promotion name is required")
        String name,

        @NotNull(message = "Promotion type is required")
        PromotionType type,

        @NotNull(message = "Discount value is required")
        @DecimalMin(value = "0.01", message = "Discount value must be greater than zero")
        BigDecimal discountValue,

        @DecimalMin(value = "0.00", message = "Minimum order subtotal must be non-negative")
        BigDecimal minOrderSubtotal,

        @DecimalMin(value = "0.01", message = "Maximum discount amount must be greater than zero")
        BigDecimal maxDiscountAmount,

        @NotNull(message = "Start timestamp is required")
        Instant startsAt,

        @NotNull(message = "End timestamp is required")
        Instant endsAt,

        @Min(value = 1, message = "Usage limit must be at least 1")
        Integer usageLimit,

        boolean active
) {
}
