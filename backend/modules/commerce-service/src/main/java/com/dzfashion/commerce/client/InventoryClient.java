package com.dzfashion.commerce.client;

import java.util.UUID;

public interface InventoryClient {
    ReservationResult reserveStock(UUID variantId, int quantity, String orderNumber, String idempotencyKey);
    void releaseStock(UUID reservationId, String reason);
    void commitStock(UUID reservationId);
}
