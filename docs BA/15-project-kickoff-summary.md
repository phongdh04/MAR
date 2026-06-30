# Project Kickoff Summary - MAR R1A Sprint 1

> Phiên bản cập nhật: `v2.1 - Architecture version/convention lock - 2026-06-29`.
> Baseline kỹ thuật hiện hành: `Java + Spring Boot ecosystem`, `PostgreSQL`, `Flyway`, `Spring Data JPA/Hibernate`.
> Ghi chú: file này đã được đồng bộ theo quyết định chọn Spring Boot và MAR-ARCH-1.0; development commitment vẫn phụ thuộc sign-off `SP1-D01` đến `SP1-D10`.
## 1. Trạng thái

| Hạng mục | Giá trị |
|---|---|
| Dự án | MAR Lead-to-Enrollment MVP |
| Giai đoạn | R1A Sprint 1 - Foundation Setup |
| Ngày lập | 2026-06-29 |
| Người soạn | Senior BA |
| Trạng thái | Draft for project kickoff |
| Ghi chú | Backend ecosystem đã chọn Spring Boot; chưa dùng làm development commitment nếu `SP1-D01` đến `SP1-D10` chưa được sign-off đầy đủ |

## 2. Tổng quan dự án

MAR là hệ thống tuyển sinh giúp trung tâm ngoại ngữ gom lead đa nguồn, quản lý tư vấn, đo SLA, appointment, enrollment/payment và attribution doanh thu theo campaign/ngôn ngữ/chương trình.

Sprint 1 chỉ tập trung xây foundation để các sprint sau có thể làm lead import, dedup, opportunity, pipeline, assignment/SLA và dashboard.

## 3. Scope Sprint 1

### In scope

- Tenant, branch, user, role và permission matrix nền.
- API-level permission enforcement và tenant isolation.
- Minimal AuditLog cho permission/sensitive setup changes.
- Language/program/course catalog baseline.
- ImportBatch/ImportRow/history/error rows foundation.
- Admin setup UI và catalog UI smoke.
- QA pack cho API/UI/security/tenant isolation.

### Out of scope

- Lead import preview/confirm production flow.
- Dedup/customer/opportunity.
- Pipeline/assignment/SLA nghiệp vụ.
- Appointment/payment/dashboard/revenue attribution.
- Full messaging automation, AI scoring, advanced attribution.

## 4. Effort sơ bộ theo vai trò

Đơn vị: person-day. Đây là estimate BA-level để kickoff, cần team estimate lại trước sprint commitment.

| Vai trò/thành viên | Trọng tâm | Effort sơ bộ |
|---|---|---|
| PO/Product Owner | Scope, non-goals, demo path, acceptance | 1-2 PD |
| Senior BA | Traceability, decision log, scope control, clarification | 2-3 PD |
| Solution Architect | Tenant isolation, permission architecture, audit, DB direction | 1-2 PD |
| Tech Lead | Technical decisions, migration/API baseline, review | 2-3 PD |
| Backend Engineer(s) | DB migration, tenant/branch/user/permission/catalog/import APIs | 10-14 PD |
| Frontend Engineer(s) | Admin setup UI, catalog UI, import history shell | 6-9 PD |
| QA Engineer | API/UI/security test cases, Tenant A/B isolation, release gate | 4-6 PD |
| UX/FE Lead | Screen flow, route/layout confirmation, UI review | 1-2 PD |
| DevOps/Infra | QA/staging env, migration runtime, seed execution, CI/CD support | 1-2 PD |

Tổng effort sơ bộ: khoảng 28-43 PD, tùy số lượng BE/FE, tình trạng auth sẵn có và mức độ hoàn thiện UI.

## 5. Công nghệ sử dụng/dự kiến

| Nhóm | Định hướng hiện tại | Trạng thái |
|---|---|---|
| Backend/API | Java 21 + Spring Boot 3.5.x target 3.5.14; REST API namespace `/api/v1`; no Thymeleaf | Locked by MAR-ARCH-1.0 |
| Database | PostgreSQL; dùng JSONB cho `mapping_config`, `raw_row`, `normalized_row` | User-selected baseline |
| Migration tool | Flyway; naming `VYYYYMMDD_NN__lower_snake_case_description.sql` | Locked by MAR-ARCH-1.0 |
| ORM/data access | Spring Data JPA/Hibernate; native query khi cần report/JSONB tối ưu | User-selected baseline |
| API error format | `{ error: { code, message, details[] }, meta: { request_id } }` | Proposed final `SP1-D09` |
| Auth/session | Platform auth có sẵn hoặc `R1A-TECH-001` dev/test fixture | Cần chốt `SP1-D07` |
| Request context | `tenant_id`, `actor_id`, `role_code` | Must-have |
| Authorization | API/service-level permission matrix; `TEAM` scope reserved/disabled | Must-have |
| Audit | Minimal append-only `audit_logs` | Must-have Sprint 1 |
| Enum convention | DB/API dùng `UPPER_SNAKE_CASE`; UI label tách riêng | Proposed final `SP1-D08` |
| Frontend | Web admin UI; framework FE chưa chốt trong BA docs | TBD |
| QA tools | Postman/Insomnia/automated API runner; Chrome/Edge cho UI smoke | Proposed |
| Seed data | System seed, demo seed, test seed tách riêng | Must-have |
| Import foundation | ImportBatch/ImportRow/history/errors; draft API hoặc fixture script để QA test | Cần chốt `SP1-D10` |

## 6. Quyết định cần chốt trước khi code

| ID | Nội dung cần chốt |
|---|---|
| SP1-D01 | MAR-ARCH-1.0 locked: Java 21, Spring Boot 3.5.x target 3.5.14, PostgreSQL 17, Flyway, JPA/Hibernate, root package `vn.mar`, REST API only/no Thymeleaf |
| SP1-D03 | Minimal AuditLog là mandatory |
| SP1-D07 | Auth/session tenant context hoặc `R1A-TECH-001` |
| SP1-D08 | Enum convention `UPPER_SNAKE_CASE` |
| SP1-D09 | API error envelope |
| SP1-D10 | Import draft API hoặc fixture script cho QA |

## 7. Kickoff recommendation

BA recommendation: dùng file này làm kickoff summary ngắn cho stakeholder/team, nhưng development chỉ bắt đầu khi `14-r1a-sprint-1-signoff-kickoff-pack.md` đã được chạy xong và file `13-r1a-sprint-1-signoff-decision-log.md` ghi rõ Go/Conditional Go/No-Go.
