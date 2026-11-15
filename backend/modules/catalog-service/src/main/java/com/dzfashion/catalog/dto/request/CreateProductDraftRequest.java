package com.dzfashion.catalog.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record CreateProductDraftRequest(
        @NotBlank(message = "Product name is required")
        @Size(max = 255, message = "Product name cannot exceed 255 characters")
        String name,

        @NotBlank(message = "Slug is required")
        @Size(max = 280, message = "Slug cannot exceed 280 characters")
        @Pattern(regexp = "^[a-z0-9-]+$", message = "Slug must contain only lowercase letters, numbers, and hyphens")
        String slug,

        @NotNull(message = "Category ID is required")
        UUID categoryId,

        String description,

        @Size(max = 100)
        String fabric,

        @Size(max = 100)
        String occasion,

        List<@Valid CreateVariantDraftRequest> variants,

        List<@Valid CreateMediaRequest> media
) {
}
