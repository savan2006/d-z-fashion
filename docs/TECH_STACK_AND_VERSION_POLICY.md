# Technology and version policy

## Locked by owner

- Java 21, preferably the installed Microsoft Build of OpenJDK 21 (MS-21 JDK).
- Spring Boot 4.1.2.
- Maven wrapper from the owner's Spring Initializr reference (Maven 3.10.0 distribution).
- Next.js frontend.
- Supabase PostgreSQL for relational data.
- Cloudinary for image/media storage.
- Simulated online payments only in the initial release.

## Verified compatible supporting versions as of 15 Nov 2026

- Spring Cloud 2025.1.4 is officially announced as supporting Spring Boot 4.1.2.
- Next.js 16.4.6 is the latest stable release found at preparation time; do not select a canary/pre-release.
- React 19.2.0 is pinned in the initial frontend package manifest.

Before changing a major/minor version, the AI must check the official compatibility matrix and changelog, record the reason in an ADR, keep the dependency lockfile consistent, and run all tests. Avoid SNAPSHOT, M1, RC, canary and dynamic '+' versions.

Spring Boot's dependency management should manage Spring Boot ecosystem dependency versions. Do not add explicit versions to Spring Boot-managed starters, Spring Data, Hibernate, Jackson, PostgreSQL driver, Flyway, test starters or Micrometer unless the official dependency management does not manage a required library and the exception is recorded. The Spring Cloud BOM is imported once in the backend parent POM.

Sources:
- Spring Boot 4.1.2 release announcement: https://spring.io/blog/2026/10/23/spring-boot-4-1-2-available-now/
- Spring Cloud 2025.1.4 release announcement and Boot 4.1.2 compatibility: https://spring.io/blog/2026/10/23/spring-cloud-2025-1-4-has-been-released/
- Next.js stable release listing: https://next-js-release.vercel.app/
- Next.js support policy: https://nextjs.org/support-policy
