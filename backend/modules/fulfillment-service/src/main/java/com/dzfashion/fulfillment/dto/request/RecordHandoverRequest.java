package com.dzfashion.fulfillment.dto.request;

import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.UUID;

public record RecordHandoverRequest(
        @NotNull(message = "Transport agency ID is required")
        UUID transportAgencyId,

        String billReference,
        Instant handoverAt,
        String contactNotes
) {
}
