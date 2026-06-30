# MAR OUR ARCHITECTURE CONVENTIONS - INDEX & GOVERNANCE

**Ngày tạo:** 30/06/2026  
**Phiên bản:** MAR-CONV-1.1  
**Tác giả:** Tech Lead / Solution Architect  
**Trạng thái:** Locked for Sprint 1 technical kickoff; pending final alignment checklist before dev baseline freeze  
**Nguồn tham chiếu:** OASIS architecture conventions, MAR BA docs v2.1, `MAR_ARCHITECTURE_VERSION_CONVENTION.md`  
**Baseline:** Java 21, Spring Boot 3.5.x, PostgreSQL 17, Flyway, REST API, root package `vn.mar`

## 1. TỔNG QUAN & MỤC ĐÍCH

Thư mục này là bộ convention kỹ thuật riêng cho dự án **MAR Lead-to-Enrollment MVP**.

Mục đích:

- Chuyển discipline/pattern của OASIS thành convention phù hợp với MAR.
- Chốt cách backend MAR dùng Spring Boot ecosystem, PostgreSQL, Flyway, REST API và tenant isolation.
- Làm nguồn tham chiếu cho dev kickoff, code review, QA gate và release gate.
- Ngăn việc copy nguyên domain, package, database syntax hoặc UI pattern không phù hợp từ dự án tham chiếu.
- Tạo một bộ file đủ đầy để dev/QA/BA/SA cùng trace khi có câu hỏi kỹ thuật.

Nguyên tắc ghi nhớ:

> **"OASIS là pattern tham khảo; MAR có domain, stack và release gate riêng."**

## 2. PHẠM VI ÁP DỤNG

Áp dụng cho:

- Backend MAR Sprint 1 foundation.
- Code/package/API/database/security/audit/logging/testing convention.
- Import foundation và file/storage boundary.
- Dev workflow, PR review, release gate.
- Các quyết định kỹ thuật cần map với BA docs.

Không áp dụng cho:

- Việc thay thế tài liệu BA.
- OpenAPI final nếu chưa sinh/duyệt.
- DDL final nếu chưa implement migration.
- Frontend design system chi tiết.
- Production infrastructure/runbook cuối cùng nếu chưa có ops decision.

## 3. NGUYÊN TẮC CHUNG

1. **MAR-first:** mọi rule phải map với Lead-to-Enrollment MVP.
2. **Spring Boot ecosystem:** ưu tiên Spring Boot/Spring Security/Spring Data/Flyway/Micrometer trước khi thêm công nghệ ngoài.
3. **REST API only:** backend MAR cung cấp API JSON, không làm server-rendered UI.
4. **PostgreSQL baseline:** PostgreSQL 17 là DB baseline; Flyway quản lý schema.
5. **Tenant isolation first:** mọi resource tenant-scoped phải có tenant boundary.
6. **Permission testable:** permission matrix phải enforce và test ở API/service.
7. **Audit sensitive changes:** permission/user/tenant/import milestone phải audit theo convention.
8. **Do not oversell scope:** import foundation không đồng nghĩa full parser/import product.
9. **Convention is versioned:** thay đổi convention phải có lý do, version/changelog.
10. **OASIS reference is secondary:** nếu xung đột, MAR convention thắng.

## 4. QUY TẮC ĐẶT TÊN

### 4.1. File naming

Convention files dùng prefix số để thể hiện thứ tự đọc:

```text
01-architecture-baseline.md
02-coding-package-convention.md
03-rest-api-convention.md
...
12-dev-workflow-release-convention.md
```

Rules:

- Lowercase.
- Kebab-case.
- Có prefix hai chữ số.
- Tên file phản ánh chủ đề convention.

### 4.2. Version naming

Convention version:

```text
MAR-CONV-1.x
```

Current convention index version:

```text
MAR-CONV-1.1
```

Architecture baseline:

```text
MAR-ARCH-1.0
```

### 4.3. Decision naming

Open decision theo pattern BA/Sprint:

```text
SP1-D01
SP1-D02
...
```

Technical decision trong architecture docs nên ghi:

```text
MAR-TECH-D01
```

nếu sau này cần tách riêng decision log kỹ thuật.

Active decision register hiện tại dùng file `16-techlead-sa-decision-register.md`; decision cuối cùng cho Sprint 1 phải được phản ánh lại ở `13-r1a-sprint-1-signoff-decision-log.md` và `14-r1a-sprint-1-signoff-kickoff-pack.md`.

