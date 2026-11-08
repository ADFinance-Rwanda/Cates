# ADR 004: Redis is an accelerator and fails open

## Status

Accepted

## Problem

Redis holds the short-lived dashboard copy, the speed limit, and a wake-up nudge for the worker. None of those are where tasks are stored. The API still has to decide what happens when Redis is down.

## Options considered

- Stop edits when Redis is down, so the speed limit cannot be skipped. Task editing stops during a cache outage.
- Keep editing. The dashboard is calculated from the database, and the speed limit pauses until Redis returns.
- Also store one-time create keys in Redis. That was turned down in [ADR 003](003-postgres-idempotency-and-version.md).

## Decision

When Redis is unavailable:

- Projects and tasks can still be created, read, updated, and deleted. One-time create keys stay in PostgreSQL.
- The dashboard runs the database query.
- The speed limit logs a warning and allows the change.
- The worker stops listening. Jobs stay `PENDING` in `job_outbox` and are picked up when Redis returns. The stream is a nudge, not the queue of record.

`/ready` depends on PostgreSQL. Redis is reported, and being down does not by itself mark the API unready.

Dashboard copies are named with the company, the person, whether they see the whole company or only their tasks, and a version. A task or project write bumps the version instead of hunting through keys. The copy lasts 60 seconds. Members see their own tasks. Other roles see the company.

The change limit is 60 requests in 60 seconds for that person, that method, and that kind of call. Past that, the answer is "too many" and how long to wait.

## Why

A local platform should keep task editing available when the helper store is down. The queue table already protects jobs. "Are we ready?" should follow the database that holds the tasks.

## Trade-offs

A Redis outage temporarily removes the speed limit. That is acceptable here, and it would be the wrong choice if every change had to be limited. The worker can fall behind. It does not lose jobs that were saved with the task. Sessions are not stored in Redis. Login proofs do not need a server-side session.
