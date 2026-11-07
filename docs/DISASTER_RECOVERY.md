# Disaster recovery

This stack is one machine. Recovery is a backup file plus Compose restart, not a second region.

## Backup

`scripts/backup.sh` runs `pg_dump -Fc` inside `taskflow-postgres` and writes `backups/taskflow-<utc-timestamp>.dump`. `backups/` is gitignored. An empty or failed dump is deleted so a partial file cannot be restored by mistake. A captured run is in [evidence/pg_dump.log](evidence/pg_dump.log).

On Render the application database is `adfinance-rwanda-cates-db`, reached only from the API and the worker. The same `pg_dump -Fc` command works against that database once its external URL is enabled in the Render dashboard. The local script and the localhost port `5433` stay the Compose path. The deployed API is https://adfinance-rwanda-cates-api.onrender.com.

The dump includes schema and data for the application database only. Keycloak has its own database and is rebuilt from `keycloak/realm-export.json` on a fresh volume. Redis is a cache and a queue hint. Losing it does not lose tasks. Pending outbox rows are in Postgres and will be published again.

## Restore

Stop writers first (`docker compose stop backend worker`), then:

```bash
./scripts/restore.sh backups/taskflow-YYYYMMDDTHHMMSSZ.dump
```

The script copies the dump into the Postgres container and runs `pg_restore --clean --if-exists --no-owner --exit-on-error`. It then prints `organizations` and `tasks` counts.

If restore stops halfway, do not keep serving that database. Restore a known-good dump, or remove the `postgres_data` volume and start clean. Flyway will migrate and seed. The audit trigger is part of the dump. If `--clean` reports a dependency error, drop and recreate the database, then restore before serving traffic.

## RPO

Recovery point is the last successful `backup.sh`. There is no continuous WAL shipping. If backups run once a day, up to one day of commits can be lost. Run the script on the schedule you can afford to lose.

## RTO

Recovery time is the time to stop the API and worker, run `restore.sh`, and start them again. On the demo database that is a few minutes. A larger database takes as long as `pg_restore` needs.

## PostgreSQL failure

While Postgres is down, `/ready` returns 503 and Compose marks the API unhealthy. Task reads and writes fail. Redis still holds cache entries, but they are not used as a substitute database. After Postgres returns, restart the API if it did not recover on its own. If the data files are lost, restore the latest dump. If the volume is removed, Flyway recreates the schema and the demo seed, which replaces whatever was not in a dump.

## Redis failure

Task CRUD continues. Idempotency stays in Postgres. The dashboard is computed with SQL. The rate limiter allows mutations and logs a warning. `/ready` stays 200 and reports Redis as `DOWN`. The worker cannot publish the wake-up stream. Rows remain `PENDING` in `job_outbox` and are picked up when Redis returns. No task is lost because the outbox row was committed with the task.

## Worker failure

The API keeps accepting requests and writing `job_outbox`. Restarting the worker resumes `PENDING` rows. A job that throws waits 5 seconds, then 15, then 45, then 2 minutes. After 5 attempts it is `DEAD` and a copy is added to `jobs:dlq`. `DEAD` rows are not retried automatically. `notification_deliveries` uses the outbox id as its primary key, so a retry does not send the same notification twice.

## Application recovery

Compose `restart: unless-stopped` brings the API and worker back after a crash. Health checks call `/health`. The API is not marked healthy until that endpoint answers, and the frontend waits for a healthy API. A bad deploy is reverted by rebuilding the previous image and running `docker compose up -d`. Database schema changes stay in Flyway, so an application restart does not recreate tables.
