package com.dzfashion.payment.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

public record InvoiceItemResponse(
        UUID id,
        UUID productId,
        UUID variantId,
        String productName,
        String sku,
        String hsnCode,
        int quantity,
        BigDecimal unitPrice,
        BigDecimal taxableAmount,
        BigDecimal gstRatePercent,
        BigDecimal cgstAmount,
        BigDecimal sgstAmount,
        BigDecimal igstAmount,
        BigDecimal totalAmount
) {
}
