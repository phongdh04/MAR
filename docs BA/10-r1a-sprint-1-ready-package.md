# R1A Sprint 1 Ready Package - Foundation Setup

> Phiên bản cập nhật: `v2.1 - Architecture version/convention lock - 2026-06-29`.
> Baseline kỹ thuật hiện hành: `Java + Spring Boot ecosystem`, `PostgreSQL`, `Flyway`, `Spring Data JPA/Hibernate`.
> Ghi chú: file này đã được đồng bộ theo quyết định chọn Spring Boot và MAR-ARCH-1.0; development commitment vẫn phụ thuộc sign-off `SP1-D01` đến `SP1-D10`.
## 1. Trạng thái tài liệu

| Thuộc tính | Giá trị |
|---|---|
| Tên tài liệu | R1A Sprint 1 Ready Package - Foundation Setup |
| Vai trò tài liệu | Gói ticket dev-ready đề xuất cho Sprint 1 |
| Nguồn baseline | `09-r1a-dev-backlog.md`, `05-r1a-api-contract.md`, `06-r1a-db-schema-erd.md`, `07-r1a-wireframe-checklist.md` |
| Trạng thái | Draft for sprint planning, not yet committed |
| Ngày lập | 2026-06-29 |

Tài liệu này chuyển nhóm Sprint 1 trong R1A backlog thành các ticket có thể estimate. Trạng thái vẫn là draft vì kiến trúc/version/convention đã khóa theo `MAR_ARCHITECTURE_VERSION_CONVENTION.md`, nhưng vẫn cần PO/Tech Lead xác nhận UX final, effort và Sprint sign-off trước khi đưa vào sprint.

## 2. Sprint 1 Goal

Sprint 1 tạo nền hệ thống để các sprint sau có thể nhận lead, dedup, tạo opportunity và vận hành pipeline.

Mục tiêu cụ thể:

- Có tenant foundation với timezone/currency/status.
- Có branch, user, role và scope cơ bản.
- Có permission matrix và API authorization baseline.
- Có language/program/course catalog.
- Có import batch/import row foundation để Sprint 2 làm import preview.
- Có UI shell cho admin setup và catalog management.

## 3. Sprint 1 Non-goals

Sprint 1 chưa làm:

- Lead import preview/confirm đầy đủ.
- Webhook website/Meta.
- Dedup/customer/opportunity.
- Pipeline/stage transition.
- Assignment/SLA.
- Dashboard.
- Payment/enrollment.

## 4. Sprint 1 Candidate Items

| Item ID | Tên | Type | Priority | Sprint readiness |
|---|---|---|---|---|
| R1A-TECH-001 | Auth/session tenant context baseline | Technical | P0 | Required if auth is not already available |
| R1A-BE-001 | Tenant schema and service | Backend | P0 | Ready with MAR-ARCH-1.0 baseline verification |
| R1A-BE-002 | Branch, user and role model | Backend | P0 | Ready with Team decision |
| R1A-BE-003 | Permission matrix enforcement | Backend | P0 | Ready with permission matrix confirmation |
| R1A-BE-004 | Language/program/course catalog | Backend | P0 | Ready |
| R1A-BE-005 | Import batch and import row model | Backend | P0 | Ready with DB schema review |
| R1A-FE-001 | Tenant/branch/user/permission screens | Frontend | P0 | Needs wireframe confirmation |
| R1A-FE-002 | Catalog management screen | Frontend | P0 | Needs UX confirmation |

## 5. Sprint 1 Assumptions

