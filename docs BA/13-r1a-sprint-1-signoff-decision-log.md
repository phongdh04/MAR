# R1A Sprint 1 Sign-off Checklist & Decision Log

> Phiên bản cập nhật: `v2.2 - Dev baseline freeze - 2026-06-30`.
> Baseline kỹ thuật hiện hành: `Java 21 + Spring Boot ecosystem`, `PostgreSQL 17`, `Flyway`, `Spring Data JPA/Hibernate`, `Docker Compose local/QA`.
> Ghi chú: technical baseline đã freeze để chuyển sang dev kickoff/backend bootstrap; release acceptance vẫn phụ thuộc implementation evidence và QA gate.
## 1. Trạng thái tài liệu

| Thuộc tính | Giá trị |
|---|---|
| Tên tài liệu | R1A Sprint 1 Sign-off Checklist & Decision Log |
| Vai trò tài liệu | Checklist chốt trước khi commit/code Sprint 1 |
| Nguồn baseline | `10-r1a-sprint-1-ready-package.md`, `11-r1a-sprint-1-technical-handoff.md`, `12-r1a-sprint-1-qa-acceptance-pack.md` |
| Trạng thái | Baseline frozen - Ready for dev kickoff/backend bootstrap |
| Ngày lập | 2026-06-29 |

Tài liệu này là trang chốt nhanh cho PO, Tech Lead, SA, QA và BA. Khi các mục bắt buộc được approve, Sprint 1 có thể chuyển từ planning sang development.

## 2. Sprint 1 decision summary

| Hạng mục | Trạng thái đề xuất |
|---|---|
| Sprint scope | Foundation setup only |
| Development approval | Approved for backend bootstrap and Sprint 1 foundation implementation planning |
| Sprint commitment | Technical baseline frozen; ticket owner/estimate still handled in dev kickoff package |
| Must-have before code | Docker PostgreSQL local/QA env, Spring Boot bootstrap, Flyway baseline, auth/error/request-id skeleton |
| Main risk | Permission/tenant isolation nếu chỉ làm UI-level hoặc không có request context/test evidence |

## 3. Required approvers

| Role | Người/nhóm approve | Bắt buộc? | Nội dung approve |
|---|---|---|---|
| PO/Product Owner | TBD | Yes | Scope, demo path, non-goals |
| Tech Lead | TBD | Yes | API, migration, dependency, sprint feasibility |
| Solution Architect | TBD | Yes | DB/schema direction, tenant isolation, permission architecture |
| QA Lead | TBD | Yes | Test scope, release gate, defect severity |
| UX/FE Lead | TBD | Should | Admin setup UI, catalog UI, route/screen approach |
| BA | Senior BA | Yes | Requirement traceability and scope integrity |

## 4. Kickoff decisions to close

| ID | Decision | BA recommendation | Owner | Status | Final decision | Impact if not closed |
|---|---|---|---|---|---|---|
| SP1-D01 | Spring Boot/PostgreSQL/Flyway version and convention | Java 21, Spring Boot 3.5.x exact patch verified during bootstrap, PostgreSQL 17, Flyway, JPA/Hibernate, root package `vn.mar`, REST API only, no Thymeleaf. Local/QA DB runs by Docker Compose. | Tech Lead/SA | Approved / Frozen | MAR-CONV-1.1 + Docker Compose baseline | BE can start backend bootstrap with Docker PostgreSQL and Flyway baseline |
| SP1-D02 | Team entity in Sprint 1 | Defer Team entity. Use branch + role scope first. Reserve `TEAM` scope in permission model but keep disabled until later. | PO/SA | Approved / Frozen | Team deferred; branch + role scope used in Sprint 1 | Avoid overbuilding user/permission model |
| SP1-D03 | Minimal AuditLog in Sprint 1 | Minimal audit is mandatory because permission and sensitive setup changes must be auditable. BA term `AuditLog`; DB table `audit_events`; Java entity `AuditEvent`. | Tech Lead/QA | Approved / Frozen | Include minimal `audit_events` table/service in Sprint 1 | Permission Matrix cannot be accepted safely without audit |
| SP1-D04 | GET/list APIs for FE | Include GET/list/detail APIs for tenant, branch, user, permission, catalog and import history. | Tech Lead/FE Lead | Approved / Frozen | GET/list/detail APIs included for Sprint 1 foundation entities | FE and QA can verify setup flows |
| SP1-D05 | Import foundation depth | Sprint 1 stores ImportBatch/ImportRow/history only. No CSV preview/confirm parser. Testability defaults to BE fixture command. | PO/Tech Lead | Approved / Frozen | Store foundation only; fixture command default; internal draft API optional/local-QA only | QA can execute import tests consistently without overselling parser |
| SP1-D06 | Seed data | Split seed into system seed, demo seed and test seed. Demo seed is not production migration data. | Tech Lead/QA | Approved / Frozen | System seed required; demo/test seed repeatable per environment | QA and demo setup repeatable |
| SP1-D07 | Auth and tenant context | Local JWT is default unless platform auth is confirmed before bootstrap. Request context must expose `tenant_id`, `actor_id`, `role_code`. | Tech Lead/SA | Approved / Frozen | `tenant_id`, `actor_id`, `role_code` available in request context | Permission and tenant isolation become testable |
| SP1-D08 | Enum convention | DB/API use `UPPER_SNAKE_CASE`; UI labels are separate and may be localized. | Tech Lead/SA/FE Lead | Approved / Frozen | Adopt `UPPER_SNAKE_CASE` for DB/API enums | FE/BE/DB values stay stable |
| SP1-D09 | API error envelope | Use `{ error: { code, message, details[] }, meta: { request_id } }` for validation, permission and conflict errors. | Tech Lead/QA | Approved / Frozen | Standard error envelope required for Sprint 1 APIs | QA assertions and FE error handling stable |
| SP1-D10 | Import foundation testability | Default executable path: BE fixture command for ImportBatch/ImportRow. Internal draft API only if FE demo truly needs it and it is local/QA-only. | Tech Lead/QA | Approved / Frozen | Fixture script is default before QA run | Import foundation tests executable |

