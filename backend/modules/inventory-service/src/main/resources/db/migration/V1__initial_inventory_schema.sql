-- Inventory Service - Initial PostgreSQL Schema
-- Schema ownership: inventory

CREATE TABLE IF NOT EXISTS warehouse_stock (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    variant_id UUID NOT NULL UNIQUE,
    sku VARCHAR(60) NOT NULL UNIQUE,
    physical_qty INT NOT NULL DEFAULT 0 CHECK (physical_qty >= 0),
    reserved_qty INT NOT NULL DEFAULT 0 CHECK (reserved_qty >= 0),
    available_qty INT GENERATED ALWAYS AS (physical_qty - reserved_qty) STORED CHECK (available_qty >= 0),
    low_stock_threshold INT NOT NULL DEFAULT 5 CHECK (low_stock_threshold >= 0),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_warehouse_stock_variant ON warehouse_stock (variant_id);
CREATE INDEX IF NOT EXISTS idx_warehouse_stock_sku ON warehouse_stock (sku);
CREATE INDEX IF NOT EXISTS idx_warehouse_stock_available ON warehouse_stock (available_qty);

CREATE TABLE IF NOT EXISTS stock_reservations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id UUID NOT NULL,
    variant_id UUID NOT NULL,
    quantity INT NOT NULL CHECK (quantity > 0),
    status VARCHAR(20) NOT NULL DEFAULT 'RESERVED' CHECK (status IN ('RESERVED', 'COMMITTED', 'RELEASED')),
    expires_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_stock_reservations_order ON stock_reservations (order_id);
CREATE INDEX IF NOT EXISTS idx_stock_reservations_variant_status ON stock_reservations (variant_id, status);

CREATE TABLE IF NOT EXISTS stock_movements (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    variant_id UUID NOT NULL,
    change_qty INT NOT NULL,
    balance_after INT NOT NULL CHECK (balance_after >= 0),
    reason VARCHAR(50) NOT NULL CHECK (reason IN ('NEW_MANUFACTURE', 'MANUAL_CORRECTION', 'ORDER_RESERVATION', 'ORDER_RELEASE', 'DISPATCH', 'RETURN_RESTOCK')),
    reference_type VARCHAR(50),
    reference_id VARCHAR(100),
    actor_id UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_stock_movements_variant ON stock_movements (variant_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_stock_movements_reason ON stock_movements (reason);
