-- Fulfillment & Shipping Service - Initial PostgreSQL Schema
-- Schema ownership: fulfillment

CREATE TABLE IF NOT EXISTS transport_agencies (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL UNIQUE,
    contact_person VARCHAR(100),
    phone_number VARCHAR(20) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS shipments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id UUID NOT NULL UNIQUE,
    transport_agency_id UUID NOT NULL REFERENCES transport_agencies(id),
    tracking_reference VARCHAR(100),
    status VARCHAR(30) NOT NULL DEFAULT 'PROCESSING' CHECK (status IN ('PROCESSING', 'DISPATCHED', 'OUT_FOR_DELIVERY', 'DELIVERED', 'DELIVERY_ATTEMPTED_1', 'DELIVERY_ATTEMPTED_2', 'RETURNING_TO_WAREHOUSE', 'RETURNED_TO_WAREHOUSE')),
    dispatch_date DATE,
    delivery_date DATE,
    delivery_attempts INT NOT NULL DEFAULT 0 CHECK (delivery_attempts BETWEEN 0 AND 2),
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_shipments_agency ON shipments (transport_agency_id);
CREATE INDEX IF NOT EXISTS idx_shipments_status ON shipments (status);
CREATE INDEX IF NOT EXISTS idx_shipments_dispatch_date ON shipments (dispatch_date);

CREATE TABLE IF NOT EXISTS daily_dispatch_batches (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    batch_number VARCHAR(64) NOT NULL UNIQUE,
    dispatch_date DATE NOT NULL,
    prepared_by UUID NOT NULL,
    parcel_count INT NOT NULL DEFAULT 0 CHECK (parcel_count >= 0),
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_daily_dispatch_date ON daily_dispatch_batches (dispatch_date DESC);

CREATE TABLE IF NOT EXISTS return_inspections (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    return_request_id UUID NOT NULL UNIQUE,
    order_id UUID NOT NULL,
    inspected_by UUID NOT NULL,
    result VARCHAR(20) NOT NULL CHECK (result IN ('PASSED', 'REJECTED')),
    condition_notes TEXT NOT NULL,
    inspected_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_return_inspections_order ON return_inspections (order_id);
