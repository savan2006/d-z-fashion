package com.dzfashion.payment.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateCodSettlementBatchRequest(
        @NotBlank(message = "Transport agency name is required")
        String transportAgencyName,

        @NotNull(message = "Settlement date is required")
        LocalDate settlementDate,

        @NotNull(message = "Total collected amount is required")
        @DecimalMin(value = "0.00", message = "Total collected amount must be non-negative")
        BigDecimal totalCollectedAmount,

        @NotNull(message = "Remitted amount is required")
        @DecimalMin(value = "0.00", message = "Remitted amount must be non-negative")
        BigDecimal remittedAmount,

        String notes
) {
}
