package com.dzfashion.commerce.dto.response;

import com.dzfashion.commerce.enums.OrderStatus;
import com.dzfashion.commerce.enums.PaymentMethod;
import com.dzfashion.commerce.enums.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record OrderSummaryResponse(
        UUID id,
        String orderNumber,
        UUID customerId,
        OrderStatus status,
        PaymentMethod paymentMethod,
        PaymentStatus paymentStatus,
        UUID claimedBy,
        BigDecimal finalPayableAmount,
        int totalItems,
        Instant confirmedAt
) {
}
