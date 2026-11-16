package com.dzfashion.fulfillment.dto.request;

import java.time.Instant;

public record RecordPickupRequest(
        Instant pickedUpAt,
        String notes
) {
}