| ID | Assumption | Impact nếu sai |
|---|---|---|
| SP1-A01 | R1A dùng branch + role scope, chưa thêm Team entity trong Sprint 1 | Nếu pilot cần team thật, DB/user scope phải bổ sung `teams`, `user_teams` |
| SP1-A02 | R1A dùng PostgreSQL JSONB cho mapping_config/raw_row | Nếu đổi khỏi PostgreSQL trong tương lai, import foundation cần thiết kế lại lưu trữ JSON |
| SP1-A03 | Sprint 1 chỉ tạo import batch foundation, chưa parse file preview | Nếu PO muốn demo import preview ngay, phải kéo R1A-BE-006 vào sprint |
| SP1-A04 | Permission enforcement làm ở API/service layer, UI chỉ phản ánh quyền | Nếu chỉ làm UI, rủi ro bảo mật cao |
| SP1-A05 | Config entity không hard delete, dùng `ACTIVE`/`INACTIVE` | Nếu cần delete thật, phải chốt audit và referential integrity |
| SP1-A06 | Minimal AuditLog là Must-have trong Sprint 1 | Permission change không được đi qua nếu không có audit append-only |
| SP1-A07 | DB/API enum dùng `UPPER_SNAKE_CASE`, UI label tách riêng | Nếu không chốt, FE/BE/DB dễ lệch enum |
| SP1-A08 | Team scope reserved/disabled trong Sprint 1 nếu chưa có Team entity | Nếu cho chọn Team mà không có schema enforce, permission sẽ sai |

### R1A-TECH-001 - Auth/session tenant context baseline

| Field | Value |
|---|---|
| Type | Technical |
| Priority | P0 |
| Depends on | Spring Boot security/auth decision |
| Sprint readiness | Required if project does not already provide auth/session |

Scope:

- Provide tenant context for every API request.
- Provide actor_id and role/permission claims for API authorization.
- Support dev/test auth fixture if production auth is not ready.
- Document how FE attaches session/JWT/header.

Acceptance:

- API can read tenant_id, actor_id and role from request context.
- Unauthorized request returns 401.
- Authenticated but unauthorized request returns 403.
- Tenant isolation tests can run without manual DB setup.

## 6. Shared Definition of Ready

Một ticket Sprint 1 chỉ được đưa vào sprint khi có đủ:

- Scope rõ theo ticket dưới đây.
- DB impact rõ.
- API endpoint hoặc UI flow rõ.
- Permission rule rõ.
- Validation rule rõ.
- Acceptance criteria testable.
- QA test case tối thiểu.
- Dependency không còn blocker nghiêm trọng.
- Auth/session tenant context rõ hoặc có dev/test fixture.
- Minimal AuditLog path rõ nếu ticket thay đổi permission/config nhạy cảm.
- Enum DB/API dùng `UPPER_SNAKE_CASE`.

## 7. Shared Definition of Done

Một ticket Sprint 1 được xem là done khi:

- Code được merge theo quy trình team.
- Unit/API/UI tests liên quan pass.
- Permission rule được enforce ở API nếu ticket có endpoint.
- Validation error trả message/code rõ.
- Audit log có với action nhạy cảm đã quy định.
- Không phá tenant isolation.
- Có seed hoặc test data tối thiểu nếu ticket cần.
- PO/QA có thể chạy qua acceptance criteria.

## 8. Ticket R1A-BE-001 - Tenant Schema and Service

### Summary

Là Admin, tôi muốn tạo và cập nhật tenant profile để mỗi trung tâm pilot có cấu hình dữ liệu, timezone, currency và trạng thái tách biệt.

### Business value

Tenant là ranh giới dữ liệu gốc. Nếu tenant isolation sai, toàn bộ lead/customer/opportunity/payment sau này sẽ có rủi ro lẫn dữ liệu giữa trung tâm.

### Scope

In scope:

- DB table/model `tenants`.
- API tạo tenant.
- API cập nhật tenant.
- Default timezone và currency.
- Tenant status `ACTIVE`/`INACTIVE`.
- Event `tenant.created`.

Out of scope:

- Multi-tenant billing.
- Tenant subscription.
- Tenant deletion.
- Full tenant settings nâng cao.

### DB impact

Table: `tenants`.

| Column | Required | Rule |
|---|---|---|
| tenant_id | Yes | UUID, PK |
| tenant_name | Yes | Không rỗng |
| timezone | Yes | Default `Asia/Ho_Chi_Minh` |
| default_currency | Yes | Default `VND` |
| status | Yes | `ACTIVE`/`INACTIVE` |
| created_at | Yes | System timestamp |
| updated_at | Yes | System timestamp |

