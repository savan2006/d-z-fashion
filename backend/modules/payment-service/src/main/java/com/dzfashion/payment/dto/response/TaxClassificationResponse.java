package com.dzfashion.payment.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

public record TaxClassificationResponse(
        UUID id,
        String hsnCode,
        String description,
        BigDecimal gstRatePercent,
        boolean active
) {
}
