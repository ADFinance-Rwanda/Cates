# Disaster recovery

This demo runs on one machine. Getting it back means a backup file and starting the containers again. There is no second data center.

## Backup

`scripts/backup.sh` asks the Taskflow database for a full copy and writes `backups/taskflow-<utc-timestamp>.dump`. The `backups/` folder is not committed. An empty or failed file is deleted, so a half-written file cannot be restored by mistake. One captured run is in [evidence/pg_dump.log](evidence/pg_dump.log).

The file holds the Taskflow tables and their data. Keycloak has its own database and is rebuilt from `keycloak/realm-export.json` when its volume is new. Redis is a cache and a nudge for the worker. Losing Redis does not lose tasks. Jobs that were waiting stay in PostgreSQL and run again later.

## Restore

Stop the programs that write first:

```bash
docker compose stop backend worker
./scripts/restore.sh backups/taskflow-YYYYMMDDTHHMMSSZ.dump
```

The script copies the file into the database container, replaces the current objects, and then prints how many organizations and tasks are there.

If a restore stops halfway, do not keep serving that database. Restore a file you trust, or delete the `postgres_data` volume and start clean. A clean start rebuilds the tables and the demo data. The rule that protects audit rows is inside the backup. If the restore complains about a dependency, drop and recreate the database, then restore again before letting people in.

## RPO

The newest safe point is the last successful `backup.sh`. Changes are not shipped somewhere else as they happen. If you run the script once a day, you can lose up to a day of work. Run it as often as that loss is acceptable.

## RTO

The time to stop the API and the worker, run `restore.sh`, and start them again. On the demo database that is a few minutes. A bigger database takes as long as the restore itself needs.

## PostgreSQL failure

`/ready` answers "not ready", and Compose marks the API unhealthy. Reading and writing tasks fails. Redis may still hold old dashboard numbers, and those numbers are not used as a stand-in for the database. When PostgreSQL is back, restart the API if it did not recover on its own. If the data files are gone, restore the latest backup. If the volume was deleted, the next start rebuilds the demo, which replaces anything that was not in a backup.

## Redis failure

Creating and editing tasks continues. One-time create keys stay in PostgreSQL. The dashboard is calculated from the database. The speed limit allows the change and logs a warning. `/ready` stays healthy and reports Redis as down, because the tasks themselves are safe. The worker cannot send the wake-up nudge. Waiting jobs stay `PENDING` and are picked up when Redis returns. No task is lost, because the job row was saved together with the task.

## Worker failure

The API keeps taking requests and writing jobs. Starting the worker again continues the ones still marked `PENDING`. A job that fails waits 5 seconds, then 15, then 45, then 2 minutes. After 5 tries it is `DEAD`, and a copy is added to `jobs:dlq`. Dead jobs are not tried again on their own. A delivery is recorded by the job id, so a retry does not send the same notice twice.

## Application recovery

Compose restarts the API and the worker unless you stopped them. Health checks call `/health`. The API is not treated as healthy until that answers, and the screens wait for a healthy API. To undo a bad release, rebuild the previous image and run `docker compose up -d`. Table changes live in the versioned SQL files, so restarting the application does not rebuild the tables.