Index/constraint:

- PK `tenant_id`.
- Index `(status)`.
- Constraint `tenant_name <> ''`.

### API impact

Create:

```text
POST /api/v1/tenants
Permission: tenant.manage
```

Update:

```text
PATCH /api/v1/tenants/{tenant_id}
Permission: tenant.manage
```

Detail:

```text
GET /api/v1/tenants/{tenant_id}
Permission: tenant.manage hoặc tenant.view nếu tách quyền view
```

Minimum create request:

```json
{
  "tenant_name": "ABC Language Center",
  "timezone": "Asia/Ho_Chi_Minh",
  "default_currency": "VND",
  "status": "ACTIVE"
}
```

### Validation

| Rule | Expected |
|---|---|
| Missing tenant_name | Reject with validation error |
| Missing timezone | Use `Asia/Ho_Chi_Minh` |
| Missing default_currency | Use `VND` |
| Invalid status | Reject |
| Inactive tenant receiving lead later | Lead intake must reject/park in later tickets |

### Permission

- Only Admin with `tenant.manage`.
- CEO may view later if UI supports; no management in Sprint 1 unless PO confirms.

### Acceptance criteria

- Given Admin submits valid tenant data, when creating tenant, then system creates tenant with timezone/currency/status.
- Given tenant_name is blank, when creating tenant, then system rejects request with validation error.
- Given Admin updates status to Inactive, when saving, then tenant status changes without deleting data.
- Given non-Admin calls tenant create/update, then API returns permission denied.
- Given tenant is created, then `tenant.created` event or equivalent internal hook is emitted/logged.

### Test cases

| Test ID | Scenario | Expected |
|---|---|---|
| SP1-TEN-001 | Create tenant valid | 201, tenant returned |
| SP1-TEN-002 | Create tenant missing name | 400/422 validation error |
| SP1-TEN-003 | Create tenant missing timezone | Default Asia/Ho_Chi_Minh |
| SP1-TEN-004 | Update tenant inactive | Status updated, no data deleted |
| SP1-TEN-005 | Unauthorized user creates tenant | 403 |

### Dependencies

- MAR-ARCH-1.0 architecture baseline verification.
- Auth/role mechanism baseline.

### Open questions

| ID | Question | BA recommendation |
|---|---|---|
| SP1-Q-TEN-001 | Có cần tenant detail GET trong Sprint 1 không? | Nên có nếu FE cần load form edit, nhưng có thể nằm trong same ticket |

## 9. Ticket R1A-BE-002 - Branch, User and Role Model

### Summary

Là Admin, tôi muốn quản lý branch, user và role để hệ thống biết ai thuộc cơ sở nào và ai có thể nhận lead trong các sprint sau.

### Business value

Branch/user/role là nền cho assignment, inbox, permission và SLA. Sprint sau không thể giao lead nếu chưa có user active và branch scope.

### Scope

In scope:

- DB table/model `branches`.
- DB table/model `users`.
- Supporting table `user_branches`.
- Role enum: `CEO`, `ADMIN`, `MARKETING`, `SALES_LEAD`, `ADVISOR`, `CSKH`, `FINANCE`.
- API tạo/cập nhật branch.
- API tạo/cập nhật user.
- `ACTIVE`/`INACTIVE` status.

Out of scope:

- Password/authentication flow nếu hệ thống đã có auth riêng.
- Team entity, trừ khi PO/SA chốt bắt buộc.
- Advanced org chart.

### DB impact

Tables:

- `branches`.
- `users`.
- `user_branches`.

Key rules:

- Branch name active unique trong tenant.
- User email unique trong tenant nếu có.
- User inactive không nhận assignment mới.

### API impact

Branch:

```text
GET /api/v1/branches
GET /api/v1/branches/{branch_id}
POST /api/v1/branches
PATCH /api/v1/branches/{branch_id}
Permission: branch.manage
```

User:

```text
GET /api/v1/users
GET /api/v1/users/{user_id}
POST /api/v1/users
PATCH /api/v1/users/{user_id}
Permission: user.manage
```

