# R1A Sprint 1 QA & Acceptance Pack

> Phiên bản cập nhật: `v2.1 - Architecture version/convention lock - 2026-06-29`.
> Baseline kỹ thuật hiện hành: `Java + Spring Boot ecosystem`, `PostgreSQL`, `Flyway`, `Spring Data JPA/Hibernate`.
> Ghi chú: file này đã được đồng bộ theo quyết định chọn Spring Boot và MAR-ARCH-1.0; development commitment vẫn phụ thuộc sign-off `SP1-D01` đến `SP1-D10`.
## 1. Trạng thái tài liệu

| Thuộc tính | Giá trị |
|---|---|
| Tên tài liệu | R1A Sprint 1 QA & Acceptance Pack |
| Vai trò tài liệu | Bộ test và nghiệm thu Sprint 1 cho QA/PO |
| Nguồn baseline | `10-r1a-sprint-1-ready-package.md`, `11-r1a-sprint-1-technical-handoff.md` |
| Trạng thái | Ready for QA planning/sign-off, not yet committed |
| Ngày lập | 2026-06-29 |

Tài liệu này tách riêng test cases, acceptance checklist và release gate cho Sprint 1. QA có thể dùng để viết test case thủ công, automation API/UI, và PO dùng để nghiệm thu demo.

## 2. Test scope

### 2.1. In scope

| Area | Scope |
|---|---|
| Tenant | Create/update, default timezone/currency, inactive |
| Branch | Create/update, duplicate active branch, inactive |
| User/role | Create user, role assignment, branch assignment, duplicate email, inactive |
| Permission | Matrix view/update, guardrail, unauthorized action |
| Catalog | Language/program/course CRUD baseline, inactive parent, negative tuition |
| Import foundation | ImportBatch/ImportRow persistence, history, error rows, tenant scope |
| UI smoke | Admin setup screens, catalog screens, import history foundation |
| Security | API-level permission and tenant isolation |

### 2.2. Out of scope

| Area | Reason |
|---|---|
| Lead import preview/confirm | Sprint 2 |
| CSV parser production flow | Sprint 2 |
| Website/Meta webhook | Later R1A |
| Dedup/customer/opportunity | Later R1A |
| Pipeline/assignment/SLA | Later R1A |
| Dashboard/payment/revenue | R1B/R1C |

## 3. Test environment assumptions

| Item | Expected |
|---|---|
| Environment | QA/staging environment with fresh migration |
| Auth | Admin and non-Admin test users available through auth/session or `R1A-TECH-001` fixture |
| Tenant context | API request exposes `tenant_id`, `actor_id`, `role_code` in request context |
| Seed data | System seed for roles/default permissions, plus demo/test seed where agreed |
| Import fixture | Internal draft API or BE seed/fixture script for ImportBatch/ImportRow |
| API error envelope | Validation/permission/conflict errors follow the signed-off envelope |
| Browser | Latest Chrome/Edge for UI smoke |
| API tool | Postman/Insomnia/automated API test runner |

## 4. Entry criteria

QA starts Sprint 1 validation when:

- DB migration runs clean.
- Required seed data is loaded or can be created via API.
- API environment is reachable.
- FE build is deployed or local QA build is available.
- Auth/session tenant context is working.
- Minimal AuditLog for permission and sensitive setup changes is included in Sprint 1 delivery.
- DB/API enum convention uses `UPPER_SNAKE_CASE`; UI labels can be localized separately.
- Import foundation testability is closed: internal draft API or repeatable seed/fixture script.
- Known open decisions for Sprint 1 are resolved or marked as accepted assumptions.

## 5. Test data

### 5.1. Positive data

| Entity | Data |
|---|---|
| Tenant | ABC Language Center |
| Timezone | Asia/Ho_Chi_Minh |
| Currency | VND |
| Branch | Hà Nội - Cầu Giấy |
| Admin | admin@abc.test |
| Advisor | advisor01@abc.test |
| Sales Lead | saleslead@abc.test |
| Marketing | marketing@abc.test |
| Language | English, Japanese, Chinese |
| Program | IELTS, JLPT N5, HSK |
| Course | IELTS Foundation, JLPT N5 Foundation, HSK Basic |

