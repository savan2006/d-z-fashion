package com.dzfashion.commerce.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record CheckoutPreviewResponse(
        BigDecimal subtotalBeforeDiscountAndTax,
        int totalPhysicalUnits,
        boolean freeDeliveryEligible,
        BigDecimal deliveryCharge,
        BigDecimal promotionalDiscountAmount,
        String appliedCouponCode,
        BigDecimal gstAmount,
        BigDecimal gstRatePercent,
        BigDecimal storeCreditUsed,
        BigDecimal finalPayableAmount,
        boolean codServiceable,
        boolean priceConfirmationRequired,
        List<String> warnings
) {
}