Minimum branch request:

```json
{
  "branch_name": "Hà Nội - Cầu Giấy",
  "city": "Hà Nội",
  "address": "Cầu Giấy",
  "status": "ACTIVE"
}
```

Minimum user request:

```json
{
  "full_name": "Trần Thị B",
  "email": "advisor@example.com",
  "phone": "0987654321",
  "role": "ADVISOR",
  "branch_ids": ["uuid"],
  "status": "ACTIVE"
}
```

### Validation

| Rule | Expected |
|---|---|
| Branch name blank | Reject |
| Duplicate active branch name in tenant | Reject |
| User full_name blank | Reject |
| User role blank/invalid | Reject |
| Duplicate email in tenant | Reject |
| Inactive user in assignment pool later | Must be excluded |

### Permission

- Admin manages branch/user.
- Sales Lead may view branch/team later, not manage in Sprint 1 unless PO confirms.

### Acceptance criteria

- Admin can create active branch.
- Admin can create user with role and branch assignment.
- User email duplicate in same tenant is rejected.
- Inactive user remains in system but is marked unavailable for assignment.
- Branch with existing data should be inactive instead of hard-deleted.

### Test cases

| Test ID | Scenario | Expected |
|---|---|---|
| SP1-USR-001 | Create branch valid | Branch created |
| SP1-USR-002 | Create branch missing name | Validation error |
| SP1-USR-003 | Create duplicate branch active | Conflict/validation error |
| SP1-USR-004 | Create Advisor with branch | User created, user_branches linked |
| SP1-USR-005 | Create duplicate email | Conflict/validation error |
| SP1-USR-006 | Set user inactive | User inactive, not deleted |

### Dependencies

- R1A-BE-001.
- Team entity decision.

### Open questions

| ID | Question | BA recommendation |
|---|---|---|
| SP1-Q-USR-001 | Có thêm Team entity trong Sprint 1 không? | Không, dùng branch + role trước; Team scope reserved/disabled nếu chưa có Team entity |
| SP1-Q-USR-002 | User login/auth có nằm trong Sprint 1 không? | Nếu project chưa có auth, dùng R1A-TECH-001 để có tenant/auth context baseline |

## 10. Ticket R1A-BE-003 - Permission Matrix Enforcement

### Summary

Là Admin, tôi muốn cấu hình permission matrix cơ bản để mỗi role chỉ thao tác đúng phạm vi nghiệp vụ.

### Business value

Permission là lớp bảo vệ dữ liệu lead/customer/revenue sau này. R1A phải enforce ở API ngay từ đầu để tránh UI-only security.

### Scope

In scope:

- DB table/model `permission_profiles`.
- Permission code baseline.
- API xem/cập nhật permission matrix.
- Authorization middleware/service.
- Audit permission changes.

Out of scope:

- Field-level permission sâu.
- Custom permission per user.
- Complex policy engine.

### DB impact

Table: `permission_profiles`.

Unique:

- `(tenant_id, role_code, function_code)`.

Key fields:

- role_code.
- function_code.
- access_level.
- scope.

### Permission codes Sprint 1

| Code | Purpose |
|---|---|
| tenant.manage | Manage tenant |
| branch.manage | Manage branch |
| user.manage | Manage user |
| permission.manage | Manage permission matrix |
| catalog.manage | Manage language/program/course |
| lead.import | Prepare import foundation |
| lead.view | View lead later |
| duplicate.manage | Later Sprint 2 |
| opportunity.update | Later Sprint 3 |
| assignment.manage | Later Sprint 4 |

Default Sprint 1 permission seed:

| Role | Baseline |
|---|---|
| ADMIN | tenant.manage, branch.manage, user.manage, permission.manage, catalog.manage, lead.import |
| CEO | tenant.view, catalog.view nếu bật quyền view |
| MARKETING | catalog.view, lead.import nếu PO bật |
| SALES_LEAD | branch.view, user.view, lead.import nếu PO bật |
| ADVISOR | Không có admin setup |
| FINANCE | catalog.view nếu cần |
| CSKH | catalog.view nếu cần |

