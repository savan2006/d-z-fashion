package com.dzfashion.fulfillment.migration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FulfillmentMigrationValidationTests {

    @Test
    @DisplayName("Verify Fulfillment V1 migration script defines transport agencies, shipments, dispatch bills, and return inspections")
    void testFulfillmentV1MigrationScript() throws IOException {
        ClassPathResource resource = new ClassPathResource("db/migration/V1__initial_fulfillment_schema.sql");
        assertTrue(resource.exists(), "Fulfillment V1 migration script must exist");

        String sql = resource.getContentAsString(StandardCharsets.UTF_8);
        assertFalse(sql.isBlank(), "Migration script must not be empty");

        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS transport_agencies"), "Must create transport_agencies table");
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS shipments"), "Must create shipments table");
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS daily_dispatch_batches"), "Must create daily_dispatch_batches table");
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS return_inspections"), "Must create return_inspections table");

        assertTrue(sql.contains("CHECK (delivery_attempts BETWEEN 0 AND 2)"), "Must enforce max 2 delivery attempts");
        assertTrue(sql.contains("CHECK (result IN ('PASSED', 'REJECTED'))"), "Must validate inspection result");

        assertFalse(sql.contains("REFERENCES commerce."), "Must not contain cross-schema foreign keys");
        assertFalse(sql.contains("REFERENCES inventory."), "Must not contain cross-schema foreign keys");
    }

    @Test
    @DisplayName("Verify Fulfillment V2 migration script adds line items, attempt logs, agency settlements, and outbox")
    void testFulfillmentV2MigrationScript() throws IOException {
        ClassPathResource resource = new ClassPathResource("db/migration/V2__fulfillment_enhancements.sql");
        assertTrue(resource.exists(), "Fulfillment V2 migration script must exist");

        String sql = resource.getContentAsString(StandardCharsets.UTF_8);
        assertFalse(sql.isBlank(), "Migration script must not be empty");

        assertTrue(sql.contains("ALTER TABLE shipments ALTER COLUMN transport_agency_id DROP NOT NULL"), "Must allow nullable transport agency before handover");
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS shipment_items"), "Must create shipment_items table");
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS delivery_attempt_logs"), "Must create delivery_attempt_logs table");
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS agency_cod_settlements"), "Must create agency_cod_settlements table");
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS outbox_events"), "Must create outbox_events table");
        assertTrue(sql.contains("CHECK (attempt_number IN (1, 2))"), "Must enforce attempt 1 or 2 check");

        assertFalse(sql.contains("REFERENCES commerce."), "Must not contain cross-schema foreign keys");
        assertFalse(sql.contains("REFERENCES payment."), "Must not contain cross-schema foreign keys");
    }

    @Test
    @DisplayName("Verify Fulfillment V3 migration script adds return requests, pickup tracking, exchange fields, and inspection disposition")
    void testFulfillmentV3MigrationScript() throws IOException {
        ClassPathResource resource = new ClassPathResource("db/migration/V3__return_and_exchange_workflow.sql");
        assertTrue(resource.exists(), "Fulfillment V3 migration script must exist");

        String sql = resource.getContentAsString(StandardCharsets.UTF_8);
        assertFalse(sql.isBlank(), "Migration script must not be empty");

        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS return_requests"), "Must create return_requests table");
        assertTrue(sql.contains("CHECK (reason IN ('DAMAGED', 'DEFECTIVE', 'INCORRECT_PRODUCT'))"), "Must enforce strict 3-day return policy reasons");
        assertTrue(sql.contains("CHECK (resolution_type IN ('EXCHANGE', 'REFUND'))"), "Must validate resolution type");
        assertTrue(sql.contains("ALTER TABLE return_inspections ADD COLUMN IF NOT EXISTS disposition"), "Must enhance return inspections with disposition");
        assertTrue(sql.contains("fk_return_inspection_request"), "Must link return inspections to return requests");

        assertFalse(sql.contains("REFERENCES commerce."), "Must not contain cross-schema foreign keys");
        assertFalse(sql.contains("REFERENCES payment."), "Must not contain cross-schema foreign keys");
    }
}
