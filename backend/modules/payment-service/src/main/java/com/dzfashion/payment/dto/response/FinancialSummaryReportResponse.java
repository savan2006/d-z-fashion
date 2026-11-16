package com.dzfashion.payment.dto.response;

import java.math.BigDecimal;

public record FinancialSummaryReportResponse(
        BigDecimal totalSimulatedOnlineRevenue,
        BigDecimal totalCodCollectedRevenue,
        BigDecimal totalCodPendingReceivable,
        BigDecimal totalStoreCreditOutstanding,
        BigDecimal totalInvoicedGstTax,
        long totalCompletedOrders
) {
}
