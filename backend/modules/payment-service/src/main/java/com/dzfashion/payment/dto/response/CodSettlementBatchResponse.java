package com.dzfashion.payment.dto.response;

import com.dzfashion.payment.enums.CodSettlementStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record CodSettlementBatchResponse(
        UUID id,
        String batchReference,
        String transportAgencyName,
        LocalDate settlementDate,
        BigDecimal totalCollectedAmount,
        BigDecimal remittedAmount,
        BigDecimal discrepancyAmount,
        CodSettlementStatus status,
        UUID preparedBy,
        UUID approvedBy,
        String notes,
        Instant createdAt,
        Instant approvedAt
) {
}
