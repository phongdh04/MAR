# R1A Dev Kickoff Package - Backend Bootstrap & Foundation

> Phiên bản: `v1.0 - Dev kickoff package - 2026-06-30`  
> Vai trò: gói kickoff triển khai sau baseline freeze.  
> Nguồn baseline: `18-r1a-dev-baseline-freeze.md`, `our architecture/README.md`, `our architecture/01`-`12`.  
> Kết luận: **Ready to start backend bootstrap**.  
> Lưu ý: mọi code/migration/test phải tuân theo MAR conventions; nếu convention và ticket mâu thuẫn, dừng lại và raise decision trước khi code tiếp.

## 1. Mục tiêu kickoff

Dev kickoff này chuyển Sprint 1 từ baseline freeze sang implementation.

Mục tiêu gần nhất:

- Bootstrap backend Spring Boot theo `vn.mar`.
- Dựng Docker Compose PostgreSQL 17 local/QA.
- Tạo database rỗng `mar`.
- Cấu hình profile `local`, `qa`, `prod`, `test`.
- Tạo Flyway baseline migration.
- Implement request id, error envelope, Local JWT/request context.
- Chạy vertical slice đầu tiên: Tenant/Branch/User foundation.

Không phải mục tiêu của kickoff này:

- Không làm import parser/preview/confirm production.
- Không làm dedup/customer/opportunity/pipeline/SLA.
- Không bật Redis/Kafka/object storage production.
- Không tạo schema tay trong DB.
- Không bỏ qua security để test cho nhanh.

## 2. Convention bắt buộc

| Chủ đề | Convention phải theo |
|---|---|
| Architecture/root package/stack | `our architecture/01-architecture-baseline.md` |
| Package/layer/naming | `our architecture/02-coding-package-convention.md` |
| REST API/envelope/pagination | `our architecture/03-rest-api-convention.md` |
| DB/Flyway/entity/repository | `our architecture/04-database-flyway-convention.md` |
| Auth/authz/tenant context | `our architecture/05-security-auth-authz-convention.md` |
| Error/i18n/error code | `our architecture/06-exception-error-i18n-convention.md` |
| Logging/request id/MDC/metrics | `our architecture/07-logging-observability-convention.md` |
| Audit events | `our architecture/08-audit-convention.md` |
| Testing/CI/quality gate | `our architecture/09-testing-quality-convention.md` |
| Cache/async/scheduler | `our architecture/10-cache-async-scheduler-convention.md` |
| Import/file/storage | `our architecture/11-import-file-storage-convention.md` |
| PR/release/workflow | `our architecture/12-dev-workflow-release-convention.md` |

Hard rules:

- Code root package: `vn.mar`.
- API base path: `/api/v1`.
- DB schema tạo bằng Flyway, không tạo tay.
- PostgreSQL 17 chạy bằng Docker Compose local/QA.
- Local JWT default nếu chưa có platform auth.
- `permission_profiles` là source of truth Sprint 1.
- Audit table là `audit_events`, không tạo `audit_logs`.
- Error response dùng `{ error: { code, message, details[] }, meta: { request_id } }`.
- `RequestIdFilter` là source duy nhất cho request id.
- Import Sprint 1 dùng fixture command, không claim parser/preview/confirm.

## 3. Vai trò và owner

| Vai trò | Owner hiện tại | Trách nhiệm kickoff |
|---|---|---|
| Tech Lead | TBD | Chốt bootstrap, review PR, giữ convention, approve dependency |
| Solution Architect | TBD | Review DB/security/tenant/audit decisions |
| Backend bootstrap owner | TBD | Spring Boot skeleton, Maven, profiles, Docker Compose, health check |
| Backend DB owner | TBD | Flyway migration, PostgreSQL schema, seed baseline |
| Backend security owner | TBD | Local JWT, request context, permission guardrail |
| Backend domain owner | TBD | Tenant/branch/user/catalog/import foundation |
| QA owner | TBD | Test matrix, evidence format, P0 API/security/tenant tests |
| DevOps/CI owner | TBD | Docker Compose, env var naming, CI commands |
| FE/UX owner | TBD | Consume GET/list/detail APIs, route/screen dependency |
| BA/PO | Senior BA / PO | Scope integrity, demo path, non-goals |

Nếu một người kiêm nhiều vai, PR vẫn phải có checklist đủ theo `12-dev-workflow-release-convention.md`.

## 4. Implementation order

### Phase 0 - Bootstrap repo and environment

