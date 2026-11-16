package com.dzfashion.payment.dto.response;

import com.dzfashion.payment.enums.PaymentMethod;
import com.dzfashion.payment.enums.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentResponse(
        UUID id,
        UUID orderId,
        UUID customerId,
        BigDecimal amount,
        String currency,
        PaymentMethod method,
        PaymentStatus status,
        String transactionReference,
        boolean simulated,
        String deliveryPincode,
        Instant collectedAt,
        UUID collectedBy,
        Instant createdAt
) {
}
