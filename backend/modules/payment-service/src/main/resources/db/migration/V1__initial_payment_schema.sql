-- Payment & Credit Service - Initial PostgreSQL Schema
-- Schema ownership: payment

CREATE TABLE IF NOT EXISTS payments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id UUID NOT NULL,
    amount NUMERIC(12,2) NOT NULL CHECK (amount >= 0),
    currency VARCHAR(3) NOT NULL DEFAULT 'INR',
    method VARCHAR(20) NOT NULL CHECK (method IN ('SIMULATED_ONLINE', 'COD')),
    status VARCHAR(20) NOT NULL CHECK (status IN ('SUCCESS', 'PENDING', 'FAILED', 'VOIDED', 'REVERSED')),
    transaction_reference VARCHAR(100) UNIQUE,
    is_simulated BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_payments_order ON payments (order_id);
CREATE INDEX IF NOT EXISTS idx_payments_status ON payments (status);

CREATE TABLE IF NOT EXISTS store_credit_ledgers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_id UUID NOT NULL,
    amount NUMERIC(12,2) NOT NULL,
    balance_after NUMERIC(12,2) NOT NULL CHECK (balance_after >= 0),
    entry_type VARCHAR(30) NOT NULL CHECK (entry_type IN ('CREDIT_ISSUED', 'CREDIT_USED', 'MANUAL_ADJUSTMENT', 'ORDER_REFUND')),
    reference_type VARCHAR(30) NOT NULL CHECK (reference_type IN ('ORDER', 'RETURN_EXCHANGE', 'MANUAL_ADJUSTMENT')),
    reference_id VARCHAR(100) NOT NULL,
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_store_credit_customer ON store_credit_ledgers (customer_id, created_at DESC);

CREATE TABLE IF NOT EXISTS promotions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(50) UNIQUE,
    name VARCHAR(100) NOT NULL,
    type VARCHAR(30) NOT NULL CHECK (type IN ('PERCENTAGE_COUPON', 'FIXED_COUPON', 'CATEGORY_DISCOUNT', 'PRODUCT_DISCOUNT', 'AUTOMATIC_CAMPAIGN')),
    discount_value NUMERIC(12,2) NOT NULL CHECK (discount_value > 0),
    min_order_subtotal NUMERIC(12,2) NOT NULL DEFAULT 0.00 CHECK (min_order_subtotal >= 0),
    max_discount_amount NUMERIC(12,2) CHECK (max_discount_amount IS NULL OR max_discount_amount > 0),
    starts_at TIMESTAMPTZ NOT NULL,
    ends_at TIMESTAMPTZ NOT NULL,
    usage_limit INT CHECK (usage_limit IS NULL OR usage_limit > 0),
    usage_count INT NOT NULL DEFAULT 0 CHECK (usage_count >= 0),
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_by UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_promotions_code ON promotions (code);
CREATE INDEX IF NOT EXISTS idx_promotions_active ON promotions (is_active, starts_at, ends_at);

CREATE TABLE IF NOT EXISTS cod_settlement_batches (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    batch_reference VARCHAR(64) NOT NULL UNIQUE,
    transport_agency_name VARCHAR(100) NOT NULL,
    settlement_date DATE NOT NULL,
    total_collected_amount NUMERIC(12,2) NOT NULL CHECK (total_collected_amount >= 0),
    remitted_amount NUMERIC(12,2) NOT NULL CHECK (remitted_amount >= 0),
    discrepancy_amount NUMERIC(12,2) NOT NULL DEFAULT 0.00,
    status VARCHAR(20) NOT NULL DEFAULT 'SUBMITTED' CHECK (status IN ('SUBMITTED', 'APPROVED', 'DISCREPANCY_FLAGGED')),
    prepared_by UUID NOT NULL,
    approved_by UUID,
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    approved_at TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_cod_settlement_status ON cod_settlement_batches (status);
