package com.dzfashion.fulfillment.dto.response;

import com.dzfashion.fulfillment.enums.DailyDispatchStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record DailyDispatchBatchResponse(
        UUID id,
        String batchNumber,
        LocalDate dispatchDate,
        UUID transportAgencyId,
        String transportAgencyName,
        UUID preparedBy,
        int parcelCount,
        String billReference,
        DailyDispatchStatus status,
        Instant handoverAt,
        String notes,
        Instant createdAt
) {
}
