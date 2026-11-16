package com.dzfashion.commerce.dto.request;

import com.dzfashion.commerce.enums.SupportTicketChannel;
import com.dzfashion.commerce.enums.SupportTicketPriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record ManualSupportIntakeRequest(
        @NotNull(message = "Customer ID is required")
        UUID customerId,

        UUID orderId,

        @NotNull(message = "Channel is required (WHATSAPP or PHONE)")
        SupportTicketChannel channel,

        @NotBlank(message = "Subject is required")
        @Size(max = 200, message = "Subject cannot exceed 200 characters")
        String subject,

        @NotBlank(message = "Inquiry notes are required")
        @Size(min = 5, max = 3000, message = "Notes must be between 5 and 3000 characters")
        String notes,

        SupportTicketPriority priority
) {
}
