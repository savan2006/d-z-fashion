package com.dzfashion.commerce.dto.request;

import com.dzfashion.commerce.enums.SupportTicketStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateTicketStatusRequest(
        @NotNull(message = "Status is required")
        SupportTicketStatus status,

        @Size(max = 1000, message = "Status note cannot exceed 1000 characters")
        String note
) {
}
