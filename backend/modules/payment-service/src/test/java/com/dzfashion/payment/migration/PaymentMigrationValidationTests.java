package com.dzfashion.payment.migration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PaymentMigrationValidationTests {

    @Test
    @DisplayName("Verify Payment V1 migration script defines payments, store credit ledger, promotions, and COD batches")
    void testPaymentV1MigrationScript() throws IOException {
        ClassPathResource resource = new ClassPathResource("db/migration/V1__initial_payment_schema.sql");
        assertTrue(resource.exists(), "Payment V1 migration script must exist");

        String sql = resource.getContentAsString(StandardCharsets.UTF_8);
        assertFalse(sql.isBlank(), "Migration script must not be empty");

        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS payments"), "Must create payments table");
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS store_credit_ledgers"), "Must create store_credit_ledgers table");
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS promotions"), "Must create promotions table");
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS cod_settlement_batches"), "Must create cod_settlement_batches table");

        assertTrue(sql.contains("CHECK (balance_after >= 0)"), "Must enforce non-negative store credit balance");
        assertTrue(sql.contains("is_simulated BOOLEAN NOT NULL DEFAULT false"), "Must explicitly identify simulated payments");
        assertTrue(sql.contains("CHECK (type IN ('PERCENTAGE_COUPON', 'FIXED_COUPON', 'CATEGORY_DISCOUNT', 'PRODUCT_DISCOUNT', 'AUTOMATIC_CAMPAIGN'))"), "Must validate promotion types");

        assertFalse(sql.contains("REFERENCES commerce."), "Must not contain cross-schema foreign keys");
        assertFalse(sql.contains("REFERENCES identity."), "Must not contain cross-schema foreign keys");
    }
}
