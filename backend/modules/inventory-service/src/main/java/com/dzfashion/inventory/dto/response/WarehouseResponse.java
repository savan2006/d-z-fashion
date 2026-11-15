package com.dzfashion.inventory.dto.response;

import java.time.Instant;
import java.util.UUID;

public record WarehouseResponse(
        UUID id,
        String code,
        String name,
        String city,
        String state,
        String country,
        boolean active,
        Instant createdAt
) {
}
