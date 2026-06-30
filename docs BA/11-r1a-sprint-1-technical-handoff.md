# R1A Sprint 1 Technical Handoff

> Phiên bản cập nhật: `v2.1 - Architecture version/convention lock - 2026-06-29`.
> Baseline kỹ thuật hiện hành: `Java + Spring Boot ecosystem`, `PostgreSQL`, `Flyway`, `Spring Data JPA/Hibernate`.
> Ghi chú: file này đã được đồng bộ theo quyết định chọn Spring Boot và MAR-ARCH-1.0; development commitment vẫn phụ thuộc sign-off `SP1-D01` đến `SP1-D10`.
## 1. Trạng thái tài liệu

| Thuộc tính | Giá trị |
|---|---|
| Tên tài liệu | R1A Sprint 1 Technical Handoff |
| Vai trò tài liệu | Handoff cho Tech Lead/SA/FE/BE/QA trước Sprint 1 |
| Nguồn baseline | `10-r1a-sprint-1-ready-package.md`, `05-r1a-api-contract.md`, `06-r1a-db-schema-erd.md`, `07-r1a-wireframe-checklist.md` |
| Trạng thái | Draft for technical kickoff, not yet committed |
| Ngày lập | 2026-06-29 |

Tài liệu này gom phần kỹ thuật cần chốt để bắt đầu Sprint 1. Đây không thay thế technical design chi tiết, nhưng đủ để Tech Lead phân việc, kiểm tra dependency và tránh hiểu sai phạm vi.

## 2. Sprint 1 technical objective

Sprint 1 tạo lớp foundation cho R1A:

```text
Tenant
-> Branch/User/Role
-> Permission matrix
-> Language/Program/Course catalog
-> ImportBatch/ImportRow foundation
-> Admin setup UI shell
```

Sprint 1 không claim các chức năng lead import preview, dedup, opportunity, pipeline, assignment hoặc SLA là đã hoàn tất.

Selected technical baseline for this handoff:

| Area | Decision |
|---|---|
| Backend ecosystem | Java + Spring Boot ecosystem |
| Database | PostgreSQL |
| Migration | Flyway |
| Data access | Spring Data JPA/Hibernate; dùng native query khi cần tối ưu report/JSONB |
| API style | REST under `/api/v1` |
| MAR baseline doc | `architecture/MAR_ARCHITECTURE_VERSION_CONVENTION.md` / MAR-ARCH-1.0 |

## 3. Workstream ownership đề xuất

| Workstream | Owner chính | Involved | Output |
|---|---|---|---|
| DB migration | Tech Lead/BE | SA, QA | Migration scripts, seed data |
| API foundation | BE | Tech Lead, QA | Tenant/config/permission/catalog/import history APIs |
| Permission enforcement | BE | SA, FE, QA | API-level authorization baseline |
| Admin setup UI | FE | UX, BE, QA | Tenant/branch/user/permission screens |
| Catalog UI | FE | UX, BE, QA | Language/program/course screens |
| QA pack | QA | BA, BE, FE | API/UI test cases and demo script |

## 4. Sprint 1 scope lock

### 4.1. In scope

| Area | Included |
|---|---|
| Tenant | Create/update, timezone, currency, status |
| Branch | Create/update, active/inactive, tenant scope |
| User/role | Create/update, role enum, branch assignment |
| Permission | Permission matrix, guardrails, API enforcement |
| Catalog | Language, program, course CRUD baseline |
| Import foundation | ImportBatch, ImportRow, import history/error storage foundation |
| UI | Admin setup and catalog management screens |
| Minimal audit | Permission changes and sensitive setup changes if team confirms |

### 4.2. Explicitly out of scope

| Area | Reason |
|---|---|
| Lead import preview/confirm | Sprint 2 |
| CSV parser production-ready | Sprint 2 |
| Website/Meta webhook | Sprint 3 candidate |
| Dedup/customer/opportunity | Sprint 2/3 |
| Pipeline/stage history | Sprint 3 |
| Assignment/SLA | Sprint 4 |
| Payment/enrollment | R1B |
| Dashboard/attribution | R1C |

