package com.dzfashion.fulfillment.dto.response;

import com.dzfashion.fulfillment.enums.AgencyCodSettlementStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record AgencyCodSettlementResponse(
        UUID id,
        String batchReference,
        UUID transportAgencyId,
        String transportAgencyName,
        LocalDate settlementDate,
        int totalDeliveredOrders,
        BigDecimal totalExpectedCod,
        BigDecimal remittedAmount,
        BigDecimal discrepancyAmount,
        AgencyCodSettlementStatus status,
        UUID preparedBy,
        UUID approvedBy,
        String proofReference,
        String notes,
        Instant createdAt,
        Instant approvedAt
) {
}
