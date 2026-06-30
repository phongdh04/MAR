# DEV WORKFLOW & RELEASE CONVENTION - QUY TẮC PHÁT TRIỂN MAR

**Ngày tạo:** 30/06/2026  
**Phiên bản:** MAR-CONV-1.1  
**Tác giả:** Tech Lead / Solution Architect  
**Trạng thái:** Locked for Sprint 1 technical kickoff  
**Stack:** Git, Maven, Spring Boot profiles, Flyway, CI pipeline, PR review  
**Tham chiếu:**
- `D:\Documents-for-Expert-Design-Database\MAR\architecture\coding_convention.md` - Git/PR/testing pattern OASIS
- `D:\Documents-for-Expert-Design-Database\MAR\architecture\configuration_convention.md` - Config/environment pattern OASIS
- `D:\Documents-for-Expert-Design-Database\MAR\architecture\15_common_code_vs_principle.md` - Review common code principle
- `D:\Documents-for-Expert-Design-Database\MAR\docs BA\README.md` - BA docs and open decision index

## 1. TỔNG QUAN & MỤC ĐÍCH

Tài liệu này chuẩn hóa workflow phát triển, review, merge và release gate cho MAR.

Mục đích:

- Ngăn dev bắt đầu khi quyết định nền chưa đủ.
- Đảm bảo mỗi PR có scope, test, migration, permission và rollback rõ.
- Giữ convention/code/docs version nhất quán.
- Bảo vệ Sprint 1 foundation: tenant isolation, permission, migration, error envelope, audit.
- Gắn kỹ thuật với BA sign-off và release acceptance pack.

Nguyên tắc ghi nhớ:

> **"Merge nhanh không bằng merge đúng foundation."**

## 2. PHẠM VI ÁP DỤNG

Áp dụng cho:

- Git branch/commit/PR.
- Code review.
- Dependency approval.
- Flyway migration review.
- Environment config.
- CI test gate.
- Sprint 1 technical kickoff/release gate.
- Convention versioning.
- Rollback/roll-forward note.

Không áp dụng cho:

- Company-wide HR workflow.
- Manual sales/operation process ngoài phần mềm.
- Frontend design review chi tiết nếu frontend repo tách riêng.
- Production incident process đầy đủ nếu chưa có ops runbook.

## 3. NGUYÊN TẮC CHUNG

1. **Decision before commitment:** chưa chốt decision nền thì chỉ technical planning, chưa development commitment.
2. **Small PR:** mỗi PR có scope logic rõ, tránh nhét scope creep.
3. **Test evidence required:** không merge chỉ vì chạy được local.
4. **Migration is code:** DB change phải review như code.
5. **No secret in repo:** config theo environment/secret manager.
6. **Dependency by approval:** thêm dependency phải có lý do, risk và owner.
7. **Protect foundation:** tenant isolation, permission, migration repeatability ưu tiên hơn UI convenience.
8. **Docs move with behavior:** đổi API/DB/security phải cập nhật docs liên quan.
9. **Roll forward by default:** lỗi migration đã apply thì fix forward.
10. **Release gate is explicit:** Go/Conditional Go/No-Go phải ghi owner và pass condition.

## 4. QUY TẮC ĐẶT TÊN

### 4.1. Branch naming

Pattern:

```text
<type>/<ticket-id>-<short-description>
```

Examples:

```text
feature/r1a-be-001-tenant-foundation
feature/r1a-be-003-permission-matrix
fix/r1a-api-error-envelope
test/r1a-import-jsonb-storage
docs/mar-convention-update
chore/mar-bootstrap
```

Rules:

- Lowercase.
- Kebab-case.
- Có ticket/story id nếu có.
- Không dùng tên cá nhân trong branch chính.

### 4.2. Commit message

Pattern:

```text
type(scope): short description
```

Types:

```text
feat
fix
docs
test
refactor
chore
build
ci
perf
```

Examples:

```text
feat(tenant): add tenant create api
test(permission): cover forbidden matrix update
docs(architecture): update mar database convention
fix(api): return request id in error response
```

Scope registry gợi ý:

```text
tenant
branch
user
permission
catalog
leadimport
audit
security
api
db
test
docs
build
release
```

