package com.dzfashion.commerce.dto.response;

import java.time.Instant;
import java.util.UUID;

public record SupportTicketMessageResponse(
        UUID id,
        UUID ticketId,
        UUID senderId,
        String senderRole,
        String message,
        boolean internalNote,
        Instant createdAt
) {
}
