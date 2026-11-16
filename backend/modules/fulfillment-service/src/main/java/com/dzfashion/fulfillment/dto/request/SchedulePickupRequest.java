package com.dzfashion.fulfillment.dto.request;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

public record SchedulePickupRequest(
        @NotNull(message = "Transport agency ID is required")
        UUID transportAgencyId,

        @NotNull(message = "Scheduled pickup date is required")
        LocalDate pickupScheduledDate,

        String pickupReference,
        String notes
) {
}
