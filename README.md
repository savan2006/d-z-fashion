# D & Z Fashion — IntelliJ Workspace

This workspace is a clean, IntelliJ-openable starting point for the D & Z Fashion platform.

## Stack baseline

- Java 21 (MS-21 JDK) and Spring Boot 4.1.2 for the backend.
- Maven multi-module build with version-managed dependencies.
- Next.js 16.4.6 / React 19.2 baseline for the frontend (stable versions checked 15 Nov 2026; re-check security updates before implementation).
- Supabase PostgreSQL for relational business data.
- Cloudinary for product, variant, review and campaign media.
- Simulated online payment only; COD is controlled by Owner-maintained PIN-code availability.

## Open in IntelliJ IDEA

1. Extract this archive to a writable development directory.
2. Open the extracted `D-Z-Fashion-IntelliJ-Workspace` folder in IntelliJ IDEA.
3. Set the Project SDK to the installed MS-21 / Java 21 JDK.
4. Open `backend/pom.xml` as a Maven project when IntelliJ asks, or import it from the Maven tool window.
5. Open `frontend/package.json` and allow IntelliJ to detect the Node project.
6. Copy `.env.example` to `.env` only if you prefer a complete template. The included `.env` is intentionally blank except for safe local defaults. Fill credentials locally.
7. Read `.ai-workspace/START_HERE.txt` and `.ai-workspace/MASTER_ORCHESTRATOR_PROMPT.txt`.

## Private AI workspace

All 15 component prompts, AI skills, agent role instructions, original reference ZIP, project context, templates and execution tracker live under `.ai-workspace/`. The root `.gitignore` excludes this entire directory. These materials are for local IntelliJ AI use and must never be committed to GitHub.

## Important architecture note

Supabase is the relational database. Cloudinary is media storage, not a second relational database. If “two databases” means two independent database engines, stop before schema implementation and ask the owner which second database is intended. Do not silently add MongoDB, Redis or another database.

## Current scaffold status

This archive contains a structured workspace, Maven parent and service module skeletons, a minimal Next.js app shell, development scripts, and private AI implementation plans. It is not a completed application. The AI must execute the 15 components sequentially, implement code, run tests and commit/push each verified component.

## GitHub behaviour

The AI is instructed to commit and push each verified component to the already configured GitHub remote. It must not create or guess a remote, force-push, rewrite shared history, or commit `.ai-workspace`, `.env`, credentials, build outputs or unrelated user files. If no `origin` or authentication exists, it must keep the safe local commit and report the exact blocker.


## Environment variables

The frontend calls Spring Boot APIs through `NEXT_PUBLIC_API_BASE_URL`. Spring services use `SUPABASE_JDBC_URL` plus a service-specific database username/password. Spring Identity owns customer email-OTP authentication; Supabase Auth is not required in phase one. Cloudinary API secrets are backend-only.
