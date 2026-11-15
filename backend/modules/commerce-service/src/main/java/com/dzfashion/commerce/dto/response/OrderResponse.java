package com.dzfashion.commerce.dto.response;

import com.dzfashion.commerce.enums.OrderStatus;
import com.dzfashion.commerce.enums.PaymentMethod;
import com.dzfashion.commerce.enums.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderResponse(
        UUID id,
        String orderNumber,
        String idempotencyKey,
        UUID customerId,
        OrderStatus status,
        PaymentMethod paymentMethod,
        PaymentStatus paymentStatus,
        UUID claimedBy,
        Instant claimedAt,
        BigDecimal subtotalBeforeDiscountAndTax,
        BigDecimal promotionalDiscountAmount,
        UUID appliedPromotionId,
        BigDecimal gstAmount,
        BigDecimal deliveryCharge,
        BigDecimal storeCreditUsed,
        BigDecimal finalPayableAmount,
        String recipientName,
        String recipientPhone,
        String shippingAddressLine1,
        String shippingAddressLine2,
        String shippingCity,
        String shippingState,
        String shippingPostalCode,
        String shippingCountry,
        Instant confirmedAt,
        Instant dispatchedAt,
        Instant deliveredAt,
        Instant cancelledAt,
        UUID cancelledBy,
        String cancellationReason,
        List<OrderItemResponse> items
) {
}