## 5. Scope sign-off checklist

### 5.1. In scope

| Checkbox | Item | Owner |
|---|---|---|
| [ ] | Tenant create/update/status/timezone/currency | PO + Tech Lead |
| [ ] | Branch create/update/active-inactive | PO + Tech Lead |
| [ ] | User create/update/role/branch assignment | PO + Tech Lead |
| [ ] | Permission matrix and API-level authorization | Tech Lead + SA |
| [x] | Local JWT auth/session tenant context baseline nếu platform auth chưa sẵn sàng | Tech Lead + SA |
| [ ] | Language/program/course catalog | PO + Tech Lead |
| [x] | ImportBatch/ImportRow foundation and import history, testability bằng fixture command | PO + Tech Lead |
| [ ] | Admin setup UI shell | PO + UX/FE |
| [ ] | Catalog management UI | PO + UX/FE |
| [ ] | QA/API/UI/security acceptance pack | QA Lead |

### 5.2. Out of scope

| Checkbox | Item | Owner |
|---|---|---|
| [x] | Lead import preview/confirm is not part of Sprint 1 | PO + BA |
| [x] | CSV parser production flow is not part of Sprint 1 | PO + Tech Lead |
| [x] | Website/Meta webhook is not part of Sprint 1 | PO + Tech Lead |
| [x] | Dedup/customer/opportunity is not part of Sprint 1 | PO + BA |
| [x] | Pipeline/assignment/SLA is not part of Sprint 1 | PO + BA |
| [x] | Dashboard/payment/revenue is not part of Sprint 1 | PO + BA |

## 6. Technical sign-off checklist

| Checkbox | Gate | Pass condition | Owner |
|---|---|---|---|
| [x] | Migration order accepted | tenants -> branches/users -> permissions/audit -> catalog -> import foundation | Tech Lead/SA |
| [x] | Tenant isolation design accepted | Every Sprint 1 table/API is tenant-scoped | SA |
| [x] | Permission enforcement accepted | Permission is enforced at API/service layer | Tech Lead/SA |
| [x] | Audit minimal accepted | Permission change and sensitive setup changes must be logged to `audit_events` | Tech Lead/QA |
| [x] | Auth/session context accepted | Request context includes `tenant_id`, `actor_id`, `role_code`; missing auth returns `UNAUTHENTICATED` | Tech Lead/SA/QA |
| [x] | Enum convention accepted | DB/API enums use `UPPER_SNAKE_CASE`; UI labels are separate | Tech Lead/SA/FE Lead |
| [x] | API error envelope accepted | Validation/permission/conflict errors have `code`, `message`, `details[]`, `request_id` | Tech Lead/QA |
| [ ] | FE routes accepted | Admin setup and catalog route plan accepted | FE Lead/PO |
| [x] | Seed data accepted | System/demo/test seed split is accepted and repeatable | Tech Lead/QA |
| [x] | Import foundation boundary accepted | Import history exists, preview/confirm is not claimed complete, and fixture command setup is clear | PO/Tech Lead/QA |

## 7. QA sign-off checklist

| Checkbox | Gate | Pass condition | Owner |
|---|---|---|---|
| [ ] | API P0 tests defined | Tenant, branch, user, permission, catalog, import foundation tests ready | QA Lead |
| [ ] | UI smoke tests defined | Admin setup and catalog screen smoke tests ready | QA Lead/FE |
| [ ] | Security tests defined | Tenant isolation and permission bypass tests ready | QA Lead/SA |
| [ ] | Defect severity accepted | Blocker/Critical/Major rules accepted | QA Lead/PO |
| [ ] | Demo acceptance accepted | PO demo script accepted | PO/QA |
| [ ] | Exit criteria accepted | No Blocker/Critical open; P0 tests pass; release gate signed or waived | PO/QA/Tech Lead |

