# AI usage

## Tools Used

Cursor, with Grok 4.7, was used to draft, debug, and try approaches. Generated code was read, run, and changed where it was wrong.

## Where AI Was Used

The assistant drafted the Java API, the Angular screens, the Keycloak login theme, the database files, the tests, the Compose setup, and these notes. The folder layout, the rule that a company comes from membership, and the choice to keep one-time create keys out of Redis were corrected in review. The first draft was not accepted as finished. The checks below were run so a generated file could not stand in for a passing test.

## At Least 3 Real Prompts

1. Build a production-oriented, Dockerized, secure multi-tenant project and task platform. Do not change files yet. Plan it.
2. Implement the approved plan: one shared database, Keycloak for who the person is, PostgreSQL for what they may do, Redis as a helper, Angular screens, tests, and notes for someone operating it.
3. The signed-in pages do not match the provided layout, and they are not using the green palette. Restyle the shell, dashboard, and project, task, and member screens, then rebuild the frontend.

## AI Code You Rejected

**Suggested:** remember the one-time create key in Redis, because a retry would be fast.

**What we did instead:** the key is written in PostgreSQL in the same save as the task.

**Why:** if Redis were down during a retry, a second task would be created. Redis can remember an answer later. It is not the authority. The same key and the same task returns the original task. The same key and a different task is rejected.

**Suggested:** trust the company id when the browser sends it on the URL or in the body.

**What we did instead:** the company is loaded from the person's membership after the login proof is checked.

**Why:** someone at Meridian could pass Northline's id and read that company. A mismatched company id is refused. An id that belongs to the other company looks like it does not exist. Naming a company in a header works only for the platform owner.

**Suggested:** the first time a Keycloak user calls the API, create their membership automatically.

**What we did instead:** the first call only saves the person. It does not add them to a company.

**Why:** being able to log in is not an invitation. Membership comes from the demo data or from an admin.

Other generated code was changed after a real run failed. The JSON library class had moved in this version of Spring Boot. A 64-character hash column was declared too strictly for the database mapper. The Redis settings type had moved package. The test-database library names were the old ones, and Spring Boot already manages the current version.

## Verification

`./mvnw test` in `backend/` runs the API against a real PostgreSQL and Redis started for the test. The tests cover a missing login, a role that is not allowed, reading another company's task, a faked company id, two editors saving at once, a repeated create, the dashboard cache, the speed limit, and a worker job that fails until it is set aside. `npx ng test --watch=false` covers the screen's role helpers. [docs/DATABASE.md](DATABASE.md) records what the database actually chose when listing tasks. The sign-in, dashboard, projects, tasks, and members screens were clicked in a browser against the running stack, not against a fake. `scripts/backup.sh` and `scripts/restore.sh` were run, and the task count matched.
