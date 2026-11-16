package com.dzfashion.fulfillment.dto.request;

import com.dzfashion.fulfillment.enums.ReturnReason;
import com.dzfashion.fulfillment.enums.ReturnResolutionType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record CreateReturnRequest(
        @NotNull(message = "Order ID is required")
        UUID orderId,

        @NotBlank(message = "Order number is required")
        String orderNumber,

        @NotNull(message = "Order item ID is required")
        UUID orderItemId,

        @NotBlank(message = "SKU is required")
        String sku,

        @NotBlank(message = "Product name is required")
        String productName,

        @Min(value = 1, message = "Return quantity must be at least 1")
        int quantity,

        @NotNull(message = "Original unit price is required")
        @DecimalMin(value = "0.00", message = "Original unit price must be non-negative")
        BigDecimal originalUnitPrice,

        @NotNull(message = "Return reason is required")
        ReturnReason reason,

        ReturnResolutionType resolutionType,

        @NotEmpty(message = "At least one photo URL is required for quality verification")
        @Size(max = 5, message = "A maximum of 5 photos are allowed")
        List<String> photoUrls,

        String customerNotes,

        // Optional exchange replacement details
        UUID replacementProductId,
        UUID replacementVariantId,
        String replacementSku,
        String replacementProductName,
        BigDecimal replacementUnitPrice
) {
}