### API impact

```text
GET /api/v1/permissions/matrix
PATCH /api/v1/permissions/matrix
Permission: permission.manage
```

Minimum update request:

```json
{
  "changes": [
    {
      "role": "SALES_LEAD",
      "function_code": "lead.import",
      "access_level": "CREATE",
      "scope": "BRANCH"
    }
  ],
  "reason": "Pilot setup"
}
```

### Guardrails

| Rule | Expected |
|---|---|
| Advisor export data | Always blocked |
| Marketing write payment | Always blocked |
| Permission update without permission.manage | 403 |
| Permission change | AuditLog required |

### Acceptance criteria

- Admin can view permission matrix.
- Admin can update permission matrix with reason.
- Permission changes are recorded in AuditLog.
- API rejects action when role lacks permission.
- Guardrail permissions cannot be enabled for disallowed roles.

### Test cases

| Test ID | Scenario | Expected |
|---|---|---|
| SP1-PERM-001 | Admin views matrix | Matrix returned |
| SP1-PERM-002 | Admin updates SALES_LEAD lead.import | Update success, audit written |
| SP1-PERM-003 | Advisor tries tenant create | 403 |
| SP1-PERM-004 | Enable Advisor export | Rejected |
| SP1-PERM-005 | Marketing write payment | Rejected or impossible in matrix |

### Dependencies

- R1A-BE-002.
- Minimal AuditLog table/service is required in Sprint 1; full audit reporting can remain later, but permission changes must persist audit rows now.

### Open questions

| ID | Question | BA recommendation |
|---|---|---|
| SP1-Q-PERM-001 | AuditLog phạm vi Sprint 1 là gì? | Minimal audit_logs table + service là Must-have; full audit reporting có thể để sprint sau |

## 11. Ticket R1A-BE-004 - Language, Program, Course Catalog

### Summary

Là Admin, tôi muốn cấu hình language, program và course để lead/opportunity có thể gắn đúng nhu cầu học mà không hard-code IELTS/JLPT/HSK.

### Business value

Catalog là nền cho import lead, assignment và pipeline theo language/program. Nếu hard-code từ đầu, sản phẩm sẽ khó mở rộng sang nhiều trung tâm/ngôn ngữ.

### Scope

In scope:

- DB table/model `languages`.
- DB table/model `programs`.
- DB table/model `courses`.
- CRUD APIs tối thiểu.
- `ACTIVE`/`INACTIVE` status.
- Validation active parent.

Out of scope:

- Pricing rules phức tạp.
- Course schedule/classroom.
- LMS/course content.
- Exam logic hard-coded.

### DB impact

Tables:

- `languages`.
- `programs`.
- `courses`.

Key constraints:

- Active language name unique trong tenant.
- Active program name unique trong tenant + language.
- Course tuition_gross >= 0 nếu có.

### API impact

```text
POST /api/v1/languages
PATCH /api/v1/languages/{language_id}
POST /api/v1/programs
PATCH /api/v1/programs/{program_id}
POST /api/v1/courses
PATCH /api/v1/courses/{course_id}
Permission: catalog.manage
```

### Validation

| Rule | Expected |
|---|---|
| Language name blank | Reject |
| Program under inactive language | Reject |
| Course under inactive program | Reject |
| Tuition gross negative | Reject |
| Inactive catalog item | Not selectable for new lead/opportunity later |

### Acceptance criteria

- Admin can create English, Japanese, Chinese and custom language.
- Admin can create program under active language.
- Admin can create course under active program.
- Program cannot be created under inactive language.
- Course tuition cannot be negative.
- UI/API does not hard-code IELTS/JLPT/HSK.

### Test cases

| Test ID | Scenario | Expected |
|---|---|---|
| SP1-CAT-001 | Create Japanese language | Success |
| SP1-CAT-002 | Create custom language | Success |
| SP1-CAT-003 | Create program under active language | Success |
| SP1-CAT-004 | Create program under inactive language | Reject |
| SP1-CAT-005 | Create course with negative tuition | Reject |
| SP1-CAT-006 | Inactive program | Cannot be selected for new opportunity later |

