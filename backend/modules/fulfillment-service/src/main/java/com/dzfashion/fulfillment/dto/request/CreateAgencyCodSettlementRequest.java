package com.dzfashion.fulfillment.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record CreateAgencyCodSettlementRequest(
        @NotNull(message = "Transport agency ID is required")
        UUID transportAgencyId,

        @NotNull(message = "Settlement date is required")
        LocalDate settlementDate,

        @NotNull(message = "Remitted amount is required")
        @DecimalMin(value = "0.00", message = "Remitted amount must be non-negative")
        BigDecimal remittedAmount,

        String proofReference,
        String notes,

        @NotEmpty(message = "Delivered COD shipment IDs must not be empty")
        List<UUID> shipmentIds
) {
}
