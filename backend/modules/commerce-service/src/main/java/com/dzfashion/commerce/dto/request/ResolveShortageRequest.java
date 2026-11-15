package com.dzfashion.commerce.dto.request;

import jakarta.validation.constraints.NotBlank;

public record ResolveShortageRequest(
        @NotBlank(message = "Resolution notes are required")
        String resolutionNotes
) {
}
