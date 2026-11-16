package com.dzfashion.fulfillment.dto.response;

import java.util.UUID;

public record ShipmentItemResponse(
        UUID id,
        UUID orderItemId,
        UUID productId,
        UUID variantId,
        String productName,
        String sku,
        String size,
        String color,
        String binLocation,
        int quantity,
        int pickedQuantity,
        boolean picked,
        boolean packed
) {
}
