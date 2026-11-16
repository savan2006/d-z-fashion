package com.dzfashion.fulfillment.dto.response;

import com.dzfashion.fulfillment.enums.DeliveryAttemptResult;

import java.time.Instant;
import java.util.UUID;

public record DeliveryAttemptResponse(
        UUID id,
        int attemptNumber,
        Instant attemptedAt,
        DeliveryAttemptResult result,
        String notes,
        UUID recordedBy
) {
}
