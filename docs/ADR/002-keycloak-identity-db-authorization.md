# ADR 002: Keycloak proves identity; the database decides authorization

## Status

Accepted

## Problem

The platform needs login, proofs that expire, and a platform owner. It also needs company roles that change when someone is invited or demoted. Those membership changes are application data. They are not login-system settings.

## Options considered

- Keep company admin, project manager, and member only in Keycloak. Every role change becomes a call to the login system, and the tests need a live login server.
- Keep every role, including the platform owner, only in PostgreSQL. Login still needs Keycloak, and the platform role would be stored twice.
- Keycloak for identity and for the platform owner. PostgreSQL for the company roles.

## Decision

Keycloak is the login service. The screen uses the public client `taskflow-spa` and PKCE, so the browser does not hold an API password. The API checks that a proof came from the right place, was meant for `taskflow-api`, is signed with Keycloak's keys, and has not expired.

`SUPER_ADMIN` is a Keycloak role.

Company admin, project manager, and member live on the membership row. The first signed-in request saves the person from their login id, email, and name. It does not create a membership. A person with no membership can see who they are, and cannot call company APIs.

`X-Organization-Id` is ignored unless the caller is the platform owner.

## Why

The login proof stays small and does not have to be stored on the server. An invite or a demotion takes effect on the next request, without waiting for a new login. Tests can make their own proofs and do not start Keycloak. A normal Compose run still uses the imported login world.

## Trade-offs

The API has to stay in step with Keycloak's user ids. Demo users use the same id for the Keycloak user, the login id stored on the person, and the person row. Company roles do not show up in Keycloak's admin screen. That is accepted, because those roles belong to the application.