## 5. Decisions required before kickoff

| ID | Decision | BA recommendation | Required by |
|---|---|---|---|
| HDO-01 | Spring Boot/PostgreSQL/Flyway baseline | Locked by MAR-ARCH-1.0: Java 21, Spring Boot 3.5.x target 3.5.14, PostgreSQL 17, Flyway, JPA/Hibernate, root package `vn.mar`, REST API only | Before DB migration |
| HDO-02 | Team entity in Sprint 1 | Do not add Team entity yet; use branch + role scope. `TEAM` scope is reserved/disabled in Sprint 1 unless Team entity is added. | Before user schema |
| HDO-03 | Minimal AuditLog in Sprint 1 | Add `audit_logs` minimal table/service now because permission changes need audit | Before permission story |
| HDO-04 | GET/list APIs for FE | Include GET/list/detail APIs even if baseline only listed POST/PATCH | Before FE integration |
| HDO-05 | Import foundation depth | Sprint 1 stores batch/rows/history only; no preview parser | Before import story |
| HDO-06 | Seed data | Split system seed, demo seed and test seed; do not put demo tenant/users into production migration by default | Before QA/demo |
| HDO-07 | Auth integration | Confirm whether auth already exists; if not, create R1A-TECH-001 for tenant/auth context baseline | Before UI/API permission tests |
| HDO-08 | Enum convention | DB/API enum keys use `UPPER_SNAKE_CASE`; UI labels are separate/localized | Before API/DB implementation |

## 6. Migration order for Sprint 1

Recommended order:

1. `tenants`.
2. `branches`.
3. `users`.
4. `user_branches`.
5. `permission_profiles`.
6. `audit_logs` minimal.
7. `languages`.
8. `programs`.
9. `courses`.
10. `import_batches`.
11. `import_rows`.

Reason:

- Tenant is parent for all data.
- User/role/permission must exist before protected APIs.
- Catalog must exist before lead import maps language/program in Sprint 2.
- Import batch foundation can be built after permission and tenant scope.

## 7. DB handoff checklist

### 7.1. Required tables

| Table | Sprint 1 | Notes |
|---|---|---|
| tenants | Must | Parent tenant boundary |
| branches | Must | Branch scope and future assignment |
| users | Must | Role/user foundation |
| user_branches | Must | Many-to-many user/branch |
| permission_profiles | Must | Role/function/scope permission |
| audit_logs | Must | Minimal append-only for permission and sensitive setup changes |
| languages | Must | Catalog |
| programs | Must | Catalog child of language |
| courses | Must | Catalog child of program |
| import_batches | Must | Import foundation |
| import_rows | Must | Import row/error foundation |

### 7.2. Required enums/lookups

| Enum/lookup | Values |
|---|---|
| tenant_status | ACTIVE, INACTIVE |
| role_code | CEO, ADMIN, MARKETING, SALES_LEAD, ADVISOR, CSKH, FINANCE |
| access_level | NONE, VIEW, CREATE, UPDATE, MANAGE |
| permission_scope | TENANT, BRANCH, OWN, NONE; TEAM reserved/disabled in Sprint 1 unless Team entity is added |
| import_type | LEAD |
| lead_source_type | CSV, GOOGLE_SHEET, WEBSITE_FORM, META_LEAD_ADS, MANUAL, OTHER |
| import_batch_status | DRAFT, PREVIEWED, CONFIRMED, COMPLETED, FAILED, CANCELLED |
| import_row_status | VALID, ERROR, DUPLICATE, SKIPPED, IMPORTED |

Convention:

- DB/API persist enum keys as `UPPER_SNAKE_CASE`.
- FE renders localized labels separately.

### 7.3. Critical indexes and constraints