### 5.2. Cross-tenant data

| Entity | Data |
|---|---|
| Tenant B | XYZ Language Center |
| Branch B | TP.HCM - Quan 1 |
| Admin B | admin@xyz.test |
| Advisor B | advisor01@xyz.test |
| Language B | Korean |
| ImportBatch B | batch_xyz_001 with at least one success row and one error row |

### 5.3. Negative data

| Scenario | Data |
|---|---|
| Missing tenant name | Empty string |
| Duplicate branch | Hà Nội - Cầu Giấy |
| Duplicate email | advisor01@abc.test |
| Inactive language parent | Japanese with status `INACTIVE` |
| Negative tuition | -1000000 |
| Guardrail permission | Advisor export data |
| Cross-tenant access | Tenant A user requests Tenant B resource |

## 6. Severity guideline

| Severity | Meaning | Example |
|---|---|---|
| Blocker | Không thể nghiệm thu Sprint 1 | Migration fail, Admin không tạo được tenant |
| Critical | Sai rule bảo mật/dữ liệu lõi | Advisor gọi API tạo tenant thành công, cross-tenant leak |
| Major | Chức năng P0 sai nhưng có workaround | Duplicate email không bị chặn |
| Minor | Lỗi UI/wording không chặn flow | Toast message chưa rõ |
| Trivial | Cosmetic | Spacing/label nhỏ |

## 7. API test cases

### 7.0. Auth/session and API error envelope

| Test ID | Priority | Scenario | Steps | Expected |
|---|---|---|---|---|
| API-AUTH-001 | P0 | Missing auth | Call any protected admin API without token/session | 401 with `UNAUTHENTICATED` |
| API-AUTH-002 | P0 | Valid tenant context | Admin calls protected API with valid session | Request context includes `tenant_id`, `actor_id`, `role_code` |
| API-AUTH-003 | P0 | Unauthorized role | Advisor calls Admin-only API | 403 with permission error code |
| API-AUTH-004 | P0 | Tenant context mismatch | Tenant A actor requests Tenant B scoped resource | 403 or 404; no data leak |
| API-ERR-001 | P0 | Error envelope shape | Trigger validation, permission and conflict errors | Response follows `{ error: { code, message, details[] }, meta: { request_id } }` |

### 7.1. Tenant API

| Test ID | Priority | Scenario | Steps | Expected |
|---|---|---|---|---|
| API-TEN-001 | P0 | Create tenant valid | POST `/api/v1/tenants` with valid body | 201, tenant returned with timezone/currency/status |
| API-TEN-002 | P0 | Missing tenant name | POST tenant with blank `tenant_name` | 400/422 validation error |
| API-TEN-003 | P0 | Default timezone | POST tenant without timezone | Created with `Asia/Ho_Chi_Minh` |
| API-TEN-004 | P0 | Default currency | POST tenant without currency | Created with `VND` |
| API-TEN-005 | P0 | Update inactive | PATCH tenant status `INACTIVE` | Status updated, data not deleted |
| API-TEN-006 | P0 | Unauthorized create | Non-Admin calls POST tenant | 403 |
| API-TEN-007 | P1 | Invalid status enum | POST/PATCH status outside `ACTIVE`/`INACTIVE` | 400/422 validation error |
| API-TEN-008 | P1 | Invalid timezone | POST/PATCH unsupported timezone | 400/422 validation error |
| API-TEN-009 | P1 | Invalid currency | POST/PATCH unsupported currency | 400/422 validation error |

### 7.2. Branch API

