package com.dzfashion.commerce.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ReassignOrderRequest(
        @NotNull(message = "Target manager ID is required")
        UUID targetManagerId,

        String reason
) {
}
