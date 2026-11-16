package com.dzfashion.fulfillment.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CreateTransportAgencyRequest(
        @NotBlank(message = "Agency name is required")
        String name,

        String contactPerson,

        @NotBlank(message = "Phone number is required")
        String phoneNumber
) {
}
