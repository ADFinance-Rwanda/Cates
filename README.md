# Multi-Tenant Project & Task Management Platform

## 1. Assessment Overview & Compliance

This repository contains a complete, production-oriented implementation of the Multi-Tenant Project & Task Management Platform technical assessment. The system is designed to demonstrate senior-level software engineering judgment across architecture, security, database engineering, concurrency control, reliability, testing, and DevOps.

### Core Requirements Implemented

- **Strict Multi-Tenancy:** Server-side tenant isolation enforced via JWT claims and security context, preventing cross-tenant IDOR attacks.
- **Authentication & RBAC:** Keycloak OIDC/OAuth2 integration with fine-grained role-based access control (`SUPER_ADMIN`, `ORG_ADMIN`, `PROJECT_MANAGER`, `MEMBER`).
- **PostgreSQL Persistence:** Relational schema with foreign keys, proper constraints, Flyway migrations, and optimized indexing.
- **Redis Integration:** Used for high-performance dashboard caching (60s TTL with invalidation), API rate-limiting, and idempotency key tracking.
- **Concurrency Control:** Optimistic locking (`version` column) handling concurrent task updates with `409 Conflict` responses.
- **Idempotent Task Creation:** `Idempotency-Key` tracking via Redis to prevent duplicate task creation on retries.
- **Audit Logging:** Append-only business audit trail for critical actions (`TASK_CREATED`, `TASK_UPDATED`, etc.).
- **Background Worker:** Asynchronous worker processing for notifications and background jobs with retry logic.
- **Observability & Health:** `/health` (liveness) and `/ready` (PostgreSQL & Redis dependency checks) actuator endpoints.
- **Automated Testing:** Integration and API tests verifying security boundaries, multi-tenancy, CRUD, and concurrency.
- **Disaster Recovery:** Automated backup and restore scripts (`pg_dump`) with documented RPO/RTO strategies.

---

## 2. Architecture & Tech Stack

```text
                    ┌─────────────┐
                    │   Keycloak  │
                    └──────┬──────┘
                           │
                           ▼
┌─────────────┐      ┌─────────────┐
│  Frontend   │ ───► │ Backend API │
└─────────────┘      └──────┬──────┘
                            │
                 ┌──────────┴──────────┐
                 ▼                     ▼
          ┌─────────────┐       ┌─────────────┐
          │ PostgreSQL  │       │    Redis    │
          └─────────────┘       └─────────────┘
                                      │
                                      ▼
                               ┌─────────────┐
                               │   Worker    │
                               └─────────────┘
```

### Technology Stack

| Component | Technology |
|---|---|
| Frontend | Angular / TypeScript |
| Backend API | Java 21 & Spring Boot (REST, Spring Security, Spring Data JPA, Flyway) |
| Identity Provider | Keycloak 26 (OIDC / OAuth2 Resource Server) |
| Primary Database | PostgreSQL 17 |
| Cache & Supporting Infrastructure | Redis 7 (Caching, Rate Limiting, Idempotency) |
| Containerization | Docker & Docker Compose |

For a deep dive into data flows, component design, and scaling considerations, see [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md).

---

## 3. Getting Started & Docker Deployment

### Prerequisites

- Docker & Docker Compose
- Java 21 & Maven (optional, if running the backend outside Docker)
- Node.js 22 (optional, if running the frontend outside Docker)

### Quick Start

**1. Copy the environment template:**

```bash
cp .env.example .env
```

**2. Review and configure local secrets in `.env`.**

**3. Build and start the entire containerized stack:**

```bash
docker compose up --build -d
```

### Service Endpoints

| Service | Port / URL | Description |
|---|---|---|
| Frontend UI | http://localhost:4200 | Main application entry point & sign-in |
| Backend API | http://localhost:8082 | REST API |
| Swagger / OpenAPI | http://localhost:8082/swagger-ui.html | Interactive API documentation |
| Health Check | http://localhost:8082/health | Liveness probe |
| Readiness Check | http://localhost:8082/ready | Dependency health probe (PostgreSQL, Redis) |
| Keycloak Console | http://localhost:8081 | Identity and access management |

---

## 4. Test Users & Security Context

Keycloak automatically imports realm configuration and test users upon startup from `keycloak/realm-export.json`.

All demo accounts use the following password:

`password`

| User | Organization | Assigned Role | Permissions / Scope |
|---|---|---|---|
| `superadmin` | Platform | `SUPER_ADMIN` | Platform oversight; accesses cross-tenant audit/admin views after organization selection. |
| `alice` | Meridian | `ORG_ADMIN` | Full administrative control over Meridian (users, projects, tasks, analytics, audit logs). |
| `bob` | Meridian | `PROJECT_MANAGER` | Project and task management within Meridian. |
| `cara` | Meridian | `MEMBER` | Task creation and updates for assigned/owned tasks within Meridian. |
| `dave` | Northline | `ORG_ADMIN` | Tenant-isolated admin for Northline (strict boundary test against Meridian data). |

### Tenant Isolation Verification

Authenticating as `alice` (Meridian) and attempting to access tasks belonging to `dave` (Northline) will strictly return `403 Forbidden` or `404 Not Found`, enforced at the server layer.

---

## 5. Automated Testing

To validate the test suite locally:

### Backend Integration Tests

```bash
cd backend
./mvnw test
```

### Frontend Tests

```bash
cd frontend
npx ng test --watch=false
```

---

## 6. Database Backup & Restore

Automated operational scripts are provided in the `scripts/` directory.

### Backup Database

```bash
./scripts/backup.sh
```

### Restore Database

```bash
./scripts/restore.sh backups/<backup-file>.dump
```

---

## 7. Documentation Index

Comprehensive technical documentation required by the assessment brief is located in the `docs/` directory:

- [Architecture Design](docs/ARCHITECTURE.md)
- [Security & Tenant Isolation](docs/SECURITY.md)
- [Database Schema & Indexing](docs/DATABASE.md)
- [Disaster Recovery & Failure Modes](docs/DISASTER_RECOVERY.md)
- [AI-Assisted Development Reflection](docs/AI_USAGE.md)
- [Architecture Decision Records](docs/ADR/)
- [System Walkthrough](docs/WALKTHROUGH.md)