| Table | Index/constraint | Purpose |
|---|---|---|
| tenants | PK tenant_id; index status | Tenant lookup |
| branches | unique active branch name per tenant | Prevent duplicate active branch |
| users | unique email per tenant where email not null | Prevent duplicate login/contact identity |
| users | index tenant_id, role_code, status | Role/scope filtering |
| user_branches | unique user_id + branch_id | Prevent duplicate assignment |
| permission_profiles | unique tenant_id + role_code + function_code | One permission config per role/function |
| languages | unique active language name per tenant | Catalog quality |
| programs | unique active program name per tenant/language | Catalog quality |
| courses | check tuition_gross >= 0 | Prevent invalid tuition |
| import_batches | index tenant_id, import_type, status, imported_at desc | Import history |
| import_rows | index tenant_id, import_batch_id, row_status | Error/preview query |
| audit_logs | index tenant_id, entity_type, entity_uuid/entity_id_text, created_at desc | Traceability |

## 8. API handoff checklist

### 8.1. Required API groups

| Group | Endpoint baseline | Ticket |
|---|---|---|
| Tenant | `POST /api/v1/tenants`, `PATCH /api/v1/tenants/{tenant_id}`, `GET /api/v1/tenants/{tenant_id}` | R1A-BE-001 |
| Branch | `POST /api/v1/branches`, `PATCH /api/v1/branches/{branch_id}`, `GET /api/v1/branches`, `GET /api/v1/branches/{branch_id}` | R1A-BE-002 |
| User | `POST /api/v1/users`, `PATCH /api/v1/users/{user_id}`, `GET /api/v1/users`, `GET /api/v1/users/{user_id}` | R1A-BE-002 |
| Permission | `GET /api/v1/permissions/matrix`, `PATCH /api/v1/permissions/matrix` | R1A-BE-003 |
| Language | `POST /api/v1/languages`, `PATCH /api/v1/languages/{language_id}`, `GET /api/v1/languages` | R1A-BE-004 |
| Program | `POST /api/v1/programs`, `PATCH /api/v1/programs/{program_id}`, `GET /api/v1/programs` | R1A-BE-004 |
| Course | `POST /api/v1/courses`, `PATCH /api/v1/courses/{course_id}`, `GET /api/v1/courses` | R1A-BE-004 |
| Import history | `GET /api/v1/imports/leads`, `GET /api/v1/imports/leads/{batch_id}`, `GET /api/v1/imports/leads/{batch_id}/errors` | R1A-BE-005 |

GET/list APIs are explicitly included for FE integration even when earlier API baseline focused on create/update.

### 8.2. Standard API behavior

| Behavior | Requirement |
|---|---|
| Tenant context | Every request uses tenant context from auth/session/header |
| Authorization | API must enforce permission, not only UI |
| Validation errors | Return field/code/message |
| Cross-tenant access | Return 403 or 404 according to security policy |
| Empty lists | Return empty data array, not error |
| Status inactive | Entity remains queryable but not selectable for new downstream usage |
| Audit-sensitive update | Requires reason if configured |

Auth/session contract for Sprint 1:

- Request context must expose `tenant_id`, `actor_id`, `role_code` and permission claims or resolvable permission profile.
- Missing/invalid authentication returns 401.
- Authenticated but unauthorized action returns 403.
- Cross-tenant resource access is blocked by service/API guard, not only by UI filters.
- If production auth is not ready, implement R1A-TECH-001 dev/test auth fixture with the same context shape.

### 8.3. Error code minimum

| Code | Applies to |
|---|---|
| UNAUTHENTICATED | Missing/invalid auth/session |
| VALIDATION_ERROR | Missing/invalid fields |
| PERMISSION_DENIED | Role lacks permission |
| TENANT_INACTIVE | Tenant inactive where active required |
| DUPLICATE_ACTIVE_BRANCH | Branch duplicate |
| DUPLICATE_USER_EMAIL | User email duplicate in tenant |
| INVALID_PARENT_STATUS | Program under inactive language, course under inactive program |
| NEGATIVE_TUITION | Course tuition < 0 |
| INVALID_PERMISSION_GUARDRAIL | Disallowed permission change |
| IMPORT_BATCH_NOT_FOUND | Import batch not found in tenant |

