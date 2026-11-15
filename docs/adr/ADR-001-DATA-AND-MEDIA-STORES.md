# ADR-001: Supabase PostgreSQL and Cloudinary Responsibilities

- Status: Accepted / Clarification Gate Kept Open
- Date: 2026-11-15

## Context
The project requirement baseline names Supabase and Cloudinary. Supabase provides the managed PostgreSQL relational database, while Cloudinary provides media asset and image transformations storage. During planning, a phrase referring to "two databases" was raised. Cloudinary is an object/media store with transformation APIs, not a secondary relational database or operational document store.

## Decision
1. Treat Supabase PostgreSQL as the single relational database engine for the system.
2. Structure the database into service-owned schemas (`identity`, `catalog`, `inventory`, `commerce`, `payment`, `fulfillment`) with least-privilege database user credentials per service.
3. Treat Cloudinary strictly as media/object storage for product photographs, variant images, return inspection evidence, and marketing banners. All Cloudinary public IDs, URLs, and transformation metadata are stored within the PostgreSQL database.
4. Stop before introducing any second database engine (e.g., MongoDB, Redis, MySQL, or a second Supabase instance) without an explicit owner requirement and an approved Architecture Decision Record.

## Consequences
- Prevents unnecessary operational complexity, multi-database sync issues, and unvetted hosting costs.
- Maintains strict service schema boundaries while keeping infrastructure consolidated on Supabase PostgreSQL.
- Local configuration is supplied through environment variables defined in the root `.env` without duplicate module secrets.
