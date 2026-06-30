# R1A Sprint 1 Sign-off Kickoff Pack

> Phiên bản cập nhật: `v2.2 - Dev baseline freeze - 2026-06-30`.
> Baseline kỹ thuật hiện hành: `Java 21 + Spring Boot ecosystem`, `PostgreSQL 17`, `Flyway`, `Spring Data JPA/Hibernate`, `Docker Compose local/QA`.
> Ghi chú: technical baseline đã freeze; development có thể chuyển sang dev kickoff/backend bootstrap. Actual release acceptance vẫn phụ thuộc implementation evidence và QA gate.
## 1. Trạng thái tài liệu

| Hạng mục | Giá trị |
|---|---|
| Tên tài liệu | R1A Sprint 1 Sign-off Kickoff Pack |
| Vai trò tài liệu | Agenda, decision ballot và biên bản chốt trước khi dev commit Sprint 1 |
| Người soạn | Senior BA |
| Ngày lập | 2026-06-29 |
| Trạng thái | Baseline freeze completed |
| Development commitment | Ready for backend bootstrap/foundation implementation planning |
| Trạng thái sau meeting | `Meeting completed - Go for backend bootstrap` |
| Nguồn baseline | `10-r1a-sprint-1-ready-package.md`, `11-r1a-sprint-1-technical-handoff.md`, `12-r1a-sprint-1-qa-acceptance-pack.md`, `13-r1a-sprint-1-signoff-decision-log.md` |

Tài liệu này dùng để chạy buổi Sprint 1 sign-off/kickoff. Mục tiêu là chốt các Sprint 1 P0 decisions, xác nhận release gate pass condition, và quyết định Sprint 1 có được chuyển sang `Ready for development` hay chưa.

Sau meeting, BA phải đổi trạng thái tài liệu theo kết quả thực tế và cập nhật lại các linked docs nếu có decision `Approved with change`.

## 2. Kết quả cần đạt sau buổi họp

| Kết quả | Điều kiện đạt |
|---|---|
| Sprint 1 scope accepted | PO/BA xác nhận đúng foundation scope, không claim Sprint 2 feature |
| Technical baseline accepted | Tech Lead/SA xác nhận DB, auth/tenant context, permission, enum, API error envelope |
| QA baseline accepted | QA Lead xác nhận P0 API/UI/security tests executable |
| Release gate accepted | Owner từng gate đồng ý pass condition |
| Development commitment decision | Go / Conditional Go / No-Go được ghi rõ |
| Change traceability | Mọi thay đổi được approve phải được phản ánh vào linked docs trong 1 working day |

Không được chuyển sang development commitment nếu còn một trong các blocker:

- Spring Boot/PostgreSQL/Flyway/Docker baseline chưa reflected trong project bootstrap plan.
- Auth/session tenant context chưa rõ hoặc chưa có `R1A-TECH-001`.
- Permission chỉ enforce ở UI, chưa enforce ở API/service.
- Minimal audit event không nằm trong Sprint 1.
- Enum convention/API error envelope chưa thống nhất.
- Import foundation testability chưa có fixture command hoặc approved local/QA internal draft API.
- QA không test được tenant isolation/detail-level access.

## 3. Thành phần tham dự

| Vai trò | Bắt buộc? | Nội dung cần chốt |
|---|---|---|
| PO/Product Owner | Yes | Scope, non-goals, demo path, acceptance |
| Tech Lead | Yes | DB/migration, API contract, implementation feasibility |
| Solution Architect | Yes | Multi-tenant, permission architecture, audit, system boundary |
| QA Lead | Yes | Testability, release gate, severity, automation/manual scope |
| UX/FE Lead | Should | Admin setup routes, FE dependency on GET/list/detail APIs |
| DevOps/Infra | Should | DB migration runtime, environment, CI/CD, seed execution |
| Security/Data Protection | Optional/Should | PII, tenant isolation, audit and access policy nếu team có vai trò riêng |
| Senior BA | Yes | Facilitation, decision traceability, scope integrity |

## 4. Pre-read bắt buộc

