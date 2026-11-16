package com.dzfashion.commerce.dto.response;

import com.dzfashion.commerce.enums.NotificationType;

import java.time.Instant;
import java.util.UUID;

public record NotificationResponse(
        UUID id,
        UUID recipientId,
        String recipientRole,
        NotificationType type,
        String title,
        String message,
        String referenceId,
        String referenceType,
        boolean read,
        Instant readAt,
        Instant createdAt
) {
}
