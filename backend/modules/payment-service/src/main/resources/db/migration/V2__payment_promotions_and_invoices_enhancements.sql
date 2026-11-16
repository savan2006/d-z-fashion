-- Payment & Credit Service - Schema Enhancements (V2)
-- Schema ownership: payment

-- 1. Payments table enhancements for COD & Customer tracking
ALTER TABLE payments ADD COLUMN IF NOT EXISTS customer_id UUID;
ALTER TABLE payments ADD COLUMN IF NOT EXISTS delivery_pincode VARCHAR(10);
ALTER TABLE payments ADD COLUMN IF NOT EXISTS collected_at TIMESTAMPTZ;
ALTER TABLE payments ADD COLUMN IF NOT EXISTS collected_by UUID;

CREATE INDEX IF NOT EXISTS idx_payments_customer ON payments (customer_id);

-- 2. Configurable GST Tax Classifications
CREATE TABLE IF NOT EXISTS tax_classifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    hsn_code VARCHAR(20) NOT NULL UNIQUE,
    description VARCHAR(255) NOT NULL,
    gst_rate_percent NUMERIC(5,2) NOT NULL CHECK (gst_rate_percent >= 0),
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Seed standard women apparel and courier HSN classifications
INSERT INTO tax_classifications (hsn_code, description, gst_rate_percent, is_active)
VALUES
    ('6204', 'Women or girls suits, ensembles, jackets, dresses, skirts', 5.00, true),
    ('6104', 'Women or girls suits, dresses, skirts knitted or crocheted', 5.00, true),
    ('9968', 'Postal and courier transportation services', 18.00, true)
ON CONFLICT (hsn_code) DO NOTHING;

-- 3. Invoices (Order receipts and dispatch GST tax invoices)
CREATE TABLE IF NOT EXISTS invoices (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    invoice_number VARCHAR(64) NOT NULL UNIQUE,
    order_id UUID NOT NULL,
    order_number VARCHAR(32) NOT NULL,
    invoice_type VARCHAR(20) NOT NULL CHECK (invoice_type IN ('RECEIPT', 'GST_INVOICE')),
    seller_legal_name VARCHAR(150) NOT NULL,
    seller_gstin VARCHAR(20) NOT NULL,
    seller_address TEXT NOT NULL,
    seller_state VARCHAR(50) NOT NULL,
    customer_id UUID NOT NULL,
    customer_name VARCHAR(100) NOT NULL,
    customer_phone VARCHAR(20) NOT NULL,
    shipping_address TEXT NOT NULL,
    place_of_supply_state VARCHAR(50) NOT NULL,
    taxable_amount NUMERIC(12,2) NOT NULL CHECK (taxable_amount >= 0),
    cgst_amount NUMERIC(12,2) NOT NULL DEFAULT 0.00 CHECK (cgst_amount >= 0),
    sgst_amount NUMERIC(12,2) NOT NULL DEFAULT 0.00 CHECK (sgst_amount >= 0),
    igst_amount NUMERIC(12,2) NOT NULL DEFAULT 0.00 CHECK (igst_amount >= 0),
    total_tax_amount NUMERIC(12,2) NOT NULL DEFAULT 0.00 CHECK (total_tax_amount >= 0),
    delivery_charge NUMERIC(12,2) NOT NULL DEFAULT 0.00 CHECK (delivery_charge >= 0),
    discount_amount NUMERIC(12,2) NOT NULL DEFAULT 0.00 CHECK (discount_amount >= 0),
    total_amount NUMERIC(12,2) NOT NULL CHECK (total_amount >= 0),
    currency VARCHAR(3) NOT NULL DEFAULT 'INR',
    issued_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_invoices_order ON invoices (order_id, invoice_type);
CREATE INDEX IF NOT EXISTS idx_invoices_customer ON invoices (customer_id, issued_at DESC);

-- 4. Invoice Line Items
CREATE TABLE IF NOT EXISTS invoice_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    invoice_id UUID NOT NULL REFERENCES invoices(id) ON DELETE CASCADE,
    product_id UUID NOT NULL,
    variant_id UUID NOT NULL,
    product_name VARCHAR(255) NOT NULL,
    sku VARCHAR(60) NOT NULL,
    hsn_code VARCHAR(20) NOT NULL,
    quantity INT NOT NULL CHECK (quantity > 0),
    unit_price NUMERIC(12,2) NOT NULL CHECK (unit_price >= 0),
    taxable_amount NUMERIC(12,2) NOT NULL CHECK (taxable_amount >= 0),
    gst_rate_percent NUMERIC(5,2) NOT NULL CHECK (gst_rate_percent >= 0),
    cgst_amount NUMERIC(12,2) NOT NULL DEFAULT 0.00,
    sgst_amount NUMERIC(12,2) NOT NULL DEFAULT 0.00,
    igst_amount NUMERIC(12,2) NOT NULL DEFAULT 0.00,
    total_amount NUMERIC(12,2) NOT NULL CHECK (total_amount >= 0)
);

CREATE INDEX IF NOT EXISTS idx_invoice_items_invoice ON invoice_items (invoice_id);

-- 5. Concurrency-Safe Invoice Sequences
CREATE TABLE IF NOT EXISTS invoice_sequences (
    fiscal_year VARCHAR(10) PRIMARY KEY,
    receipt_sequence BIGINT NOT NULL DEFAULT 0,
    tax_invoice_sequence BIGINT NOT NULL DEFAULT 0
);

-- 6. Transactional Outbox Events
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
