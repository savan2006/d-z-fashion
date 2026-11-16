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

    @Test
    @DisplayName("Verify Commerce V2 migration script defines claim history, shortage incidents, outbox, and order enhancements")
    void testCommerceV2MigrationScript() throws IOException {
        ClassPathResource resource = new ClassPathResource("db/migration/V2__commerce_enhancements.sql");
        assertTrue(resource.exists(), "Commerce V2 migration script must exist");

        String sql = resource.getContentAsString(StandardCharsets.UTF_8);
        assertFalse(sql.isBlank(), "Migration script must not be empty");

        assertTrue(sql.contains("ALTER TABLE orders ADD COLUMN IF NOT EXISTS idempotency_key"), "Must add idempotency key");
        assertTrue(sql.contains("ALTER TABLE orders ADD COLUMN IF NOT EXISTS cancelled_by"), "Must add cancelled_by");
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS order_claim_history"), "Must create order_claim_history table");
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS order_shortage_incidents"), "Must create order_shortage_incidents table");
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS outbox_events"), "Must create outbox_events table");

        assertFalse(sql.contains("REFERENCES catalog."), "Must not contain cross-schema foreign keys");
        assertFalse(sql.contains("REFERENCES inventory."), "Must not contain cross-schema foreign keys");
    }

    @Test
    @DisplayName("Verify Commerce V3 migration script defines support tickets, notifications, audit trail, and processed events")
    void testCommerceV3MigrationScript() throws IOException {
        ClassPathResource resource = new ClassPathResource("db/migration/V3__integration_support_notifications_audit.sql");
        assertTrue(resource.exists(), "Commerce V3 migration script must exist");

        String sql = resource.getContentAsString(StandardCharsets.UTF_8);
        assertFalse(sql.isBlank(), "Migration script must not be empty");

        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS support_tickets"), "Must create support_tickets table");
        assertTrue(sql.contains("CHECK (channel IN ('WEB', 'WHATSAPP', 'PHONE'))"), "Must support multi-channel intake");
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS support_ticket_messages"), "Must create support_ticket_messages table");
        assertTrue(sql.contains("is_internal_note BOOLEAN"), "Must support internal staff notes hidden from customer");
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS notifications"), "Must create in-account notifications table");
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS notification_deliveries"), "Must create notification_deliveries tracking table");
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS audit_logs"), "Must create audit_logs table");
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS processed_events"), "Must create processed_events table for idempotency");

        assertFalse(sql.contains("REFERENCES identity."), "Must not contain cross-schema foreign keys");
        assertFalse(sql.contains("REFERENCES fulfillment."), "Must not contain cross-schema foreign keys");
    }
}
