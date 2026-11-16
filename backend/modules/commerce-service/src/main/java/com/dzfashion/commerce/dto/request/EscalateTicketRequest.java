package com.dzfashion.commerce.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EscalateTicketRequest(
        @NotBlank(message = "Escalation reason is required")
        @Size(min = 5, max = 1000, message = "Escalation reason must be between 5 and 1000 characters")
        String escalationReason
) {
}
