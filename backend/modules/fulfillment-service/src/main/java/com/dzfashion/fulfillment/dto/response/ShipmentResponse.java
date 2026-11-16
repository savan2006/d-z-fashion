package com.dzfashion.fulfillment.dto.response;

import com.dzfashion.fulfillment.enums.ShipmentStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record ShipmentResponse(
        UUID id,
        UUID orderId,
        String orderNumber,
        UUID customerId,
        String recipientName,
        String recipientPhone,
        String shippingAddress,
        String city,
        String state,
        String pincode,
        UUID transportAgencyId,
        String transportAgencyName,
        String trackingReference,
        ShipmentStatus status,
        LocalDate dispatchDate,
        LocalDate deliveryDate,
        int deliveryAttempts,
        boolean cod,
        BigDecimal codAmount,
        BigDecimal codCollectedAmount,
        UUID dailyDispatchBatchId,
        Instant pickedAt,
        Instant packedAt,
        Instant dispatchedAt,
        Instant returnedToWarehouseAt,
        Instant eligibleReturnUntil,
        UUID codSettlementBatchId,
        String notes,
        Instant createdAt,
        Instant updatedAt,
        List<ShipmentItemResponse> items,
        List<DeliveryAttemptResponse> attemptLogs
) {
}