## 8. Release gate sign-off

| Gate | Owner | Required before Sprint 1 acceptance? | Status | Notes |
|---|---|---|---|---|
| DB migration runs clean | Tech Lead/BE | Yes | Pass condition accepted; actual pending implementation | Docker PostgreSQL 17 + Flyway baseline |
| Seed data available | BE/QA | Yes | Pass condition accepted; actual pending implementation | System seed required; demo/test seed repeatable |
| Auth/session context verified | Tech Lead/SA/QA | Yes | Pass condition accepted; actual pending implementation | Local JWT default unless platform auth is confirmed |
| Enum convention verified | Tech Lead/QA | Yes | Pending | DB/API `UPPER_SNAKE_CASE`; UI labels separate |
| API error envelope verified | Tech Lead/QA | Yes | Pending | Validation, permission and conflict errors covered |
| Import testability verified | BE/QA | Yes | Pass condition accepted; actual pending implementation | Repeatable fixture command is default |
| API P0 tests pass | QA/BE | Yes | Pending | |
| UI smoke passes | QA/FE | Yes | Pending | |
| Permission blocked at API | QA/BE | Yes | Pending | |
| Tenant isolation verified | QA/BE/SA | Yes | Pending | |
| Audit minimal verified | QA/BE | Yes | Pass condition accepted; actual pending implementation | Mandatory `audit_events` for permission and sensitive setup changes |
| PO demo path passes | PO/QA | Yes | Pending | |
| Scope integrity verified | PO/BA | Yes | Pending | No Sprint 2 feature claimed complete |

## 9. Go/No-Go criteria

### Go to Sprint 1 development

All must be true:

- Active Sprint 1 P0 decisions have final decisions.
- Sprint scope and non-goals are accepted.
- Tech Lead accepts migration/API/permission approach.
- QA accepts test scope and release gate.
- PO accepts demo path.
- No unresolved blocker remains in Sprint 1 ticket scope.

### No-Go / hold development

Hold if any is true:

- MAR-CONV-1.1/MAR-ARCH baseline not reflected in project bootstrap.
- Auth/tenant context unclear.
- Permission enforcement only planned at UI level.
- Team entity decision blocks user model.
- Enum convention is not accepted across DB/API/FE.
- API error envelope is not accepted.
- Import foundation testability path is not executable by QA.
- Audit event is not included while permission matrix can be changed.
- PO expects lead import preview/confirm inside Sprint 1.
- QA cannot test tenant isolation.
- Demo path not accepted by PO.

## 10. Decision log update protocol

| Rule | Description |
|---|---|
| One owner per decision | Each decision has one accountable owner |
| No silent changes | Any scope/API/DB change after sign-off must be recorded |
| Change impact required | Change must state impact on BE, FE, QA, timeline |
| BA traceability | BA updates linked docs if decision changes requirement |
| Re-sign if critical | Changes to tenant isolation, permission, audit, enum, API error envelope or import scope require re-sign-off |

## 11. Sign-off table

| Role | Name | Decision | Date | Notes |
|---|---|---|---|---|
| PO/Product Owner | Project owner / User | Baseline scope accepted for foundation | 2026-06-30 | Docker selected for local/QA implementation baseline |
| Tech Lead | Tech Lead role | Technical baseline frozen | 2026-06-30 | Java 21, Spring Boot 3.5.x, PostgreSQL 17, Flyway, Docker Compose |
| Solution Architect | SA role | Architecture baseline frozen | 2026-06-30 | Modular monolith, REST API, tenant isolation, `audit_events`, `permission_profiles` |
| QA Lead | QA role | Pass conditions accepted; actual result pending implementation | 2026-06-30 | Fixture command default for import foundation |
| UX/FE Lead | TBD | Pending | TBD | |
| BA | Senior BA | Pending | TBD | |

## 12. BA recommendation

BA recommendation at current state:

| Item | Recommendation |
|---|---|
| Go to development now | Yes for backend bootstrap and Sprint 1 foundation implementation setup |
| Go to sprint planning | Yes |
| Main action before code | Create dev kickoff package, then bootstrap Spring Boot + Docker PostgreSQL + Flyway |
| Minimum acceptable Sprint 1 outcome | Foundation setup works end-to-end with API-level permission and tenant isolation |

Baseline freeze result: Sprint 1 can move from `Draft for planning` to `Ready for development commitment` for backend bootstrap/foundation implementation. Actual release gate result remains pending until code, migration and QA evidence exist.
