package com.dzfashion.fulfillment.dto.response;

import com.dzfashion.fulfillment.enums.CustomerShipmentStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record CustomerTrackingResponse(
        UUID orderId,
        String orderNumber,
        CustomerShipmentStatus customerStatus,
        String transportAgencyName,
        String trackingReference,
        LocalDate dispatchDate,
        LocalDate deliveryDate,
        int deliveryAttempts,
        Instant eligibleReturnUntil
) {
}
