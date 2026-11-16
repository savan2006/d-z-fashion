package com.dzfashion.fulfillment.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record RecordDeliverySuccessRequest(
        @NotNull(message = "Delivery date is required")
        LocalDate deliveryDate,

        @DecimalMin(value = "0.00", message = "Collected COD amount must be non-negative")
        BigDecimal codCollectedAmount,

        String notes
) {
}
