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
    @DisplayName("Verify Payment V1 migration script defines payments, store credit, promotions, and COD batches")
    void testPaymentV1MigrationScript() throws IOException {
        ClassPathResource resource = new ClassPathResource("db/migration/V1__initial_payment_schema.sql");
        assertTrue(resource.exists(), "Payment V1 migration script must exist");

        String sql = resource.getContentAsString(StandardCharsets.UTF_8);
        assertFalse(sql.isBlank(), "Migration script must not be empty");

        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS payments"), "Must create payments table");
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS store_credit_ledgers"), "Must create store_credit_ledgers table");
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS promotions"), "Must create promotions table");
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS cod_settlement_batches"), "Must create cod_settlement_batches table");

        assertTrue(sql.contains("is_simulated BOOLEAN NOT NULL DEFAULT false"), "Must mark simulated payment records");
        assertTrue(sql.contains("CHECK (status IN ('SUCCESS', 'PENDING', 'FAILED', 'VOIDED', 'REVERSED'))"), "Must enforce standard payment statuses");
        assertTrue(sql.contains("CHECK (balance_after >= 0)"), "Must enforce non-negative store credit balance");

        assertFalse(sql.contains("REFERENCES commerce."), "Must not contain cross-schema foreign keys");
        assertFalse(sql.contains("REFERENCES identity."), "Must not contain cross-schema foreign keys");
    }

    @Test
    @DisplayName("Verify Payment V2 migration script defines tax classifications, invoices, sequences, and outbox")
    void testPaymentV2MigrationScript() throws IOException {
        ClassPathResource resource = new ClassPathResource("db/migration/V2__payment_promotions_and_invoices_enhancements.sql");
        assertTrue(resource.exists(), "Payment V2 migration script must exist");

        String sql = resource.getContentAsString(StandardCharsets.UTF_8);
        assertFalse(sql.isBlank(), "Migration script must not be empty");

        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS tax_classifications"), "Must create tax classifications table");
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS invoices"), "Must create invoices table");
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS invoice_items"), "Must create invoice items table");
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS invoice_sequences"), "Must create invoice sequences table");
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS outbox_events"), "Must create outbox events table");

        assertFalse(sql.contains("REFERENCES commerce."), "Must not contain cross-schema foreign keys");
    }
}
