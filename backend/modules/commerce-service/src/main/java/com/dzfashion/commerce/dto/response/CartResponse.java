package com.dzfashion.commerce.dto.response;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record CartResponse(
        UUID id,
        UUID customerId,
        List<CartItemResponse> items,
        int totalItems,
        BigDecimal subtotal,
        boolean priceConfirmationRequired
) {
}
