# ADR 001: Shared-schema multi-tenancy

## Status

Accepted

## Problem

Taskflow has to host many companies without letting one read or change another's projects and tasks. This demo is one Docker Compose stack, not a separate database for each customer.

## Options considered

- One database per company. Strong separation, and expensive to update, back up, and connect from one API.
- One schema per company, inside a shared database. Still multiplies the updates and the connections.
- One shared set of tables, with the company id on every row that belongs to a company, checked by the API and by the database links.

## Decision

Use one PostgreSQL database and one set of tables. The company id is on projects, tasks, memberships, one-time create keys, and audit rows.

The company is not taken from the form body or from `?organization_id=`. The API finds it from the checked login proof, the local person row, and the membership. A normal user has one membership. The platform owner has no home company and must name one with `X-Organization-Id` after the server has confirmed that role.

A task cannot point at another company's project. The database link requires both the project id and the company id.

Opening something by an id from another company looks like "not found". Asking outright for another company's id is "forbidden".

## Why

One set of tables means one history of changes, one backup, and one pool of connections. That matches a Compose setup that might still grow to many companies. Separation is then "every question includes the company" plus a rule the database can check.

## Trade-offs

Every query has to include the company. This version does not add a second database policy on every row. A forgotten filter would leak data. The paired keys and the cross-company tests are what catch that. A database per company would hide a mistake more strongly, and it would cost more to run.
