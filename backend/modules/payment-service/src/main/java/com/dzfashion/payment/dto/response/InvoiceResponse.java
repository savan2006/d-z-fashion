package com.dzfashion.payment.dto.response;

import com.dzfashion.payment.enums.InvoiceType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record InvoiceResponse(
        UUID id,
        String invoiceNumber,
        UUID orderId,
        String orderNumber,
        InvoiceType invoiceType,
        String sellerLegalName,
        String sellerGstin,
        String sellerAddress,
        String sellerState,
        UUID customerId,
        String customerName,
        String customerPhone,
        String shippingAddress,
        String placeOfSupplyState,
        BigDecimal taxableAmount,
        BigDecimal cgstAmount,
        BigDecimal sgstAmount,
        BigDecimal igstAmount,
        BigDecimal totalTaxAmount,
        BigDecimal deliveryCharge,
        BigDecimal discountAmount,
        BigDecimal totalAmount,
        String currency,
        Instant issuedAt,
        List<InvoiceItemResponse> items
) {
}