| Test ID | Priority | Scenario | Steps | Expected |
|---|---|---|---|---|
| API-BR-001 | P0 | Create branch valid | POST `/api/v1/branches` | Branch created under tenant |
| API-BR-002 | P0 | Missing branch name | POST branch blank name | Validation error |
| API-BR-003 | P0 | Duplicate active branch | Create same branch name active twice | Conflict/validation error |
| API-BR-004 | P0 | Inactive branch | PATCH status `INACTIVE` | Branch inactive, not deleted |
| API-BR-005 | P0 | Tenant scoped list | GET branches as tenant A | Only tenant A branches |
| API-BR-006 | P0 | Unauthorized create | Advisor calls create branch | 403 |
| API-BR-007 | P0 | Branch detail | GET `/api/v1/branches/{branch_id}` for tenant-owned branch | Branch detail returned |
| API-BR-008 | P0 | Cross-tenant branch detail | Tenant A GET Tenant B branch detail | 403 or 404 |
| API-BR-009 | P1 | Branch list pagination | GET branches with `page` and `page_size` | Pagination envelope and tenant-scoped data |

### 7.3. User/role API

| Test ID | Priority | Scenario | Steps | Expected |
|---|---|---|---|---|
| API-USR-001 | P0 | Create Advisor valid | POST `/api/v1/users` role `ADVISOR` with branch | User created, branch linked |
| API-USR-002 | P0 | Missing full name | POST user blank full_name | Validation error |
| API-USR-003 | P0 | Missing role | POST user without role | Validation error |
| API-USR-004 | P0 | Duplicate email in tenant | Create same email twice | Conflict/validation error |
| API-USR-005 | P0 | Inactive user | PATCH user status `INACTIVE` | User inactive, not deleted |
| API-USR-006 | P0 | Tenant scoped list | GET users as tenant A | Only tenant A users |
| API-USR-007 | P1 | Invalid role enum | POST role outside signed-off enum | Validation error |
| API-USR-008 | P0 | User detail | GET `/api/v1/users/{user_id}` for tenant-owned user | User detail returned |
| API-USR-009 | P0 | Cross-tenant user detail | Tenant A GET `/api/v1/users/{user_id}` of Tenant B | 403 or 404 |
| API-USR-010 | P1 | User list filters | GET users filter by role/status/branch | Correct tenant-scoped filtered result |
| API-USR-011 | P1 | Empty user list | GET users in tenant with no matching filter | Empty array, not error |

### 7.4. Permission API

| Test ID | Priority | Scenario | Steps | Expected |
|---|---|---|---|---|
| API-PERM-001 | P0 | View permission matrix | Admin GET `/api/v1/permissions/matrix` | Matrix returned |
| API-PERM-002 | P0 | Update permission valid | Admin PATCH `SALES_LEAD` `lead.import` | Update success |
| API-PERM-003 | P0 | Permission change audit | Update permission with reason | AuditLog row created with actor, entity and reason |
| API-PERM-004 | P0 | Advisor creates tenant | Advisor POST tenant | 403 |
| API-PERM-005 | P0 | Enable Advisor export | PATCH matrix to allow Advisor export | Rejected |
| API-PERM-006 | P0 | Enable Marketing payment write | PATCH matrix to allow Marketing payment write | Rejected or impossible |
| API-PERM-007 | P0 | Non-Admin update matrix | Non-Admin PATCH matrix | 403 |

### 7.5. Catalog API

