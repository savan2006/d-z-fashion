-- Fulfillment & Shipping Service - Returns, Exchanges, and Inspections Schema
-- Schema ownership: fulfillment

-- 1. Create return requests table for 3-day quality policy returns and exchanges
CREATE TABLE IF NOT EXISTS return_requests (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    return_number VARCHAR(64) NOT NULL UNIQUE,
    order_id UUID NOT NULL,
    order_number VARCHAR(64) NOT NULL,
    order_item_id UUID NOT NULL,
    customer_id UUID NOT NULL,
    sku VARCHAR(60) NOT NULL,
    product_name VARCHAR(255) NOT NULL,
    quantity INT NOT NULL CHECK (quantity > 0),
    original_unit_price NUMERIC(12,2) NOT NULL CHECK (original_unit_price >= 0),
    reason VARCHAR(30) NOT NULL CHECK (reason IN ('DAMAGED', 'DEFECTIVE', 'INCORRECT_PRODUCT')),
    resolution_type VARCHAR(30) NOT NULL DEFAULT 'EXCHANGE' CHECK (resolution_type IN ('EXCHANGE', 'REFUND')),
    status VARCHAR(35) NOT NULL DEFAULT 'REQUESTED' CHECK (status IN (
        'REQUESTED',
        'PICKUP_SCHEDULED',
        'PICKED_UP',
        'IN_TRANSIT_TO_SURAT',
        'RECEIVED_AT_SURAT',
        'INSPECTED',
        'EXCHANGE_PREPARED',
        'EXCHANGE_DISPATCHED',
        'REFUND_PENDING_OWNER_APPROVAL',
        'REFUND_APPROVED',
        'REFUND_PROCESSED',
        'REJECTED',
        'CLOSED'
    )),
    photo_urls TEXT[] NOT NULL DEFAULT '{}',
    customer_notes TEXT,

    -- Transport pickup details (arranged by Manager, paid by D&Z)
    transport_agency_id UUID REFERENCES transport_agencies(id),
    pickup_reference VARCHAR(100),
    pickup_scheduled_date DATE,
    picked_up_at TIMESTAMPTZ,
    received_at_warehouse_at TIMESTAMPTZ,

    -- Exchange details
    replacement_product_id UUID,
    replacement_variant_id UUID,
    replacement_sku VARCHAR(60),
    replacement_product_name VARCHAR(255),
    replacement_unit_price NUMERIC(12,2),
    price_difference NUMERIC(12,2) NOT NULL DEFAULT 0.00,
    price_difference_settled BOOLEAN NOT NULL DEFAULT true,
    replacement_shipment_id UUID REFERENCES shipments(id),

    -- Refund details (exceptional/legally required only, Owner approval mandatory)
    refund_amount NUMERIC(12,2),
    refund_deadline TIMESTAMPTZ,
    refund_approved_by UUID,
    refund_approved_at TIMESTAMPTZ,
    refund_rejection_reason TEXT,
    is_cod_refund BOOLEAN NOT NULL DEFAULT false,

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_return_requests_order ON return_requests (order_id);
CREATE INDEX IF NOT EXISTS idx_return_requests_customer ON return_requests (customer_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_return_requests_status ON return_requests (status);
CREATE INDEX IF NOT EXISTS idx_return_requests_agency ON return_requests (transport_agency_id);

-- 2. Enhance return_inspections with disposition and photo evidence
ALTER TABLE return_inspections ADD COLUMN IF NOT EXISTS disposition VARCHAR(30) NOT NULL DEFAULT 'DAMAGED_QUARANTINE';
ALTER TABLE return_inspections ADD COLUMN IF NOT EXISTS photo_urls TEXT[] DEFAULT '{}';
ALTER TABLE return_inspections ADD CONSTRAINT fk_return_inspection_request FOREIGN KEY (return_request_id) REFERENCES return_requests(id) ON DELETE CASCADE;