### 4.4. File reference rule

Khi một convention file tham chiếu file khác:

- Ghi đúng tên file.
- Ghi rõ công dụng tham chiếu.
- Không nói chung chung “xem tài liệu liên quan” mà không chỉ file.

## 5. CẤU TRÚC FILE & PACKAGE

### 5.1. Folder structure

```text
D:\Documents-for-Expert-Design-Database\MAR\our architecture
├── README.md
├── 01-architecture-baseline.md
├── 02-coding-package-convention.md
├── 03-rest-api-convention.md
├── 04-database-flyway-convention.md
├── 05-security-auth-authz-convention.md
├── 06-exception-error-i18n-convention.md
├── 07-logging-observability-convention.md
├── 08-audit-convention.md
├── 09-testing-quality-convention.md
├── 10-cache-async-scheduler-convention.md
├── 11-import-file-storage-convention.md
└── 12-dev-workflow-release-convention.md
```

### 5.2. File index

| File | Công dụng | Cần chốt/đọc khi |
|---|---|---|
| `01-architecture-baseline.md` | Chốt nền kiến trúc, stack, dependency baseline, module priority | Bootstrap project, chọn công nghệ, mở dev kickoff |
| `02-coding-package-convention.md` | Package `vn.mar`, layer, naming, controller/service/repository/DTO/entity rules | Viết code backend, review PR |
| `03-rest-api-convention.md` | REST API, `/api/v1`, DTO, response/error envelope, pagination, permission | Thiết kế endpoint/API contract |
| `04-database-flyway-convention.md` | PostgreSQL, Flyway, table/index/constraint/entity/repository rules | Tạo migration, entity, repository |
| `05-security-auth-authz-convention.md` | Authentication, authorization, role/permission, tenant context, CORS/CSRF/JWT | Làm login/protected API/permission matrix |
| `06-exception-error-i18n-convention.md` | Exception hierarchy, error code, handler, i18n-ready message | Làm error response/validation/security errors |
| `07-logging-observability-convention.md` | MDC, request id, log level, masking, Actuator, metrics | Debug/monitoring/log review |
| `08-audit-convention.md` | Audit event model, action/resource, before/after payload, query permission | Sensitive change, permission/user/import audit |
| `09-testing-quality-convention.md` | Unit/API/security/repository/migration/audit/import test gates | Viết test, CI, acceptance |
| `10-cache-async-scheduler-convention.md` | Cache key/TTL, async event, scheduler, ShedLock optional, idempotency | Cache permission/catalog, background job |
| `11-import-file-storage-convention.md` | Import batch/row, JSONB, file metadata/storage boundary, parser scope | Lead import foundation/file upload |
| `12-dev-workflow-release-convention.md` | Branch/commit/PR/release gate/config/dependency workflow | Merge, release, kickoff, Go/No-Go |

### 5.3. Source of truth by topic

Nếu có mâu thuẫn giữa các convention theo chủ đề, dùng file thắng sau:

| Topic | Source of truth |
|---|---|
| Stack/version/package root | `01-architecture-baseline.md` |
| Code/package/layer/class naming | `02-coding-package-convention.md` |
| REST path/envelope/pagination | `03-rest-api-convention.md` |
| DB/Flyway/table/index/entity | `04-database-flyway-convention.md` |
| Auth/authz/tenant context | `05-security-auth-authz-convention.md` |
| Error code/error envelope/i18n | `06-exception-error-i18n-convention.md` |
| Request id/log/MDC/metrics | `07-logging-observability-convention.md` |
| Audit event/action/resource | `08-audit-convention.md` |
| Test/CI/quality gate | `09-testing-quality-convention.md` |
| Cache/async/scheduler | `10-cache-async-scheduler-convention.md` |
| Import/file/storage | `11-import-file-storage-convention.md` |
| PR/release/workflow | `12-dev-workflow-release-convention.md` |

### 5.4. OASIS reference folder

OASIS reference nằm tại:

```text
D:\Documents-for-Expert-Design-Database\MAR\architecture
```

Chỉ dùng để tham khảo pattern, không copy nguyên stack/domain/package.

## 6. CÁC PATTERN BẮT BUỘC

### 6.1. Convention document pattern

Mỗi file convention phải có đủ các phần:

1. Tổng quan & mục đích.
2. Phạm vi áp dụng.
3. Nguyên tắc chung.
4. Quy tắc đặt tên.
5. Cấu trúc file & package.
6. Các pattern bắt buộc.
7. Quy tắc riêng của module/chủ đề.
8. Ví dụ code mẫu hoặc ví dụ good/bad.
9. Anti-patterns cần tránh.
10. Testing conventions.
11. Code review checklist.
12. Tài liệu liên quan.
13. Lịch sử cập nhật.

### 6.2. Priority pattern

Nếu tài liệu mâu thuẫn, ưu tiên theo thứ tự:

1. Active Sprint sign-off/decision đã được approve.
2. `our architecture` convention set.
3. `MAR_ARCHITECTURE_VERSION_CONVENTION.md`.
4. OASIS reference documents.
5. Ý kiến tạm thời trong chat chưa được ghi file.

### 6.2.1. Active decision sources

| Decision layer | Source of truth | Ghi chú |
|---|---|---|
| Sprint 1 sign-off | `D:\Documents-for-Expert-Design-Database\MAR\docs BA\13-r1a-sprint-1-signoff-decision-log.md` | Chốt final decision, approver và release gate status |
| Sprint 1 kickoff | `D:\Documents-for-Expert-Design-Database\MAR\docs BA\14-r1a-sprint-1-signoff-kickoff-pack.md` | Chốt Go/Conditional Go/No-Go trong kickoff |
| Tech Lead/SA decisions | `D:\Documents-for-Expert-Design-Database\MAR\docs BA\16-techlead-sa-decision-register.md` | Chốt P0/P1 technical decisions |
| Technical rationale | `D:\Documents-for-Expert-Design-Database\MAR\docs BA\17-techlead-sa-decision-rationale.md` | Giải thích lý do chọn stack/architecture |
| Convention implementation | `D:\Documents-for-Expert-Design-Database\MAR\our architecture\README.md` + `01`-`12` | Quy tắc triển khai sau khi decisions đã approve |

Rules:

- File `16` đề xuất/chốt technical decision; file `13`/`14` là nơi phản ánh final sprint commitment.
- Nếu active decision register thêm decision P0 mới, README và file convention liên quan phải được kiểm tra lại.
- Không development commitment nếu P0 decision còn pending mà ảnh hưởng DB/auth/tenant/permission/import/testability.

### 6.3. Baseline decision pattern

Baseline MAR:

```text
Java 21
Spring Boot 3.5.x; exact patch version verified by Tech Lead during project bootstrap
Maven Wrapper 3.9.x
Spring MVC REST API only
Spring Security 6.x
Local JWT or compatible platform auth adapter
PostgreSQL 17
Flyway
Spring Data JPA / Hibernate
Root package: vn.mar
API base path: /api/v1
Flyway naming: VYYYYMMDD_NN__lower_snake_case_description.sql
```

### 6.4. OASIS reuse pattern

Reuse:

- Cách viết convention có cấu trúc.
- Naming/layer/package discipline.
- Spring Security, audit, logging, error, cache, scheduler pattern.
- Flyway/JPA discipline.
- Code review checklist mindset.

Không reuse trực tiếp:

- Domain OASIS.
- Package OASIS.
- Database syntax không thuộc PostgreSQL.
- UI server-render pattern.
- Module nặng chưa nằm trong scope Sprint 1.

### 6.5. Sprint 1 mapping pattern

| Sprint 1 area | Convention cần đọc |
|---|---|
| Dev bootstrap | `01`, `02`, `04`, `05`, `09`, `12` |
| Tenant/branch/user foundation | `01`, `02`, `03`, `04`, `05`, `08`, `09` |
| Auth/session tenant context | `01`, `03`, `05`, `06`, `07`, `08`, `09` |
| Permission matrix | `03`, `05`, `06`, `08`, `09` |
| Catalog | `02`, `03`, `04`, `09` |
| Import foundation | `04`, `08`, `09`, `11`, `12` |
| Import fixture/testability | `09`, `11`, `12` |
| Logging/error/debug | `06`, `07`, `09` |
| API error contract | `03`, `05`, `06`, `07`, `09` |
| Audit-sensitive setup changes | `04`, `05`, `07`, `08`, `09`, `12` |
| Release gate | `09`, `12` |

## 7. QUY TẮC RIÊNG CỦA BỘ CONVENTION MAR

### 7.1. Khi nào cập nhật convention

Cập nhật convention khi:

