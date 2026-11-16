package com.dzfashion.commerce.dto.response;

import com.dzfashion.commerce.enums.SupportTicketChannel;
import com.dzfashion.commerce.enums.SupportTicketPriority;
import com.dzfashion.commerce.enums.SupportTicketStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record SupportTicketResponse(
        UUID id,
        String ticketNumber,
        UUID customerId,
        UUID orderId,
        String subject,
        SupportTicketChannel channel,
        SupportTicketPriority priority,
        SupportTicketStatus status,
        UUID assignedManagerId,
        boolean escalated,
        String escalationReason,
        Instant escalatedAt,
        Instant resolvedAt,
        Instant createdAt,
        Instant updatedAt,
        List<SupportTicketMessageResponse> messages
) {
}