| Test ID | Priority | Scenario | Steps | Expected |
|---|---|---|---|---|
| API-CAT-001 | P0 | Create English language | POST `/api/v1/languages` | Language created |
| API-CAT-002 | P0 | Create custom language | POST custom language | Created |
| API-CAT-003 | P0 | Duplicate active language | Create same active language twice | Conflict/validation error |
| API-CAT-004 | P0 | Create program under active language | POST `/api/v1/programs` | Program created |
| API-CAT-005 | P0 | Create program under inactive language | Inactive language then POST program | Rejected |
| API-CAT-006 | P0 | Create course under active program | POST `/api/v1/courses` | Course created |
| API-CAT-007 | P0 | Negative tuition | POST course with negative tuition | Rejected |
| API-CAT-008 | P0 | Tenant scoped catalog | GET catalog as tenant A | Only tenant A data |
| API-CAT-009 | P1 | Inactive program | PATCH program inactive | Program inactive and not deleted |
| API-CAT-010 | P0 | Cross-tenant catalog detail/edit | Tenant A GET/PATCH Tenant B language/program/course | 403 or 404; no data leak |
| API-CAT-011 | P1 | Catalog filters/pagination | GET catalog by status/language/program with page/page_size | Correct tenant-scoped result and pagination envelope |

### 7.6. Import foundation API

Execution note:

- If Sprint 1 includes an internal draft API, QA may use it to create ImportBatch/ImportRow fixtures.
- If Sprint 1 does not include a draft API, BE must provide a repeatable seed/fixture script, and QA verifies only the GET history/detail/error endpoints plus data constraints.
- FE demo list must use demo seed with at least one batch and one error row; preview/confirm remains out of scope.

| Test ID | Priority | Scenario | Steps | Expected |
|---|---|---|---|---|
| API-IMP-001 | P0 | Store import batch fixture | Create batch via internal draft API or BE fixture | Batch persisted |
| API-IMP-002 | P0 | Store import rows | Add rows linked to batch via internal draft API or BE fixture | Rows persisted |
| API-IMP-003 | P0 | Query import history | GET `/api/v1/imports/leads` | Tenant-scoped batch list |
| API-IMP-004 | P0 | Query batch detail | GET `/api/v1/imports/leads/{batch_id}` | Summary and mapping_config returned |
| API-IMP-005 | P0 | Query error rows | GET `/api/v1/imports/leads/{batch_id}/errors` | Row number, code, message returned |
| API-IMP-006 | P0 | Negative count rejected | Create/update batch with negative count | Rejected |
| API-IMP-007 | P0 | Cross-tenant batch row | Tenant A row linked to tenant B batch | Rejected or impossible |
| API-IMP-008 | P1 | Empty import history | Tenant with no batches | Empty list, not error |
| API-IMP-009 | P0 | Cross-tenant import batch detail | Tenant A GET `/api/v1/imports/leads/{batch_id}` of Tenant B | 403 or 404; no row data returned |
| API-IMP-010 | P1 | Import history pagination | GET import history with `page` and `page_size` | Pagination envelope and stable ordering |

## 8. UI smoke test cases

### 8.1. Tenant setup UI

| Test ID | Priority | Scenario | Steps | Expected |
|---|---|---|---|---|
| UI-TEN-001 | P0 | Open tenant setup | Navigate `/admin/tenant` | Form loads with timezone/currency defaults |
| UI-TEN-002 | P0 | Missing tenant name | Clear name and save | Inline validation error |
| UI-TEN-003 | P0 | Save tenant valid | Fill valid data and save | Success message, tenant persisted |
| UI-TEN-004 | P1 | Inactive warning | Set tenant inactive | Warning that inactive tenant will not receive active leads |

### 8.2. Branch UI

| Test ID | Priority | Scenario | Steps | Expected |
|---|---|---|---|---|
| UI-BR-001 | P0 | Empty branch list | Open `/admin/branches` on new tenant | Empty state with add action |
| UI-BR-002 | P0 | Create branch | Add Hà Nội - Cầu Giấy | Branch appears in list |
| UI-BR-003 | P0 | Duplicate branch | Add same branch again | Error shown |
| UI-BR-004 | P1 | Inactive branch | Set branch inactive | Row shows inactive state |

### 8.3. User UI

| Test ID | Priority | Scenario | Steps | Expected |
|---|---|---|---|---|
| UI-USR-001 | P0 | Create Advisor | Add Advisor with branch | User appears with role/branch |
| UI-USR-002 | P0 | Missing role | Save user without role | Inline error |
| UI-USR-003 | P0 | Duplicate email | Add same email twice | Error shown |
| UI-USR-004 | P1 | Inactive user | Set user inactive | User row inactive |

