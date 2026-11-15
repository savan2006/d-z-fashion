package com.dzfashion.identity.migration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IdentityMigrationValidationTests {

    @Test
    @DisplayName("Verify Identity V1 migration script exists and defines required tables and constraints")
    void testIdentityV1MigrationScript() throws IOException {
        ClassPathResource resource = new ClassPathResource("db/migration/V1__initial_identity_schema.sql");
        assertTrue(resource.exists(), "Flyway migration V1 script must exist");

        String sql = resource.getContentAsString(StandardCharsets.UTF_8);
        assertFalse(sql.isBlank(), "Migration script must not be empty");

        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS app_users"), "Must create app_users table");
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS customer_profiles"), "Must create customer_profiles table");
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS email_otps"), "Must create email_otps table");

        assertTrue(sql.contains("gen_random_uuid()"), "Must use UUID primary keys");
        assertTrue(sql.contains("CHECK (role IN ('CUSTOMER', 'MANAGER', 'OWNER'))"), "Must enforce strict role constraint");
        assertTrue(sql.contains("TIMESTAMPTZ"), "Must use timezone-aware timestamps");

        assertFalse(sql.contains("REFERENCES catalog."), "Must not contain cross-schema foreign keys");
        assertFalse(sql.contains("REFERENCES commerce."), "Must not contain cross-schema foreign keys");
    }
}
