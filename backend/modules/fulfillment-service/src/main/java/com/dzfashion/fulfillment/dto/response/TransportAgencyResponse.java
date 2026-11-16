package com.dzfashion.fulfillment.dto.response;

import java.time.Instant;
import java.util.UUID;

public record TransportAgencyResponse(
        UUID id,
        String name,
        String contactPerson,
        String phoneNumber,
        boolean active,
        Instant createdAt
) {
}
