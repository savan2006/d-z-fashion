# Backend Maven Reactor

The parent build is `backend/pom.xml`. All service modules inherit Spring Boot 4.1.2 and Java 21. Spring-managed dependency versions are intentionally omitted from child POMs. The Spring Cloud BOM is centrally pinned to 2025.1.4, which Spring announced as compatible with Boot 4.1.2 on 23 Oct 2026.

The module layout is a starting architecture. The IntelliJ AI must implement each module only when its numbered component is reached. Do not create duplicated entities or share JPA entities across services.

Each deployable service owns its own schema and Flyway migrations within the single Supabase PostgreSQL project. Database roles/credentials should be least-privilege where supported. Do not let one service write another service's tables.

For a local all-modules compile: `./mvnw -DskipTests package` (PowerShell: `mvnw.cmd -DskipTests package`). Use Java 21.
