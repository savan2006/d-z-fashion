package com.dzfashion.payment.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record InvoiceItemData(
        @NotNull(message = "Product ID is required")
        UUID productId,

        @NotNull(message = "Variant ID is required")
        UUID variantId,

        @NotBlank(message = "Product name is required")
        String productName,

        @NotBlank(message = "SKU is required")
        String sku,

        String hsnCode,

        @Min(value = 1, message = "Quantity must be at least 1")
        int quantity,

        @NotNull(message = "Unit price is required")
        @DecimalMin(value = "0.00", message = "Unit price must be non-negative")
        BigDecimal unitPrice,

        @NotNull(message = "Taxable amount is required")
        @DecimalMin(value = "0.00", message = "Taxable amount must be non-negative")
        BigDecimal taxableAmount,

        BigDecimal gstRatePercent
) {
}
