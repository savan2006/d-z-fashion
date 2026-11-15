package com.dzfashion.commerce.service;

import java.math.BigDecimal;
import java.util.UUID;

public record PromotionEvaluationResult(
        boolean valid,
        UUID promotionId,
        String couponCode,
        BigDecimal discountAmount,
        String message
) {
    public static PromotionEvaluationResult none() {
        return new PromotionEvaluationResult(false, null, null, BigDecimal.ZERO, null);
    }

    public static PromotionEvaluationResult applied(UUID promotionId, String couponCode, BigDecimal discountAmount) {
        return new PromotionEvaluationResult(true, promotionId, couponCode, discountAmount, "Promotion applied successfully");
    }

    public static PromotionEvaluationResult invalid(String message) {
        return new PromotionEvaluationResult(false, null, null, BigDecimal.ZERO, message);
    }
}
