package com.dzfashion.payment.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record GenerateDispatchInvoiceRequest(
        @NotNull(message = "Order ID is required")
        UUID orderId,

        @NotBlank(message = "Order number is required")
        String orderNumber,

        @NotNull(message = "Customer ID is required")
        UUID customerId,

        @NotBlank(message = "Customer name is required")
        String customerName,

        @NotBlank(message = "Customer phone is required")
        String customerPhone,

        @NotBlank(message = "Shipping address is required")
        String shippingAddress,

        @NotBlank(message = "Place of supply state is required")
        String placeOfSupplyState,

        @NotNull(message = "Taxable amount is required")
        @DecimalMin(value = "0.00", message = "Taxable amount must be non-negative")
        BigDecimal taxableAmount,

        @NotNull(message = "Delivery charge is required")
        @DecimalMin(value = "0.00", message = "Delivery charge must be non-negative")
        BigDecimal deliveryCharge,

        @NotNull(message = "Discount amount is required")
        @DecimalMin(value = "0.00", message = "Discount amount must be non-negative")
        BigDecimal discountAmount,

        @NotNull(message = "Total amount is required")
        @DecimalMin(value = "0.00", message = "Total amount must be non-negative")
        BigDecimal totalAmount,

        @NotEmpty(message = "Items list must not be empty")
        List<@Valid InvoiceItemData> items
) {
}
