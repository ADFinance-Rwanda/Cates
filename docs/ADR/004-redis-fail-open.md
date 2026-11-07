# ADR 004: Redis is an accelerator and fails open

## Status

Accepted

## Problem

Redis holds the dashboard cache, the sliding-window rate limit, and a wake-up stream for the worker. None of those are the system of record for tasks. The API still has to define what happens when Redis is down.

## Options considered

- Fail closed. If Redis is down, refuse mutations so the rate limit cannot be bypassed. Task editing stops during a cache outage.
- Fail open. CRUD continues, the dashboard hits SQL, and throttling pauses until Redis returns.
- Store idempotency in Redis as well. Rejected in [ADR 003](003-postgres-idempotency-and-version.md).

## Decision

If Redis is unavailable:

- Project and task CRUD continue. Idempotency keys remain in Postgres.
- `GET /api/dashboard` runs the SQL aggregate.
- The rate limiter logs a warning and allows the mutation.
- The worker stops consuming. Rows stay in `job_outbox` as `PENDING` and are published again when Redis returns. The stream is a hint, not the queue of record.

`GET /ready` requires Postgres. Redis is reported as a component and does not by itself return 503.

Dashboard cache keys are `dashboard:{orgId}:{userId}:{scope}:{version}`. A task or project write increments `dashboard:ver:{orgId}` instead of scanning keys. TTL is 60 seconds. Members see the `ASSIGNED` scope. Other roles see `ORGANIZATION`.

The mutation limit is 60 requests per 60 seconds per user, method, and route template. Excess is `429` with `Retry-After`.

## Why

A local platform should keep task editing available when the cache is down. The outbox already protects jobs. Readiness should follow the database that holds the tasks.

## Trade-offs

A Redis outage temporarily removes throttling. That is accepted for this deployment and would be the wrong policy if every mutation had to be throttled. The worker can lag. It does not lose jobs that were committed to the outbox. Sessions are not stored in Redis. Access tokens are stateless JWTs.
