package com.dzfashion.fulfillment.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateShipmentItemRequest(
        @NotNull(message = "Order item ID is required")
        UUID orderItemId,

        @NotNull(message = "Product ID is required")
        UUID productId,

        @NotNull(message = "Variant ID is required")
        UUID variantId,

        @NotBlank(message = "Product name is required")
        String productName,

        @NotBlank(message = "SKU is required")
        String sku,

        String size,
        String color,
        String binLocation,

        @Min(value = 1, message = "Quantity must be at least 1")
        int quantity
) {
}