Ưu tiên dùng scope ổn định trong registry để tránh lẫn `authz`, `permissions`, `permission-matrix`, `perm` cho cùng một vùng nghiệp vụ.

### 4.3. PR title

Pattern:

```text
[R1A-BE-001] Tenant foundation APIs
```

Nếu chưa có ticket id:

```text
[MAR-ARCH] Bootstrap Spring Boot baseline
```

### 4.4. Version naming

Convention version:

```text
MAR-CONV-1.0
```

Architecture baseline:

```text
MAR-ARCH-1.0
```

Release tag đề xuất:

```text
r1a-sprint1-foundation-rc1
r1a-sprint1-foundation
```

## 5. CẤU TRÚC FILE & PACKAGE

### 5.1. Suggested repository structure

```text
mar-backend
├── pom.xml
├── src
│   ├── main
│   │   ├── java/vn/mar
│   │   └── resources
│   │       ├── application.yml
│   │       ├── application-local.yml
│   │       ├── application-qa.yml
│   │       ├── application-prod.yml
│   │       └── db/migration
│   └── test
│       ├── java/vn/mar
│       └── resources
├── docs
│   ├── api
│   └── release
└── README.md
```

### 5.2. Config files

```text
application.yml
application-local.yml
application-qa.yml
application-prod.yml
application-test.yml
```

Rules:

- `application.yml` chứa default non-secret.
- Profile files chứa config theo environment.
- Secret lấy từ env var/secret manager.
- Không commit `.env` có secret thật.

### 5.3. Migration directory

```text
src/main/resources/db/migration
```

Mọi migration đi theo `04-database-flyway-convention.md`.

### 5.4. Release docs

```text
docs/release
├── r1a-sprint1-release-note.md
├── r1a-sprint1-deployment-checklist.md
└── r1a-sprint1-known-issues.md
```

## 6. CÁC PATTERN BẮT BUỘC

### 6.1. Development commitment pattern

Development commitment chỉ xảy ra khi:

- Tất cả Sprint 1 P0 decisions trong active sign-off decision register mới nhất được approved hoặc conditional approved.
- Release gate owner accept pass condition.
- Kickoff pack ghi rõ `Go` hoặc `Conditional Go`.
- Project bootstrap verify theo `MAR-ARCH-1.0`.
- Sprint backlog/story specs đủ testable acceptance criteria.

Nếu chưa đủ:

```text
Ready for technical planning, not Ready for development commitment.
```

Không hard-code range decision như `SP1-D01` đến `SP1-D10` trong release gate; decision mới về implementation mode, backend/frontend framework, local/QA environment hoặc import PII access cũng phải được tính nếu nằm trong active register.

### 6.2. PR template pattern

```md
## Scope

## API Impact

## DB/Flyway Impact

## Permission & Tenant Isolation

## Audit/Logging Impact

## Tests

## Risks & Rollback

## Docs Updated
```

PR không có DB/API/security impact vẫn phải ghi `None`.

### 6.2.1. PR size and branch protection pattern

PR size guideline:

- Một PR nên là một logical change.
- PR > khoảng 500-800 dòng meaningful change hoặc chạm nhiều module có thể bị yêu cầu split.
- Migration + entity + repository + service + controller vẫn chấp nhận nếu cùng một ticket/scope.
- Không giấu scope creep trong refactor PR.

Branch protection:

- `main` branch protected.
- Không direct push vào `main`.
- Merge cần CI pass.
- Merge cần required reviewer theo loại PR.
- Blocking comment/conversation phải resolved trước merge.

Required reviewers:

| PR type | Reviewer tối thiểu |
|---|---|
| Backend business/API | Tech Lead hoặc backend owner |
| DB/Flyway/schema/index | Tech Lead/SA |
| Security/auth/permission/tenant isolation | Tech Lead/SA |
| Audit/logging/PII/import raw row | Tech Lead/SA; QA acknowledgement nếu ảnh hưởng acceptance |
| Test scope/acceptance behavior | QA acknowledgement hoặc owner acceptance |
| Docs-only convention change | Tech Lead/SA nếu rule behavior đổi |

### 6.3. Dependency approval pattern

Không thêm dependency mới nếu chưa có Tech Lead/SA approval.

