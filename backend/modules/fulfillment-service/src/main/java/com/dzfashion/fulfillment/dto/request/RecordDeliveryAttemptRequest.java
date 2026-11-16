package com.dzfashion.fulfillment.dto.request;

import com.dzfashion.fulfillment.enums.DeliveryAttemptResult;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record RecordDeliveryAttemptRequest(
        Instant attemptedAt,

        @NotNull(message = "Attempt result is required")
        DeliveryAttemptResult result,

        String notes
) {
}
