package com.dzfashion.fulfillment.dto.response;

import com.dzfashion.fulfillment.enums.ReturnReason;
import com.dzfashion.fulfillment.enums.ReturnRequestStatus;
import com.dzfashion.fulfillment.enums.ReturnResolutionType;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record ReturnRequestResponse(
        UUID id,
        String returnNumber,
        UUID orderId,
        String orderNumber,
        UUID orderItemId,
        UUID customerId,
        String sku,
        String productName,
        int quantity,
        BigDecimal originalUnitPrice,
        ReturnReason reason,
        ReturnResolutionType resolutionType,
        ReturnRequestStatus status,
        List<String> photoUrls,
        String customerNotes,
        UUID transportAgencyId,
        String transportAgencyName,
        String pickupReference,
        LocalDate pickupScheduledDate,
        Instant pickedUpAt,
        Instant receivedAtWarehouseAt,
        UUID replacementProductId,
        UUID replacementVariantId,
        String replacementSku,
        String replacementProductName,
        BigDecimal replacementUnitPrice,
        BigDecimal priceDifference,
        boolean priceDifferenceSettled,
        UUID replacementShipmentId,
        BigDecimal refundAmount,
        Instant refundDeadline,
        UUID refundApprovedBy,
        Instant refundApprovedAt,
        String refundRejectionReason,
        boolean isCodRefund,
        ReturnInspectionResponse inspection,
        Instant createdAt,
        Instant updatedAt
) {
}