### Dependencies

- R1A-BE-001.

### Open questions

| ID | Question | BA recommendation |
|---|---|---|
| SP1-Q-CAT-001 | Có cần seed English/Japanese/Chinese trong Sprint 1 không? | Có, nên seed để demo nhanh |
| SP1-Q-CAT-002 | Có cần GET/list APIs trong ticket này không? | Có, FE catalog cần list/detail để vận hành |

## 12. Ticket R1A-BE-005 - Import Batch and Import Row Model

### Summary

Là Marketing/Admin, tôi muốn hệ thống có nền import batch/import row để Sprint 2 có thể preview, validate, confirm import lead mà vẫn truy vết được nguồn dữ liệu.

### Business value

Import là nguồn lead P0 quan trọng nhất. Sprint 1 chưa cần parse file đầy đủ nhưng phải có schema đúng cho preview, error report, duplicate preview và import history.

### Scope

In scope:

- DB table/model `import_batches`.
- DB table/model `import_rows`.
- Import history API baseline.
- Store mapping_config.
- Store row status/error fields.

Out of scope:

- CSV parser hoàn chỉnh.
- Google Sheet connector.
- Lead creation from import.
- Duplicate detection.
- Confirm import.

### DB impact

Tables:

- `import_batches`.
- `import_rows`.

Key fields:

- import_type.
- source_type.
- mapping_config.
- total_rows, valid_count, error_count, duplicate_count.
- row_number, raw_row, normalized_row, row_status, error_code, error_message.

Index:

- `import_batches(tenant_id, import_type, status, imported_at desc)`.
- `import_rows(tenant_id, import_batch_id, row_status)`.
- `import_rows(tenant_id, import_batch_id, row_number)`.

### API impact

Minimum for Sprint 1:

```text
GET /api/v1/imports/leads
GET /api/v1/imports/leads/{batch_id}
GET /api/v1/imports/leads/{batch_id}/errors
```

Optional technical/internal endpoint if useful:

```text
POST /api/v1/imports/leads/draft
```

Note: `POST /imports/leads/preview` is Sprint 2 item `R1A-BE-006`, not Sprint 1 unless team pulls it in.

### Validation

| Rule | Expected |
|---|---|
| Import batch must have tenant_id | Required |
| mapping_config required once preview exists | Required for previewed batches |
| Row belongs to same tenant as batch | Enforced |
| Counts cannot be negative | Reject |
| Status invalid | Reject |

### Acceptance criteria

- System can persist an ImportBatch with mapping_config and count fields.
- System can persist ImportRows linked to ImportBatch.
- Import history API returns batch list scoped by tenant.
- Import detail API returns mapping_config and summary counts.
- Import errors API returns row_number, field/code/message if rows exist.
- No official Lead is created by this ticket.

### Test cases

| Test ID | Scenario | Expected |
|---|---|---|
| SP1-IMP-001 | Create/store import batch fixture | Batch persisted |
| SP1-IMP-002 | Store import rows | Rows linked to batch |
| SP1-IMP-003 | Query import history by tenant | Only tenant batches returned |
| SP1-IMP-004 | Query error rows | Error rows returned with row number/message |
| SP1-IMP-005 | Negative count | Rejected |
| SP1-IMP-006 | Cross-tenant batch row | Rejected or impossible by FK/service |

### Dependencies

- R1A-BE-001.
- R1A-BE-003 for permission.
- DB schema review.

### Open questions

| ID | Question | BA recommendation |
|---|---|---|
| SP1-Q-IMP-001 | Có tạo draft import API trong Sprint 1 không? | Chỉ làm nếu BE cần để test UI shell; preview chính để Sprint 2 |
| SP1-Q-IMP-002 | raw_row dùng JSONB hay text JSON? | PostgreSQL đã chọn; dùng JSONB |

## 13. Ticket R1A-FE-001 - Tenant, Branch, User, Permission Screens

### Summary

