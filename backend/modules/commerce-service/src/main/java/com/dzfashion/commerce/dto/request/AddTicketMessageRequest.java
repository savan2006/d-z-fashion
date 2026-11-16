package com.dzfashion.commerce.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AddTicketMessageRequest(
        @NotBlank(message = "Message content is required")
        @Size(min = 1, max = 3000, message = "Message must be between 1 and 3000 characters")
        String message,

        boolean internalNote
) {
    public AddTicketMessageRequest(String message) {
        this(message, false);
    }
}
