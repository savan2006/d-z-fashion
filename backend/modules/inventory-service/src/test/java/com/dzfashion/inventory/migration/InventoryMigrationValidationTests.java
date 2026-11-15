package com.dzfashion.inventory.migration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InventoryMigrationValidationTests {

    @Test
    @DisplayName("Verify Inventory V1 migration script defines stock, reservations, and audit movements")
    void testInventoryV1MigrationScript() throws IOException {
        ClassPathResource resource = new ClassPathResource("db/migration/V1__initial_inventory_schema.sql");
        assertTrue(resource.exists(), "Inventory V1 migration script must exist");

        String sql = resource.getContentAsString(StandardCharsets.UTF_8);
        assertFalse(sql.isBlank(), "Migration script must not be empty");

        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS warehouse_stock"), "Must create warehouse_stock table");
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS stock_reservations"), "Must create stock_reservations table");
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS stock_movements"), "Must create stock_movements table");

        assertTrue(sql.contains("CHECK (physical_qty >= 0)"), "Must enforce non-negative physical stock");
        assertTrue(sql.contains("CHECK (reserved_qty >= 0)"), "Must enforce non-negative reserved stock");
        assertTrue(sql.contains("available_qty INT GENERATED ALWAYS AS (physical_qty - reserved_qty) STORED"), "Must compute available quantity deterministically");
        assertTrue(sql.contains("CHECK (available_qty >= 0)"), "Must prevent overselling below 0 available stock");

        assertFalse(sql.contains("REFERENCES catalog."), "Must not contain cross-schema foreign keys");
        assertFalse(sql.contains("REFERENCES commerce."), "Must not contain cross-schema foreign keys");
    }
}