Là Admin, tôi muốn có UI cấu hình tenant, branch, user và permission để setup pilot mà không cần can thiệp trực tiếp vào DB.

### Business value

Admin setup UI giúp Sprint 1 có demo thật và giúp Sprint 2/Sprint 3 dùng dữ liệu cấu hình chính thống.

### Scope

In scope:

- Tenant setup form.
- Branch list/form.
- User list/form.
- Permission matrix screen.
- Inline validation.
- Loading/empty/error states cơ bản.

Out of scope:

- Advanced user invite flow.
- Password reset.
- Bulk user import.
- Complex permission policy editor.

### UI screens

| Screen | Source | Required |
|---|---|---|
| Tenant setup | WF-01 | Yes |
| Branch management | WF-02 | Yes |
| User and role management | WF-03 | Yes |
| Permission matrix | WF-04 | Yes |

### API dependencies

- `GET/POST/PATCH /api/v1/tenants`.
- `GET/POST/PATCH /api/v1/branches`.
- `GET/POST/PATCH /api/v1/users`.
- `GET/PATCH /api/v1/permissions/matrix`.

### UI validation

| Field/action | Expected |
|---|---|
| Tenant name blank | Inline error |
| Branch name blank | Inline error |
| User full_name blank | Inline error |
| User role blank | Inline error |
| Permission disallowed guardrail | Disabled or blocked with message |
| Save API error | Show clear error |

### Acceptance criteria

- Admin can create/update tenant from UI.
- Admin can create/update branch from UI.
- Admin can create/update user and assign role/branch from UI.
- Admin can view and update permission matrix.
- UI hides/disables actions when user lacks permission.
- Save success and save failure states are visible.

### Test cases

| Test ID | Scenario | Expected |
|---|---|---|
| SP1-FESET-001 | Open tenant setup empty | Defaults visible |
| SP1-FESET-002 | Save tenant missing name | Inline error |
| SP1-FESET-003 | Create branch | Branch appears in list |
| SP1-FESET-004 | Create user Advisor | User appears with role |
| SP1-FESET-005 | Update permission | Matrix updates, success message |
| SP1-FESET-006 | Non-Admin opens setup | No access or readonly |

### Dependencies

- R1A-BE-001.
- R1A-BE-002.
- R1A-BE-003.

### Open questions

| ID | Question | BA recommendation |
|---|---|---|
| SP1-Q-FESET-001 | Có cần invite email trong Sprint 1 không? | Không, để auth/user onboarding riêng nếu cần |
| SP1-Q-FESET-002 | Permission matrix có edit trực tiếp grid hay modal? | Grid edit nhanh hơn, nhưng guardrail phải rõ |

## 14. Ticket R1A-FE-002 - Catalog Management Screen

### Summary

Là Admin, tôi muốn quản lý language, program và course trên UI để chuẩn bị dữ liệu cho lead import và opportunity.

### Business value

Catalog UI giúp Admin tự cấu hình ngôn ngữ/chương trình mà không phụ thuộc developer, đồng thời giữ sản phẩm không hard-code theo IELTS/JLPT/HSK.

### Scope

In scope:

- Language list/form.
- Program list/form.
- Course list/form.
- `ACTIVE`/`INACTIVE` controls.
- Basic validation and empty states.

Out of scope:

- Pricing plan phức tạp.
- Schedule/class session.
- LMS/course content.
- Bulk catalog import.

### UI behavior

Language:

- Name.
- Code.
- Status.

Program:

- Language select.
- Program name.
- Exam track.
- Status.

Course:

- Program select.
- Course name.
- Level.
- Tuition gross.
- Currency.
- Status.

### API dependencies

- `POST/PATCH/GET /api/v1/languages`.
- `POST/PATCH/GET /api/v1/programs`.
- `POST/PATCH/GET /api/v1/courses`.

### Acceptance criteria

- Admin can create English, Japanese, Chinese and custom language.
- Admin can create program under active language.
- Admin can create course under active program.
- UI blocks negative tuition.
- Inactive language/program is not selectable for new child item.
- UI does not hard-code IELTS/JLPT/HSK behavior.

