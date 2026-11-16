package com.dzfashion.commerce.dto.response;

import java.math.BigDecimal;

public record OwnerAnalyticsReportResponse(
        long totalOrders,
        BigDecimal grossMerchandiseValue,
        BigDecimal totalDiscountGiven,
        BigDecimal netSalesRevenue,
        long onlinePaidOrders,
        BigDecimal onlinePaidAmount,
        long codOrders,
        BigDecimal codAmount,
        long cancelledOrders,
        long openSupportTickets,
        long escalatedSupportTickets,
        long reportedShortages
) {
}