## 9. FE handoff checklist

### 9.1. Route proposal

| Route | Screen | Ticket |
|---|---|---|
| `/admin/tenant` | Tenant setup | R1A-FE-001 |
| `/admin/branches` | Branch management | R1A-FE-001 |
| `/admin/users` | User and role management | R1A-FE-001 |
| `/admin/permissions` | Permission matrix | R1A-FE-001 |
| `/admin/catalog/languages` | Language list/form | R1A-FE-002 |
| `/admin/catalog/programs` | Program list/form | R1A-FE-002 |
| `/admin/catalog/courses` | Course list/form | R1A-FE-002 |
| `/admin/imports/leads` | Import history foundation | R1A-FE-001 or later FE item, depending capacity |

### 9.2. Screen requirements

| Screen | Required states |
|---|---|
| Tenant setup | Empty/default, validation error, save success, save failed, inactive warning |
| Branch management | Empty list, loading, create/edit, duplicate error, inactive row |
| User management | Empty list, create/edit, duplicate email error, inactive user |
| Permission matrix | Loading, guardrail disabled state, save reason, save success/error |
| Catalog language | Empty, create/edit, inactive, duplicate error |
| Catalog program | Filter by language, inactive parent blocked, create/edit |
| Catalog course | Filter by program, negative tuition error, inactive parent blocked |
| Import history | Empty list, list, batch detail, error rows if any |

### 9.3. FE guardrails

| Guardrail | UI behavior |
|---|---|
| User lacks permission | Hide action or show disabled with reason |
| Advisor export data | Always unavailable |
| Marketing payment write | Always unavailable |
| Inactive parent catalog | Disable selection for new child |
| Tenant inactive | Show warning |
| Import preview not in Sprint 1 | Do not show working preview/confirm as completed feature |

## 10. Seed data handoff

### 10.1. System seed

| Seed group | Values |
|---|---|
| Roles | CEO, ADMIN, MARKETING, SALES_LEAD, ADVISOR, CSKH, FINANCE |
| Permissions | Default P0 matrix from BA docs |
| Enum lookup | UPPER_SNAKE_CASE keys where lookup table is used |

### 10.2. Demo seed

Demo seed is for local/demo/staging only, not production migration by default.

| Seed group | Values |
|---|---|
| Languages | English, Japanese, Chinese |
| Programs | IELTS, English Communication, JLPT N5, JLPT N4, HSK, Chinese Communication |
| Tenant demo | ABC Language Center |
| Branch demo | Hà Nội - Cầu Giấy |
| Users demo | Admin, Marketing, Sales Lead, Advisor |
| Courses | IELTS Foundation, JLPT N5 Foundation, HSK Basic |
| ImportBatch fixture | Demo import history foundation |
| ImportRows fixture | Demo error rows without real parser |

### 10.3. Test seed

| Seed | Purpose |
|---|---|
| Duplicate branch | Validate duplicate active branch handling |
| Duplicate user email | Validate unique email per tenant |
| Inactive parent language | Validate child create/update guard |
| Negative tuition | Validate course amount rule |
| Permission guardrail | Validate unauthorized role/action returns 403 |

## 11. Test data proposal

### 11.1. Tenant/config

| Data | Value |
|---|---|
| Tenant | ABC Language Center |
| Timezone | Asia/Ho_Chi_Minh |
| Currency | VND |
| Branch | Hà Nội - Cầu Giấy |
| Admin | admin@abc.test |
| Advisor | advisor01@abc.test |

### 11.2. Catalog

| Language | Program | Course | Tuition |
|---|---|---|---|
| English | IELTS | IELTS Foundation | 5000000 |
| Japanese | JLPT N5 | JLPT N5 Foundation | 4500000 |
| Chinese | HSK | HSK Basic | 4000000 |

