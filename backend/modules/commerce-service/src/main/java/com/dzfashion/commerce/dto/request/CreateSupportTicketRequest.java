package com.dzfashion.commerce.dto.request;

import com.dzfashion.commerce.enums.SupportTicketPriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateSupportTicketRequest(
        UUID orderId,

        @NotBlank(message = "Subject is required")
        @Size(max = 200, message = "Subject cannot exceed 200 characters")
        String subject,

        @NotBlank(message = "Initial message is required")
        @Size(min = 5, max = 3000, message = "Message must be between 5 and 3000 characters")
        String message,

        SupportTicketPriority priority
) {
}
