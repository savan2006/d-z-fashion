package com.dzfashion.fulfillment.dto.request;

import jakarta.validation.constraints.NotBlank;

public record UpdateTransportAgencyRequest(
        String contactPerson,

        @NotBlank(message = "Phone number is required")
        String phoneNumber,

        boolean active
) {
}