| Tài liệu | Người cần đọc | Mục tiêu đọc |
|---|---|---|
| `10-r1a-sprint-1-ready-package.md` | PO, Tech Lead, QA, BA | Hiểu Sprint 1 scope và ticket readiness |
| `11-r1a-sprint-1-technical-handoff.md` | Tech Lead, SA, QA | Hiểu technical baseline, API/schema, auth/audit assumptions |
| `12-r1a-sprint-1-qa-acceptance-pack.md` | QA, Tech Lead, PO | Hiểu test scope, release gate, demo path |
| `13-r1a-sprint-1-signoff-decision-log.md` | Tất cả approvers | Chốt `SP1-D01` đến `SP1-D10` |

Pre-read phải hoàn tất trước meeting. Nếu approver chưa đọc phần liên quan đến vai trò của mình, decision của role đó không được xem là final; BA ghi lại là `Blocked` hoặc `Needs follow-up`.

## 5. Agenda đề xuất

### 5.1. Pre-vote rule

Nếu giữ meeting 60 phút, tất cả approvers phải pre-vote trước meeting theo ba giá trị:

- `Approved`
- `Approved with change`
- `Blocked`

Meeting 60 phút chỉ xử lý decision có disagreement hoặc blocker. Nếu chưa có pre-vote, dùng agenda 75-90 phút.

### 5.2. Agenda khuyến nghị 75 phút

| Thời lượng | Nội dung | Owner |
|---|---|---|
| 5 phút | Mục tiêu cuộc họp và nguyên tắc Go/No-Go | BA |
| 10 phút | Review Sprint 1 scope và non-goals | PO + BA |
| 30 phút | Chốt `SP1-D01` đến `SP1-D10`, ưu tiên decision có blocker | Tech Lead + SA + QA |
| 15 phút | Review release gate, QA testability và demo path | QA + PO + Tech Lead |
| 10 phút | Xác nhận blocker/action items | Tất cả |
| 5 phút | Ghi quyết định Go / Conditional Go / No-Go | BA + PO |

Decision kỹ thuật nền nên chốt theo thứ tự: `SP1-D01`, `SP1-D07`, `SP1-D03`, `SP1-D04`, `SP1-D08`, `SP1-D09`, `SP1-D10`, rồi mới đến `SP1-D02`, `SP1-D05`, `SP1-D06`.

## 6. Decision ballot

| ID | Decision | BA proposed final | Required meeting output | Approver cần chốt | Pre-vote | Meeting decision | Notes/action |
|---|---|---|---|---|---|---|---|
| SP1-D01 | Spring Boot/PostgreSQL/Flyway version and convention | Java 21, Spring Boot 3.5.x exact patch verified during bootstrap, PostgreSQL 17, Flyway, JPA/Hibernate, root package `vn.mar`, REST API only, no Thymeleaf, Docker Compose local/QA | Bootstrap must follow MAR-CONV-1.1 and Docker PostgreSQL baseline | Tech Lead/SA | Approved | Approved | User chose Spring Boot ecosystem and Docker; SA mapped OASIS conventions to MAR |
| SP1-D02 | Team entity in Sprint 1 | Defer Team; dùng branch + role scope; reserve `TEAM` scope nhưng disabled | Confirm Team is deferred and `TEAM` scope is reserved/disabled only | PO/SA | Approved | Approved | |
| SP1-D03 | Minimal AuditLog | BA term `AuditLog`; implementation uses append-only `audit_events` table/service for permission and sensitive setup changes | Confirm `audit_events` minimal table/service is in Sprint 1 and required for acceptance | Tech Lead/QA | Approved | Approved | |
| SP1-D04 | GET/list/detail APIs | Include cho tenant, branch, user, permission, catalog, import history | Confirm list/detail endpoints needed by FE/QA are in Sprint 1 | Tech Lead/FE Lead | Approved | Approved | |
| SP1-D05 | Import foundation depth | Chỉ ImportBatch/ImportRow/history; không preview/confirm parser | Confirm PO demo will not claim preview/confirm/parser as complete | PO/Tech Lead | Approved | Approved | |
| SP1-D06 | Seed data | Tách system seed, demo seed, test seed; demo seed không phải production migration data | Confirm owner and execution path for each seed type | Tech Lead/QA | Approved | Approved | |
| SP1-D07 | Auth and tenant context | Local JWT default nếu platform auth chưa sẵn; request context có `tenant_id`, `actor_id`, `role_code` | Confirm local JWT/platform auth baseline is first prerequisite before permission tickets | Tech Lead/SA | Approved | Approved | |
| SP1-D08 | Enum convention | DB/API dùng `UPPER_SNAKE_CASE`; UI label tách riêng/localized | Confirm shared/common contract location or documented enum source of truth | Tech Lead/SA/FE Lead | Approved | Approved | |
| SP1-D09 | API error envelope | Chuẩn `{ error: { code, message, details[] }, meta: { request_id } }` | Confirm envelope applies to validation, permission and conflict errors | Tech Lead/QA | Approved | Approved | |
| SP1-D10 | Import foundation testability | Default là BE fixture command; internal draft API chỉ optional local/QA nếu FE demo thật sự cần | Fixture script and QA usage rule are default | Tech Lead/QA | Approved | Approved | |

