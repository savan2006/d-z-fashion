package com.dzfashion.payment.dto.response;

import com.dzfashion.payment.enums.PromotionType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PromotionResponse(
        UUID id,
        String code,
        String name,
        PromotionType type,
        BigDecimal discountValue,
        BigDecimal minOrderSubtotal,
        BigDecimal maxDiscountAmount,
        Instant startsAt,
        Instant endsAt,
        Integer usageLimit,
        int usageCount,
        boolean active,
        Instant createdAt
) {
}
