package com.dzfashion.catalog.client;

import java.util.UUID;

public interface PurchaseVerificationClient {

    record PurchaseVerificationResult(
            boolean verified,
            String failureReason
    ) {
        public static PurchaseVerificationResult success() {
            return new PurchaseVerificationResult(true, null);
        }

        public static PurchaseVerificationResult failure(String reason) {
            return new PurchaseVerificationResult(false, reason);
        }
    }

    PurchaseVerificationResult verifyDeliveredPurchase(UUID customerId, UUID orderId, UUID orderItemId, UUID productId);
}