Pre-vote values:

- `Approved`
- `Approved with change`
- `Blocked`
- `Not reviewed`

Meeting decision values:

- `Approved`: chốt đúng theo BA proposed final.
- `Approved with change`: chốt nhưng có thay đổi; BA phải cập nhật linked docs.
- `Blocked`: chưa thể dev commit vì quyết định còn blocker.

Notes for critical decisions:

- `SP1-D01` đã freeze theo MAR-CONV-1.1; không approve implementation nếu project bootstrap không theo Java 21, Spring Boot 3.5.x exact patch verified during bootstrap, PostgreSQL 17, Docker Compose local/QA, Flyway naming `VYYYYMMDD_NN__...`, root package `vn.mar`, REST API only/no Thymeleaf.
- `SP1-D07` nếu chưa có auth/session sẵn thì `R1A-TECH-001` là prerequisite của các ticket có permission/tenant isolation.
- `SP1-D10` đã chọn fixture script là default. QA không test create batch API nếu không có draft API; QA test GET history/detail/errors, tenant isolation và data constraints.

## 7. Release gate ballot

| Gate | Owner | Pass condition | Pass condition accepted? | Actual result after Sprint | Notes/action |
|---|---|---|---|---|---|
| DB migration | Tech Lead/BE | Runs clean on fresh Docker PostgreSQL 17 environment | Accepted | Pending implementation | Docker Compose local/QA selected |
| Seed data | BE/QA | System seed available; demo/test seed repeatable | Accepted | Pending implementation | |
| Auth/session | Tech Lead/SA/QA | Local JWT or platform auth/session baseline verified | Accepted | Pending implementation | Local JWT default if platform auth not ready |
| Enum convention | Tech Lead/QA | DB/API enums use `UPPER_SNAKE_CASE`; UI labels separate | Accepted | Pending implementation | |
| API error envelope | Tech Lead/QA | Validation, permission and conflict errors follow signed-off envelope | Accepted | Pending implementation | |
| Import testability | BE/QA | Fixture command supports ImportBatch/ImportRow tests | Accepted | Pending implementation | Internal draft API optional only local/QA |
| API P0 tests | QA/BE | All P0 API tests executable and accepted | TBD | Pending | |
| UI smoke | QA/FE | Admin setup and catalog smoke tests accepted | TBD | Pending | |
| Permission | QA/BE | Unauthorized actions blocked at API/service layer | TBD | Pending | |
| Tenant isolation | QA/BE/SA | Cross-tenant list/detail access blocked | TBD | Pending | |
| Audit minimal | QA/BE | Permission and sensitive setup changes logged to `audit_events` | Accepted | Pending implementation | |
| Demo path | PO/QA | PO demo script accepted | TBD | Pending | |
| Scope integrity | PO/BA | No Sprint 2 feature claimed as complete | TBD | Pending | |

Ở sign-off/kickoff meeting, team chỉ accept pass condition. `Actual result after Sprint` chỉ được chuyển sang `Pass` hoặc `Fail` sau khi implementation và QA validation thực tế hoàn tất.

## 8. Câu hỏi chốt theo vai trò

### 8.1. PO/Product Owner

