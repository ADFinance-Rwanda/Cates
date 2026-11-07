# Walkthrough

Local and Render addresses:

| | Local | Render |
|---|---|---|
| Application | http://localhost:4200 | https://adfinance-rwanda-cates-web.onrender.com |
| API | http://localhost:8082 | https://adfinance-rwanda-cates-api.onrender.com |
| Keycloak | http://localhost:8081 | https://adfinance-rwanda-cates-auth.onrender.com |

Sign in as `alice` / `password`. She is `ORG_ADMIN` of Acme. `bob` is `PROJECT_MANAGER`, `cara` is `MEMBER`, `dave` is `ORG_ADMIN` of Globex, and `superadmin` has no home organization until one is selected. The Render links answer after [RENDER.md](RENDER.md) is applied. Nothing below removes the local ports.

## Keycloak login

The SPA starts Authorization Code with PKCE. The browser leaves port 4200 for the Keycloak realm and returns with a code. The API then checks issuer, audience `taskflow-api`, signature, and expiry.

![Keycloak sign-in](evidence/keycloak-login.png)

## Roles and organization structure

Acme owns the project Website refresh. Alice, Bob, and Cara are members of Acme only. Dave's organization is Globex. A task id from the other organization returns 404. A query `organization_id` that does not match the caller returns 403. `SUPER_ADMIN` must send `X-Organization-Id`.

## Task CRUD

The tasks screen is the catalog. Create, update, and delete are the same API the form uses. `POST` requires `Idempotency-Key`. `PUT` sends `version`. This run created a task, updated it, and deleted it, so the seed rows stayed at three:

```text
POST /api/tasks -> 201
GET  /api/tasks/{id} -> 200  status=TODO
PUT  /api/tasks/{id} -> 200  status=IN_PROGRESS version=1
DELETE /api/tasks/{id} -> 204
```

Full lines: [evidence/task-crud.log](evidence/task-crud.log).

![Task list](evidence/tasks.png)

## Tenant isolation

The tenant is the membership of the token subject. The client does not get to choose it. The task foreign key is `(project_id, organization_id)`, so a task cannot point at another organization's project.

## Dashboard

Alice's dashboard is the organization scope. Counts come from Postgres and are cached in Redis for 60 seconds.

![Dashboard](evidence/dashboard.png)

`GET /api/dashboard` returned scope `ORGANIZATION`, total 3, completed 1, pending 2, overdue 1.

## Redis

Redis answered `PONG`. After the dashboard call the version key `dashboard:ver:{acme}` was `6`, and a cache entry for Alice's organization scope had a TTL of about 60 seconds. Rate-limit keys were present too. Idempotency keys are not in Redis. The log is [evidence/redis.log](evidence/redis.log).

## Rate limiting

Task and member mutations use a 60-request window. Over the limit the API returns 429 and `Retry-After`. If Redis is down the call is allowed. That is [ADR 004](ADR/004-redis-fail-open.md).

## Concurrent updates

The update above moved `version` from 0 to 1. A second writer still holding version 0 gets 409 `STALE_VERSION`.

## Audit logging

The create, update, and delete wrote `audit_logs` in the same transaction. Members cannot read that list. The table trigger rejects update and delete.

## Background worker

The worker is the same JAR with profile `worker`. It publishes `job_outbox`, retries, and after five failures marks the row `DEAD` and copies it to `jobs:dlq`. On Render this process is `adfinance-rwanda-cates-worker`.

## Automated tests

`backend/`: `./mvnw test` against PostgreSQL and Redis in Testcontainers. `frontend/`: `npx ng test --watch=false`.

## PostgreSQL backup

`./scripts/backup.sh` runs `pg_dump -Fc`. The captured dump is a PostgreSQL custom archive, version 17.11, 28584 bytes, and `pg_restore -l` lists `organizations`, `projects`, `tasks`, `users`, and `audit_logs`. Log: [evidence/pg_dump.log](evidence/pg_dump.log).

## PostgreSQL restore

`./scripts/restore.sh backups/<file>.dump` stops nothing by itself. Stop the API and worker first, then restore. The script uses `pg_restore --clean --if-exists` and prints organization and task counts.

## Docker

`docker compose up --build` is the local stack. [render.yaml](../render.yaml) is the same system on Render, with the public URLs in the table above.
