package com.dzfashion.payment.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CreateTaxClassificationRequest(
        @NotBlank(message = "HSN code is required")
        String hsnCode,

        @NotBlank(message = "Description is required")
        String description,

        @NotNull(message = "GST rate percent is required")
        @DecimalMin(value = "0.00", message = "GST rate must be non-negative")
        BigDecimal gstRatePercent
) {
}
