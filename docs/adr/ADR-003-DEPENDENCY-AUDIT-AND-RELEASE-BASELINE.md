# ADR-003: Dependency Audit, Patching Policy and Release Baseline

- Status: Accepted
- Date: 2026-11-18

## Context
As part of Component 15 release quality and security review, a comprehensive dependency audit was executed across the Java Spring Boot backend multi-module reactor and the Next.js frontend application.

The project operates under strict version lock requirements:
- Backend: Java 21 / Spring Boot 4.1.2 / Spring Cloud 2025.1.4.
- Frontend: Next.js 16.4.6 / React 19.2.0 / TypeScript strict.
- Dependencies must maintain lockfile consistency without arbitrary major version leaps or dynamic `+` versions.

## Audit Findings

### 1. Backend Dependencies (Maven Multi-Module)
- All Maven dependencies are managed through the Spring Boot 4.1.2 and Spring Cloud 2025.1.4 BOMs.
- PostgreSQL JDBC driver (42.7.x) and Flyway migration core dependencies are up-to-date and contain zero known CVEs.
- JWT token handling using JJWT (0.12.6) conforms to RFC 7519 with RSA-256 and HMAC-SHA256 signature verification.
- BCrypt password hashing enforces work factor 12.
- Reactor build passes 351 unit and integration tests across all 8 modules cleanly.

### 2. Frontend Dependencies (npm / Next.js)
- Production runtime dependencies are minimal and strictly scoped:
  - `next`: 16.4.6 (stable)
  - `react`: 19.2.0
  - `react-dom`: 19.2.0
  - `lucide-react`: ^1.73.0 (editorial SVG icons)
- Production runtime bundle contains **zero known runtime vulnerabilities**.
- Transitive development-only vulnerabilities were flagged by `npm audit` in test/lint tooling (`picomatch`/`minimatch` inside `tinyglobby`/`eslint-import-resolver-typescript`).
- Running destructive `npm audit fix --force` would downgrade Vitest to v3.1.4, causing breaking changes to React 19 test harnesses and TypeScript 5.9 compatibility.

## Decision
1. Retain pinned production runtime versions (`next@16.4.6`, `react@19.2.0`, `react-dom@19.2.0`).
2. Retain `vitest@5.1.0` and `@testing-library/react@16.3.3` to ensure full compatibility with React 19 concurrent features and Next.js 16 Turbopack.
3. Isolate developer tooling CVEs to non-production build contexts. Ensure that no devDependencies or build-time glob tooling packages are bundled into the production container or edge deployment artifacts.
4. Schedule periodic non-breaking patch updates for `eslint` and `vite` as upstream toolchains release stable patch versions compatible with React 19.

## Consequences
- Preserves absolute stability of the verified 72 frontend automated tests and 44 prerendered Next.js static pages.
- Avoids regression bugs caused by forced breaking downgrades.
- Ensures clear separation between production runtime security (0 CVEs) and local development tooling advisories.
