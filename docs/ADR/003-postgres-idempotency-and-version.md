# ADR 003: Postgres-backed idempotency and optimistic versions

## Status

Accepted

## Problem

People retry "create a task" when the network blinks. Two retries must not create two tasks. Two people must not silently overwrite the same task. Redis can disappear. The tasks still have to be right.

## Options considered

- Keep the one-time key only in Redis. Fast, and wrong when Redis is down: the retry inserts a second task.
- Use a web cache header instead of a version number. Same idea, and clumsier for the JSON the screen already sends.
- Store the one-time key in PostgreSQL in the same save as the task, and require the version number on update.

## Decision

Creating a task requires a header called `Idempotency-Key`. In the same save as the task, the API writes a row with a fingerprint of the request. The key is unique for that company, that person, and that key. Two creates at the same moment take a short lock, then the second one reads the winner.

- Same key and the same task: return the original task, and say it was a replay.
- Same key and a different body: reject it. The key was already used for something else.
- Keys older than 24 hours are deleted by the worker.

Updates send `version`. The write only happens if the id, the company, and the version still match. If nothing was updated, the answer is `409 STALE_VERSION`.

The audit row is written in that same save. A database trigger refuses any later edit or delete of an audit row.

## Why

The unique key and the new task are saved together, or neither is saved. Restarting Redis cannot duplicate a task. The version check is one conditional update, so the second editor gets a conflict instead of a quiet overwrite.

## Trade-offs

The screen has to keep the one-time key for the retry window, and it has to reload after a conflict. PostgreSQL does a little extra writing on every create. A member can update a task they own or are assigned, and they cannot change the project or the assignee. That check is in the service, not only in the form.
