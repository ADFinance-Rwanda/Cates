# ADR 003: Postgres-backed idempotency and optimistic versions

## Status

Accepted

## Problem

Clients retry `POST /api/tasks`. Two retries must not create two tasks. Two people must not silently overwrite the same task. Redis can disappear. Correctness cannot.

## Options considered

- Store the idempotency key only in Redis. Fast, and wrong during a Redis outage: the retry inserts a second task.
- Use `ETag` / `If-Match` without a version column. Equivalent idea, more awkward for the JSON API the SPA already sends.
- Store the idempotency key in Postgres in the same transaction as the task, and require `version` on update.

## Decision

`POST /api/tasks` requires `Idempotency-Key`. In the same transaction as the insert, the API writes `task_idempotency_keys` with a SHA-256 of the canonical request. The unique key is `(organization_id, user_id, key)`. Concurrent duplicates take a transaction-scoped advisory lock, then read the winner.

- Same key and same hash: return the original task with `200` and `Idempotent-Replay: true`.
- Same key and a different body: `409 IDEMPOTENCY_KEY_REUSED`.
- Keys older than 24 hours are deleted by the worker.

Task updates send `version`. The write is conditional on `id`, `organization_id`, and `version`. Zero rows is `409 STALE_VERSION`.

Audit rows are inserted in that same transaction. A database trigger rejects `UPDATE` and `DELETE` on `audit_logs`.

## Why

The unique constraint and the task insert commit or roll back together. A Redis restart cannot duplicate a task. The version check is one conditional update, so the second editor gets a conflict instead of a silent overwrite.

## Trade-offs

The client must keep the idempotency key for the retry window and must reload after 409. Postgres does extra writes on every create. Members can update a task they own or are assigned, but they cannot change `projectId` or `assigneeId`. That check is in the service, not only in the form.
