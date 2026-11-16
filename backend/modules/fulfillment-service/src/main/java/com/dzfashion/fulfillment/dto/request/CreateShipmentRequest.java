package com.dzfashion.fulfillment.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record CreateShipmentRequest(
        @NotNull(message = "Order ID is required")
        UUID orderId,

        @NotBlank(message = "Order number is required")
        String orderNumber,

        @NotNull(message = "Customer ID is required")
        UUID customerId,

        @NotBlank(message = "Recipient name is required")
        String recipientName,

        @NotBlank(message = "Recipient phone is required")
        String recipientPhone,

        @NotBlank(message = "Shipping address is required")
        String shippingAddress,

        @NotBlank(message = "City is required")
        String city,

        @NotBlank(message = "State is required")
        String state,

        @NotBlank(message = "Pincode is required")
        String pincode,

        boolean cod,

        @DecimalMin(value = "0.00", message = "COD amount must be non-negative")
        BigDecimal codAmount,

        @NotEmpty(message = "Shipment items list must not be empty")
        List<@Valid CreateShipmentItemRequest> items
) {
}
