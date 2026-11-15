package com.dzfashion.commerce.migration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommerceMigrationValidationTests {

    @Test
    @DisplayName("Verify Commerce V1 migration script defines carts, orders, order items, and COD pincodes")
    void testCommerceV1MigrationScript() throws IOException {
        ClassPathResource resource = new ClassPathResource("db/migration/V1__initial_commerce_schema.sql");
        assertTrue(resource.exists(), "Commerce V1 migration script must exist");

        String sql = resource.getContentAsString(StandardCharsets.UTF_8);
        assertFalse(sql.isBlank(), "Migration script must not be empty");

        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS carts"), "Must create carts table");
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS cart_items"), "Must create cart_items table");
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS orders"), "Must create orders table");
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS order_items"), "Must create order_items table");
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS cod_serviceable_pincodes"), "Must create cod_serviceable_pincodes table");

        assertTrue(sql.contains("subtotal_before_discount_and_tax NUMERIC(12,2)"), "Must store subtotal snapshot");
        assertTrue(sql.contains("promotional_discount_amount NUMERIC(12,2)"), "Must store discount snapshot");
        assertTrue(sql.contains("delivery_charge NUMERIC(12,2)"), "Must store delivery charge snapshot");
        assertTrue(sql.contains("CHECK (status IN ('CONFIRMED', 'PROCESSING', 'SHIPPED', 'DELIVERED', 'CANCELLED'))"), "Must enforce standard customer status cycle");

        assertFalse(sql.contains("REFERENCES identity."), "Must not contain cross-schema foreign keys");
        assertFalse(sql.contains("REFERENCES inventory."), "Must not contain cross-schema foreign keys");
    }
}
