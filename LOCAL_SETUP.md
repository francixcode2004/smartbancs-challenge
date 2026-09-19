# SmartBancs local

Requirements: Java 21 and Docker Compose (with Docker running).

1. From the repository root, start PostgreSQL:
   `docker compose up -d --wait`
2. Run `SmartBancsAppApplication` from the IDE. The application uses port 8080.
3. Stop PostgreSQL when finished:
   `docker compose down`

PostgreSQL: localhost:5432, database/user `smartbancs`, local-only password `smartbancs_local`.
Data persists in the Docker volume when running `docker compose down`.
The initialization SQL creates the users and transactions tables. No business endpoints have been implemented yet; an HTTP 404 at `/` is expected.
The remaining directories are placeholders for the technical challenge.

## Recover after an initialization SQL error

The image automatically runs initialization scripts only for an empty data directory.
If a previous SQL script failed, fix it and run these commands in order to keep the existing volume and apply the schema:

```powershell
docker compose up -d --wait
docker compose exec -T postgres psql -U smartbancs -d smartbancs -v ON_ERROR_STOP=1 -f /docker-entrypoint-initdb.d/database.sql
docker compose exec -T postgres psql -U smartbancs -d smartbancs -c '\dt'
```

If PostgreSQL still fails to start, inspect the logs:
`docker compose logs --tail=100 postgres`
