# Database Ownership and Schema Map

This document defines the schema architecture, table ownership, and data dictionary for D & Z Fashion on Supabase PostgreSQL.

## 1. Schema Ownership Matrix

| Schema | Service Module | Owned Tables | Description |
|---|---|---|---|
| `identity` | `identity-service` | `app_users`, `customer_profiles`, `email_otps` | User authentication, roles, saved single address snapshot, OTP delivery state |
| `catalog` | `catalog-service` | `categories`, `products`, `product_variants`, `product_revisions`, `media_assets` | Dynamic categories, ready-stock garment definitions, SKU attributes, Cloudinary metadata |
| `inventory` | `inventory-service` | `warehouse_stock`, `stock_reservations`, `stock_movements` | Surat warehouse inventory, atomic checkout reservations, append-only stock movement log |
| `commerce` | `commerce-service` | `carts`, `cart_items`, `orders`, `order_items`, `cod_serviceable_pincodes` | Customer carts, confirmed orders, immutable financial snapshots, COD coverage |
| `payment` | `payment-service` | `payments`, `store_credit_ledgers`, `promotions`, `cod_settlement_batches` | Simulated online transactions, append-only store credit ledger, Owner offers, COD reconciliation |
| `fulfillment` | `fulfillment-service` | `transport_agencies`, `shipments`, `daily_dispatch_batches`, `return_inspections` | External logistics agencies, manual tracking updates, warehouse dispatch batches, physical inspection results |

---

## 2. Cross-Service Reference Guidelines
- Cross-schema foreign keys (e.g. `commerce.orders` referencing `identity.app_users`) are **strictly prohibited**.
- Cross-boundary references are stored as opaque `UUID` columns (such as `customer_id`, `order_id`, `variant_id`).
- Foreign keys are applied strictly **within** the same schema (e.g. `product_variants.product_id` -> `products.id`).

---

## 3. Data Dictionary Template & Invariant Constraints

### `inventory.warehouse_stock`
- `physical_qty`: Total stock physically located in the Surat warehouse (`>= 0`).
- `reserved_qty`: Stock locked by in-flight/confirmed unfulfilled orders (`>= 0`).
- `available_qty`: Automatically computed generated column (`physical_qty - reserved_qty`). Must never be negative (`CHECK (available_qty >= 0)`). Prevents concurrent overselling.

### `commerce.orders`
- `subtotal_before_discount_and_tax`: Product gross amount before any discounts and GST. Used for the free shipping threshold (`> 2000`).
- `promotional_discount_amount`: Value of the single active promotion applied.
- `delivery_charge`: Explicit delivery fee (`0.00` if subtotal > 2000, else `110.00 * physical_units`).
- `store_credit_used`: Tender amount deducted from customer's store credit balance.
- `final_payable_amount`: Net amount payable by customer (Online or COD).

### `payment.store_credit_ledgers`
- Immutable append-only audit trail. Never directly overwrite user credit balance without recording an entry with transaction reference, delta amount, and resulting `balance_after`.