### 8.4. Permission UI

| Test ID | Priority | Scenario | Steps | Expected |
|---|---|---|---|---|
| UI-PERM-001 | P0 | Matrix loads | Open `/admin/permissions` | Matrix visible |
| UI-PERM-002 | P0 | Guardrail disabled | Find Advisor export or Marketing payment | Control disabled or unavailable |
| UI-PERM-003 | P0 | Update valid permission | Update Sales Lead (`SALES_LEAD`) `lead.import` | Success state |
| UI-PERM-004 | P1 | Non-Admin access | Open permission screen as Advisor | Blocked/read-only |

### 8.5. Catalog UI

| Test ID | Priority | Scenario | Steps | Expected |
|---|---|---|---|---|
| UI-CAT-001 | P0 | Create custom language | Add language | Success |
| UI-CAT-002 | P0 | Create program under active language | Add IELTS under English | Success |
| UI-CAT-003 | P0 | Program under inactive language | Inactive language then add program | Blocked |
| UI-CAT-004 | P0 | Create course | Add IELTS Foundation | Success |
| UI-CAT-005 | P0 | Negative tuition | Enter negative tuition | Inline error |
| UI-CAT-006 | P1 | Inactive program | Set program inactive | Hidden/disabled for new course |

### 8.6. Import history UI

| Test ID | Priority | Scenario | Steps | Expected |
|---|---|---|---|---|
| UI-IMP-001 | P1 | Empty import history | Open import history with no fixture | Empty state |
| UI-IMP-002 | P1 | Batch list | Seed import batch and open list | Batch appears |
| UI-IMP-003 | P1 | Error rows | Open batch errors | Error rows show row number/message |
| UI-IMP-004 | P0 | Preview not claimed | UI does not show working preview/confirm as completed feature | Scope integrity preserved |

## 9. Security and tenant isolation tests

| Test ID | Priority | Scenario | Steps | Expected |
|---|---|---|---|---|
| SEC-001 | P0 | Cross-tenant branch read | Tenant A user requests Tenant B branch | Blocked or not found |
| SEC-002 | P0 | Cross-tenant user read | Tenant A user requests Tenant B user | Blocked or not found |
| SEC-003 | P0 | Cross-tenant catalog edit | Tenant A user edits Tenant B language/program/course | Blocked |
| SEC-004 | P0 | Cross-tenant import history | Tenant A user lists Tenant B import batch | Blocked/not returned |
| SEC-005 | P0 | Non-Admin permission update | Advisor/Marketing PATCH matrix | 403 |
| SEC-006 | P0 | Advisor opens admin setup | Advisor navigates admin route | Blocked or read-only by policy |
| SEC-007 | P0 | API bypass UI guardrail | Direct API call enables Advisor export | Rejected |
| SEC-008 | P0 | API bypass payment guardrail | Direct API call enables Marketing payment write | Rejected |
| SEC-009 | P0 | Non-admin direct permission update | Direct PATCH matrix as Marketing/Sales Lead | 403 |
| SEC-010 | P0 | Detail-level tenant isolation | Tenant A requests Tenant B branch/user/catalog/import detail | 403 or 404 |

## 10. Acceptance checklist by ticket

| Ticket | Acceptance checklist |
|---|---|
| R1A-TECH-001 | Missing auth returns `401 UNAUTHENTICATED`; valid request context has `tenant_id`, `actor_id`, `role_code`; unauthorized role returns 403 |
| R1A-BE-001 | Tenant create/update works; default timezone/currency; inactive status; unauthorized blocked |
| R1A-BE-002 | Branch/user CRUD baseline works; duplicate branch/email blocked; inactive user retained |
| R1A-BE-003 | Permission matrix view/update works; guardrails enforced; API authorization active; AuditLog created for permission/sensitive setup changes |
| R1A-BE-004 | Language/program/course CRUD works; inactive parent blocked; negative tuition blocked |
| R1A-BE-005 | ImportBatch/ImportRow persisted through draft API or fixture; history/errors tenant-scoped; no official Lead created |
| R1A-FE-001 | Admin setup screens load and save; validation/error/success states visible |
| R1A-FE-002 | Catalog screens create/update records; inactive/negative validation visible |

