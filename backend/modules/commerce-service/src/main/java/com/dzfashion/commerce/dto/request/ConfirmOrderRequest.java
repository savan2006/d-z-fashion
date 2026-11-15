package com.dzfashion.commerce.dto.request;

import com.dzfashion.commerce.enums.PaymentMethod;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record ConfirmOrderRequest(
        @NotNull(message = "Payment method is required")
        PaymentMethod paymentMethod,

        String couponCode,

        BigDecimal storeCreditToUse,

        boolean confirmPriceChanges,

        @NotBlank(message = "Recipient name is required")
        String recipientName,

        @NotBlank(message = "Recipient phone is required")
        String recipientPhone,

        @NotBlank(message = "Shipping address line 1 is required")
        String shippingAddressLine1,

        String shippingAddressLine2,

        @NotBlank(message = "City is required")
        String shippingCity,

        @NotBlank(message = "State is required")
        String shippingState,

        @NotBlank(message = "Postal code is required")
        String shippingPostalCode,

        String shippingCountry
) {
    public String getEffectiveCountry() {
        return (shippingCountry != null && !shippingCountry.isBlank()) ? shippingCountry : "India";
    }
}
