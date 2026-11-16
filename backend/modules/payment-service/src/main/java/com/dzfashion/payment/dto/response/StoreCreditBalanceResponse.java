package com.dzfashion.payment.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

public record StoreCreditBalanceResponse(
        UUID customerId,
        BigDecimal currentBalance
) {
}
