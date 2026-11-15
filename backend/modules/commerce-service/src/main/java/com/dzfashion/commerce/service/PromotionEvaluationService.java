package com.dzfashion.commerce.service;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

@Service
public class PromotionEvaluationService {

    /**
     * Evaluates promotion/coupon eligibility.
     * Note: Per architectural requirement 7, promotion combination / tie-break policy remains a configurable seam.
     * Only one promotion is applied per order.
     */
    public PromotionEvaluationResult evaluate(String couponCode, BigDecimal subtotal) {
        if (couponCode == null || couponCode.trim().isEmpty()) {
            return PromotionEvaluationResult.none();
        }

        String normalizedCode = couponCode.trim().toUpperCase();

        // Sample deterministic coupon policies for phase one:
        if ("WELCOME10".equals(normalizedCode)) {
            // 10% discount on subtotal
            BigDecimal discount = subtotal.multiply(new BigDecimal("0.10")).setScale(2, RoundingMode.HALF_UP);
            return PromotionEvaluationResult.applied(UUID.fromString("11111111-1111-1111-1111-111111111111"), "WELCOME10", discount);
        }

        if ("FLAT200".equals(normalizedCode)) {
            // Flat ₹200 discount (capped by subtotal)
            BigDecimal discount = subtotal.min(new BigDecimal("200.00")).setScale(2, RoundingMode.HALF_UP);
            return PromotionEvaluationResult.applied(UUID.fromString("22222222-2222-2222-2222-222222222222"), "FLAT200", discount);
        }

        return PromotionEvaluationResult.invalid("Coupon code '" + couponCode + "' is invalid or expired");
    }
}
