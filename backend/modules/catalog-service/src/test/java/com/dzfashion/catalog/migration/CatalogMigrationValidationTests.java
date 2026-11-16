package com.dzfashion.catalog.migration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CatalogMigrationValidationTests {

    @Test
    @DisplayName("Verify Catalog V1 migration script defines categories, products, variants, and media tables")
    void testCatalogV1MigrationScript() throws IOException {
        ClassPathResource resource = new ClassPathResource("db/migration/V1__initial_catalog_schema.sql");
        assertTrue(resource.exists(), "Catalog V1 migration script must exist");

        String sql = resource.getContentAsString(StandardCharsets.UTF_8);
        assertFalse(sql.isBlank(), "Migration script must not be empty");

        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS categories"), "Must create categories table");
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS products"), "Must create products table");
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS product_variants"), "Must create product_variants table");
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS product_revisions"), "Must create product_revisions table");
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS media_assets"), "Must create media_assets table");

        assertTrue(sql.contains("NUMERIC(12,2)"), "Must use NUMERIC for prices");
        assertTrue(sql.contains("sku VARCHAR(60) NOT NULL UNIQUE"), "Must enforce SKU uniqueness");
        assertTrue(sql.contains("uq_product_variant_size_colour"), "Must enforce unique size-colour combination per product");
        assertTrue(sql.contains("CHECK (status IN ('DRAFT', 'PUBLISHED', 'ARCHIVED'))"), "Must validate product lifecycle status");

        assertFalse(sql.contains("REFERENCES inventory."), "Must not contain cross-schema foreign keys");
        assertFalse(sql.contains("REFERENCES identity."), "Must not contain cross-schema foreign keys");
    }

    @Test
    @DisplayName("Verify Catalog V2 migration script defines product_reviews table with verified purchase constraints")
    void testCatalogV2MigrationScript() throws IOException {
        ClassPathResource resource = new ClassPathResource("db/migration/V2__product_reviews.sql");
        assertTrue(resource.exists(), "Catalog V2 migration script must exist");

        String sql = resource.getContentAsString(StandardCharsets.UTF_8);
        assertFalse(sql.isBlank(), "Migration script must not be empty");

        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS product_reviews"), "Must create product_reviews table");
        assertTrue(sql.contains("order_item_id UUID NOT NULL UNIQUE"), "Must enforce one review per purchased order line");
        assertTrue(sql.contains("CHECK (rating >= 1 AND rating <= 5)"), "Must enforce 1-5 star ratings");
        assertTrue(sql.contains("CHECK (status IN ('PUBLISHED', 'HIDDEN', 'REMOVED'))"), "Must enforce valid review statuses");

        assertFalse(sql.contains("REFERENCES commerce."), "Must not contain cross-schema foreign keys");
        assertFalse(sql.contains("REFERENCES fulfillment."), "Must not contain cross-schema foreign keys");
    }
}
