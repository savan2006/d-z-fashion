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
}
