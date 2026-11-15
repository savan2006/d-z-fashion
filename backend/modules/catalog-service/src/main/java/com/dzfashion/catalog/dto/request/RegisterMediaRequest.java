package com.dzfashion.catalog.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record RegisterMediaRequest(
        @NotBlank(message = "Cloudinary public ID is required")
        @Size(max = 255)
        String cloudinaryPublicId,

        @NotBlank(message = "Secure URL is required")
        String secureUrl,

        String format,

        Integer width,

        Integer height,

        @Size(max = 255)
        String altText,

        int displayOrder,

        boolean primary,

        UUID variantId
) {
}
