# Taskflow

## Project overview

Taskflow is a place where several companies can keep projects and tasks, on one system, without seeing each other's work. Meridian and Northline are the two demo companies. Alice at Meridian cannot open Dave's tasks at Northline, even if she pastes his link.

## Architecture

The browser has two conversations. Sign-in goes to Keycloak. Everything else, such as tasks and the dashboard, goes to one Java API. PostgreSQL is where the real data lives. Redis is a helper: it remembers dashboard numbers for a short time, slows people who click too fast, and nudges a background worker. That worker is the same program, started in a different mode, so it can send reminders without slowing the pages people are using. More detail is in [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md).

## Technology stack

- Screens: Angular
- API: Java 17 and Spring Boot
- Sign-in: Keycloak. The browser is sent to a login page and comes back with a short-lived proof of who the person is. The password is not stored in Taskflow.
- Database: PostgreSQL. Table changes are versioned files that run in order (Flyway).
- Helper store: Redis, for the short-lived dashboard copy and the speed limit
- How it runs: Docker Compose, one command starts every piece

## Prerequisites

Docker and Docker Compose, and a browser on the same computer. You only need Java 17 and Node 22 if you want to run the API or the screens outside Docker.

## Installation

```bash
cp .env.example .env
```

Open `.env` and replace the placeholder passwords. Do not commit that file. It holds the local secrets.

## Docker startup

```bash
docker compose up --build
```

The first start takes a minute. Keycloak imports the demo users before the sign-in page will work. Add `-d` if you want it in the background. `docker compose down` stops everything and keeps the data. `docker compose down -v` also deletes the saved databases, so the next start is a fresh demo.

| What | Address |
|---|---|
| The app. Start here and sign in. | http://localhost:4200 |
| http://localhost:8082/swagger-ui.html | http://localhost:8082/swagger-ui.html |
| "Is the API process up?" | http://localhost:8082/health |
| "Can the API reach the database?" | http://localhost:8082/ready |
| The sign-in service, if you want to open it directly | http://localhost:8081 |

The API is on port 8082. The background worker uses the same program on port 8083, and that port is not published to your computer. You read its work in the logs.

To run the API on the machine instead of in Docker, start the database, Redis, and Keycloak first, then from `backend/`:

```bash
export SPRING_DATASOURCE_PASSWORD=...   # POSTGRES_PASSWORD from .env
export SPRING_DATA_REDIS_PASSWORD=...   # REDIS_PASSWORD from .env
./mvnw spring-boot:run
```

The screen dev server forwards `/api` to `localhost:8082`:

```bash
cd frontend && npm ci && npm start
```

## Keycloak setup

Compose starts Keycloak and loads [keycloak/realm-export.json](keycloak/realm-export.json). A realm is Keycloak's name for this application's login world. Ours is called `taskflow`. The browser app is the public client `taskflow-spa`. The API only accepts login proofs that were meant for `taskflow-api`. The login page look comes from `keycloak/themes/taskflow`.

In the browser the login address is `http://localhost:8081/realms/taskflow`. Inside Docker, the API asks Keycloak for its signing keys at `http://keycloak:8000`, because `localhost` inside the API container is the API itself, not Keycloak. The login page is allowed to send people back to `http://localhost:4200`.

## Test users

Every demo password is `password`. That is only for this local login world. Do not reuse it anywhere real.

| User | Organization | Role |
|---|---|---|
| `superadmin` | none, until they pick one | Platform owner. Can see every company, but only after choosing one. |
| `alice` | Meridian | Company admin. Members, projects, all tasks, audit, full dashboard. |
| `bob` | Meridian | Project manager. Projects and all tasks. Cannot change who is a member. |
| `cara` | Meridian | Member. Can create tasks and edit the ones she owns or is assigned. Cannot delete. |
| `dave` | Northline | Company admin, but only inside Northline. |

Meridian's id is `c2f0c8b6-ab19-475e-ae57-09a4c2c329b6`. Northline's id is `3d5c8b81-9c1e-4751-b52c-d9d8d791d28e`. Those are random ids, not something a person types.

## Environment variables

Copy `.env.example`. Compose reads:

| Variable | Use |
|---|---|
| `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD` | The Taskflow database |
| `REDIS_PASSWORD` | Redis |
| `KEYCLOAK_DB`, `KEYCLOAK_DB_USER`, `KEYCLOAK_DB_PASSWORD` | Keycloak's own database |
| `KEYCLOAK_ADMIN_USER`, `KEYCLOAK_ADMIN_PASSWORD` | The Keycloak admin, used only to bootstrap |
| `KEYCLOAK_URL`, `KEYCLOAK_REALM`, `KEYCLOAK_CLIENT_ID`, `KEYCLOAK_CLIENT_SECRET` | Names of the login world and the API client. The API checks login proofs with Keycloak's public keys. It does not use this secret to trust a user. |

## API documentation

Open http://localhost:8082/swagger-ui.html while the stack is running. That page lists every call, what it expects, and what it returns. Turn that page off before the API is reachable by anyone except you.

## Testing

From `backend/`: `./mvnw test`. From `frontend/`: `npx ng test --watch=false`.

GitHub Actions (`.github/workflows/ci.yml`) runs those tests, builds the Angular app, and builds both images. It does not start Keycloak. The API tests create their own login proofs and still check that the proof came from the right place and was meant for this API.

## Backup/restore

```bash
./scripts/backup.sh
./scripts/restore.sh backups/<file>.dump
```

Restore prints how many organizations and tasks came back. How often to back up, and how long a restore takes, is in [docs/DISASTER_RECOVERY.md](docs/DISASTER_RECOVERY.md).

## Known limitations

An ordinary person belongs to one company. Assignment notices are written to the log, not sent as email. There is one database, one Keycloak, and no metrics dashboard.

## Walkthrough

The short tour, with screenshots and logs, is [docs/WALKTHROUGH.md](docs/WALKTHROUGH.md). 

Other notes: [docs/SECURITY.md](docs/SECURITY.md), [docs/DATABASE.md](docs/DATABASE.md), [docs/AI_USAGE.md](docs/AI_USAGE.md), [docs/ADR](docs/ADR). The assessment brief is [docs/ASSESSMENT.md](docs/ASSESSMENT.md).