Approval phải nêu:

- Vì sao Java/Spring Boot standard không đủ.
- Dependency có được Spring Boot parent quản lý version không.
- Maintenance risk.
- Security/CVE risk.
- License concern.
- Runtime/ops impact.
- Alternative đã cân nhắc.

Ví dụ:

```text
Dependency: Caffeine
Reason: local in-memory cache for permission profile
Alternative: ConcurrentHashMap custom cache rejected due TTL/invalidation complexity
Spring managed: yes/no
Owner: Tech Lead
```

MAR dependency examples:

- Caffeine cần approval nếu permission/catalog cache được implement.
- ShedLock cần approval nếu scheduler chạy production hoặc multi-instance.
- Spring Retry/Resilience4j cần approval nếu retry được implement.
- CSV/Excel parser dependency cần approval trước Sprint 2 import parser.
- Object storage SDK cần approval trước production upload.

### 6.4. Migration PR pattern

Mọi DB change PR phải có:

- Flyway migration file.
- Entity/repository update.
- Migration test hoặc verification evidence.
- Roll-forward note.
- Data migration note nếu ảnh hưởng data hiện có.

Không sửa migration đã apply ở shared environment.

### 6.5. API change pattern

Mọi API change phải ghi:

- Endpoint/method.
- Request/response DTO thay đổi.
- Status/error code thay đổi.
- Permission thay đổi.
- Backward compatible hay breaking.
- Test evidence.

Breaking change cần Tech Lead/SA approval.

### 6.6. Security change pattern

Security-related PR phải có:

- Permission code.
- Authenticated/unauthenticated behavior.
- Forbidden behavior.
- Tenant isolation behavior.
- Audit impact.
- Security tests.

### 6.7. Environment config pattern

```yaml
spring:
  datasource:
    url: ${MAR_DB_URL}
    username: ${MAR_DB_USERNAME}
    password: ${MAR_DB_PASSWORD}
```

Rules:

- Không hard-code DB password/JWT secret.
- Production debug off.
- Demo seed không bật production mặc định.
- Actuator endpoint protected đúng environment.

### 6.8. Release gate pattern

Release gate status:

```text
Go
Conditional Go
No-Go
```

`Conditional Go` bắt buộc có:

- Known blocker/risk.
- Owner.
- Due date.
- Pass condition.
- Confirmation rằng dev có thể bắt đầu mà không tạo rework lớn.

`No-Go` khi:

- Auth/session context chưa rõ cho protected API.
- DB/migration baseline chưa rõ.
- Permission model không test được.
- Tenant isolation chưa testable.
- Release owner reject pass condition.

Không dùng `Conditional Go` để bỏ qua nền bắt buộc:

- DB/migration tool chưa chốt.
- Auth/session tenant context chưa testable.
- API-level permission chưa có.
- Tenant isolation chưa testable.
- Minimal audit cho sensitive setup chưa có.
- Error envelope chưa thống nhất.
- Import foundation testability chưa có nếu import foundation nằm trong sprint.

### 6.9. Rollback/roll-forward pattern

Code rollback:

- Revert commit/PR nếu chưa release hoặc rollback safe.

DB migration:

- Fix forward bằng migration mới nếu đã apply shared/prod.
- Không edit migration đã apply.
- Destructive migration cần explicit approval và backup/restore plan.

Feature flag/config:

- Dùng khi cần tắt tính năng risky mà không rollback code.

## 7. QUY TẮC RIÊNG CỦA MAR RELEASE

### 7.1. Sprint 1 release gate

Sprint 1 foundation pass khi:

- Fresh DB migration chạy sạch.
- App boot với `ddl-auto=validate`.
- P0 API tests pass.
- Permission unauthorized actions blocked at API.
- Cross-tenant access blocked.
- Error envelope có `request_id`.
- Audit có cho permission/user/tenant sensitive changes.
- No secret/sensitive log.
- Scope integrity: không claim lead import/dedup/opportunity complete nếu mới có foundation.
- Nếu PR dùng cache/job/async: có cache invalidation, event timing, idempotency và metrics tests.
- Nếu PR chạm import/raw row/file: có PII/raw row access impact và masking/audit evidence.

