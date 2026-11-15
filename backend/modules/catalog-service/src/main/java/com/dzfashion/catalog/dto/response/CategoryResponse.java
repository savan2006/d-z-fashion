package com.dzfashion.catalog.dto.response;

import java.util.List;
import java.util.UUID;

public record CategoryResponse(
        UUID id,
        UUID parentId,
        String name,
        String slug,
        String description,
        boolean active,
        int displayOrder,
        List<CategoryResponse> children
) {
}
