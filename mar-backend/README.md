# MAR API

Spring Boot backend for MAR Lead-to-Enrollment MVP.

## Baseline

- Java 21
- Spring Boot 3.5.14, under the approved 3.5.x line
- PostgreSQL 17
- Flyway
- REST API under `/api/v1`
- Root package `vn.mar`
- Docker Compose for local PostgreSQL

## Local Run

1. Copy `.env.example` to `.env.local` and adjust local values if needed.
2. Start PostgreSQL:

```powershell
docker compose --env-file .env.local up -d mar-postgres
```

3. Run the API:

```powershell
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

4. Smoke check:

```powershell
curl http://localhost:8080/api/v1/health
```

## Test

```powershell
mvn test
```

The `test` profile excludes datasource, JPA and Flyway auto-configuration for bootstrap-level unit and API smoke tests. PostgreSQL/Testcontainers integration tests start from ticket `R1A-DB-001`.

## Database Migration

Flyway migrations live in:

```text
src/main/resources/db/migration
```

The first Sprint 1 foundation migration is:

```text
V20260630_01__create_sprint_1_foundation.sql
```

Run default tests:

```powershell
.\mvnw.cmd test
```

Run PostgreSQL/Testcontainers migration verification when Docker Desktop is running:

```powershell
.\mvnw.cmd verify -Pintegration
```

## Import Fixture

Seed repeatable Sprint 1 lead import fixture data in local/QA profiles:

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=qa" "-Dspring-boot.run.arguments=--mar.import.fixture.enabled=true --mar.import.fixture.tenant-id=<tenant_uuid> --mar.import.fixture.actor-id=<actor_uuid> --mar.import.fixture.exit-on-complete=true"
```

The fixture creates `ImportBatch`/`ImportRow` history data only. It does not parse files, confirm imports, or create official leads.

## Lead Import Preview

Preview a small CSV lead import synchronously:

```text
POST /api/v1/imports/leads/preview
Content-Type: multipart/form-data

parts:
- file: CSV file, max 1 MB and 500 data rows
- mapping_config: JSON with column_mappings
```

Preview stores `ImportBatch`/`ImportRow` with status `PREVIEWED`, row errors and duplicate candidates. It does not confirm the batch or create official leads.

## Convention Notes

- Do not use Hibernate `ddl-auto=update`.
- Do not create schema manually; use Flyway migration files in `src/main/resources/db/migration`.
- Keep API responses in `ApiResponse<T>` or `ErrorResponse` envelope.
- Keep request id propagation through `X-Request-Id`, MDC and `meta.request_id`.
- Keep health public; all business APIs are authenticated by default.