| Order | Item | Owner | Output | Evidence |
|---:|---|---|---|---|
| 0.1 | Create backend project skeleton | Backend bootstrap | Spring Boot app, root package `vn.mar` | App compile, health endpoint planned |
| 0.2 | Add Maven Wrapper / Maven config | Backend bootstrap | Reproducible build command | `mvn test` or wrapper equivalent |
| 0.3 | Add Docker Compose PostgreSQL 17 | DevOps/BE | `docker-compose.yml`, env sample | `docker compose up` creates PostgreSQL |
| 0.4 | Create DB config profiles | Backend bootstrap | `local`, `qa`, `prod`, `test` profiles | App boots with local env |
| 0.5 | Add baseline README/env docs | Backend bootstrap | How to run local | No secret committed |

### Phase 1 - Database and migration baseline

| Order | Item | Owner | Output | Evidence |
|---:|---|---|---|---|
| 1.1 | Add Flyway setup | DB owner | `db/migration` enabled | Flyway runs on empty DB |
| 1.2 | Create foundation migration | DB owner/SA | tenants, branches, users, roles, permissions, permission_profiles, audit_events, catalog, import tables | Fresh DB migration pass |
| 1.3 | Add seed strategy skeleton | DB owner/QA | system seed, demo seed, test seed paths | Seed commands documented |
| 1.4 | Add repository integration test baseline | DB owner/QA | PostgreSQL/Testcontainers or integration profile | Migration/repository tests pass |

### Phase 2 - Common API foundation

| Order | Item | Owner | Output | Evidence |
|---:|---|---|---|---|
| 2.1 | Request id + MDC | Backend bootstrap | `RequestIdFilter`, `X-Request-Id`, MDC | Error/log tests assert request id |
| 2.2 | Error envelope | Backend bootstrap | DTOs, `GlobalExceptionHandler`, security handlers | Golden error tests |
| 2.3 | Health/Actuator baseline | Backend bootstrap | Health endpoint, protected metrics policy | Config/security test |
| 2.4 | Logging/masking baseline | Backend bootstrap/QA | Logback, masking utility | No body/token/password logging |

### Phase 3 - Security and tenant foundation

| Order | Item | Owner | Output | Evidence |
|---:|---|---|---|---|
| 3.1 | Local JWT baseline | Security owner | Login/token validation or dev auth fixture | 401/403 tests |
| 3.2 | CurrentUserContext | Security owner | `tenant_id`, `actor_id`, `role_code` available | Context tests |
| 3.3 | Permission profile resolver | Security owner | Permission from DB/cache, not token-only | Permission tests |
| 3.4 | Tenant isolation guard pattern | Security/Domain owner | Repository/service patterns require tenant | Cross-tenant tests |

### Phase 4 - Vertical slice 1

| Order | Item | Owner | Output | Evidence |
|---:|---|---|---|---|
| 4.1 | Tenant API | Domain owner | Create/detail/update/status | Validation, permission, audit tests |
| 4.2 | Branch API | Domain owner | Create/detail/list/status | Duplicate/code/tenant tests |
| 4.3 | User/role/branch assignment | Domain owner/Security | User create/status/branch assignment | Duplicate email, permission, audit tests |
| 4.4 | Permission matrix foundation | Security owner | `permission_profiles` API/service | Guardrail, cache/evict if used, audit tests |
| 4.5 | Audit event foundation | Domain/QA | `audit_events` service/query if in scope | Payload sanitizer, query tenant isolation |

### Phase 5 - Supporting foundation

| Order | Item | Owner | Output | Evidence |
|---:|---|---|---|---|
| 5.1 | Catalog foundation | Domain owner | Language/program/course APIs | Parent inactive, tuition validation |
| 5.2 | Import foundation | Domain owner | Import batch/row history + fixture command | JSONB, tenant isolation, fixture evidence |
| 5.3 | CI gate baseline | CI/QA | `ci-fast`, `ci-integration` commands | Evidence in PR/release note |

## 5. Proposed ticket list

