package com.dzfashion.fulfillment.dto.request;

import com.dzfashion.fulfillment.enums.ItemDisposition;
import com.dzfashion.fulfillment.enums.ReturnInspectionResult;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record PerformInspectionRequest(
        @NotNull(message = "Inspection result is required")
        ReturnInspectionResult result,

        @NotNull(message = "Item disposition is required")
        ItemDisposition disposition,

        @NotBlank(message = "Condition notes are required")
        String conditionNotes,

        List<String> photoUrls
) {
}
