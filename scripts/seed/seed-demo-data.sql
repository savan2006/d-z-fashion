-- ============================================================================
-- D & Z Fashion — Turnkey Enterprise Demonstration & Seed Data
-- Region: Surat, Gujarat, India (Ethnic Garment Hub)
-- Idempotent script: Safe to run on blank or partially seeded databases
-- Default credentials for all demo accounts: Password@123
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 1. IDENTITY & ACCESS (identity schema)
-- ----------------------------------------------------------------------------
INSERT INTO identity.app_users (id, username, email, password_hash, role, status, created_at, updated_at)
VALUES
    ('11111111-1111-1111-1111-111111111111', 'owner_dz', 'owner@dzfashion.com', '$2a$12$jN4iLmporJz3xCA8ow1zSeDY7SdeYJowDJy8lZXdgWNzhBRtSUrai', 'OWNER', 'ACTIVE', NOW(), NOW()),
    ('22222222-2222-2222-2222-222222222222', 'manager_surat', 'manager@dzfashion.com', '$2a$12$jN4iLmporJz3xCA8ow1zSeDY7SdeYJowDJy8lZXdgWNzhBRtSUrai', 'MANAGER', 'ACTIVE', NOW(), NOW()),
    ('33333333-3333-3333-3333-333333333333', 'customer_priya', 'priya.sharma@example.com', '$2a$12$jN4iLmporJz3xCA8ow1zSeDY7SdeYJowDJy8lZXdgWNzhBRtSUrai', 'CUSTOMER', 'ACTIVE', NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

INSERT INTO identity.customer_profiles (id, full_name, phone_number, address_line1, address_line2, city, state, postal_code, country, created_at, updated_at)
VALUES
    ('33333333-3333-3333-3333-333333333333', 'Priya Sharma', '+919876543210', 'Flat 402, Royal Residency', 'Ring Road', 'Surat', 'Gujarat', '395002', 'India', NOW(), NOW())
ON CONFLICT (id) DO UPDATE SET
    full_name = EXCLUDED.full_name,
    phone_number = EXCLUDED.phone_number,
    city = EXCLUDED.city,
    postal_code = EXCLUDED.postal_code;

-- ----------------------------------------------------------------------------
-- 2. CATALOG & TAXONOMY (catalog schema)
-- ----------------------------------------------------------------------------
INSERT INTO catalog.categories (id, parent_id, name, slug, description, is_active, display_order, created_at, updated_at)
VALUES
    ('aaaaaaa1-1111-1111-1111-111111111111', NULL, 'Banarasi Sarees', 'banarasi-sarees', 'Authentic Banarasi woven silk sarees featuring handcrafted zari brocades.', true, 1, NOW(), NOW()),
    ('aaaaaaa2-2222-2222-2222-222222222222', NULL, 'Kanjeevaram Sarees', 'kanjeevaram-sarees', 'Pure silk bridal Kanjeevaram sarees woven with gold & silver temple motifs.', true, 2, NOW(), NOW()),
    ('aaaaaaa3-3333-3333-3333-333333333333', NULL, 'Georgette Lehengas', 'georgette-lehengas', 'Surat-crafted semi-stitched flared lehengas with delicate resham and mirror work.', true, 3, NOW(), NOW()),
    ('aaaaaaa4-4444-4444-4444-444444444444', NULL, 'Anarkali Kurtis', 'anarkali-kurtis', 'Floor-length Chanderi and pure cotton ethnic Anarkali kurtis with dupattas.', true, 4, NOW(), NOW()),
    ('aaaaaaa5-5555-5555-5555-555555555555', NULL, 'Girls Ethnic Wear', 'girls-ethnic-wear', 'Festive Bandhani flared frocks and lehengas tailored for young girls.', true, 5, NOW(), NOW())
ON CONFLICT (id) DO UPDATE SET
    name = EXCLUDED.name,
    slug = EXCLUDED.slug,
    is_active = true;

-- Products
INSERT INTO catalog.products (id, category_id, name, slug, description, fabric, occasion, status, default_price, created_by, approved_by, created_at, updated_at)
VALUES
    ('bbbbbbb1-1111-1111-1111-111111111111', 'aaaaaaa1-1111-1111-1111-111111111111', 'Royal Banarasi Silk Saree - Ruby Red', 'royal-banarasi-silk-saree-ruby-red', 'Rich crimson Banarasi silk saree adorned with intricate floral jaal weaving and gold zari border. Includes matching unstitched blouse piece.', 'Banarasi Silk', 'Bridal / Festive', 'PUBLISHED', 3499.00, '11111111-1111-1111-1111-111111111111', '11111111-1111-1111-1111-111111111111', NOW(), NOW()),
    ('bbbbbbb2-2222-2222-2222-222222222222', 'aaaaaaa2-2222-2222-2222-222222222222', 'Heritage Kanjeevaram Zari Saree - Royal Gold', 'heritage-kanjeevaram-zari-saree-royal-gold', 'Exquisite pure silk Kanjeevaram drape in radiant temple gold with rich contrast pallu.', 'Pure Silk & Zari', 'Wedding / Reception', 'PUBLISHED', 4999.00, '11111111-1111-1111-1111-111111111111', '11111111-1111-1111-1111-111111111111', NOW(), NOW()),
    ('bbbbbbb3-3333-3333-3333-333333333333', 'aaaaaaa3-3333-3333-3333-333333333333', 'Embroidered Georgette Semi-Stitched Lehenga', 'embroidered-georgette-semi-stitched-lehenga', 'Stunning blush pink georgette lehenga choli with mirror work embroidery and net can-can lining.', 'Georgette & Mirror Work', 'Sangeet / Festive', 'PUBLISHED', 2899.00, '11111111-1111-1111-1111-111111111111', '11111111-1111-1111-1111-111111111111', NOW(), NOW()),
    ('bbbbbbb4-4444-4444-4444-444444444444', 'aaaaaaa4-4444-4444-4444-444444444444', 'Surat Chanderi Anarkali Kurti Set', 'surat-chanderi-anarkali-kurti-set', 'Deep maroon breathable Chanderi silk Anarkali kurti paired with woven dupatta and churidar.', 'Chanderi Cotton', 'Daily / Festive', 'PUBLISHED', 1599.00, '11111111-1111-1111-1111-111111111111', '11111111-1111-1111-1111-111111111111', NOW(), NOW()),
    ('bbbbbbb5-5555-5555-5555-555555555555', 'aaaaaaa5-5555-5555-5555-555555555555', 'Girls Bandhani Flared Festive Lehenga', 'girls-bandhani-flared-festive-lehenga', 'Bright mustard yellow Bandhani print traditional flared lehenga choli for young girls.', 'Cotton Silk', 'Festive', 'PUBLISHED', 1199.00, '11111111-1111-1111-1111-111111111111', '11111111-1111-1111-1111-111111111111', NOW(), NOW())
ON CONFLICT (id) DO UPDATE SET
    status = 'PUBLISHED',
    default_price = EXCLUDED.default_price;

-- Product Variants
INSERT INTO catalog.product_variants (id, product_id, sku, size, colour, price_override, is_active, created_at, updated_at)
VALUES
    ('ccccccc1-1111-1111-1111-111111111111', 'bbbbbbb1-1111-1111-1111-111111111111', 'SRI-BAN-RED-FS', 'Free Size', 'Ruby Red', NULL, true, NOW(), NOW()),
    ('ccccccc1-2222-2222-2222-222222222222', 'bbbbbbb1-1111-1111-1111-111111111111', 'SRI-BAN-GRN-FS', 'Free Size', 'Emerald Green', 3699.00, true, NOW(), NOW()),
    ('ccccccc2-1111-1111-1111-111111111111', 'bbbbbbb2-2222-2222-2222-222222222222', 'SRI-KAN-GLD-FS', 'Free Size', 'Royal Gold', NULL, true, NOW(), NOW()),
    ('ccccccc3-1111-1111-1111-111111111111', 'bbbbbbb3-3333-3333-3333-333333333333', 'LHG-GEO-PNK-M', 'M', 'Blush Pink', NULL, true, NOW(), NOW()),
    ('ccccccc3-2222-2222-2222-222222222222', 'bbbbbbb3-3333-3333-3333-333333333333', 'LHG-GEO-PNK-L', 'L', 'Blush Pink', 3099.00, true, NOW(), NOW()),
    ('ccccccc4-1111-1111-1111-111111111111', 'bbbbbbb4-4444-4444-4444-444444444444', 'KRT-CHN-MRN-M', 'M', 'Maroon', NULL, true, NOW(), NOW()),
    ('ccccccc4-2222-2222-2222-222222222222', 'bbbbbbb4-4444-4444-4444-444444444444', 'KRT-CHN-MRN-L', 'L', 'Maroon', NULL, true, NOW(), NOW()),
    ('ccccccc5-1111-1111-1111-111111111111', 'bbbbbbb5-5555-5555-5555-555555555555', 'GRL-BDH-YLW-28', '28', 'Mustard Yellow', NULL, true, NOW(), NOW())
ON CONFLICT (id) DO UPDATE SET
    is_active = true,
    price_override = EXCLUDED.price_override;

-- Media Assets
INSERT INTO catalog.media_assets (id, product_id, variant_id, cloudinary_public_id, secure_url, format, width, height, alt_text, display_order, is_primary, created_at)
VALUES
    ('eeeeeee1-1111-1111-1111-111111111111', 'bbbbbbb1-1111-1111-1111-111111111111', 'ccccccc1-1111-1111-1111-111111111111', 'dzfashion/banarasi_red_01', 'https://res.cloudinary.com/dzfashion/image/upload/v1/catalog/banarasi_red.webp', 'webp', 1200, 1600, 'Royal Banarasi Silk Saree Ruby Red front drape', 1, true, NOW()),
    ('eeeeeee2-2222-2222-2222-222222222222', 'bbbbbbb2-2222-2222-2222-222222222222', 'ccccccc2-1111-1111-1111-111111111111', 'dzfashion/kanjeevaram_gold_01', 'https://res.cloudinary.com/dzfashion/image/upload/v1/catalog/kanjeevaram_gold.webp', 'webp', 1200, 1600, 'Heritage Kanjeevaram Zari Saree Royal Gold pallu', 1, true, NOW()),
    ('eeeeeee3-3333-3333-3333-333333333333', 'bbbbbbb3-3333-3333-3333-333333333333', 'ccccccc3-1111-1111-1111-111111111111', 'dzfashion/lehenga_pink_01', 'https://res.cloudinary.com/dzfashion/image/upload/v1/catalog/lehenga_pink.webp', 'webp', 1200, 1600, 'Embroidered Georgette Semi-Stitched Lehenga in Blush Pink', 1, true, NOW()),
    ('eeeeeee4-4444-4444-4444-444444444444', 'bbbbbbb4-4444-4444-4444-444444444444', 'ccccccc4-1111-1111-1111-111111111111', 'dzfashion/kurti_maroon_01', 'https://res.cloudinary.com/dzfashion/image/upload/v1/catalog/kurti_maroon.webp', 'webp', 1200, 1600, 'Surat Chanderi Anarkali Kurti Set Maroon full profile', 1, true, NOW()),
    ('eeeeeee5-5555-5555-5555-555555555555', 'bbbbbbb5-5555-5555-5555-555555555555', 'ccccccc5-1111-1111-1111-111111111111', 'dzfashion/girls_lehenga_01', 'https://res.cloudinary.com/dzfashion/image/upload/v1/catalog/girls_lehenga.webp', 'webp', 1200, 1600, 'Girls Bandhani Flared Festive Lehenga Mustard Yellow', 1, true, NOW())
ON CONFLICT (id) DO NOTHING;

-- ----------------------------------------------------------------------------
-- 3. INVENTORY & WAREHOUSE STOCK (inventory schema)
-- Note: available_qty is a GENERATED ALWAYS column (physical_qty - reserved_qty)
-- ----------------------------------------------------------------------------
INSERT INTO inventory.warehouse_stock (id, variant_id, sku, physical_qty, reserved_qty, low_stock_threshold, updated_at)
VALUES
    ('fffffff1-1111-1111-1111-111111111111', 'ccccccc1-1111-1111-1111-111111111111', 'SRI-BAN-RED-FS', 35, 0, 5, NOW()),
    ('fffffff1-2222-2222-2222-222222222222', 'ccccccc1-2222-2222-2222-222222222222', 'SRI-BAN-GRN-FS', 20, 0, 5, NOW()),
    ('fffffff2-1111-1111-1111-111111111111', 'ccccccc2-1111-1111-1111-111111111111', 'SRI-KAN-GLD-FS', 15, 0, 5, NOW()),
    ('fffffff3-1111-1111-1111-111111111111', 'ccccccc3-1111-1111-1111-111111111111', 'LHG-GEO-PNK-M', 28, 0, 5, NOW()),
    ('fffffff3-2222-2222-2222-222222222222', 'ccccccc3-2222-2222-2222-222222222222', 'LHG-GEO-PNK-L', 22, 0, 5, NOW()),
    ('fffffff4-1111-1111-1111-111111111111', 'ccccccc4-1111-1111-1111-111111111111', 'KRT-CHN-MRN-M', 45, 0, 8, NOW()),
    ('fffffff4-2222-2222-2222-222222222222', 'ccccccc4-2222-2222-2222-222222222222', 'KRT-CHN-MRN-L', 40, 0, 8, NOW()),
    ('fffffff5-1111-1111-1111-111111111111', 'ccccccc5-1111-1111-1111-111111111111', 'GRL-BDH-YLW-28', 30, 0, 5, NOW())
ON CONFLICT (variant_id) DO UPDATE SET
    physical_qty = EXCLUDED.physical_qty,
    reserved_qty = 0,
    low_stock_threshold = EXCLUDED.low_stock_threshold;

-- Initial Stock Movement Audit Record
INSERT INTO inventory.stock_movements (id, variant_id, change_qty, balance_after, reason, reference_type, reference_id, actor_id, created_at)
VALUES
    (gen_random_uuid(), 'ccccccc1-1111-1111-1111-111111111111', 35, 35, 'NEW_MANUFACTURE', 'INITIAL_INVENTORY', 'BATCH-SRT-2026-01', '11111111-1111-1111-1111-111111111111', NOW()),
    (gen_random_uuid(), 'ccccccc2-1111-1111-1111-111111111111', 15, 15, 'NEW_MANUFACTURE', 'INITIAL_INVENTORY', 'BATCH-SRT-2026-02', '11111111-1111-1111-1111-111111111111', NOW())
ON CONFLICT DO NOTHING;

-- ----------------------------------------------------------------------------
-- 4. COMMERCE & COD PINCODES (commerce schema)
-- ----------------------------------------------------------------------------
INSERT INTO commerce.cod_serviceable_pincodes (id, pincode, is_active, enabled_by, created_at, updated_at)
VALUES
    (gen_random_uuid(), '395002', true, '11111111-1111-1111-1111-111111111111', NOW(), NOW()), -- Surat Textile Market / Ring Road
    (gen_random_uuid(), '395007', true, '11111111-1111-1111-1111-111111111111', NOW(), NOW()), -- Surat Vesu
    (gen_random_uuid(), '395003', true, '11111111-1111-1111-1111-111111111111', NOW(), NOW()), -- Surat Varachha
    (gen_random_uuid(), '380001', true, '11111111-1111-1111-1111-111111111111', NOW(), NOW()), -- Ahmedabad
    (gen_random_uuid(), '400001', true, '11111111-1111-1111-1111-111111111111', NOW(), NOW()), -- Mumbai South
    (gen_random_uuid(), '110001', true, '11111111-1111-1111-1111-111111111111', NOW(), NOW()), -- New Delhi
    (gen_random_uuid(), '560001', true, '11111111-1111-1111-1111-111111111111', NOW(), NOW())  -- Bengaluru
ON CONFLICT (pincode) DO UPDATE SET
    is_active = true;

-- ----------------------------------------------------------------------------
-- 5. PAYMENT, DISCOUNTS & STORE CREDIT (payment schema)
-- ----------------------------------------------------------------------------
INSERT INTO payment.promotions (id, code, name, type, discount_value, min_order_subtotal, max_discount_amount, starts_at, ends_at, usage_limit, usage_count, is_active, created_by, created_at, updated_at)
VALUES
    (gen_random_uuid(), 'FESTIVE10', 'Surat Festive 10% Off', 'PERCENTAGE_COUPON', 10.00, 1500.00, 500.00, NOW() - INTERVAL '1 day', NOW() + INTERVAL '90 days', 500, 0, true, '11111111-1111-1111-1111-111111111111', NOW(), NOW()),
    (gen_random_uuid(), 'SURATFIRST', 'First Order Special Discount', 'FIXED_COUPON', 200.00, 1000.00, NULL, NOW() - INTERVAL '1 day', NOW() + INTERVAL '90 days', 1000, 0, true, '11111111-1111-1111-1111-111111111111', NOW(), NOW()),
    (gen_random_uuid(), 'BRIDAL500', 'Bridal Collection Discount', 'FIXED_COUPON', 500.00, 3000.00, NULL, NOW() - INTERVAL '1 day', NOW() + INTERVAL '90 days', 200, 0, true, '11111111-1111-1111-1111-111111111111', NOW(), NOW())
ON CONFLICT (code) DO UPDATE SET
    is_active = true,
    discount_value = EXCLUDED.discount_value,
    min_order_subtotal = EXCLUDED.min_order_subtotal;

-- Welcome Store Credit for Demo Customer
INSERT INTO payment.store_credit_ledgers (id, customer_id, amount, balance_after, entry_type, reference_type, reference_id, notes, created_at)
VALUES
    (gen_random_uuid(), '33333333-3333-3333-3333-333333333333', 150.00, 150.00, 'CREDIT_ISSUED', 'MANUAL_ADJUSTMENT', 'WELCOME-CREDIT-2026', 'Welcome to D & Z Fashion Store Credit Gift', NOW())
ON CONFLICT DO NOTHING;

-- ----------------------------------------------------------------------------
-- 6. FULFILLMENT & LOGISTICS (fulfillment schema)
-- ----------------------------------------------------------------------------
INSERT INTO fulfillment.transport_agencies (id, name, contact_person, phone_number, is_active, created_at, updated_at)
VALUES
    ('ddddddd1-1111-1111-1111-111111111111', 'Blue Dart Express (Surat Hub)', 'Rajesh Patel', '+912612345678', true, NOW(), NOW()),
    ('ddddddd2-2222-2222-2222-222222222222', 'Delhivery Surface Logistics', 'Vikram Desai', '+912618765432', true, NOW(), NOW()),
    ('ddddddd3-3333-3333-3333-333333333333', 'DTDC Surat City Express', 'Mahesh Shah', '+912619876543', true, NOW(), NOW())
ON CONFLICT (name) DO UPDATE SET
    phone_number = EXCLUDED.phone_number,
    is_active = true;

-- ============================================================================
-- End of Seed Script
-- ============================================================================
