-- Commerce Service - Initial PostgreSQL Schema
-- Schema ownership: commerce

CREATE TABLE IF NOT EXISTS carts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_id UUID NOT NULL UNIQUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS cart_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    cart_id UUID NOT NULL REFERENCES carts(id) ON DELETE CASCADE,
    product_id UUID NOT NULL,
    variant_id UUID NOT NULL,
    quantity INT NOT NULL CHECK (quantity > 0),
    price_snapshot NUMERIC(12,2) NOT NULL CHECK (price_snapshot >= 0),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_cart_variant UNIQUE (cart_id, variant_id)
);

CREATE INDEX IF NOT EXISTS idx_cart_items_cart ON cart_items (cart_id);

CREATE TABLE IF NOT EXISTS orders (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_number VARCHAR(32) NOT NULL UNIQUE,
    customer_id UUID NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'CONFIRMED' CHECK (status IN ('CONFIRMED', 'PROCESSING', 'SHIPPED', 'DELIVERED', 'CANCELLED')),
    payment_method VARCHAR(20) NOT NULL CHECK (payment_method IN ('SIMULATED_ONLINE', 'COD')),
    payment_status VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (payment_status IN ('PENDING', 'PAID', 'FAILED', 'REFUNDED')),
    claimed_by UUID,
    claimed_at TIMESTAMPTZ,
    subtotal_before_discount_and_tax NUMERIC(12,2) NOT NULL CHECK (subtotal_before_discount_and_tax >= 0),
    promotional_discount_amount NUMERIC(12,2) NOT NULL DEFAULT 0.00 CHECK (promotional_discount_amount >= 0),
    applied_promotion_id UUID,
    gst_amount NUMERIC(12,2) NOT NULL DEFAULT 0.00 CHECK (gst_amount >= 0),
    delivery_charge NUMERIC(12,2) NOT NULL DEFAULT 0.00 CHECK (delivery_charge >= 0),
    store_credit_used NUMERIC(12,2) NOT NULL DEFAULT 0.00 CHECK (store_credit_used >= 0),
    final_payable_amount NUMERIC(12,2) NOT NULL CHECK (final_payable_amount >= 0),
    recipient_name VARCHAR(100) NOT NULL,
    recipient_phone VARCHAR(20) NOT NULL,
    shipping_address_line1 VARCHAR(255) NOT NULL,
    shipping_address_line2 VARCHAR(255),
    shipping_city VARCHAR(100) NOT NULL,
    shipping_state VARCHAR(100) NOT NULL,
    shipping_postal_code VARCHAR(20) NOT NULL,
    shipping_country VARCHAR(50) NOT NULL DEFAULT 'India',
    confirmed_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    dispatched_at TIMESTAMPTZ,
    delivered_at TIMESTAMPTZ,
    cancelled_at TIMESTAMPTZ,
    cancellation_reason TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_orders_customer ON orders (customer_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_orders_status ON orders (status);
CREATE INDEX IF NOT EXISTS idx_orders_claimed ON orders (claimed_by);
CREATE INDEX IF NOT EXISTS idx_orders_confirmed ON orders (confirmed_at DESC);

CREATE TABLE IF NOT EXISTS order_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id UUID NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    product_id UUID NOT NULL,
    variant_id UUID NOT NULL,
    product_name VARCHAR(255) NOT NULL,
    sku VARCHAR(60) NOT NULL,
    size VARCHAR(20) NOT NULL,
    colour VARCHAR(50) NOT NULL,
    quantity INT NOT NULL CHECK (quantity > 0),
    unit_price NUMERIC(12,2) NOT NULL CHECK (unit_price >= 0),
    total_price NUMERIC(12,2) NOT NULL CHECK (total_price >= 0)
);

CREATE INDEX IF NOT EXISTS idx_order_items_order ON order_items (order_id);

CREATE TABLE IF NOT EXISTS cod_serviceable_pincodes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    pincode VARCHAR(10) NOT NULL UNIQUE,
    is_active BOOLEAN NOT NULL DEFAULT true,
    enabled_by UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_cod_pincodes_active ON cod_serviceable_pincodes (is_active);
