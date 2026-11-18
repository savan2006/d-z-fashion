# D & Z Fashion — Operator & Deployment Guide

This guide describes local setup, environment configuration, service ports, test accounts, migration procedures, and operational rules for operating the D & Z Fashion platform.

---

## 1. System Overview & Architecture

D & Z Fashion is an enterprise-grade luxury Indian ethnic wear platform with a distributed backend and a Next.js frontend:

```
[ Next.js 16.4.6 Frontend ] (Port 3000)
             │
             ▼
   [ API Gateway Service ] (Port 8080) ─── X-Correlation-ID Tracing
             │
   ┌─────────┼─────────┬─────────┬─────────┬─────────┬─────────┐
   ▼         ▼         ▼         ▼         ▼         ▼         ▼
Identity  Catalog  Inventory Commerce Payment Fulfillment (Eureka /
(8081)    (8082)   (8083)    (8084)   (8085)  (8086)      Direct)
   │         │         │         │         │         │
   └─────────┴─────────┴────┬────┴─────────┴─────────┘
                            ▼
           [ Supabase PostgreSQL 15+ Database ]
    (Schemas: identity, catalog, inventory, commerce, payment, fulfillment)
```

---

## 2. Port Allocations & Health Endpoints

| Service Name | Port | Base Path | Health Check Probe |
|---|---|---|---|
| **API Gateway** | `8080` | `/` | `http://localhost:8080/actuator/health` |
| **Identity Service** | `8081` | `/api/v1/auth`, `/api/v1/users` | `http://localhost:8081/actuator/health` |
| **Catalog Service** | `8082` | `/api/v1/catalog`, `/api/v1/categories`, `/api/v1/products` | `http://localhost:8082/actuator/health` |
| **Inventory Service** | `8083` | `/api/v1/inventory`, `/api/v1/stock` | `http://localhost:8083/actuator/health` |
| **Commerce Service** | `8084` | `/api/v1/cart`, `/api/v1/orders`, `/api/v1/checkout` | `http://localhost:8084/actuator/health` |
| **Payment Service** | `8085` | `/api/v1/payments`, `/api/v1/store-credit`, `/api/v1/cod-settlements` | `http://localhost:8085/actuator/health` |
| **Fulfillment Service** | `8086` | `/api/v1/shipments`, `/api/v1/returns`, `/api/v1/manifests` | `http://localhost:8086/actuator/health` |
| **Frontend Web App** | `3000` | `/`, `/admin/*`, `/manager/*` | `http://localhost:3000` |

---

## 3. Environment Variables & Configuration

Copy `.env.example` to `.env` in the project root:

```bash
cp .env.example .env
```

### Essential Environment Variables

```ini
# Supabase PostgreSQL Database (Managed Relational Engine)
SUPABASE_JDBC_URL=jdbc:postgresql://<SUPABASE_HOST>:5432/postgres?sslmode=require
SUPABASE_DB_USER=postgres
SUPABASE_DB_PASSWORD=<SECURE_DATABASE_PASSWORD>

# JWT Authentication (RSA / HMAC 256-bit)
JWT_SECRET_KEY=<SECURE_256_BIT_SECRET_KEY_MIN_32_CHARS>
JWT_ACCESS_EXPIRATION_SECONDS=900
JWT_REFRESH_EXPIRATION_SECONDS=604800

# Cloudinary Media Storage (Backend Uploads & Transformations)
CLOUDINARY_CLOUD_NAME=<YOUR_CLOUD_NAME>
CLOUDINARY_API_KEY=<YOUR_API_KEY>
CLOUDINARY_API_SECRET=<YOUR_API_SECRET>

# Frontend Gateway Connection
NEXT_PUBLIC_API_BASE_URL=http://localhost:8080
```

> [!IMPORTANT]
> The `.env` file and `.ai-workspace` folder are excluded by `.gitignore` and must **never** be committed to version control.

---

## 4. Database Setup & Schema Migrations

The system utilizes service-owned PostgreSQL schemas with automated Flyway migrations:

1. **Verify Database Connectivity:**
   Ensure PostgreSQL 15+ or Supabase project is reachable on port 5432.
