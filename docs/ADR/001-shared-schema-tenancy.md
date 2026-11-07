# ADR 001: Shared-schema multi-tenancy

## Status

Accepted

## Problem

Taskflow must host many organizations without letting one read or change another's projects and tasks. The deployment is one Docker Compose stack, not a database per customer.

## Options considered

- One database per organization. Strong isolation, expensive to migrate, back up, and connect from one API.
- One schema per organization. Still multiplies migrations and connection handling.
- One shared schema with `organization_id` on every tenant-owned row, enforced by the API and by foreign keys.

## Decision

Use one PostgreSQL database and one schema. `organization_id` is on projects, tasks, memberships, idempotency keys, and audit rows.

The tenant is not taken from the request body or from `?organization_id=`. The API resolves it from the validated JWT `sub`, the local `users` row, and `organization_members`. A normal user has one membership (`UNIQUE(user_id)`). `SUPER_ADMIN` has no home organization and must send `X-Organization-Id` after the server has confirmed that realm role.

A task cannot point at another organization's project. The foreign key is `(project_id, organization_id) → projects (id, organization_id)`.

Cross-tenant access by resource id returns 404. An explicit mismatched `organization_id` query parameter returns 403.

## Why

One schema keeps a single migration history, one backup, and one connection pool, which matches a Compose deployment that may still grow to many organizations. Isolation is then a predicate on every query plus a constraint the database can check.

## Trade-offs

Application queries must include the organization. There is no Postgres row-level security in this version. A missed filter would leak data. Composite keys and the cross-tenant tests are the compensating controls. A database-per-tenant design would isolate mistakes more strongly and would cost more to operate.