1. PO có đồng ý Sprint 1 chỉ là foundation setup, chưa claim import preview/confirm, dedup, pipeline, assignment/SLA, dashboard/payment/revenue không?
2. PO có đồng ý demo path Sprint 1 chỉ chứng minh tenant/branch/user/permission/catalog/import history foundation không?
3. PO có blocker nào về business scope trước khi dev commit không?

### 8.2. Tech Lead

1. Spring Boot ecosystem, PostgreSQL, Flyway và JPA/Hibernate đã đủ rõ để bắt đầu ticket BE đầu tiên chưa?
2. GET/list/detail APIs trong Sprint 1 có khả thi không?
3. Auth/session tenant context có sẵn chưa, hay cần tạo `R1A-TECH-001` trước?
4. Minimal AuditLog, enum convention và API error envelope có được đưa vào implementation baseline không?
5. Enum convention và API error envelope có được đưa vào shared package/library/common contract hoặc source of truth ngay Sprint 1 không?

### 8.3. Solution Architect

1. Tenant isolation có được enforce ở API/service/database access pattern không?
2. Permission architecture có đủ guardrail để chặn direct API bypass không?
3. Team entity defer nhưng reserve `TEAM` scope có ổn cho kiến trúc mở rộng không?

### 8.4. QA Lead

1. QA có đủ fixture/test data để test Tenant A/Tenant B và detail-level access không?
2. Import foundation testability đã executable chưa: internal draft API hay fixture script?
3. P0 API/UI/security tests trong file 12 có đủ để nghiệm thu Sprint 1 không?
4. QA có test được cả list-level và detail-level cross-tenant access không?

### 8.5. UX/FE Lead

1. FE có đủ GET/list/detail APIs để làm admin setup và catalog screens không?
2. FE có đồng ý tách UI labels khỏi DB/API enum `UPPER_SNAKE_CASE` không?
3. Có route/screen dependency nào block Sprint 1 không?

### 8.6. DevOps/Infra và Security/Data Protection nếu có

1. Environment, migration runtime, seed execution và CI/CD có đủ để chạy Sprint 1 foundation không?
2. PII, tenant isolation, audit log và data access policy có blocker nào trước development commitment không?

## 9. Go/No-Go template

| Hạng mục | Kết quả |
|---|---|
| Decision status | All Sprint 1 P0 baseline decisions approved/frozen |
| Release gate status | Pass conditions accepted; actual results pending implementation |
| Blockers | None for backend bootstrap |
| Approved changes to docs | Docker Compose local/QA baseline; `audit_events`; fixture command default; exact Spring Boot patch verified during bootstrap |
| Final decision | Go for backend bootstrap/foundation implementation planning |
| Effective status after meeting | Ready for development commitment |

### Go

Chỉ chọn `Go` khi:

- Active Sprint 1 P0 decisions đều `Approved`.
- Release gate được accepted bởi owner tương ứng.
- Không còn blocker trong scope Sprint 1.
- PO/Tech Lead/SA/QA/BA đồng ý development commitment.

### Conditional Go

Chọn `Conditional Go` chỉ khi action còn lại là nhỏ, có owner, due date, linked decision/gate và xác nhận không ảnh hưởng API/schema/security baseline.

Không dùng `Conditional Go` nếu còn mở bất kỳ nền tảng bắt buộc nào sau đây:

- MAR-ARCH-1.0 project bootstrap verification.
- Auth/tenant context.
- API-level permission guardrail.
- Minimal AuditLog.
- Enum convention.
- API error envelope.
- Import foundation testability.
- Tenant isolation/detail-level access testability.

Nếu action ảnh hưởng schema, API contract, security, tenant isolation, audit hoặc QA testability, decision phải là `No-Go` hoặc `Blocked`, không phải `Conditional Go`.

### No-Go

Chọn `No-Go` nếu còn blocker ảnh hưởng development commitment hoặc QA testability.

## 10. Action items sau meeting

