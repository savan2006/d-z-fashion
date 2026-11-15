# ADR-002: PostgreSQL Service-Owned Schemas and Migration Strategy

- Status: Accepted
- Date: 2026-11-15

## Context
D & Z Fashion operates six core business services (`identity`, `catalog`, `inventory`, `commerce`, `payment`, `fulfillment`) on a single managed Supabase PostgreSQL instance. If services share tables or write directly across boundaries, service coupling increases, preventing independent changes, auditing, and scalability.

## Decision
1. **Schema Partitioning**: The single PostgreSQL database is partitioned into six dedicated schemas matching service boundaries:
   - `identity`: User accounts, customer profile addresses, OTP verification.
   - `catalog`: Dynamic categories, products, variants, owner revisions, Cloudinary media metadata.
   - `inventory`: Surat warehouse stock, atomic reservations, stock movement ledger.
   - `commerce`: Carts, orders, immutable price/tax snapshots, COD serviceable PIN codes.
   - `payment`: Simulated payments, non-expiring store credit ledger, promotional discounts, COD remittance batches.
   - `fulfillment`: External transport agencies, manual shipment updates, picking/dispatch slips, return inspections.
2. **Strict Write Isolation**: Services only write to and read from their designated schema. Direct cross-schema SQL joins and cross-schema foreign keys are forbidden. Cross-service entity references must use opaque UUIDs and be resolved via REST APIs or domain events.
3. **Migration Management**: Flyway manages schema evolution per service independently using `src/main/resources/db/migration/V<n>__<description>.sql`.
4. **Hibernate Schema Mode**: Hibernate schema auto-generation (`ddl-auto`) is strictly set to `validate` in all environments, preventing accidental schema mutations at runtime.
5. **Data Types**: All primary keys are UUIDs generated with `gen_random_uuid()`. Financial amounts use `NUMERIC(12,2)`. Physical counts use `INT` with non-negative check constraints. Timestamps use `TIMESTAMPTZ` (UTC storage).

## Consequences
- Clean separation of database concerns without requiring multiple distinct database instances.
- Zero risk of circular foreign-key deadlocks across services.
- Database roles can be configured with least privilege per schema.
