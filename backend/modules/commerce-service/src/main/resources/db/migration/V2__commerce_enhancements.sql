-- Commerce Service - Schema Enhancements (V2)
-- Schema ownership: commerce

-- 1. Orders table enhancements
ALTER TABLE orders ADD COLUMN IF NOT EXISTS idempotency_key VARCHAR(128);
CREATE UNIQUE INDEX IF NOT EXISTS uq_orders_idempotency_key ON orders (idempotency_key) WHERE idempotency_key IS NOT NULL;

ALTER TABLE orders ADD COLUMN IF NOT EXISTS cancelled_by UUID;

-- 2. Order claim and reassignment audit history
CREATE TABLE IF NOT EXISTS order_claim_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id UUID NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    actor_id UUID NOT NULL,
    action VARCHAR(30) NOT NULL CHECK (action IN ('CLAIMED', 'REASSIGNED', 'DISPATCHED', 'DELIVERED', 'SHORTAGE_REPORTED')),
    previous_manager_id UUID,
    new_manager_id UUID,
    reason TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_order_claim_history_order ON order_claim_history (order_id, created_at DESC);

-- 3. Order physical shortage incidents
CREATE TABLE IF NOT EXISTS order_shortage_incidents (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    incident_number VARCHAR(32) NOT NULL UNIQUE,
    order_id UUID NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    variant_id UUID NOT NULL,
    reported_by UUID NOT NULL,
    expected_quantity INT NOT NULL CHECK (expected_quantity > 0),
    actual_quantity INT NOT NULL CHECK (actual_quantity >= 0),
    notes TEXT NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'REPORTED' CHECK (status IN ('REPORTED', 'CONTACTED_CUSTOMER', 'RESOLVED')),
    resolution_notes TEXT,
    resolved_by UUID,
    resolved_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_shortage_incidents_order ON order_shortage_incidents (order_id);
CREATE INDEX IF NOT EXISTS idx_shortage_incidents_status ON order_shortage_incidents (status);

-- 4. Transactional Outbox Events
CREATE TABLE IF NOT EXISTS outbox_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    aggregate_type VARCHAR(50) NOT NULL,
    aggregate_id VARCHAR(64) NOT NULL,
    event_type VARCHAR(60) NOT NULL,
    payload_json TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    processed_at TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_outbox_events_processed ON outbox_events (processed_at) WHERE processed_at IS NULL;
