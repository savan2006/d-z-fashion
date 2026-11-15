package com.dzfashion.commerce.dto.request;

import com.dzfashion.commerce.enums.PaymentMethod;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CheckoutPreviewRequest(
        @NotNull(message = "Payment method is required")
        PaymentMethod paymentMethod,

        String couponCode,

        BigDecimal storeCreditToUse,

        String postalCode
) {
}
