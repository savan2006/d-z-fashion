package com.dzfashion.commerce.client.impl;

import com.dzfashion.commerce.client.InventoryClient;
import com.dzfashion.commerce.client.ReservationResult;
import com.dzfashion.commerce.config.CommerceProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
@ConditionalOnMissingBean(name = "customInventoryClient")
public class DefaultInventoryClient implements InventoryClient {

    private final CommerceProperties properties;
    private final Set<UUID> outOfStockVariants = ConcurrentHashMap.newKeySet();
    private final Map<UUID, ReservationRecord> reservations = new ConcurrentHashMap<>();

    public record ReservationRecord(UUID reservationId, UUID variantId, int quantity, String orderNumber, String status) {}

    public DefaultInventoryClient(CommerceProperties properties) {
        this.properties = properties;
    }

    public void markOutOfStock(UUID variantId) {
        outOfStockVariants.add(variantId);
    }

    public void markInStock(UUID variantId) {
        outOfStockVariants.remove(variantId);
    }

    @Override
    public ReservationResult reserveStock(UUID variantId, int quantity, String orderNumber, String idempotencyKey) {
        if (outOfStockVariants.contains(variantId)) {
            return ReservationResult.failure(variantId, quantity, "Insufficient available stock for variant " + variantId);
        }
        UUID reservationId = UUID.randomUUID();
        reservations.put(reservationId, new ReservationRecord(reservationId, variantId, quantity, orderNumber, "RESERVED"));
        return ReservationResult.success(reservationId, variantId, quantity);
    }

    @Override
    public void releaseStock(UUID reservationId, String reason) {
        ReservationRecord record = reservations.get(reservationId);
        if (record != null) {
            reservations.put(reservationId, new ReservationRecord(record.reservationId(), record.variantId(), record.quantity(), record.orderNumber(), "RELEASED"));
        }
    }

    @Override
    public void commitStock(UUID reservationId) {
        ReservationRecord record = reservations.get(reservationId);
        if (record != null) {
            reservations.put(reservationId, new ReservationRecord(record.reservationId(), record.variantId(), record.quantity(), record.orderNumber(), "COMMITTED"));
        }
    }
}
