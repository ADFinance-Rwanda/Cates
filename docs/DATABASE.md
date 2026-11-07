# Database

Flyway owns the schema. Hibernate `ddl-auto` is `validate`. Migrations are `V1__create_users.sql` through `V5__create_tasks.sql`, then `V6__create_audit_idempotency_and_jobs.sql`. Demo organizations, users, projects, and tasks are `V7__seed_demo_data.sql`.

## Entity relationships

An organization has many projects and many members. A user has at most one membership. A project belongs to one organization and has many tasks. A task belongs to one project and one organization, has an owner, and may have an assignee. Idempotency keys, audit rows, and outbox jobs point at the organization and the entity they record.

```text
organizations
    ├── organization_members ── users
    ├── projects
    │     └── tasks ── users (owner, assignee)
    ├── task_idempotency_keys
    └── audit_logs
job_outbox ── notification_deliveries
```

## Important tables

- `organizations` — name unique case-insensitively, status `ACTIVE` or `SUSPENDED`.
- `users` — `keycloak_sub` unique. Email unique case-insensitively. No foreign key to an organization.
- `organization_members` — one row per user (`UNIQUE(user_id)`), role `ORG_ADMIN`, `PROJECT_MANAGER`, or `MEMBER`.
- `projects` — unique `(id, organization_id)` so tasks can use a composite foreign key. Unique `(organization_id, name)`.
- `tasks` — status and priority checks, `version` for optimistic locking, composite foreign key `(project_id, organization_id)`.
- `task_idempotency_keys` — unique `(organization_id, user_id, idempotency_key)`, `request_hash` varchar(64).
- `audit_logs` — append-only. Trigger `audit_logs_append_only` raises on update or delete.
- `job_outbox` — `PENDING`, `DONE`, or `DEAD`.
- `notification_deliveries` — primary key `outbox_id`, so a retried notification is a no-op.

## Indexes

Tenant filters lead with `organization_id`. Extra single-column indexes on `status` or `created_at` were skipped because they would mix every tenant and because those columns are already the second part of a composite.

| Index | Query |
|---|---|
| `tasks_org_created_idx (organization_id, created_at DESC)` | Default task list |
| `tasks_org_project_idx (organization_id, project_id)` | Project board |
| `tasks_org_status_idx (organization_id, status)` | Status filter and dashboard |
| `tasks_org_assignee_idx (organization_id, assignee_id)` | Assignee filter |
| `tasks_org_due_idx (organization_id, due_date) WHERE due_date IS NOT NULL` | Overdue and reminders |
| `audit_logs_org_created_idx (organization_id, created_at DESC)` | Audit list |
| `users_email_lower_idx` | Invite by email |
| `job_outbox_pending_idx (status, available_at)` | Worker poll |

## Constraints

- Status and priority values are `CHECK` constraints, not free text.
- `organization_members.user_id` is unique, so an ordinary user cannot belong to two organizations.
- `projects (id, organization_id)` is unique so the task foreign key can include the organization.
- `tasks (project_id, organization_id)` references `projects (id, organization_id)`. A task in Acme cannot point at a Globex project.
- `task_idempotency_keys (organization_id, user_id, idempotency_key)` is unique.
- `notification_deliveries.outbox_id` is the primary key.
- `audit_logs` has a `BEFORE UPDATE OR DELETE` trigger, `taskflow_reject_audit_mutation`, which raises `audit_logs is append-only`.

## Transactions

Creating a task inserts the task, the idempotency row, the audit row, and any outbox row in one transaction. A concurrent duplicate takes a transaction-scoped advisory lock, then reads the winner. Updating a task compares `version` in that same transaction. Zero rows means 409 `STALE_VERSION`. If the transaction rolls back, the audit row rolls back with it. User provisioning uses a separate transaction so a failed membership lookup does not undo the user upsert.

## Performance considerations

`database/explain/task-list.sql` is the status filter used by `GET /api/tasks?status=TODO` for Acme, ordered by `created_at DESC`. Captured on the seed database (three Acme tasks) after `docker compose up`:

The list query uses `tasks_org_created_idx` whether or not `tasks_org_status_idx` exists. That is the right index for this sort: the condition is `organization_id`, the order is `created_at DESC`, and `status` is a filter. On the seed data the filter removed 2 rows. Execution was about 0.06 ms. A sequential scan was not chosen.

```text
Limit
  -> Index Scan using tasks_org_created_idx on tasks
       Index Cond: (organization_id = 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa')
       Filter: (status = 'TODO')
       Rows Removed by Filter: 2
```

`tasks_org_status_idx` is the right index when the query does not need `created_at` order (dashboard counts, status-only filters). With the other task indexes dropped inside a transaction that was rolled back, the same list query used it and then sorted:

```text
Limit
  -> Sort (created_at DESC)
       -> Index Scan using tasks_org_status_idx on tasks
            Index Cond: (organization_id = '...' AND status = 'TODO')
```

Re-run the script once an organization has thousands of tasks. At that size, confirm the planner still prefers `tasks_org_created_idx` for the default list and `tasks_org_status_idx` for status aggregates. Lists are paginated, default 20 rows, maximum 100, so a request cannot scan an unbounded result into memory.