| Ticket | Tên | Owner role | Depends on | Required conventions | Evidence bắt buộc |
|---|---|---|---|---|---|
| R1A-BOOT-001 | Spring Boot backend skeleton | Backend bootstrap | None | `01`, `02`, `12` | Compile, app boots local profile |
| R1A-BOOT-002 | Docker PostgreSQL 17 local/QA | DevOps/BE | None | `04`, `12`, `18` | `docker compose up`, DB `mar` exists |
| R1A-BOOT-003 | Profile/env config baseline | Backend bootstrap | R1A-BOOT-001/002 | `01`, `07`, `12` | local/test profile boot, no secret committed |
| R1A-DB-001 | Flyway baseline migration | DB owner/SA | R1A-BOOT-002 | `04`, `08`, `11` | Fresh DB migrate clean |
| R1A-COMMON-001 | Request id/logging baseline | Backend bootstrap | R1A-BOOT-001 | `06`, `07`, `09` | `X-Request-Id`, `meta.request_id`, MDC clear tests |
| R1A-COMMON-002 | Error envelope baseline | Backend bootstrap | R1A-COMMON-001 | `03`, `06`, `09` | Validation/401/403/404/409 golden tests |
| R1A-SEC-001 | Local JWT/current user context | Security owner | R1A-DB-001 | `05`, `06`, `07`, `09` | 401/403, tenant/actor/role context tests |
| R1A-SEC-002 | Permission profile resolver | Security owner | R1A-SEC-001 | `05`, `08`, `09`, `10` | Permission from DB/cache, update reflected |
| R1A-BE-001 | Tenant foundation | Domain owner | R1A-SEC-001 | `03`, `04`, `05`, `06`, `08`, `09` | Validation, permission, audit, tenant tests |
| R1A-BE-002 | Branch/user/role foundation | Domain owner | R1A-BE-001 | `03`, `04`, `05`, `06`, `08`, `09` | Duplicate, branch assignment, cross-tenant tests |
| R1A-BE-003 | Permission matrix enforcement | Security owner | R1A-SEC-002 | `05`, `06`, `08`, `09`, `10` | Forbidden tests, guardrail, audit |
| R1A-BE-004 | Catalog foundation | Domain owner | R1A-BE-001 | `03`, `04`, `06`, `09` | Parent inactive, duplicate, negative tuition |
| R1A-BE-005 | Import foundation fixture/history | Domain owner/QA | R1A-DB-001, R1A-SEC-001 | `04`, `08`, `09`, `11`, `12` | Fixture command, JSONB, tenant isolation |
| R1A-CI-001 | CI/test gate baseline | QA/CI owner | R1A-BOOT-001 | `09`, `12` | `mvn test`, integration command documented |

## 6. Docker and DB kickoff contract

Backend bootstrap must create:

```text
docker-compose.yml
.env.example
README.md
src/main/resources/application.yml
src/main/resources/application-local.yml
src/main/resources/application-qa.yml
src/main/resources/application-prod.yml
src/main/resources/application-test.yml
src/main/resources/db/migration
```

Local DB contract:

| Config | Value |
|---|---|
| Image | `postgres:17` |
| DB | `mar` |
| User | `mar_app` |
| Port | `5432` |
| Schema | `public` |
| Migration | Flyway on app startup/test command |

Rules:

- `.env.example` can be committed; real `.env.local` must not contain production secret.
- App must not use `ddl-auto=update`.
- Local/QA should use Flyway migrations to create tables.
- If DB container starts but Flyway fails, bootstrap is not accepted.

## 7. Flyway baseline order

Initial migration order follows `04-database-flyway-convention.md`:

1. `tenants`
2. `branches`
3. `users`
4. `roles`
5. `permissions`
6. `permission_profiles`
7. `audit_events`
8. `user_branches`
9. `languages`
10. `programs`
11. `courses`
12. `import_batches`
13. `import_rows`
14. optional system seed tables/data if required for app boot

Naming:

```text
VYYYYMMDD_NN__lower_snake_case_description.sql
```

No manual DB edits are allowed as acceptance evidence.

## 8. Evidence required per PR

Every PR must include:

- Scope.
- API impact or `None`.
- DB/Flyway impact or `None`.
- Permission & tenant isolation impact.
- Audit/logging impact.
- Tests run.
- Risks & rollback.
- Docs updated or `None`.

Minimum commands:

```text
mvn test
```

For DB/security/import/cache/job PR:

```text
mvn verify -Pintegration
```

If command names differ during bootstrap, PR must document equivalent evidence.

## 9. Definition of Ready for first implementation tickets

A ticket can enter implementation only when:

- Convention source of truth is identified.
- DB impact is known.
- API/permission/tenant impact is known.
- Error/audit/logging impact is known.
- Test evidence is known.
- Owner is assigned.
- No unresolved P0 decision affects implementation.

## 10. Definition of Done for first implementation tickets

A ticket is done only when:

- Code compiles.
- Tests required by risk area pass.
- Flyway migration exists if schema changed.
- API uses standard envelope and error contract.
- Protected endpoints enforce permission at API/service layer.
- Tenant-scoped resources are tenant-isolated.
- Audit exists for sensitive setup changes.
- No password/token/raw import row is logged.
- PR evidence follows `12-dev-workflow-release-convention.md`.

## 11. Release gate carried forward

Actual release acceptance is still pending. The following must pass after implementation:

- Fresh Docker PostgreSQL migration.
- App boot smoke.
- P0 API tests.
- Security/permission tests.
- Cross-tenant tests.
- Audit tests.
- Import foundation fixture/history tests.
- Error envelope contract tests.
- No sensitive log/audit payload.

## 12. Next immediate action

After this kickoff package is accepted, start:

```text
R1A-BOOT-001 - Spring Boot backend skeleton
R1A-BOOT-002 - Docker PostgreSQL 17 local/QA
```

Suggested backend repo path for bootstrap decision:

```text
D:\Documents-for-Expert-Design-Database\MAR\mar-backend
```

If a different path is chosen, update this package and the backend README.