- Có quyết định kỹ thuật mới.
- Stack/version baseline đổi.
- Sprint review phát hiện rule thiếu.
- PR review lặp lại cùng một lỗi nhiều lần.
- BA scope đổi làm ảnh hưởng API/DB/security.

### 7.2. Ai được chốt

- Tech Lead/SA chốt technical convention.
- BA/PO chốt business scope/acceptance.
- QA lead góp ý testability/release gate.
- Dev có thể đề xuất nhưng không tự đổi rule nền khi chưa review.

### 7.3. Cách ghi thay đổi

Mỗi file có `LỊCH SỬ CẬP NHẬT`.

Thay đổi quan trọng phải ghi:

- Version.
- Ngày.
- Người cập nhật.
- Nội dung.
- Lý do nếu thay đổi rule đã chốt.

### 7.4. Scope integrity

Không dùng convention để “hứa” feature chưa làm.

Ví dụ:

- Có `import_batches`/`import_rows` không có nghĩa full lead import hoàn chỉnh.
- Có permission model không có nghĩa UI permission matrix đã làm.
- Có file storage convention không có nghĩa production object storage đã triển khai.

### 7.5. Language rule

- Convention docs viết tiếng Việt có dấu.
- Code comment, SQL comment, log message trong source dùng tiếng Anh.
- Error code dùng UPPER_SNAKE_CASE tiếng Anh.

### 7.6. Final alignment checklist before dev baseline freeze

Trước khi freeze dev baseline, Tech Lead/SA tick các điểm sau:

- [ ] Exact Spring Boot 3.5.x patch verified during bootstrap.
- [ ] Audit naming chốt: BA term `AuditLog`, DB table `audit_events`, Java entity `AuditEvent`.
- [ ] Permission schema chốt: `permission_profiles` là source of truth Sprint 1.
- [ ] Permission code import chốt: Sprint 1 dùng `import.view`/`import.manage`; `lead.import` reserved cho scope lead import/pipeline sau này.
- [ ] Pagination chốt `page` + `size`, không dùng `page_size`.
- [ ] List response chốt `ApiResponse<PageResponse<T>>`.
- [ ] Enum DB/API chốt `UPPER_SNAKE_CASE`.
- [ ] Import Sprint 1 chỉ foundation/history/testability, không claim preview/confirm/parser production.
- [ ] Raw row/import file PII access đã có permission, masking và audit rule.
- [ ] Active decision register đã map với README priority pattern.
- [ ] README index khớp đúng tên file thực tế trong folder.

## 8. VÍ DỤ CODE MẪU

### 8.1. Good example - dùng convention để implement story

```text
Story: R1A-BE-003 Permission matrix

Đọc:
- 03-rest-api-convention.md
- 05-security-auth-authz-convention.md
- 06-exception-error-i18n-convention.md
- 08-audit-convention.md
- 09-testing-quality-convention.md
- 12-dev-workflow-release-convention.md

Kết quả PR phải có:
- API contract
- Permission code
- Tenant rule
- Audit event
- Forbidden test
- Audit test
```

### 8.2. Good example - khi OASIS có pattern không hợp MAR

```text
OASIS có pattern A cho domain cũ.
MAR chỉ lấy cấu trúc kiểm soát/version/checklist.
MAR tự map lại bằng tenant/branch/user/catalog/import domain.
```

### 8.3. Bad example - copy reference mù

```text
Copy nguyên package/domain/DB syntax từ OASIS vào MAR.
```

Vấn đề:

- Sai root package `vn.mar`.
- Sai domain tuyển sinh.
- Sai PostgreSQL baseline.
- Tăng nợ kỹ thuật ngay từ bootstrap.

## 9. ANTI-PATTERNS CẦN TRÁNH

Không được:

- Xem OASIS là source code/template để copy nguyên.
- Bỏ qua `our architecture` và chỉ đọc file OASIS.
- Tự thêm công nghệ ngoài Spring Boot ecosystem khi chưa có approval.
- Dùng convention để thay thế BA acceptance criteria.
- Bỏ qua open decision nhưng vẫn development commitment.
- Chỉnh một file convention mà không cập nhật file liên quan.
- Đổi API/DB/security rule mà không cập nhật tests/release gate.
- Nói Sprint 1 đã hoàn thành feature ngoài scope.
- Để file convention thiếu 13 phần bắt buộc.

## 10. TESTING CONVENTIONS

### 10.1. Documentation completeness test

Mỗi file convention phải có:

