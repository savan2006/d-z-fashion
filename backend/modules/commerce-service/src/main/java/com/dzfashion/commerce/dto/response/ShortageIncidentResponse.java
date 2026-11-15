package com.dzfashion.commerce.dto.response;

import com.dzfashion.commerce.enums.ShortageIncidentStatus;

import java.time.Instant;
import java.util.UUID;

public record ShortageIncidentResponse(
        UUID id,
        String incidentNumber,
        UUID orderId,
        String orderNumber,
        UUID variantId,
        UUID reportedBy,
        int expectedQuantity,
        int actualQuantity,
        String notes,
        ShortageIncidentStatus status,
        String resolutionNotes,
        UUID resolvedBy,
        Instant resolvedAt,
        Instant createdAt
) {
}
