-- Fulfillment & Shipping Service - Enhancements (Component 08)
-- Schema ownership: fulfillment

-- 1. Allow transport_agency_id to be nullable during initial picking/packing before handover
ALTER TABLE shipments ALTER COLUMN transport_agency_id DROP NOT NULL;

-- 2. Add recipient, order, parcel, and lifecycle columns to shipments
ALTER TABLE shipments ADD COLUMN IF NOT EXISTS order_number VARCHAR(64);
ALTER TABLE shipments ADD COLUMN IF NOT EXISTS customer_id UUID;
ALTER TABLE shipments ADD COLUMN IF NOT EXISTS recipient_name VARCHAR(100);
ALTER TABLE shipments ADD COLUMN IF NOT EXISTS recipient_phone VARCHAR(20);
ALTER TABLE shipments ADD COLUMN IF NOT EXISTS shipping_address TEXT;
ALTER TABLE shipments ADD COLUMN IF NOT EXISTS city VARCHAR(50);
ALTER TABLE shipments ADD COLUMN IF NOT EXISTS state VARCHAR(50);
ALTER TABLE shipments ADD COLUMN IF NOT EXISTS pincode VARCHAR(10);
ALTER TABLE shipments ADD COLUMN IF NOT EXISTS is_cod BOOLEAN NOT NULL DEFAULT false;
ALTER TABLE shipments ADD COLUMN IF NOT EXISTS cod_amount NUMERIC(12, 2) NOT NULL DEFAULT 0.00;
ALTER TABLE shipments ADD COLUMN IF NOT EXISTS cod_collected_amount NUMERIC(12, 2) DEFAULT 0.00;
ALTER TABLE shipments ADD COLUMN IF NOT EXISTS daily_dispatch_batch_id UUID;
ALTER TABLE shipments ADD COLUMN IF NOT EXISTS picked_at TIMESTAMPTZ;
ALTER TABLE shipments ADD COLUMN IF NOT EXISTS packed_at TIMESTAMPTZ;
ALTER TABLE shipments ADD COLUMN IF NOT EXISTS dispatched_at TIMESTAMPTZ;
ALTER TABLE shipments ADD COLUMN IF NOT EXISTS returned_to_warehouse_at TIMESTAMPTZ;
ALTER TABLE shipments ADD COLUMN IF NOT EXISTS eligible_return_until TIMESTAMPTZ;
ALTER TABLE shipments ADD COLUMN IF NOT EXISTS cod_settlement_batch_id UUID;

CREATE INDEX IF NOT EXISTS idx_shipments_order_number ON shipments (order_number);
CREATE INDEX IF NOT EXISTS idx_shipments_customer ON shipments (customer_id);
CREATE INDEX IF NOT EXISTS idx_shipments_dispatch_batch ON shipments (daily_dispatch_batch_id);
CREATE INDEX IF NOT EXISTS idx_shipments_cod_settlement ON shipments (cod_settlement_batch_id);

-- 3. Enhance daily_dispatch_batches with agency, handover details and status
ALTER TABLE daily_dispatch_batches ADD COLUMN IF NOT EXISTS transport_agency_id UUID REFERENCES transport_agencies(id);
ALTER TABLE daily_dispatch_batches ADD COLUMN IF NOT EXISTS bill_reference VARCHAR(100);
ALTER TABLE daily_dispatch_batches ADD COLUMN IF NOT EXISTS status VARCHAR(30) NOT NULL DEFAULT 'PREPARED';
ALTER TABLE daily_dispatch_batches ADD COLUMN IF NOT EXISTS handover_at TIMESTAMPTZ;

-- 4. Create shipment line items for picking and packing
CREATE TABLE IF NOT EXISTS shipment_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    shipment_id UUID NOT NULL REFERENCES shipments(id) ON DELETE CASCADE,
    order_item_id UUID NOT NULL,
    product_id UUID NOT NULL,
    variant_id UUID NOT NULL,
    product_name VARCHAR(255) NOT NULL,
    sku VARCHAR(60) NOT NULL,
    size VARCHAR(20),
    color VARCHAR(50),
    bin_location VARCHAR(50) DEFAULT 'SURAT-WH-MAIN',
    quantity INT NOT NULL CHECK (quantity > 0),
    picked_quantity INT NOT NULL DEFAULT 0 CHECK (picked_quantity >= 0),
    is_picked BOOLEAN NOT NULL DEFAULT false,
    is_packed BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_shipment_items_shipment ON shipment_items (shipment_id);
CREATE INDEX IF NOT EXISTS idx_shipment_items_sku ON shipment_items (sku);

-- 5. Delivery attempt logs (strictly max 2 attempts enforced by application & check constraint)
CREATE TABLE IF NOT EXISTS delivery_attempt_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    shipment_id UUID NOT NULL REFERENCES shipments(id) ON DELETE CASCADE,
    attempt_number INT NOT NULL CHECK (attempt_number IN (1, 2)),
    attempted_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    result VARCHAR(50) NOT NULL,
    notes TEXT,
    recorded_by UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_delivery_attempts_shipment ON delivery_attempt_logs (shipment_id);

-- 6. Agency-specific COD settlement batches
CREATE TABLE IF NOT EXISTS agency_cod_settlements (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    batch_reference VARCHAR(64) NOT NULL UNIQUE,
    transport_agency_id UUID NOT NULL REFERENCES transport_agencies(id),
    settlement_date DATE NOT NULL,
    total_delivered_orders INT NOT NULL DEFAULT 0,
    total_expected_cod NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    remitted_amount NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    discrepancy_amount NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    status VARCHAR(30) NOT NULL DEFAULT 'PREPARED' CHECK (status IN ('PREPARED', 'APPROVED', 'DISCREPANCY_FLAGGED')),
    prepared_by UUID NOT NULL,
    approved_by UUID,
    proof_reference VARCHAR(100),
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    approved_at TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_agency_cod_settlements_agency ON agency_cod_settlements (transport_agency_id);
CREATE INDEX IF NOT EXISTS idx_agency_cod_settlements_status ON agency_cod_settlements (status);

-- 7. Outbox events for asynchronous reliable event dispatch
CREATE TABLE IF NOT EXISTS outbox_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    aggregate_type VARCHAR(50) NOT NULL,
    aggregate_id VARCHAR(64) NOT NULL,
    event_type VARCHAR(60) NOT NULL,
    payload_json TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    processed_at TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_fulfillment_outbox_unprocessed ON outbox_events (processed_at) WHERE processed_at IS NULL;

-- 8. Seed standard Surat transport agencies
INSERT INTO transport_agencies (name, contact_person, phone_number, is_active)
VALUES
    ('Delhivery Logistics', 'Rajesh Patel', '+919876543210', true),
    ('BlueDart Express', 'Vikram Desai', '+919876543211', true),
    ('DTDC Express', 'Anil Mehta', '+919876543212', true),
    ('Shree Tirupati Courier', 'Suresh Shah', '+919876543213', true)
ON CONFLICT (name) DO NOTHING;
