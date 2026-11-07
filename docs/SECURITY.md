# Security

## Authentication

Keycloak realm `taskflow` issues tokens. The SPA client `taskflow-spa` is public and requires PKCE S256. Redirect URIs are `http://localhost:4200/*` and `https://adfinance-rwanda-cates-web.onrender.com/*`. The access token includes audience `taskflow-api`. Locally the issuer is `http://localhost:8081/realms/taskflow`. On Render it is `https://adfinance-rwanda-cates-auth.onrender.com/realms/taskflow`.

The API rejects tokens with the wrong issuer, audience, signature, or expiry. CSRF protection is off because the API is stateless and does not authorize with cookies. CORS allows only `APP_CORS_ALLOWED_ORIGINS` (default `http://localhost:4200`) and the headers the SPA sends.

Logout is implemented in the SPA. It drops the local token and ends the Keycloak session.

Local demo users and the password `password` exist only in the realm import. They are not application secrets and must not be reused outside this Compose stack.

## Authorization

Every protected route checks the caller again on the server. Hiding a button is not the control.

| Role | Where it lives | What it can do |
|---|---|---|
| `SUPER_ADMIN` | Keycloak realm role | Manage organizations. Act inside an organization only with `X-Organization-Id`. |
| `ORG_ADMIN` | `organization_members.role` | Members, projects, all tasks, audit, full dashboard. |
| `PROJECT_MANAGER` | membership | Create projects, all tasks, full dashboard. No member administration. |
| `MEMBER` | membership | Create tasks. Update tasks they own or are assigned. Cannot delete, reassign, move a task to another project, or read audit. Dashboard is limited to those tasks. |
| `NONE` | no membership | `GET /api/me` only. |

A missing permission is 403. Another organization's resource id is 404, so the caller does not learn that the row exists. A query `organization_id` that does not match the caller's tenant is 403. Suspended organizations block ordinary members. A super admin can still operate after selecting that organization.

The first valid call upserts `users` from `sub` in a new transaction (`keycloak_sub` unique). Membership is never implied by a token. Seed memberships are loaded by Flyway so the demo users match the Keycloak ids.

## Multi-tenancy

The tenant comes from the authenticated user, not from the client. For an ordinary user the API reads `sub`, finds `users`, then reads the single `organization_members` row. That organization id is applied to every query and write.

`X-Organization-Id` is accepted only when the token has `SUPER_ADMIN`. For everyone else the header is ignored. Putting another organization's id in the body or in `?organization_id=` does not switch tenants.

A task cannot reference another organization's project. The foreign key is the pair `(project_id, organization_id)`.

## RBAC

The four roles required by the assessment are `SUPER_ADMIN`, `ORG_ADMIN`, `PROJECT_MANAGER`, and `MEMBER`.

| Action | SUPER_ADMIN | ORG_ADMIN | PROJECT_MANAGER | MEMBER |
|---|---|---|---|---|
| Manage organizations | yes | no | no | no |
| Manage organization users | yes | yes | no | no |
| Create projects | yes | yes | yes | no |
| Delete tasks | yes | yes | yes | no |
| Create tasks | yes | yes | yes | yes |
| Update a permitted task | yes | yes | yes | own or assigned only |
| Organization dashboard | yes | yes | yes | assigned tasks only |
| Read audit | yes | yes | no | no |

## Input validation

Request bodies are validated before a service runs. Missing titles, bad emails, unknown status values, and page sizes above 100 are rejected. Bean validation and explicit checks return 400. Business rules that the payload cannot express, such as a member editing someone else's task, return 403 or 422. Search text is escaped so `%` and `_` are not wildcards. Queries use parameterized SQL and JPA criteria, not string-concatenated SQL.

## Rate limiting

`POST`, `PUT`, and `DELETE` on tasks, and member mutations, are limited to 60 calls per 60 seconds per user and route. The counter is a Redis sliding window. Over the limit the response is 429 with `Retry-After`. If Redis cannot answer, the call is allowed and a warning is logged. That choice is [ADR 004](ADR/004-redis-fail-open.md).

## Secret management

Secrets come from `.env`, which is gitignored. `.env.example` has placeholders only. Compose passes database and Redis passwords as environment variables. The API and worker images run as the non-root user `app`. The frontend image is `nginxinc/nginx-unprivileged`. The Keycloak client secret in `.env.example` is a local audience-client value. Token checks use the realm's public keys, not that secret.

## Audit logging

Creates, updates, deletes, and role changes write an `audit_logs` row in the same transaction as the change. The row stores organization, user, action, entity type, entity id, client IP, and metadata. Metadata holds ids and field changes, not tokens or passwords. There is no API to update or delete those rows. The trigger `taskflow_reject_audit_mutation` rejects `UPDATE` and `DELETE` even from a database console. `GET /api/audit` is limited to `ORG_ADMIN` and `SUPER_ADMIN`.

## Security assumptions

- The Compose ports stay on localhost. Springdoc at `/swagger-ui.html` is unauthenticated for that reason and must be turned off before any wider exposure.
- The demo password is acceptable only inside this realm import.
- Isolation is enforced in services and by foreign keys. This version does not use Postgres row-level security. A missed organization filter would leak data, which is why the cross-tenant tests exist.
- Redis failing open means a cache outage also disables throttling until Redis returns.
- Keycloak is a single container. A production system would run it highly available and would not enable the password grant used for local curls.
