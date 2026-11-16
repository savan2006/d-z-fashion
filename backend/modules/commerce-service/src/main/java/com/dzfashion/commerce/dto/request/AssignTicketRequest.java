package com.dzfashion.commerce.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AssignTicketRequest(
        @NotNull(message = "Manager ID is required")
        UUID managerId
) {
}
