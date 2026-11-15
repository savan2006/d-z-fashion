package com.dzfashion.commerce.service;

import com.dzfashion.commerce.config.CommerceProperties;
import com.dzfashion.commerce.dto.response.CartItemResponse;
import com.dzfashion.commerce.dto.response.CartResponse;
import com.dzfashion.commerce.dto.response.CheckoutPreviewResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CheckoutCalculationServiceTest {

    private CheckoutCalculationService calculationService;
    private CommerceProperties properties;
    private PromotionEvaluationService promotionService;

    @BeforeEach
    void setUp() {
        properties = new CommerceProperties();
        properties.setGstRatePercent(new BigDecimal("5.00"));
        properties.setFreeDeliveryThreshold(new BigDecimal("2000.00"));
        properties.setPerUnitDeliveryFee(new BigDecimal("110.00"));

        promotionService = new PromotionEvaluationService();
        calculationService = new CheckoutCalculationService(properties, promotionService);
    }

    @Test
    @DisplayName("Delivery boundary: exactly ₹2000 subtotal does NOT qualify for free delivery")
    void deliveryBoundaryExactly2000IncursFee() {
        // 2 units at ₹1000 each = ₹2000.00 subtotal
        CartItemResponse item = new CartItemResponse(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                "Silk Kurti", "DZF-KURTI-01", "M", "Burgundy",
                2, new BigDecimal("1000.00"), new BigDecimal("1000.00"),
                false, true, new BigDecimal("2000.00")
        );
        CartResponse cart = new CartResponse(UUID.randomUUID(), UUID.randomUUID(), List.of(item), 2, new BigDecimal("2000.00"), false);

        CheckoutPreviewResponse preview = calculationService.calculate(cart, null, null, true);

        assertEquals(new BigDecimal("2000.00"), preview.subtotalBeforeDiscountAndTax());
        assertFalse(preview.freeDeliveryEligible(), "Subtotal ₹2000.00 must not be free delivery (rule: strictly > ₹2000)");
        // 2 units * ₹110 = ₹220.00
        assertEquals(new BigDecimal("220.00"), preview.deliveryCharge());
    }

    @Test
    @DisplayName("Delivery boundary: subtotal ₹2000.01 qualifies for free delivery")
    void deliveryBoundary2000Point01IsFreeDelivery() {
        CartItemResponse item = new CartItemResponse(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                "Silk Kurti", "DZF-KURTI-01", "M", "Burgundy",
                1, new BigDecimal("2000.01"), new BigDecimal("2000.01"),
                false, true, new BigDecimal("2000.01")
        );
        CartResponse cart = new CartResponse(UUID.randomUUID(), UUID.randomUUID(), List.of(item), 1, new BigDecimal("2000.01"), false);

        CheckoutPreviewResponse preview = calculationService.calculate(cart, null, null, true);

        assertEquals(new BigDecimal("2000.01"), preview.subtotalBeforeDiscountAndTax());
        assertTrue(preview.freeDeliveryEligible(), "Subtotal ₹2000.01 must be free delivery");
        assertEquals(new BigDecimal("0.00"), preview.deliveryCharge());
    }

    @Test
    @DisplayName("Delivery charge: 3 physical units incur ₹330 delivery fee")
    void threeUnitsIncur330DeliveryFee() {
        // 1 line with 3 units at ₹300 = ₹900 subtotal (<= ₹2000)
        CartItemResponse item = new CartItemResponse(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                "Cotton Tunic", "DZF-TUNIC-01", "S", "Wine",
                3, new BigDecimal("300.00"), new BigDecimal("300.00"),
                false, true, new BigDecimal("900.00")
        );
        CartResponse cart = new CartResponse(UUID.randomUUID(), UUID.randomUUID(), List.of(item), 3, new BigDecimal("900.00"), false);

        CheckoutPreviewResponse preview = calculationService.calculate(cart, null, null, true);

        assertEquals(3, preview.totalPhysicalUnits());
        assertFalse(preview.freeDeliveryEligible());
        assertEquals(new BigDecimal("330.00"), preview.deliveryCharge(), "3 units * ₹110 = ₹330");
    }

    @Test
    @DisplayName("Coupon discount: WELCOME10 applies 10% discount and recalculates GST")
    void couponDiscountAppliesCorrectly() {
        // Subtotal ₹3000 -> free delivery
        CartItemResponse item = new CartItemResponse(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                "Designer Dress", "DZF-DRESS-01", "L", "Ivory",
                1, new BigDecimal("3000.00"), new BigDecimal("3000.00"),
                false, true, new BigDecimal("3000.00")
        );
        CartResponse cart = new CartResponse(UUID.randomUUID(), UUID.randomUUID(), List.of(item), 1, new BigDecimal("3000.00"), false);

        CheckoutPreviewResponse preview = calculationService.calculate(cart, "WELCOME10", null, true);

        assertEquals(new BigDecimal("3000.00"), preview.subtotalBeforeDiscountAndTax());
        assertEquals(new BigDecimal("300.00"), preview.promotionalDiscountAmount()); // 10% of 3000
        assertEquals("WELCOME10", preview.appliedCouponCode());

        // Base = 3000 - 300 = 2700; GST at 5% = 135.00
        assertEquals(new BigDecimal("135.00"), preview.gstAmount());
        // Delivery = 0
        assertEquals(new BigDecimal("0.00"), preview.deliveryCharge());
        // Final = 2700 + 135 = 2835.00
        assertEquals(new BigDecimal("2835.00"), preview.finalPayableAmount());
    }

    @Test
    @DisplayName("Store credit tender reduces final payable amount without reducing delivery threshold")
    void storeCreditReducesPayableAmount() {
        // Subtotal ₹2500 -> free delivery. GST at 5% = 125. Total = 2625. Store credit ₹500 -> payable 2125
        CartItemResponse item = new CartItemResponse(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                "Designer Dress", "DZF-DRESS-01", "L", "Ivory",
                1, new BigDecimal("2500.00"), new BigDecimal("2500.00"),
                false, true, new BigDecimal("2500.00")
        );
        CartResponse cart = new CartResponse(UUID.randomUUID(), UUID.randomUUID(), List.of(item), 1, new BigDecimal("2500.00"), false);

        CheckoutPreviewResponse preview = calculationService.calculate(cart, null, new BigDecimal("500.00"), true);

        assertTrue(preview.freeDeliveryEligible());
        assertEquals(new BigDecimal("500.00"), preview.storeCreditUsed());
        assertEquals(new BigDecimal("2125.00"), preview.finalPayableAmount());
    }
}
