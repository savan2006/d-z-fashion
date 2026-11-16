package com.dzfashion.fulfillment.dto.request;

import java.time.Instant;

public record RecordReturnArrivalRequest(
        Instant receivedAt,
        String notes
) {
}
