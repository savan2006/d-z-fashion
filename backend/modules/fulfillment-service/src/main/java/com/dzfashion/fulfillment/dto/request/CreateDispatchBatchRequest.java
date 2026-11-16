package com.dzfashion.fulfillment.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record CreateDispatchBatchRequest(
        @NotNull(message = "Dispatch date is required")
        LocalDate dispatchDate,

        @NotNull(message = "Transport agency ID is required")
        UUID transportAgencyId,

        String billReference,
        String notes,

        @NotEmpty(message = "At least one shipment must be selected for dispatch batch")
        List<UUID> shipmentIds
) {
}
