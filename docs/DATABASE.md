# Database

The shape of the database lives in versioned SQL files. They run in order the first time the database is empty, and again only when a new file appears. The application checks that the tables match those files. It does not invent or alter tables on its own.

The files are `V1__create_users.sql` through `V5__create_tasks.sql`, then `V6__create_audit_idempotency_and_jobs.sql`. Demo companies, people, projects, and tasks are `V7__seed_demo_data.sql`.

## Entity relationships

A company has many projects and many members. A person has at most one membership. A project belongs to one company and has many tasks. A task belongs to one project and one company, has an owner, and may have someone assigned. One-time create keys, audit rows, and background jobs point at the company and at the thing they record.

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

- `organizations` — the company. The name is unique even if the letters change case. Status is `ACTIVE` or `SUSPENDED`.
- `users` — the person. Their Keycloak id is unique. Email is unique ignoring case. A user row does not itself belong to a company.
- `organization_members` — one row per person, so a normal user cannot be in two companies. The role is company admin, project manager, or member.
- `projects` — the pair of project id and company id is unique, so a task can be tied to both. Inside one company, project names are unique.
- `tasks` — status and priority are limited to known values. `version` goes up on every successful edit, so two people cannot silently overwrite each other. The task's project must belong to the same company.
- `task_idempotency_keys` — remembers a create so a retry does not make a second task. Unique per company, person, and key.
- `audit_logs` — a history that can only grow. A database rule rejects any attempt to change or delete a row.
- `job_outbox` — work waiting to be done. Status is `PENDING`, `DONE`, or `DEAD`.
- `notification_deliveries` — one row per job, so a retry does not send the same notice twice.

## Indexes

An index is a shortcut the database uses so it does not read every row. These shortcuts all start with the company id, so one company's tasks are not mixed with everyone else's. A shortcut on status alone, or on date alone, was skipped because it would mix every company, and those columns are already the second part of a company shortcut.

| Index | Query |
|---|---|
| `tasks_org_created_idx` on company, then newest first | The default task list |
| `tasks_org_project_idx` on company and project | A project's tasks |
| `tasks_org_status_idx` on company and status | Filtering by status, and the dashboard counts |
| `tasks_org_assignee_idx` on company and assignee | "My tasks" |
| `tasks_org_due_idx` on company and due date, only when a due date exists | Overdue tasks and reminders |
| `audit_logs_org_created_idx` on company, then newest first | The audit list |
| `users_email_lower_idx` | Finding a person by email when adding a member |
| `job_outbox_pending_idx` on status and when the job may run | The worker looking for waiting jobs |

## Constraints

- Status and priority have to be one of the known words. Free text is rejected.
- A person can appear only once in `organization_members`.
- A project is unique as the pair of its id and its company, so a task can require both.
- A task's project and company must match a real project in that same company. A Meridian task cannot point at a Northline project.
- A one-time create key is unique for that company, that person, and that key.
- A delivery row's id is the job id, so there can be only one delivery per job.
- `audit_logs` has a trigger named `taskflow_reject_audit_mutation`. Updating or deleting a row raises `audit_logs is append-only`.

## Transactions

Creating a task writes the task, the one-time key, the audit row, and any background job in one save. If any part fails, none of it is kept. Two creates arriving at the same moment with the same key take a short lock, then the second one reads the row the first one saved. Updating a task only succeeds if the version still matches. If it does not, the answer is `409 STALE_VERSION`. If the save is undone, the audit row is undone with it. Saving a brand-new person uses its own save, so a failed membership lookup does not forget that the person exists.

## Performance considerations

`database/explain/task-list.sql` is the question the task list asks: Meridian's tasks that are still to do, newest first. It was run on the demo database, which has three Meridian tasks.

The list uses `tasks_org_created_idx`. That is the right shortcut, because the company is the main condition and the list is ordered by newest first. Status is an extra filter. On the demo data that filter skipped 2 rows. The database took about 0.06 ms and did not read the whole table.

```text
Limit
  -> Index Scan using tasks_org_created_idx on tasks
       Index Cond: (organization_id = 'c2f0c8b6-ab19-475e-ae57-09a4c2c329b6')
       Filter: (status = 'TODO')
       Rows Removed by Filter: 2
```

`tasks_org_status_idx` is the right shortcut when the question does not need newest-first order, such as dashboard counts. With the other task shortcuts temporarily removed, the same list used the status shortcut and then sorted:

```text
Limit
  -> Sort (created_at DESC)
       -> Index Scan using tasks_org_status_idx on tasks
            Index Cond: (organization_id = '...' AND status = 'TODO')
```

Run the script again once a company has thousands of tasks, and check that the newest-first list still prefers `tasks_org_created_idx` and the counts still prefer `tasks_org_status_idx`. Lists return 20 rows by default and never more than 100, so one request cannot pull an unlimited result into memory.
