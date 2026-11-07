# AI usage

## Tools Used

Cursor, with Grok 4.7 as the coding agent. No other assistant wrote this repository.

## Where AI Was Used

The agent drafted the Spring Boot API, the Angular screens, the Keycloak theme, Flyway migrations, Testcontainers tests, Compose files, and these documents. Package layout, tenant rules, and the decision to keep Redis out of the idempotency path were corrected in review rather than accepted from the first draft. The running checks below were used to keep generated code from standing in for a test.

## At Least 3 Real Prompts

1. Build a production-oriented, Dockerized, secure multi-tenant Project and Task Management Platform. Do not change files yet. Plan it.
2. Implement the approved plan: shared-schema tenancy, Keycloak for identity, Postgres for authorization and correctness, Redis as an accelerator, Angular screens, tests, and operator docs.
3. The signed-in pages do not match the credit-app layout, and they are not using the green palette. Restyle the shell, dashboard, and project, task, and member screens, then rebuild the frontend.

## AI Code You Rejected

**Suggested:** store `Idempotency-Key` in Redis so a retry is fast.

**Rejected:** the key is inserted in PostgreSQL in the same transaction as the task.

**Why:** a retry during a Redis outage would create a second task. Redis may cache a response later. It is not the authority. Same key and same body returns the original task. Same key and a different body returns 409.

**Suggested:** trust `organization_id` when the client sends it on the query or body.

**Rejected:** the tenant is loaded from `organization_members` after the token is validated.

**Why:** a member of Acme could pass Globex's id and read that tenant. A mismatched `organization_id` is 403. A foreign resource id is 404. `X-Organization-Id` works only for `SUPER_ADMIN`.

**Suggested:** create an organization membership the first time a Keycloak user calls the API.

**Rejected:** the first call upserts `users` only.

**Why:** a valid token is not an invitation. Membership is seed data or an admin action.

Other generated code that was changed after it failed a real run: Jackson 2's `ObjectMapper` (Boot 4.1 exposes `tools.jackson.databind.ObjectMapper`), `request_hash CHAR(64)` (Hibernate expects `VARCHAR(64)`), Boot 3 `RedisProperties` (the type moved package), and Testcontainers 1.x artifact names (Boot manages 2.0.5).

## Verification

`./mvnw test` in `backend/` runs the API against PostgreSQL and Redis in Testcontainers. Those tests cover 401, role 403, cross-tenant 404, a spoofed organization id, stale `version`, idempotent replay, dashboard cache, rate limit, and worker retry into the dead-letter stream. `npx ng test --watch=false` covers the Angular role helpers. `docs/DATABASE.md` records `EXPLAIN` output from the running database. The sign-in, dashboard, projects, tasks, and members screens were clicked in a browser against Compose, not against a mock. `scripts/backup.sh` and `scripts/restore.sh` were run, and the task count matched.
