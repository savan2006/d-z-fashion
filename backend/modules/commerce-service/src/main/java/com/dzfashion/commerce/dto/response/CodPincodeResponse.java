package com.dzfashion.commerce.dto.response;

import java.time.Instant;
import java.util.UUID;

public record CodPincodeResponse(
        UUID id,
        String pincode,
        boolean active,
        UUID enabledBy,
        Instant createdAt
) {
}