2. **Execute Schema Creation (One-time setup):**
   ```sql
   CREATE SCHEMA IF NOT EXISTS identity;
   CREATE SCHEMA IF NOT EXISTS catalog;
   CREATE SCHEMA IF NOT EXISTS inventory;
   CREATE SCHEMA IF NOT EXISTS commerce;
   CREATE SCHEMA IF NOT EXISTS payment;
   CREATE SCHEMA IF NOT EXISTS fulfillment;
   ```
3. **Run Flyway Migrations:**
   Each service executes its migrations on startup (`db/migration/V1__*.sql`).
   To migrate all schemas manually:
   ```bash
   cd backend
   ./mvnw compile flyway:migrate
   ```

---

## 5. Local Run Instructions

### Prerequisites
- Java 21 (Microsoft OpenJDK 21 or Eclipse Temurin 21)
- Node.js 20.9.0+ and npm 10+
- Git

### Step 1: Start Backend Services
Run all modules via Maven or start each service independently in IntelliJ IDEA / terminal:

```bash
# In backend root
cd backend
./mvnw clean install -DskipTests
```

To run individual services:
```bash
./mvnw -pl modules/api-gateway spring-boot:run
./mvnw -pl modules/identity-access-service spring-boot:run
./mvnw -pl modules/catalog-service spring-boot:run
./mvnw -pl modules/inventory-service spring-boot:run
./mvnw -pl modules/commerce-service spring-boot:run
./mvnw -pl modules/payment-promotions-tax-service spring-boot:run
./mvnw -pl modules/fulfillment-transport-service spring-boot:run
```

### Step 2: Start Frontend Application
```bash
cd frontend
npm install
npm run dev
```
The storefront will be accessible at: `http://localhost:3000`.

---

## 6. Seed Accounts & Role Credentials

For local testing and verification, seed the following test accounts:

| Role | Username | Email | Default Access Scope |
|---|---|---|---|
| **Owner** | `owner_dz` | `owner@dzfashion.com` | Full financial analytics, catalog pricing, promotion creation, COD settlement approval, refund authorization, audit logs. |
| **Manager** | `manager_surat` | `manager@dzfashion.com` | Surat warehouse queue, atomic order claim, picking list, dispatch manifests, delivery attempt logger, return physical inspection. **Zero financial visibility.** |
| **Customer** | `customer_priya` | `customer@example.com` | Public catalog browsing, cart, authoritative checkout, single saved address, order tracking, cancellation, quality returns. |

---

## 7. Key Operational Invariants & Policies

1. **Manager Zero-Finance Invariant:**
   Warehouse Operations Managers are strictly prohibited from viewing product prices, gross merchandise value, net settled revenue, or profit numbers in any `/manager/*` route or API response.
2. **Owner Retail Pricing Governance:**
   Product prices cannot be adjusted without entering a mandatory audit rationale. Active customer bags detect price shifts and enforce acknowledgment before checkout.
3. **Free Delivery Threshold:**
   Orders above **₹1,999** qualify for free delivery across India. Orders below the threshold incur a flat **₹150** transport delivery charge.
4. **COD Courier Cash Reconciliation:**
   Courier remittance sheets prepared by managers remain in `SUBMITTED_FOR_APPROVAL` until the Store Owner cryptographically authorizes reconciliation.
5. **Max 2 Delivery Attempts Rule:**
   Couriers attempt delivery a maximum of 2 times. If the 2nd attempt fails, the parcel is automatically converted to RTO return intake to the Surat warehouse.
6. **3-Day Quality Return Policy & 7-Day Refund SLA:**
   Customers can request return/exchange within 3 calendar days of delivery with mandatory defect photos. Once physical inspection passes at Surat, Owner approved refunds must settle within 7 days.

---

## 8. Verification & Test Commands

To verify system integrity at any time:

```bash
# Run 351 backend unit & integration tests across all 8 reactor modules:
cd backend
./mvnw test

# Run 72 frontend unit, integration & E2E tests:
cd frontend
npm test

# Run TypeScript compilation and ESLint static analysis:
npm run typecheck
npm run lint

# Run Next.js production build:
npm run build
```
