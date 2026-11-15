# Flyway Migration Playbook

This document defines how database schema changes are managed and deployed for D & Z Fashion.

## 1. Migration Location and File Naming
- Each service maintains its own migration scripts in its module directory:
  `backend/modules/<service>/src/main/resources/db/migration/`
- Migration filenames must adhere to Flyway versioned migration convention:
  `V<version>__<description>.sql` (e.g. `V1__initial_identity_schema.sql`, `V2__add_index.sql`).
- Note the two underscores `__` between the version and the description.

## 2. Connection Pooling & DDL Requirements
- **Supabase Transaction Pooler (port 6543)**: Intended for high-throughput runtime queries with short transaction lifecycles. Does not support session-level DDL locks reliably.
- **Supabase Direct Connection / Session Pooler (port 5432)**: **Required** for executing Flyway migrations (`SUPABASE_JDBC_URL`).
- All connections must enforce SSL mode: `sslmode=require`.

## 3. Schema Execution Rules
- Flyway automatically creates the schema if it does not exist (`create-schemas: true`).
- Hibernate schema generation mode is set to `validate` (`spring.jpa.hibernate.ddl-auto: validate`). Hibernate will verify that entities match the Flyway-migrated database schema and fail fast on discrepancies.
- Never edit an existing migration script that has already been applied in any shared or production environment. Always write a new incremented version (`V2`, `V3`, etc.).

## 4. Rollback and Recovery Strategy
- Forward-only migrations: in production, corrections are applied as new forward migrations rather than destructive rollbacks.
- Backups: take a snapshot of the Supabase database before running major schema migrations.
