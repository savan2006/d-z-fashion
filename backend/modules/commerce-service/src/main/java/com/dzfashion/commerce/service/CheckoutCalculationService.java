package com.dzfashion.commerce.service;

import com.dzfashion.commerce.config.CommerceProperties;
import com.dzfashion.commerce.dto.response.CartItemResponse;
import com.dzfashion.commerce.dto.response.CartResponse;
import com.dzfashion.commerce.dto.response.CheckoutPreviewResponse;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Service
public class CheckoutCalculationService {

    private final CommerceProperties commerceProperties;
    private final PromotionEvaluationService promotionEvaluationService;

    public CheckoutCalculationService(CommerceProperties commerceProperties,
                                      PromotionEvaluationService promotionEvaluationService) {
        this.commerceProperties = commerceProperties;
        this.promotionEvaluationService = promotionEvaluationService;
    }

    public CheckoutPreviewResponse calculate(CartResponse cart,
                                             String couponCode,
                                             BigDecimal storeCreditToUse,
                                             boolean codServiceable) {
        List<String> warnings = new ArrayList<>();

        // 1. Calculate physical units and product subtotal before discount and tax
        int totalPhysicalUnits = 0;
        BigDecimal subtotal = BigDecimal.ZERO;
        boolean hasUnavailableItems = false;

        for (CartItemResponse item : cart.items()) {
            if (!item.available()) {
                hasUnavailableItems = true;
                warnings.add("Item " + item.productName() + " (" + item.sku() + ") is currently unavailable or unpublished");
            }
            totalPhysicalUnits += item.quantity();
            subtotal = subtotal.add(item.currentPrice().multiply(BigDecimal.valueOf(item.quantity())));
        }

        if (cart.priceConfirmationRequired()) {
            warnings.add("One or more items in your cart had a price update. Please review and confirm current prices.");
        }

        // 2. Free delivery threshold rule:
        // Free delivery iff eligible product subtotal is strictly greater than threshold (e.g. ₹2000.00).
        // Uses subtotal BEFORE GST and BEFORE promotional discounts. Exactly ₹2000 does NOT qualify.
        boolean freeDeliveryEligible = subtotal.compareTo(commerceProperties.getFreeDeliveryThreshold()) > 0;
        BigDecimal deliveryCharge;
        if (freeDeliveryEligible) {
            deliveryCharge = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        } else {
            // ₹110 per physical unit
            deliveryCharge = commerceProperties.getPerUnitDeliveryFee()
                    .multiply(BigDecimal.valueOf(totalPhysicalUnits))
                    .setScale(2, RoundingMode.HALF_UP);
        }

        // 3. Promotional discount evaluation
        BigDecimal promotionalDiscount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        String appliedCoupon = null;

        if (couponCode != null && !couponCode.trim().isEmpty()) {
            PromotionEvaluationResult promoResult = promotionEvaluationService.evaluate(couponCode, subtotal);
            if (promoResult.valid()) {
                promotionalDiscount = promoResult.discountAmount().min(subtotal).setScale(2, RoundingMode.HALF_UP);
                appliedCoupon = promoResult.couponCode();
            } else {
                warnings.add(promoResult.message());
            }
        }

        // 4. GST calculation
        // GST-exclusive base after promotional discount
        BigDecimal discountedBase = subtotal.subtract(promotionalDiscount).max(BigDecimal.ZERO);
        BigDecimal gstRatePercent = commerceProperties.getGstRatePercent();
        BigDecimal gstRateFraction = gstRatePercent.divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP);
        BigDecimal gstAmount = discountedBase.multiply(gstRateFraction).setScale(2, RoundingMode.HALF_UP);

        // 5. Total before store credit tender
        BigDecimal totalBeforeStoreCredit = discountedBase.add(gstAmount).add(deliveryCharge);

        // 6. Store credit tender deduction (cannot exceed total or available credit)
        BigDecimal creditToApply = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        if (storeCreditToUse != null && storeCreditToUse.compareTo(BigDecimal.ZERO) > 0) {
            creditToApply = storeCreditToUse.min(totalBeforeStoreCredit).setScale(2, RoundingMode.HALF_UP);
        }

        // 7. Final payable amount
        BigDecimal finalPayableAmount = totalBeforeStoreCredit.subtract(creditToApply).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);

        return new CheckoutPreviewResponse(
                subtotal.setScale(2, RoundingMode.HALF_UP),
                totalPhysicalUnits,
                freeDeliveryEligible,
                deliveryCharge,
                promotionalDiscount,
                appliedCoupon,
                gstAmount,
                gstRatePercent.setScale(2, RoundingMode.HALF_UP),
                creditToApply,
                finalPayableAmount,
                codServiceable,
                cart.priceConfirmationRequired(),
                warnings
        );
    }
}
