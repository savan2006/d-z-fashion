package com.dzfashion.payment.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

public record PromotionEvaluationResponse(
        boolean valid,
        UUID promotionId,
        String code,
        BigDecimal discountAmount,
        String message
) {
    public static PromotionEvaluationResponse invalid(String message) {
        return new PromotionEvaluationResponse(false, null, null, BigDecimal.ZERO, message);
    }

    public static PromotionEvaluationResponse applied(UUID promotionId, String code, BigDecimal discountAmount) {
        return new PromotionEvaluationResponse(true, promotionId, code, discountAmount, "Promotion applied successfully");
    }
}