- Metadata.
- 13 heading chính.
- Tài liệu liên quan.
- Lịch sử cập nhật.
- Mapping rõ với MAR.

### 10.2. Cross-reference test

Khi thêm/sửa file:

- README index phải cập nhật nếu thêm file.
- File liên quan phải được link trong `TÀI LIỆU LIÊN QUAN`.
- Không để reference trỏ sai tên file.

### 10.3. Stack consistency test

Kiểm tra không kéo nhầm:

- Domain/package cũ.
- DB syntax không thuộc PostgreSQL.
- UI server-render pattern.
- Dependency chưa approved.

### 10.4. Release readiness test

Trước kickoff/release gate, đọc tối thiểu:

- `README`
- `01`
- `03`
- `04`
- `05`
- `09`
- `12`

để xác nhận architecture, API, DB, security, test và workflow khớp nhau.

Đồng thời kiểm tra:

- Active decision sources ở section 6.2.1 còn đúng.
- Source of truth by topic ở section 5.3 không mâu thuẫn với file convention chi tiết.
- Final alignment checklist ở section 7.6 không còn P0 bỏ ngỏ.

### 10.5. Review cadence

Review convention:

- Trước technical kickoff.
- Khi Sprint 1 dev phát hiện gap.
- Trước release acceptance.
- Sau retrospective nếu có rule cần điều chỉnh.

## 11. CODE REVIEW CHECKLIST

- [ ] File có đủ metadata.
- [ ] File có đủ 13 phần chính.
- [ ] Rule map với MAR, không copy domain reference.
- [ ] Stack khớp Java 21/Spring Boot/PostgreSQL/Flyway.
- [ ] Không tạo mâu thuẫn với file convention khác.
- [ ] Source of truth by topic được cập nhật nếu đổi chủ đề/file.
- [ ] Active decision sources vẫn đúng tên file thực tế.
- [ ] Baseline Spring Boot patch đã verify hoặc còn ghi rõ cần verify khi bootstrap.
- [ ] Có ví dụ good/bad nếu chủ đề cần.
- [ ] Có anti-patterns.
- [ ] Có testing conventions.
- [ ] Có checklist review.
- [ ] Có tài liệu liên quan.
- [ ] Có lịch sử cập nhật.
- [ ] README index cập nhật nếu thêm/sửa phạm vi file.
- [ ] Final alignment checklist được cập nhật khi có P0 cross-file decision mới.

## 12. TÀI LIỆU LIÊN QUAN

- `01-architecture-baseline.md`
- `02-coding-package-convention.md`
- `03-rest-api-convention.md`
- `04-database-flyway-convention.md`
- `05-security-auth-authz-convention.md`
- `06-exception-error-i18n-convention.md`
- `07-logging-observability-convention.md`
- `08-audit-convention.md`
- `09-testing-quality-convention.md`
- `10-cache-async-scheduler-convention.md`
- `11-import-file-storage-convention.md`
- `12-dev-workflow-release-convention.md`
- `D:\Documents-for-Expert-Design-Database\MAR\docs BA\13-r1a-sprint-1-signoff-decision-log.md`
- `D:\Documents-for-Expert-Design-Database\MAR\docs BA\14-r1a-sprint-1-signoff-kickoff-pack.md`
- `D:\Documents-for-Expert-Design-Database\MAR\docs BA\16-techlead-sa-decision-register.md`
- `D:\Documents-for-Expert-Design-Database\MAR\docs BA\17-techlead-sa-decision-rationale.md`
- `D:\Documents-for-Expert-Design-Database\MAR\architecture\MAR_ARCHITECTURE_VERSION_CONVENTION.md`
- `D:\Documents-for-Expert-Design-Database\MAR\architecture\coding_convention.md`

## 13. LỊCH SỬ CẬP NHẬT

| Phiên bản | Ngày | Người cập nhật | Nội dung |
|---|---|---|---|
| MAR-CONV-1.1 | 30/06/2026 | Tech Lead / Solution Architect | Bổ sung active decision sources, source-of-truth by topic, mềm hóa exact Spring Boot patch verification, mở rộng Sprint 1 mapping và thêm final alignment checklist trước dev baseline freeze. |
| MAR-CONV-1.0 | 30/06/2026 | Tech Lead / Solution Architect | Tạo README index/governance đầy đủ cho bộ MAR convention, chuẩn hóa theo pattern OASIS và map với Sprint 1. |
