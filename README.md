# D & Z Fashion — Enterprise Ethnic Garments Platform

An enterprise-grade, luxury ethnic wear e-commerce and operational management platform engineered for Surat's premier apparel house. Built with Java 21 Spring Boot microservices, Spring Cloud Gateway MVC, Next.js 16.4 / React 19.2 storefront, and multi-schema PostgreSQL.

---

## Architecture & Technology Stack

- **Backend:** Java 21 LTS (Eclipse Temurin) & Spring Boot 4.1.2 multi-module architecture:
  - `api-gateway` (Port `8080`): Central Spring Cloud Gateway MVC with defense-in-depth CORS, correlation tracing, and path routing.
  - `identity-service` (Port `8081`): JWT issuer, role-based access control (Owner, Manager, Customer), OTP verification, BCrypt 12 hashing.
  - `catalog-service` (Port `8082`): Categories, products, variants, Cloudinary image assets, and JSONB revision audit workflows.
  - `inventory-service` (Port `8083`): Real-time stock reservation, low-stock alerts, generated available quantity columns.
  - `commerce-service` (Port `8084`): Cart, checkout, order orchestration, COD pincode validation, support ticketing, audit logging.
  - `payment-service` (Port `8085`): Simulated payments, percentage/fixed coupons, store credit ledger, GST tax calculation, COD settlement batches.
  - `fulfillment-service` (Port `8086`): Logistics dispatch, transport agency integration, strict 2-attempt delivery rules, and return inspections.
- **Frontend:** Next.js 16.4.6, React 19.2.0, TypeScript 5.9, Tailwind-based design system:
  - Luxury Editorial Storefront & Catalog Browsing (`/`, `/collections/*`, `/products/*`)
  - Customer Bag, Checkout, Order Tracking & Self-Service Returns (`/cart`, `/checkout`, `/account/*`)
  - Manager Operational Fulfillment & Warehouse Desks (`/manager/*`) — Enforcing **Manager Zero-Finance Invariant**
  - Owner Complete Governance, Financial Reporting & Pricing Approval (`/admin/*`)
- **Database:** PostgreSQL with service-owned schemas (`identity`, `catalog`, `inventory`, `commerce`, `payment`, `fulfillment`).
- **Containerization:** Multi-stage Alpine Dockerfiles with BuildKit layer caching and turnkey `docker-compose.yml`.

---

## Automated Test Suite & Quality Status

| Component | Automated Tests | Result | Status |
|---|---|---|---|
| **API Gateway** | 3 tests | **0 Failures, 0 Errors** | Passed |
| **Identity Service** | 52 tests | **0 Failures, 0 Errors** | Passed |
| **Catalog Service** | 44 tests | **0 Failures, 0 Errors** | Passed |
| **Inventory Service** | 37 tests | **0 Failures, 0 Errors** | Passed |
| **Commerce Service** | 73 tests | **0 Failures, 0 Errors** | Passed |
| **Payment & Credit Service** | 62 tests | **0 Failures, 0 Errors** | Passed |
| **Fulfillment & Shipping Service** | 80 tests | **0 Failures, 0 Errors** | Passed |
| **Frontend Storefront & Dashboards** | 72 tests (Vitest) | **0 Failures, 0 Errors** | Passed |
| **Frontend TypeScript Typecheck** | 44 Routes | **0 Type Errors** | Passed |
| **Frontend ESLint Audit** | Entire Codebase | **0 Lint Errors / Warnings** | Passed |
| **Total Automated Tests** | **423 Tests** | **100% Pass Rate** | **Production Ready** |

---

## Turnkey Quickstart with Docker Compose

To launch the full enterprise ecosystem (PostgreSQL, 7 Spring Boot microservices/gateway, and Next.js frontend):

```bash
# 1. Build and start all services
docker compose up --build

# 2. Access the application:
# Storefront & Admin Portal: http://localhost:3000
# API Gateway:                http://localhost:8080
# PostgreSQL:                 localhost:5432
```

### Pre-Seeded Demonstration Accounts

All demo accounts come pre-configured with password: `Password@123`

| Role | Username | Email | Permissions / Features |
|---|---|---|---|
| **Owner** | `owner_dz` | `owner@dzfashion.com` | Full governance, revenue reports, pricing changes, COD batch settlements, manager staff provisioning. |
| **Manager** | `manager_surat` | `manager@dzfashion.com` | Warehouse order fulfillment, picking & packing slips (strictly zero financial figures, phone numbers masked). |
| **Customer** | `customer_priya` | `priya.sharma@example.com` | Storefront browsing, bag checkout, COD / simulated payment, order history, store credit balance. |

---

## Local Development Workflow

### 1. Database & Schemas
If running against a standalone PostgreSQL or Supabase instance:
```bash
# Initialize schemas and seed demo Surat ethnic wear catalog data
psql -U postgres -d postgres -f scripts/seed/init-schemas.sql
psql -U postgres -d postgres -f scripts/seed/seed-demo-data.sql
```

### 2. Backend Services (Spring Boot)
```bash
# Run all 351 backend unit and integration tests:
cd backend
./mvnw clean test   # On Windows: mvnw.cmd clean test

# Run individual services:
./mvnw -pl modules/api-gateway spring-boot:run
./mvnw -pl modules/identity-service spring-boot:run
./mvnw -pl modules/catalog-service spring-boot:run
./mvnw -pl modules/inventory-service spring-boot:run
./mvnw -pl modules/commerce-service spring-boot:run
./mvnw -pl modules/payment-service spring-boot:run
./mvnw -pl modules/fulfillment-service spring-boot:run
```

### 3. Frontend (Next.js 16 / React 19)
```bash
cd frontend

# Install dependencies
npm ci

# Run Vitest test suite (72 tests)
npm test

# Run TypeScript typechecker
npm run typecheck

# Run ESLint
npm run lint

# Build standalone production bundle
npm run build

# Start production server
npm start
```

---

## Documentation Links

- [OPERATOR_GUIDE.md](file:///E:/dev/D-Z-Fashion/docs/OPERATOR_GUIDE.md): Operational workflows, runbooks, and disaster recovery.
- [DATABASE_OWNERSHIP_AND_SCHEMA_MAP.md](file:///E:/dev/D-Z-Fashion/docs/DATABASE_OWNERSHIP_AND_SCHEMA_MAP.md): Schema boundary specifications.
- [PERFORMANCE_AND_ACCESSIBILITY_REPORT.md](file:///E:/dev/D-Z-Fashion/docs/PERFORMANCE_AND_ACCESSIBILITY_REPORT.md): Audit benchmarks and Core Web Vitals.
- [Architecture Decision Records (ADRs)](file:///E:/dev/D-Z-Fashion/docs/adr/): Formal architecture history.

---

## Security & Operational Invariants

1. **Manager Zero-Finance Invariant:** Managers cannot view revenue, prices, promotional values, or unmasked customer phone numbers on picking slips, dispatch lists, or return inspection screens.
2. **Owner Exclusive Governance:** Only the `OWNER` role may approve price changes, publish product revisions, approve COD remittance batches, authorize refunds, or register manager accounts.
3. **Database-Level Invariants:** Flyway migrations enforce check constraints (`delivery_attempts BETWEEN 0 AND 2`, `available_qty = physical_qty - reserved_qty`, `NUMERIC(12,2)` precision).
4. **Idempotent Checkout & Settling:** Gateway and commerce services enforce UUID/string idempotency keys to eliminate double-charges or duplicate order creation.
