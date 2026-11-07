# ADR 002: Keycloak proves identity; the database decides authorization

## Status

Accepted

## Problem

The platform needs login, token expiry, and a platform operator. It also needs organization roles that change when someone is invited or demoted. Those membership changes are application data, not identity-provider configuration.

## Options considered

- Store `ORG_ADMIN`, `PROJECT_MANAGER`, and `MEMBER` only in Keycloak. Every role change becomes an identity-provider call, and tenant tests need a live realm.
- Store every role, including `SUPER_ADMIN`, only in Postgres. Login still needs Keycloak, and the platform role would be duplicated.
- Keycloak for identity and for `SUPER_ADMIN`. Postgres for organization roles.

## Decision

Keycloak is the identity provider. The SPA uses Authorization Code with PKCE against the public client `taskflow-spa`. The API is an OAuth2 resource server. It checks issuer, audience (`taskflow-api`), signature (JWKS), and expiry.

`SUPER_ADMIN` is a Keycloak realm role and is mapped to a Spring authority.

`ORG_ADMIN`, `PROJECT_MANAGER`, and `MEMBER` live on `organization_members.role`. The first authenticated request upserts `users` from `sub`, email, and name. It does not create a membership. A user with no membership has role `NONE` and cannot call organization APIs.

`X-Organization-Id` is ignored unless the caller is `SUPER_ADMIN`.

## Why

Tokens stay small and stateless. An invite or a demotion takes effect on the next request without waiting for a new token. Tests mint JWTs and do not start Keycloak. A Compose run still uses the imported realm.

## Trade-offs

The API must stay in sync with Keycloak user ids. Seed users use the same UUID for the Keycloak user id, `users.keycloak_sub`, and `users.id`. Organization roles are not visible inside Keycloak's admin console. That is accepted because those roles are application data.
