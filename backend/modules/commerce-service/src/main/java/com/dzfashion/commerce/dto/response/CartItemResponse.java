package com.dzfashion.commerce.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

public record CartItemResponse(
        UUID id,
        UUID productId,
        UUID variantId,
        String productName,
        String sku,
        String size,
        String colour,
        int quantity,
        BigDecimal priceSnapshot,
        BigDecimal currentPrice,
        boolean priceChanged,
        boolean available,
        BigDecimal itemTotal
) {
}