### 11.3. Negative test data

| Scenario | Data |
|---|---|
| Duplicate branch | Hà Nội - Cầu Giấy |
| Duplicate user email | advisor01@abc.test |
| Inactive parent language | Japanese inactive, create JLPT N4 |
| Negative tuition | -1000000 |
| Permission guardrail | Advisor export data |

## 12. QA handoff

### 12.1. API test pack

| Pack | Test focus |
|---|---|
| Tenant API | Create, update inactive, missing name, unauthorized |
| Branch API | Create, duplicate active name, inactive |
| User API | Create role/branch, duplicate email, inactive |
| Permission API | View/update matrix, guardrail block, audit |
| Catalog API | Language/program/course create/update, inactive parent, negative tuition |
| Import foundation API | List batches, batch detail, errors, tenant scope |

### 12.2. UI smoke pack

| Screen | Smoke checks |
|---|---|
| Tenant setup | Defaults, validation, save |
| Branch | Empty, add, duplicate error |
| User | Add Advisor, inactive status |
| Permission | Matrix loads, guardrail disabled |
| Catalog | Add language/program/course |
| Import history | Empty/list state |

### 12.3. Security/tenant isolation checks

| Check | Expected |
|---|---|
| User tenant A reads tenant B branch | Blocked |
| User tenant A edits tenant B catalog | Blocked |
| Non-Admin updates permission | 403 |
| Advisor opens admin setup | Blocked or read-only according to policy |

## 13. Demo script for kickoff/end of sprint

```text
1. Admin logs in.
2. Admin creates tenant ABC Language Center.
3. Admin creates branch Hà Nội - Cầu Giấy.
4. Admin creates Advisor user and assigns branch.
5. Admin opens permission matrix and confirms guardrails.
6. Admin creates English language.
7. Admin creates IELTS program.
8. Admin creates IELTS Foundation course.
9. Admin opens import history foundation.
10. System shows no import preview/confirm yet, because that starts Sprint 2.
```

Demo pass:

- All data is tenant-scoped.
- API-level permission works.
- UI validation works.
- Catalog can be configured without hard-code.
- Import foundation is present but not oversold as full import.

## 14. Release gate for Sprint 1

| Gate | Pass condition |
|---|---|
| DB migration | Runs clean on fresh environment |
| Seed data | System seed available; demo/test seed loaded only in agreed environment |
| API tests | P0 API tests pass |
| UI smoke | Admin setup and catalog screens usable |
| Permission | Unauthorized action blocked at API |
| Tenant isolation | Cross-tenant access blocked |
| Audit minimal | Permission and sensitive setup changes are logged in minimal audit_logs |
| Scope integrity | No claim that lead import/dedup/opportunity is complete |

## 15. Tech Lead kickoff checklist

Before coding starts:

- Confirm project bootstrap follows `MAR_ARCHITECTURE_VERSION_CONVENTION.md` / MAR-ARCH-1.0.
- Confirm Team entity is deferred and TEAM scope is reserved/disabled.
- Confirm minimal AuditLog is Must in Sprint 1.
- Confirm auth/session tenant context.
- Confirm GET/list endpoint naming.
- Confirm DB/API enum convention is `UPPER_SNAKE_CASE`.
- Confirm FE route structure.
- Confirm system/demo/test seed data owner.
- Confirm whether import batch fixtures are needed for UI demo.
- Confirm API error envelope.
- Confirm QA automation scope.

## 16. Handoff summary

Sprint 1 is a foundation sprint. The most important technical risk is not missing a screen; it is building foundation shortcuts that break later R1A work. Keep these rules:

- Tenant isolation everywhere.
- API-level permission enforcement.
- Config entities use `ACTIVE`/`INACTIVE`, not hard delete.
- Catalog must remain configurable, not hard-coded by exam name.
- Import foundation stores traceable batch/row data but does not pretend to be full import.
- UI should expose only what Sprint 1 actually supports.
