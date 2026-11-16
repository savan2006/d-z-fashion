package com.dzfashion.fulfillment.dto.request;

public record ConfirmExchangePaymentRequest(
        String paymentReference,
        String notes
) {
}
