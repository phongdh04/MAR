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

## Convention Notes

- Do not use Hibernate `ddl-auto=update`.
- Do not create schema manually; use Flyway migration files in `src/main/resources/db/migration`.
- Keep API responses in `ApiResponse<T>` or `ErrorResponse` envelope.
- Keep request id propagation through `X-Request-Id`, MDC and `meta.request_id`.
- Keep health public; all business APIs are authenticated by default.
