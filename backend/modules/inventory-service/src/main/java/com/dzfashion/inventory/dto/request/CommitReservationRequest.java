package com.dzfashion.inventory.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CommitReservationRequest(
        @NotNull(message = "Order ID is required")
        UUID orderId,

        @NotNull(message = "Variant ID is required")
        UUID variantId
) {
}
