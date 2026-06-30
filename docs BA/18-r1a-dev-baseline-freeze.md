# R1A Dev Baseline Freeze

> Phiên bản: `v1.0 - Dev baseline freeze - 2026-06-30`  
> Vai trò: biên bản chốt technical baseline trước khi tạo dev kickoff package và bootstrap backend.  
> Kết luận: **Ready for development commitment for backend bootstrap/foundation implementation**.  
> Lưu ý: đây không phải release acceptance; actual pass/fail vẫn phụ thuộc code, migration, test và QA evidence.

## 1. Kết luận freeze

Baseline Sprint 1 đã được chốt để chuyển từ document/planning sang implementation planning:

| Hạng mục | Quyết định |
|---|---|
| Backend | Java 21 + Spring Boot ecosystem |
| Spring Boot | 3.5.x; exact patch verified during bootstrap |
| Architecture style | Modular monolith, REST API only |
| Root package | `vn.mar` |
| API base path | `/api/v1` |
| Database | PostgreSQL 17 |
| Local/QA environment | Docker Compose |
| Migration | Flyway, schema tạo bằng migration, không tạo tay |
| ORM | Spring Data JPA/Hibernate |
| Auth baseline | Local JWT default nếu platform auth chưa sẵn sàng |
| Permission source | `permission_profiles` là source of truth Sprint 1 |
| Audit | BA term `AuditLog`, DB table `audit_events`, Java entity `AuditEvent` |
| Import Sprint 1 | Foundation/history/testability only; no parser/preview/confirm |
| Import testability | BE fixture command là default |
| Error envelope | `{ error: { code, message, details[] }, meta: { request_id } }` |
| Request id | `RequestIdFilter` là source duy nhất |

## 2. Docker decision

User đã chốt: **dùng Docker**.

Docker baseline Sprint 1:

- Dùng Docker Compose cho PostgreSQL 17 local/QA.
- Backend Spring Boot connect vào DB qua environment variables.
- Có seed/fixture runner cho system/demo/test/import data.
- Database instance được tạo trước, schema/tables do Flyway tạo.
- Không dùng DB local thủ công làm baseline vì khó repeatable.
- Không đưa Kubernetes vào Sprint 1 vì overkill.

DB local mặc định khi bootstrap:

| Config | Giá trị đề xuất |
|---|---|
| DB name | `mar` |
| App user | `mar_app` |
| Host | `localhost` |
| Port | `5432` |
| Schema | `public` |
| Migration owner | Flyway |

Password local/dev sẽ nằm trong `.env.local` hoặc env var, không commit secret thật.

## 3. Final alignment checklist result

Theo `our architecture/README.md`, kết quả freeze:

- [x] Exact Spring Boot 3.5.x patch verified during bootstrap.
- [x] Audit naming chốt: BA term `AuditLog`, DB table `audit_events`, Java entity `AuditEvent`.
- [x] Permission schema chốt: `permission_profiles` là source of truth Sprint 1.
- [x] Permission code import chốt: Sprint 1 dùng `import.view`/`import.manage`; `lead.import` reserved cho scope lead import/pipeline sau này.
- [x] Pagination chốt `page` + `size`, không dùng `page_size`.
- [x] List response chốt `ApiResponse<PageResponse<T>>`.
- [x] Enum DB/API chốt `UPPER_SNAKE_CASE`.
- [x] Import Sprint 1 chỉ foundation/history/testability, không claim preview/confirm/parser production.
- [x] Raw row/import file PII access đã có permission, masking và audit rule.
- [x] Active decision register đã map với README priority pattern.
- [x] README index khớp đúng tên file thực tế trong folder.

Ghi chú: Spring Boot exact patch sẽ được verify khi bootstrap project thật. Nếu `3.5.14` chưa available hoặc không phù hợp, dùng latest approved `3.5.x` patch và cập nhật convention/history.

## 4. Development commitment decision

| Item | Result |
|---|---|
| Technical baseline | Frozen |
| Docker decision | Approved |
| Backend bootstrap | Go |
| Full Sprint 1 release acceptance | Pending implementation evidence |
| Remaining blocker before backend bootstrap | None |
| Remaining formal work | Backend bootstrap using `19-r1a-dev-kickoff-package.md` |

Kết luận:

```text
Ready for development commitment for backend bootstrap/foundation implementation.
Not yet release-accepted because code, migration, test and QA evidence do not exist yet.
```

## 5. Next implementation order

Sau freeze, thứ tự làm tiếp:

1. Đọc `19-r1a-dev-kickoff-package.md`.
2. Bootstrap Spring Boot backend.
3. Tạo Docker Compose PostgreSQL 17.
4. Tạo database rỗng `mar`.
5. Cấu hình datasource/local/qa/prod/test.
6. Tạo Flyway baseline migration.
7. Chạy app/Flyway để tạo schema.
8. Implement request id + error envelope.
9. Implement Local JWT/request context.
10. Implement tenant/branch/user vertical slice.

## 6. Traceability

| File | Trạng thái sau freeze |
|---|---|
| `13-r1a-sprint-1-signoff-decision-log.md` | Updated to baseline frozen / ready for backend bootstrap |
| `14-r1a-sprint-1-signoff-kickoff-pack.md` | Updated to meeting completed - Go for backend bootstrap |
| `16-techlead-sa-decision-register.md` | Updated P0 decisions and Docker baseline |
| `17-techlead-sa-decision-rationale.md` | Updated rationale for Docker and implementation baseline |
| `19-r1a-dev-kickoff-package.md` | Created to drive backend bootstrap/foundation implementation |
| `our architecture/README.md` | Final alignment checklist used as freeze gate |

## 7. Items resolved by dev kickoff package

Các item sau đã được đưa vào `19-r1a-dev-kickoff-package.md`:

- Ticket breakdown theo thứ tự bootstrap.
- Owner role cho từng ticket.
- Docker Compose file path và env naming.
- Flyway migration naming/sequence.
- CI command tối thiểu.
- Test evidence required per ticket.
- Release gate evidence format.