### Test cases

| Test ID | Scenario | Expected |
|---|---|---|
| SP1-FECAT-001 | Create custom language | Success |
| SP1-FECAT-002 | Create program under active language | Success |
| SP1-FECAT-003 | Create program under inactive language | Blocked |
| SP1-FECAT-004 | Create course negative tuition | Inline error |
| SP1-FECAT-005 | Inactive program | Hidden/disabled in course creation |
| SP1-FECAT-006 | Search/filter catalog if implemented | Returns correct list |

### Dependencies

- R1A-BE-004.

### Open questions

| ID | Question | BA recommendation |
|---|---|---|
| SP1-Q-FECAT-001 | Có cần tree view Language -> Program -> Course không? | Nên có nếu UX effort thấp; nếu không, dùng tabs/list filters |
| SP1-Q-FECAT-002 | Có cần seed button không? | Không; seed nên làm ở backend/migration |

## 15. Sprint 1 QA Regression Set

| Test pack | Includes |
|---|---|
| Tenant config | Create/update tenant, default timezone/currency, inactive |
| Branch/user | Create branch, create user, duplicate email, inactive user |
| Permission | Matrix view/update, guardrails, unauthorized API |
| Catalog | Language/program/course create/update, inactive parent, negative tuition |
| Import foundation | Batch/row persistence, tenant-scoped history, error rows |
| FE smoke | Admin setup screens load, validation, success/error states |

## 16. Sprint 1 Demo Script

Demo path:

```text
Admin logs in
-> Creates tenant ABC Language Center
-> Creates branch Hà Nội - Cầu Giấy
-> Creates Advisor user and assigns branch
-> Reviews permission matrix
-> Creates English language
-> Creates IELTS program
-> Creates IELTS Foundation course
-> Opens import history foundation
-> Confirms no lead import preview yet because that is Sprint 2
```

Demo pass criteria:

- Tenant/branch/user/catalog data is created via UI/API.
- Permission guardrails work.
- Inactive config behavior is visible.
- Import batch foundation exists at DB/API level.
- No R1A Sprint 2 behavior is claimed as complete.

## 17. Sprint 1 Exit Criteria

Sprint 1 is accepted when:

- All P0 Sprint 1 BE items pass API tests.
- Admin setup UI can operate against real APIs or agreed mocked APIs if backend not ready.
- Catalog UI can create/list/update language/program/course.
- Permission enforcement exists at API layer.
- Import batch/import row foundation is implemented and tenant-scoped.
- No cross-tenant data leakage in tested endpoints.
- PO/QA signs off demo path.

## 18. Sprint 1 Risks

| Risk | Level | Control |
|---|---|---|
| Team entity decision delays user model | Medium | Default branch + role; add Team later only if confirmed |
| AuditLog delayed but permission needs audit | High | Minimal audit table/service is Must-have in Sprint 1 |
| Auth/session tenant context unclear | High | Add/confirm R1A-TECH-001 before API permission tests |
| FE blocked by missing GET/list APIs | Medium | Include GET/list APIs in BE scope even if original baseline focused POST/PATCH |
| Import foundation misunderstood as import feature | Medium | Keep preview/confirm explicitly out of Sprint 1 |
| Permission guardrails only done on UI | High | Require API authorization tests |

## 19. Sprint Planning Checklist

Before committing Sprint 1:

- Confirm project bootstrap follows `MAR_ARCHITECTURE_VERSION_CONVENTION.md` / MAR-ARCH-1.0.
- Confirm Team entity decision.
- Confirm Team scope is reserved/disabled if Team entity is not added.
- Confirm minimal AuditLog is P0/Must in Sprint 1.
- Confirm GET/list/detail APIs needed for FE.
- Confirm enum convention for DB/API is `UPPER_SNAKE_CASE`.
- Confirm auth/session tenant context test strategy.
- Confirm UX layout for admin setup and catalog.
- Confirm whether import batch foundation includes internal draft API.
- Estimate BE/FE/QA capacity.
- Decide whether Sprint 1 includes seed data.
