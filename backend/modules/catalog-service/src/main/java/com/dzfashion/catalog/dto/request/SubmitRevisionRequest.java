package com.dzfashion.catalog.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record SubmitRevisionRequest(
        @Size(max = 255)
        String name,

        String description,

        @Size(max = 100)
        String fabric,

        @Size(max = 100)
        String occasion,

        UUID categoryId,

        List<@Valid CreateVariantDraftRequest> addedVariants,

        List<@Valid CreateMediaRequest> mediaUpdates
) {
}