| ID | Action | Linked decision/gate | Owner | Due date | Blocker? | Required before code? | Required before acceptance? | Status |
|---|---|---|---|---|---|---|---|---|
| ACT-01 | Bootstrap project theo MAR-CONV-1.1: Java 21, Spring Boot 3.5.x, `vn.mar`, REST API only | `SP1-D01` / bootstrap | Tech Lead | Next step | Yes | Yes | Yes | Open |
| ACT-02 | Tạo Docker Compose PostgreSQL 17 local/QA + database rỗng `mar` | `SP1-D01` / DB environment | Tech Lead/BE | Next step | Yes | Yes | Yes | Open |
| ACT-03 | Implement Local JWT/request context nếu platform auth chưa sẵn sàng | `SP1-D07` / Auth/session | Tech Lead/SA | Before protected API | Yes | Yes | Yes | Open |
| ACT-04 | Cung cấp BE fixture command cho ImportBatch/ImportRow | `SP1-D10` / Import testability | BE/QA | Before QA import tests | Yes | Yes | Yes | Open |
| ACT-05 | Cập nhật linked docs nếu decision được approve with change | Change traceability | BA | Within 1 working day | Depends | Depends | Yes | Open if needed |
| ACT-06 | Bổ sung demo/test seed nếu thiếu dữ liệu QA hoặc PO demo | `SP1-D06` / Seed data | BE/QA | Before acceptance | Depends | No if not blocking code | Yes | Open if needed |

## 11. Kịch bản điều hành meeting

### 11.1. Mở meeting

BA mở meeting bằng câu chốt:

```text
Sprint 1 không phải build toàn bộ R1A. Sprint 1 chỉ là foundation setup có thể test được, có tenant isolation, permission guardrail, audit, enum/error standard và import foundation rõ ràng.
```

Mục tiêu cuộc họp là chốt `SP1-D01` đến `SP1-D10`, accept release gate pass condition và quyết định `Go`, `Conditional Go` hoặc `No-Go`.

### 11.2. Scope confirmation

PO phải xác nhận Sprint 1 không claim:

- Import preview/confirm.
- CSV parser production flow.
- Dedup/customer/opportunity.
- Pipeline/assignment/SLA nghiệp vụ.
- Dashboard/payment/revenue.

Nếu PO không đồng ý non-goals, dừng development commitment và chuyển về scope renegotiation.

### 11.3. Decision flow

Chốt decision theo thứ tự:

1. `SP1-D01` MAR-ARCH-1.0 project bootstrap verification.
2. `SP1-D07` Auth and tenant context.
3. `SP1-D03` Minimal AuditLog.
4. `SP1-D04` GET/list/detail APIs.
5. `SP1-D08` Enum convention.
6. `SP1-D09` API error envelope.
7. `SP1-D10` Import foundation testability.
8. `SP1-D02` Team entity.
9. `SP1-D05` Import foundation depth.
10. `SP1-D06` Seed data.

### 11.4. Release gate confirmation

QA Lead xác nhận:

- QA test được tenant isolation cả list-level và detail-level.
- QA test được permission bypass ở API/service layer.
- QA test được import foundation bằng internal draft API hoặc fixture script.
- QA hiểu rằng meeting này accept pass condition; actual result chỉ có sau khi test thật.

### 11.5. Post-meeting update protocol

| Meeting result | BA action |
|---|---|
| Go | Update file 13 và file 14 status thành `Meeting completed - Go`; ghi rõ effective status `Ready for development` |
| Conditional Go | Ghi rõ action nhỏ còn lại, owner, due date, linked decision/gate; không dùng nếu action ảnh hưởng foundation blocker |
| No-Go | Ghi blocker, owner, required decision/gate và điều kiện để tổ chức lại sign-off |
| Approved with change | Cập nhật linked docs trong 1 working day và ghi traceability |

## 12. BA recommendation trước meeting

| Item | Recommendation |
|---|---|
| Go to sign-off meeting | Yes |
| Go to development before meeting | No |
| Expected outcome | Go or Conditional Go if `SP1-D01` to `SP1-D10` are approved |
| BA focus | Traceability, scope integrity, no silent scope creep |

BA nên mở meeting bằng câu chốt: Sprint 1 không phải build toàn bộ R1A; Sprint 1 chỉ là foundation setup có thể test được, có tenant isolation, permission guardrail, audit, enum/error standard và import foundation rõ ràng.
