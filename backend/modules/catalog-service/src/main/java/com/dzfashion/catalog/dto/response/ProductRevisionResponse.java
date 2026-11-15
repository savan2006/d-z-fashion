package com.dzfashion.catalog.dto.response;

import com.dzfashion.catalog.enums.RevisionStatus;

import java.time.Instant;
import java.util.UUID;

public record ProductRevisionResponse(
        UUID id,
        UUID productId,
        String productName,
        UUID submittedBy,
        UUID reviewedBy,
        String proposedChanges,
        RevisionStatus status,
        String rejectionReason,
        Instant createdAt,
        Instant reviewedAt
) {
}