## 11. PO demo acceptance script

Run this path for PO sign-off:

```text
1. Admin logs in.
2. Admin creates tenant ABC Language Center.
3. Admin creates branch Hà Nội - Cầu Giấy.
4. Admin creates Advisor user and assigns branch.
5. Admin opens permission matrix.
6. Admin verifies Advisor export/payment guardrails are blocked.
7. Admin creates English language.
8. Admin creates IELTS program under English.
9. Admin creates IELTS Foundation course.
10. Admin opens import history foundation.
11. System shows import foundation only; no claim that preview/confirm is complete.
```

Pass if:

- Tenant/branch/user/catalog data is created through UI/API.
- Permission guardrails work at API layer.
- UI validation works for missing/invalid fields.
- Import foundation exists and is scoped.
- Sprint 2 features are not presented as complete.

## 12. Release gate

| Gate | Owner | Pass condition | Status |
|---|---|---|---|
| DB migration | Tech Lead/BE | Runs clean on fresh environment | Pending |
| Seed data | BE/QA | System seed available; demo/test seed loaded in agreed QA environment | Pending |
| Auth/session | Tech Lead/SA/QA | `R1A-TECH-001` passes: auth, tenant context and 401/403 behavior verified | Pending |
| Enum convention | Tech Lead/QA | DB/API enums use `UPPER_SNAKE_CASE`; UI labels are separate | Pending |
| API error envelope | Tech Lead/QA | Validation, permission and conflict errors use signed-off envelope | Pending |
| Import testability | BE/QA | Internal draft API or repeatable seed/fixture supports ImportBatch/ImportRow tests | Pending |
| API P0 tests | QA/BE | All P0 API tests pass | Pending |
| UI smoke | QA/FE | Admin setup and catalog screens usable | Pending |
| Permission | QA/BE | Unauthorized actions blocked at API | Pending |
| Tenant isolation | QA/BE | Cross-tenant access blocked | Pending |
| Audit minimal | QA/BE | Permission change and sensitive setup changes are logged | Pending |
| Demo path | PO/QA | PO demo script passes | Pending |
| Scope integrity | PO/BA | No Sprint 2 feature claimed as complete | Pending |

## 13. Defect triage rules

| Defect type | Severity recommendation |
|---|---|
| Migration fail | Blocker |
| Tenant create fails | Blocker |
| API permission bypass | Critical |
| Cross-tenant data leak | Critical |
| Duplicate email/branch not blocked | Major |
| Catalog inactive parent allowed | Major |
| Negative tuition accepted | Major |
| UI validation missing but API blocks correctly | Minor/Major depending workflow |
| Import preview shown as complete | Major due scope misrepresentation |

## 14. Exit criteria

Sprint 1 can be accepted when:

- All P0 API tests pass.
- All P0 UI smoke tests pass or have PO-approved workaround.
- All P0 security/tenant isolation tests pass.
- Auth/session, enum convention, API error envelope and import testability gates pass.
- Minimal AuditLog gate passes; no permission matrix release without audit evidence.
- No Blocker/Critical defects remain open.
- Major defects have PO/Tech Lead disposition.
- Demo script passes.
- Release gate table is signed off or explicitly waived.

## 15. QA notes for Sprint 2 preparation

QA should preserve test data and notes for Sprint 2, because import preview will depend on:

- Existing tenant.
- Existing language/program/course.
- Existing Admin/Marketing permissions.
- ImportBatch/ImportRow foundation.
- Error row behavior.
- Tenant-scoped import history.