### 7.2. Definition of Ready

Ticket dev ready khi:

- Scope rõ.
- API/DB impact rõ.
- Permission rule rõ.
- Tenant rule rõ.
- Acceptance criteria testable.
- Dependency không blocking.
- UX/API đủ để implement.
- QA biết verify thế nào.

### 7.3. Definition of Done

Ticket done khi:

- Code compile.
- Tests pass.
- Migration included nếu DB changed.
- API permission enforced.
- Tenant isolation preserved.
- Error response follows convention.
- Audit included nếu required.
- Docs updated nếu behavior changed.
- QA can run acceptance criteria.

### 7.4. BA/Architecture traceability

Khi implement story:

- Story phải trace về BA docs/story specs.
- API/DB/security decision phải trace về architecture/convention.
- Nếu phát hiện gap, tạo open decision thay vì tự chốt ngầm trong code.

### 7.5. Convention update rule

Khi đổi convention:

- Increment version nếu đổi rule quan trọng.
- Update file liên quan.
- Update README/index.
- Ghi lý do thay đổi.
- Tech Lead/SA review.

### 7.6. Docs update matrix

| Change type | Docs cần update |
|---|---|
| API endpoint/request/response/error | REST API convention/OpenAPI/API docs/QA pack |
| DB table/column/index | DB/Flyway convention/ERD/migration note |
| Permission code/guardrail | Security convention/permission matrix/QA security tests |
| Audit event | Audit convention/release test pack |
| Import lifecycle/status | Import convention/API docs/QA import tests |
| Cache/async/scheduler behavior | Cache/async convention/test evidence/release note |
| Release/deployment behavior | Release note/deployment checklist |

Nếu PR ghi `Docs Updated: None`, reviewer phải vẫn kiểm tra matrix này.

## 8. VÍ DỤ CODE MẪU

### 8.1. Good PR description

```md
## Scope
Add tenant create/detail APIs for R1A-BE-001.

## API Impact
POST /api/v1/tenants
GET /api/v1/tenants/{tenant_id}

## DB/Flyway Impact
V20260630_01__create_tenant_foundation.sql

## Permission & Tenant Isolation
tenant.manage required for create.
Detail uses tenant_id guard.

## Tests
mvn test
TenantControllerTest, TenantServiceTest, TenantRepositoryIT

## Risks & Rollback
No destructive migration. Fix forward if migration issue after shared env apply.
```

### 8.2. Good commit history

```text
feat(tenant): add tenant entity and migration
feat(tenant): add create tenant service
test(tenant): cover validation and duplicate code
docs(api): document tenant endpoints
```

### 8.3. Bad PR description

```md
Done tenant stuff.
Tests later.
```

Vấn đề:

- Không rõ scope.
- Không nêu API/DB/security impact.
- Không có test evidence.
- Không có rollback note.

## 9. ANTI-PATTERNS CẦN TRÁNH

Không được:

- Merge code trước sign-off khi tạo hướng đi khó đảo.
- Add dependency vì “có thể cần sau”.
- Sửa migration đã apply.
- Merge permission endpoint thiếu forbidden tests.
- Disable security để pass API test.
- Hide scope creep trong refactor PR.
- Commit secret/config thật.
- Mark Sprint 1 complete khi feature mới chỉ stub.
- Bỏ qua docs khi API/error/permission đổi.
- Bỏ qua branch protection/required reviewer vì PR nhỏ.
- Merge PR cache/event mà không nói transaction timing/after commit.
- Merge PR import/raw row mà không nói PII/access/audit impact.
- Release khi fresh DB migration không chạy sạch.

## 10. TESTING CONVENTIONS

### 10.1. CI stages

| Stage | Khi chạy | Nội dung |
|---|---|---|
| `ci-fast` | Mọi PR | Compile, `mvn test`, unit/slice/contract tests nhanh |
| `ci-integration` | PR chạm DB/security/import/cache/job hoặc trước merge `main` | `mvn verify -Pintegration`, Testcontainers PostgreSQL, FlywayMigrationIT, SecurityIT |
| `ci-release-candidate` | Trước Sprint acceptance | Fresh DB migration, app boot smoke, P0 API, security, cross-tenant, audit, import foundation |
| `ci-doc-check` | PR đổi API/docs/convention | OpenAPI/docs/README/convention version được update |

