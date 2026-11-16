package com.dzfashion.payment.dto.response;

import com.dzfashion.payment.enums.StoreCreditEntryType;
import com.dzfashion.payment.enums.StoreCreditReferenceType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record StoreCreditEntryResponse(
        UUID id,
        UUID customerId,
        BigDecimal amount,
        BigDecimal balanceAfter,
        StoreCreditEntryType entryType,
        StoreCreditReferenceType referenceType,
        String referenceId,
        String notes,
        Instant createdAt
) {
}
