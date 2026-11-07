# Taskflow

## Project overview

Taskflow is a multi-tenant project and task platform. Each organization sees only its own projects, tasks, members, and dashboard. A user in one organization cannot read or change another organization's data.

## Architecture

The browser talks to Keycloak for login and to one Spring Boot API for work. PostgreSQL stores the data. Redis caches the dashboard, limits mutations, and wakes a worker. The worker is the same application started with a different profile. Detail is in [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md).

## Technology stack

- Frontend: Angular
- Backend: Java 17, Spring Boot
- Authentication: Keycloak (OpenID Connect, Authorization Code with PKCE)
- Database: PostgreSQL, migrations with Flyway
- Cache and rate limit: Redis
- Containers: Docker Compose

## Prerequisites

Docker and Docker Compose. A browser on the same machine. Java 17 and Node 22 are needed only if you run the API or the Angular dev server outside Compose.

## Installation

```bash
cp .env.example .env
```

Replace the placeholder passwords in `.env`. Do not commit that file.

## Docker startup

```bash
docker compose up --build
```

Add `-d` to run in the background. `docker compose down` stops the stack and keeps the data. `docker compose down -v` also deletes the database volumes.

| What | Local | Render |
|---|---|---|
| Application. Sign in starts here. | http://localhost:4200 | https://adfinance-rwanda-cates-web.onrender.com |
| OpenAPI | http://localhost:8082/swagger-ui.html | https://adfinance-rwanda-cates-api.onrender.com/swagger-ui.html |
| Liveness | http://localhost:8082/health | https://adfinance-rwanda-cates-api.onrender.com/health |
| Readiness. Postgres must be up. | http://localhost:8082/ready | https://adfinance-rwanda-cates-api.onrender.com/ready |
| Keycloak. The sign-in form is reached from the application URL. | http://localhost:8081 | https://adfinance-rwanda-cates-auth.onrender.com |

The Render column is the blueprint in [render.yaml](render.yaml). Those addresses answer after the blueprint is applied. Steps are in [docs/RENDER.md](docs/RENDER.md). Local Compose is unchanged.

The API listens on 8082. The worker uses the same image with profile `worker` on 8083 and has no public port.

To run the API on the host, start `postgres`, `redis`, and `keycloak`, then from `backend/`:

```bash
export SPRING_DATASOURCE_PASSWORD=...   # POSTGRES_PASSWORD from .env
export SPRING_DATA_REDIS_PASSWORD=...   # REDIS_PASSWORD from .env
./mvnw spring-boot:run
```

The frontend dev server proxies `/api` to `localhost:8082`:

```bash
cd frontend && npm ci && npm start
```

## Keycloak setup

Compose starts Keycloak and imports [keycloak/realm-export.json](keycloak/realm-export.json) into realm `taskflow`. The public client is `taskflow-spa` (Authorization Code, PKCE). The API audience is `taskflow-api`. The sign-in theme is `taskflow`, mounted from `keycloak/themes/taskflow`. The issuer the browser sees locally is `http://localhost:8081/realms/taskflow`. On Render it is `https://adfinance-rwanda-cates-auth.onrender.com/realms/taskflow`. Inside Compose the API loads signing keys from `http://keycloak:8000`. The Render API loads them from the public Keycloak URL above. The SPA client allows both `http://localhost:4200` and `https://adfinance-rwanda-cates-web.onrender.com`.

## Test users

Password for every user is `password`. It is local demo data in the Keycloak import, not a production credential.

| User | Organization | Role |
|---|---|---|
| `superadmin` | none until an organization is chosen | `SUPER_ADMIN` |
| `alice` | Acme | `ORG_ADMIN` |
| `bob` | Acme | `PROJECT_MANAGER` |
| `cara` | Acme | `MEMBER` |
| `dave` | Globex | `ORG_ADMIN` |

Acme is `aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa`. Globex is `bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb`.

## Environment variables

Copy `.env.example`. The Compose stack reads:

| Variable | Use |
|---|---|
| `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD` | Application database |
| `REDIS_PASSWORD` | Redis |
| `KEYCLOAK_DB`, `KEYCLOAK_DB_USER`, `KEYCLOAK_DB_PASSWORD` | Keycloak's database |
| `KEYCLOAK_ADMIN_USER`, `KEYCLOAK_ADMIN_PASSWORD` | Keycloak bootstrap admin |
| `KEYCLOAK_URL`, `KEYCLOAK_REALM`, `KEYCLOAK_CLIENT_ID`, `KEYCLOAK_CLIENT_SECRET` | Realm and API audience client. The API validates tokens with the realm keys, not with this secret. |

## API documentation

The contract is served by Springdoc at http://localhost:8082/swagger-ui.html. A short curl guide is [docs/API.md](docs/API.md). Disable Springdoc before the API is exposed beyond this machine.

## Testing

From `backend/`: `./mvnw test`. From `frontend/`: `npx ng test --watch=false`.

CI (`.github/workflows/ci.yml`) runs those tests, the Angular production build, and both image builds. Keycloak is not started in CI. Tests mint JWTs. Issuer and audience are still checked.

## Backup/restore

```bash
./scripts/backup.sh
./scripts/restore.sh backups/<file>.dump
```

The restore prints organization and task counts. Procedure, RPO, and RTO are in [docs/DISASTER_RECOVERY.md](docs/DISASTER_RECOVERY.md).

## Known limitations

One organization per ordinary user. Notifications are log lines, not email. No row-level security, no replicated Postgres, no Keycloak high availability, no Kafka, no Prometheus. Springdoc is open on localhost.

## Walkthrough

The short submission walkthrough, with screenshots and logs, is [docs/WALKTHROUGH.md](docs/WALKTHROUGH.md). Locally:

1. Open http://localhost:4200 (Render: https://adfinance-rwanda-cates-web.onrender.com) and sign in as `alice`. The header shows Acme and `ORG_ADMIN`.
2. Sign in as `cara`. Delete is hidden. `DELETE /api/tasks/{id}` is still 403.
3. As `alice`, open a seeded Acme task, change the title, and save. Edit it again from a second tab without reloading: the second save returns 409 because `version` moved.
4. Create a task. The browser sends `Idempotency-Key`. Repeating that key with the same body returns the original task. The same key with a different body returns 409.
5. Sign in as `dave` and open a task URL copied from Acme. The API returns 404. `alice` calling a Globex task id does the same.
6. As `superadmin`, pick Globex. Calls then send `X-Organization-Id`. Without that header, organization-scoped calls return 403 `TENANT_REQUIRED`.
7. Dashboard counts come from Postgres and are cached for 60 seconds. A task write increments the cache version.
8. More than 60 task or member mutations in 60 seconds return 429 and `Retry-After`. If Redis is stopped, mutations still succeed. `/ready` stays 200 while Postgres is up.
9. Assign a task and read the worker logs. `TASK_ASSIGNED` is delivered once. Failed jobs retry, then land in `job_outbox` as `DEAD` and on the `jobs:dlq` stream.
10. `GET /api/audit` as `alice` shows creates and updates. `cara` receives 403. The audit trigger rejects updates and deletes.
11. Run the tests named above.
12. Run backup, then restore, and compare the task count.

Other notes: [docs/SECURITY.md](docs/SECURITY.md), [docs/DATABASE.md](docs/DATABASE.md), [docs/AI_USAGE.md](docs/AI_USAGE.md), [docs/ADR](docs/ADR). The assessment brief is [docs/ASSESSMENT.md](docs/ASSESSMENT.md).
