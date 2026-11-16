package com.dzfashion.fulfillment.dto.request;

import jakarta.validation.constraints.NotBlank;

public record ReceiveReturnAtWarehouseRequest(
        @NotBlank(message = "Condition notes are required upon receiving parcel at warehouse")
        String conditionNotes
) {
}
