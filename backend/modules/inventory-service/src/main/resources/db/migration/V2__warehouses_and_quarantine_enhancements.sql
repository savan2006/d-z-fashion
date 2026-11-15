-- Inventory Service - Schema Enhancement
-- Warehouses, Quarantine Stock, Reservation Idempotency, and Low Stock Alerts

CREATE TABLE IF NOT EXISTS warehouses (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    city VARCHAR(50) NOT NULL DEFAULT 'Surat',
    state VARCHAR(50) NOT NULL DEFAULT 'Gujarat',
    country VARCHAR(50) NOT NULL DEFAULT 'India',
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Seed Phase One Surat Warehouse
INSERT INTO warehouses (code, name, city, state, country, is_active)
VALUES ('SURAT-MAIN', 'Surat Central Warehouse', 'Surat', 'Gujarat', 'India', true)
ON CONFLICT (code) DO NOTHING;

-- Add warehouse reference and quarantined quantity to warehouse_stock
ALTER TABLE warehouse_stock
    ADD COLUMN IF NOT EXISTS warehouse_id UUID REFERENCES warehouses(id),
    ADD COLUMN IF NOT EXISTS quarantined_qty INT NOT NULL DEFAULT 0 CHECK (quarantined_qty >= 0);

-- Update default warehouse for existing rows
UPDATE warehouse_stock
SET warehouse_id = (SELECT id FROM warehouses WHERE code = 'SURAT-MAIN' LIMIT 1)
WHERE warehouse_id IS NULL;

-- Add idempotency key to reservations
ALTER TABLE stock_reservations
    ADD COLUMN IF NOT EXISTS idempotency_key VARCHAR(100) UNIQUE;

-- Expand stock_movements reason check constraint to include quarantine reason
ALTER TABLE stock_movements
    DROP CONSTRAINT IF EXISTS stock_movements_reason_check;

ALTER TABLE stock_movements
    ADD CONSTRAINT stock_movements_reason_check
    CHECK (reason IN ('NEW_MANUFACTURE', 'MANUAL_CORRECTION', 'ORDER_RESERVATION', 'ORDER_RELEASE', 'DISPATCH', 'RETURN_RESTOCK', 'DAMAGE_QUARANTINE'));

-- Low stock alerts table for alert notification tracking and deduplication
CREATE TABLE IF NOT EXISTS low_stock_alerts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    variant_id UUID NOT NULL,
    sku VARCHAR(60) NOT NULL,
    available_qty INT NOT NULL,
    threshold INT NOT NULL,
    alert_sent_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    is_resolved BOOLEAN NOT NULL DEFAULT false,
    resolved_at TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_low_stock_alerts_unresolved ON low_stock_alerts (variant_id, is_resolved);
