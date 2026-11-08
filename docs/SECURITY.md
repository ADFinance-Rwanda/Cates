# Security

## Authentication

Keycloak's login world, named `taskflow`, is the only place that issues a proof of identity. The screen's client is `taskflow-spa`. It is public, which means the browser does not hold a password for the API. Each login also uses PKCE, so a copied redirect link is not enough to finish sign-in. People can only be sent back to `http://localhost:4200`. The proof says it was issued by `http://localhost:8081/realms/taskflow` and that it is meant for `taskflow-api`.

The API refuses a proof from the wrong place, meant for something else, badly signed, or expired. It does not use cookies, so the usual cookie-forgery protection is turned off. The browser is only allowed to call the API from `http://localhost:4200`, unless that list is changed with `APP_CORS_ALLOWED_ORIGINS`.

Log out drops the proof in the browser and ends the Keycloak session.

The demo users and the password `password` exist only in the login import. They are not secrets of the application, and they must not be reused outside this stack.

## Authorization

Hiding a button is not the rule. Every protected call checks the person again on the server.

| Role | Where it lives | What it can do |
|---|---|---|
| `SUPER_ADMIN` | Keycloak | Manage companies. Work inside a company only after naming it with `X-Organization-Id`. |
| `ORG_ADMIN` | the membership row | Members, projects, every task, the audit list, the full dashboard. |
| `PROJECT_MANAGER` | the membership row | Create projects, every task, the full dashboard. Cannot manage members. |
| `MEMBER` | the membership row | Create tasks. Edit tasks they own or are assigned. Cannot delete, reassign, move a task to another project, or read the audit list. The dashboard shows only their tasks. |
| `NONE` | no membership | Only "who am I". No company data. |

A person who is signed in but not allowed gets "forbidden". A task or project that belongs to another company looks "not found", so the caller does not learn that it exists. Asking for another company's id in the query is "forbidden". A suspended company blocks ordinary members. The platform owner can still work there after selecting it.

The first good call saves the person in `users` from the id inside the login proof. It does not add them to a company. Demo memberships are loaded with the database seed so the demo people match the Keycloak ids.

## Multi-tenancy

The company is decided from the signed-in person, not from whatever the browser sends. For a normal user the API reads the login id, finds the person, then reads their one membership. That company id is used on every read and write.

`X-Organization-Id` is accepted only for `SUPER_ADMIN`. For everyone else the header is ignored. Putting another company's id in the form body, or on the URL as `?organization_id=`, does not switch companies.

A task cannot point at another company's project. The database link is the pair of project id and company id, so a Meridian task cannot hang off a Northline project.

## RBAC

| Action | SUPER_ADMIN | ORG_ADMIN | PROJECT_MANAGER | MEMBER |
|---|---|---|---|---|
| Manage companies | yes | no | no | no |
| Manage members | yes | yes | no | no |
| Create projects | yes | yes | yes | no |
| Delete tasks | yes | yes | yes | no |
| Create tasks | yes | yes | yes | yes |
| Update a task | yes | yes | yes | only if they own it or are assigned |
| Company dashboard | yes | yes | yes | only their own tasks |
| Read the audit list | yes | yes | no | no |

## Input validation

The body is checked before any work starts. A missing title, a bad email, an unknown status, or a page size over 100 is rejected as a bad request. Rules the form cannot express, such as a member editing someone else's task, come back as forbidden or as "we understood it, but the rule says no". Search text treats `%` and `_` as ordinary characters, not as "match anything". Queries pass values separately from the SQL text, so a user cannot sneak SQL in through a title.

## Rate limiting

Creating, updating, and deleting tasks, and changing members, is limited to 60 calls in 60 seconds for that person and that kind of call. The count lives in Redis. Over the limit, the answer is "too many requests" plus how long to wait. If Redis cannot answer, the call is allowed and a warning is logged. That choice is [ADR 004](ADR/004-redis-fail-open.md).

## Secret management

Passwords come from `.env`, which git ignores. `.env.example` has placeholders only. Compose passes the database and Redis passwords in as environment variables. The API and worker run as a normal user named `app`, not as the administrator inside the container. The web image is the unprivileged nginx image. The Keycloak client secret in the example file is a local stand-in. Checking a login proof uses Keycloak's public keys, not that secret.

## Audit logging

Creates, updates, deletes, and role changes write an audit row in the same save as the change. The row stores the company, the person, the action, what kind of thing changed, its id, the client address, and a short note of what changed. The note holds ids and field changes, not tokens or passwords. There is no API to edit or delete those rows. A database trigger refuses update and delete even from a database console. Only a company admin or the platform owner can read the list.

## Security assumptions

- The ports stay on this computer. The clickable API page is open for that reason, and it must be turned off before anyone else can reach the API.
- The demo password is acceptable only inside this login import.
- Separation is enforced in the API and by database links. This version does not add a second database policy on every row. A forgotten company filter would leak data, which is why the tests try to read across companies.
- If Redis is down, the speed limit pauses until it returns.
- Keycloak is one container. A production system would run more than one, and would not leave the simple password grant on. That grant exists here so a local test can sign in without a browser.