Minimum PR command:

```text
mvn test
```

DB/security/import/cache/job PR:

```text
mvn verify -Pintegration
```

Nếu CI profile khác tên, PR phải ghi command/evidence tương đương.

### 10.2. Release candidate tests

Trước Sprint acceptance:

- Fresh DB migration.
- App boot smoke.
- P0 API test.
- Security/permission test.
- Cross-tenant test.
- Audit test.
- Import foundation test nếu scope có import.
- Cache/async/scheduler tests nếu scope có cache/job/event.
- Observability/request_id tests nếu scope chạm logging/filter/error handler.

### 10.3. Config test

Test/verify:

- Local profile boot.
- QA profile boot với env var.
- Production profile không bật demo seed/debug log.
- Actuator exposure đúng.

### 10.4. Documentation test

Review:

- API docs/spec khớp code.
- BA acceptance không bị đổi ngầm.
- Open decisions được cập nhật nếu có.
- Docs update matrix ở section 7.6 được áp dụng.
- README/index version được cập nhật nếu convention đổi.

### 10.5. Release checklist evidence

Release note phải có:

- Version/tag.
- Commit/PR list.
- Migration list.
- Known issues.
- Test evidence.
- Rollback/roll-forward note.
- Decision register snapshot hoặc link.
- Environment/profile evidence.
- Release owner/pass condition nếu `Conditional Go`.

Evidence format tối thiểu:

```text
Command:
Result:
Commit/Tag:
Environment/Profile:
Evidence link/path:
Known risk:
Owner:
Pass condition:
```

## 11. CODE REVIEW CHECKLIST

- [ ] Branch/PR title đúng convention.
- [ ] `main` branch protection/required reviewer không bị bypass.
- [ ] PR mô tả scope rõ.
- [ ] PR size hợp lý hoặc đã split/giải thích.
- [ ] API impact ghi rõ hoặc `None`.
- [ ] DB/Flyway impact ghi rõ hoặc `None`.
- [ ] Permission/tenant isolation impact ghi rõ.
- [ ] PII/raw row/log/audit impact ghi rõ nếu liên quan.
- [ ] Event transaction timing/after commit ghi rõ nếu PR publish event.
- [ ] Cache/job PR có invalidation/idempotency/lock/metric test evidence.
- [ ] Tests/evidence đầy đủ.
- [ ] Migration không sửa file đã apply.
- [ ] Dependency mới có approval.
- [ ] Không hard-code secret.
- [ ] Không log sensitive data.
- [ ] Docs updated nếu behavior changed.
- [ ] Docs update matrix đã được kiểm tra.
- [ ] Rollback/roll-forward note có.
- [ ] Scope không creep ngoài story.
- [ ] Release gate không bị bypass.

## 12. TÀI LIỆU LIÊN QUAN

- `01-architecture-baseline.md` - Nền kiến trúc.
- `02-coding-package-convention.md` - Code/package PR checklist.
- `04-database-flyway-convention.md` - Migration review.
- `05-security-auth-authz-convention.md` - Security review.
- `09-testing-quality-convention.md` - Test quality gate.
- `10-cache-async-scheduler-convention.md` - Cache/async/scheduler dependency and test gates.
- `11-import-file-storage-convention.md` - Import/raw row/file upload release gates.
- `README.md` - Convention index/version.
- OASIS reference: `coding_convention.md`, `configuration_convention.md`, `15_common_code_vs_principle.md`.

## 13. LỊCH SỬ CẬP NHẬT

| Phiên bản | Ngày | Người cập nhật | Nội dung |
|---|---|---|---|
| MAR-CONV-1.1 | 30/06/2026 | Tech Lead / Solution Architect | Đồng bộ active decision register thay vì hard-code `SP1-D01..D10`, bổ sung branch protection, required reviewers, PR size guideline, CI stages, dependency examples, docs update matrix và release evidence format. |
| MAR-CONV-1.0 | 30/06/2026 | Tech Lead / Solution Architect | Chuẩn hóa dev workflow/release convention theo pattern OASIS, gắn với MAR Sprint 1 release gate và BA sign-off. |
