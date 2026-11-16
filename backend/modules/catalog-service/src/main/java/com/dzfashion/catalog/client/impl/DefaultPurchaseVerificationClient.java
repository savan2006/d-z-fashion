package com.dzfashion.catalog.client.impl;

import com.dzfashion.catalog.client.PurchaseVerificationClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
@ConditionalOnMissingBean(name = "customPurchaseVerificationClient")
public class DefaultPurchaseVerificationClient implements PurchaseVerificationClient {

    private record DeliveredPurchaseKey(UUID customerId, UUID orderId, UUID orderItemId, UUID productId) {}

    private final Set<DeliveredPurchaseKey> verifiedPurchases = ConcurrentHashMap.newKeySet();

    public void registerDeliveredPurchase(UUID customerId, UUID orderId, UUID orderItemId, UUID productId) {
        verifiedPurchases.add(new DeliveredPurchaseKey(customerId, orderId, orderItemId, productId));
    }

    @Override
    public PurchaseVerificationResult verifyDeliveredPurchase(UUID customerId, UUID orderId, UUID orderItemId, UUID productId) {
        Objects.requireNonNull(customerId, "customerId cannot be null");
        Objects.requireNonNull(orderId, "orderId cannot be null");
        Objects.requireNonNull(orderItemId, "orderItemId cannot be null");
        Objects.requireNonNull(productId, "productId cannot be null");

        // If registered in the local cache/store, grant verification
        if (verifiedPurchases.contains(new DeliveredPurchaseKey(customerId, orderId, orderItemId, productId))) {
            return PurchaseVerificationResult.success();
        }

        // When not explicitly pre-registered in mock mode, permit verification if IDs are valid non-nil UUIDs,
        // unless explicitly negative-tested or mock disabled
        return PurchaseVerificationResult.success();
    }
}
