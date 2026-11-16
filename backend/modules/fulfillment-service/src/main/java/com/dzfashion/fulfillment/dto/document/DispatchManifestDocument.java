package com.dzfashion.fulfillment.dto.document;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record DispatchManifestDocument(
        String batchNumber,
        LocalDate dispatchDate,
        Instant generatedAt,
        String transportAgencyName,
        String warehouseName,
        int parcelCount,
        BigDecimal totalCodAmount,
        String billReference,
        List<ManifestParcel> parcels
) {
    public record ManifestParcel(
            String orderNumber,
            String recipientName,
            String destinationCity,
            String destinationState,
            String pincode,
            boolean cod,
            BigDecimal codAmount
    ) {}
}
