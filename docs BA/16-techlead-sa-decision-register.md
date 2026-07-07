# Tech Lead / Solution Architect Decision Register

> Phiên bản cập nhật: `v2.3 - Platform tenant permission ADR - 2026-07-07`.
> Baseline kỹ thuật hiện hành: `Java 21 + Spring Boot ecosystem`, `PostgreSQL 17`, `Flyway`, `Spring Data JPA/Hibernate`, `Docker Compose local/QA`.
> Ghi chú: technical baseline đã freeze; development commitment được mở cho backend bootstrap/foundation implementation planning.
## 1. Mục đích

Tài liệu này tổng hợp các điểm **cần chốt** trong bộ `docs BA` dưới góc nhìn **Tech Lead / Solution Architect**. Các item trùng nhau giữa nhiều file đã được gộp lại để dùng làm checklist sign-off kỹ thuật.

Trạng thái tổng thể: **Ready for development commitment for backend bootstrap/foundation implementation**. Các quyết định P0 đã được freeze trong `13-r1a-sprint-1-signoff-decision-log.md` / `14-r1a-sprint-1-signoff-kickoff-pack.md`; actual release gate vẫn pending implementation evidence.

## 2. Đề xuất tổng quan của Tech Lead/SA

Map với hiện trạng workspace: trong `Multi-Agents-System` hiện có các backend Java/Spring Boot dùng PostgreSQL, JPA, Spring Security, Redis và Flyway ở `DATN_backend` / `backend_hackathon`. MAR hiện vẫn là bộ BA docs, chưa thấy codebase MAR riêng.

User đã chọn Spring Boot ecosystem. Vì vậy baseline kỹ thuật được cập nhật:

| Nhóm | Đề xuất |
|---|---|
| Backend | Java + Spring Boot ecosystem |
| Frontend | React + TypeScript; Vite SPA cho admin Sprint 1, Next.js nếu cần app routing/SSR sau |
| Database | PostgreSQL |
| Migration/ORM | Flyway + Spring Data JPA/Hibernate |
| API style | REST `/api/v1` + OpenAPI sau khi chốt contract |
| Auth/session | Dùng platform auth nếu có; nếu chưa có, tạo `R1A-TECH-001` làm ticket đầu tiên |
| Authorization | Guard/middleware + service-level permission check; không chỉ rely UI |
| Multi-tenant | Mọi bảng nghiệp vụ có `tenant_id`; mọi API chạy trong request context |
| Error contract | `{ error: { code, message, details[] }, meta: { request_id } }` |
| Enum | DB/API dùng `UPPER_SNAKE_CASE`; UI label tách riêng |
| Local/QA env | Docker Compose cho PostgreSQL 17 + app + seed runner |

Ghi chú: NestJS/Prisma không còn là baseline sau quyết định của User. Chỉ xem lại nếu sau này có quyết định chính thức đổi sang greenfield full-stack TypeScript.

## 3. P0 - phải chốt trước khi dev code Sprint 1

| ID | Vấn đề cần chốt | Đề xuất Tech Lead/SA | Owner chốt | Block code? | Nguồn file |
|---|---|---|---|---|---|
| P0-01 | Spring Boot/PostgreSQL/Flyway baseline | Frozen: Java 21, Spring Boot 3.5.x exact patch verified during bootstrap, PostgreSQL 17, Flyway naming `VYYYYMMDD_NN__...`, JPA/Hibernate, root package `vn.mar`, REST API only/no Thymeleaf, Docker Compose local/QA. | Tech Lead + SA | Cleared for bootstrap | `06`, `11`, `13`, `14`, `15`, `our architecture/README.md` |
| P0-02 | Backend framework | User đã chọn Spring Boot ecosystem; MAR-CONV-1.1 locks Spring Boot modular monolith package structure under `vn.mar`. | Tech Lead | Cleared for bootstrap | `15`, `05`, workspace `DATN_backend`, `backend_hackathon`, `our architecture/01` |
| P0-03 | Frontend framework | React + TypeScript. Với admin Sprint 1, Vite SPA đủ nhẹ; Next.js chỉ cần nếu roadmap cần SSR/app routing mạnh. | FE Lead + Tech Lead | Yes | `15`, `07`, workspace React/Vite sample |
| P0-04 | Auth/session tenant context | Nếu platform auth chưa sẵn, tạo `R1A-TECH-001` là ticket đầu tiên. Request context phải có `tenant_id`, `actor_id`, `role_code`. | Tech Lead + SA | Yes | `10`, `11`, `12`, `13`, `14`, `15` |
| P0-05 | Multi-tenant isolation | Mọi table/API Sprint 1 tenant-scoped; detail endpoint cũng phải chặn cross-tenant. | SA + BE + QA | Yes | `06`, `11`, `12`, `13`, `14` |
| P0-06 | API-level permission guardrail | Permission enforce ở API/service layer; UI chỉ phản ánh quyền. | Tech Lead + SA | Yes | `04`, `05`, `10`, `11`, `12`, `13` |
| P0-07 | Minimal AuditLog | BA term `AuditLog`; implementation source of truth là append-only `audit_events` table/service cho permission và sensitive setup changes. | Tech Lead + QA | Cleared for bootstrap | `10`, `11`, `12`, `13`, `14`, `15`, `our architecture/08` |
| P0-08 | API error envelope | Chốt common error envelope và dùng đồng nhất cho validation, permission, conflict. | Tech Lead + QA | Yes | `05`, `12`, `13`, `14`, `15` |
| P0-09 | Enum source of truth | DB/API dùng `UPPER_SNAKE_CASE`; tạo shared enum/common contract hoặc documented enum registry. | Tech Lead + SA + FE Lead | Yes | `05`, `06`, `11`, `13`, `14`, `15` |
| P0-10 | Import foundation testability | Sprint 1 dùng BE fixture command để tạo ImportBatch/ImportRow cho QA. Chỉ tạo internal draft API nếu FE demo thực sự cần và chỉ local/QA. | Tech Lead + BE + QA | Cleared for bootstrap | `10`, `12`, `13`, `14`, `15`, `our architecture/11` |
| P0-11 | Seed strategy | Tách system seed, demo seed, test seed. Demo/test seed không nằm trong production migration mặc định. | Tech Lead + QA + DevOps | Yes | `10`, `11`, `12`, `13`, `14` |
| P0-12 | GET/list/detail APIs Sprint 1 | Include tenant, branch, user, permission, catalog, import history list/detail để FE/QA không bị block. | Tech Lead + FE Lead | Yes | `10`, `11`, `12`, `13`, `14` |
| P0-13 | Team entity/scope | Sprint 1 không thêm Team entity. Reserve `TEAM` scope nhưng disabled; dùng branch + role + own. | SA + PO + Tech Lead | Yes | `08`, `10`, `11`, `13`, `14` |
| P0-14 | Sprint 1 non-goals | PO phải xác nhận không claim import preview/confirm, dedup, opportunity, pipeline, assignment/SLA, dashboard/payment. | PO + BA + Tech Lead | Yes | `10`, `13`, `14`, `15` |
| P0-15 | Final Go/No-Go | Go for backend bootstrap/foundation implementation planning. Actual release gate remains pending implementation evidence. | PO + Tech Lead + SA + QA | Cleared for bootstrap | `13`, `14`, `README`, `18` |
| P0-16 | Local/QA Docker baseline | User chốt dùng Docker. Local/QA baseline là Docker Compose PostgreSQL 17 + app + seed/fixture runner; DB schema tạo bằng Flyway, không tạo tay. | Tech Lead + DevOps/BE | Cleared for bootstrap | `13`, `14`, `16`, `17`, `our architecture/04`, `our architecture/12` |
| P0-17 | Platform tenant creation permission | ADR accepted: đề xuất `platform.tenant.manage` cho `POST /api/v1/tenants`; giữ `tenant.manage` cho update/get tenant-scoped. Implementation P2b đang blocked cho tới khi có bootstrap/platform actor seed evidence. | SA + Tech Lead + Security/QA | Blocked for code until bootstrap actor decision | `20`, `our architecture/03`, `our architecture/05`, `convention-remediation-plan` |

## 4. P1 - cần chốt trước khi triển khai các story R1A sau Sprint 1

| ID | Vấn đề cần chốt | Đề xuất Tech Lead/SA | Owner chốt | Nguồn file |
|---|---|---|---|---|
| P1-01 | CustomerIdentity | Bắt buộc cho R1A P0-lite: PHONE, EMAIL, ZALO_ID trước; Facebook/cookie/platform mở sau. | SA + Tech Lead + PO | `04`, `06`, `08`, `brief` |
| P1-02 | Duplicate email exact nhưng phone khác | Không tạo opportunity vào pipeline chính ngay; tạo DuplicateCase `NEEDS_REVIEW`. | PO + Tech Lead | `08`, `03`, `04` |
| P1-03 | Strict unique phone/email | Không strict unique trên CustomerProfile; dùng CustomerIdentity + service dedup. | SA + PO | `06`, `08`, `brief` |
| P1-04 | Merge/unmerge permission | Admin được unmerge; Sales Lead có thể link/merge/ignore nếu policy bật nhưng không unmerge P0. | PO + Admin + SA | `08`, `09` |
| P1-05 | Activity/InteractionLog | Must-have cho SLA/contact success/note/follow-up; không tính SLA từ stage. | SA + Tech Lead + Sales Lead | `04`, `06`, `07`, `08`, `brief` |
| P1-06 | SLA hit vs contact success | SLA hit = first valid outbound activity trong SLA; contact success = connected/replied/contacted. | PO + Sales Lead + QA | `07`, `08`, `brief` |
| P1-07 | WorkingHours/SLA policy | Có `working_hours_configs` + `sla_policies`; default pilot 08:00-18:00 Mon-Sat theo tenant timezone. | SA + Tech Lead + PO | `04`, `06`, `07`, `08`, `brief` |
| P1-08 | Stage transition enforcement | Centralize ở Opportunity/Pipeline service; UI không phải nguồn enforce. | SA + Tech Lead | `05`, `08` |
| P1-09 | Assignment workload | R1A dùng realtime count đơn giản; sau này tối ưu bằng snapshot nếu performance không đủ. | Tech Lead | `08` |
| P1-10 | Assignment history/pool state | Có `assignment_history` và `assignment_pool_state` để reassign audit và round-robin ổn định. | SA + Tech Lead | `06`, `08` |
| P1-11 | Import preview sync/async | File nhỏ sync; file lớn async job + polling. UI phải hỗ trợ processing state nếu async. | Tech Lead + FE Lead | `05`, `08`, `10` |
| P1-12 | CSV vs Google Sheet | CSV upload là must-have; Google Sheet URL/import là should-have; không live sync liên tục trong R1A. | PO + Tech Lead | `02`, `08`, `10` |
| P1-13 | Webhook sync/async | Pilot volume nhỏ có thể sync `200`; nếu async thì `202 Accepted` + correlation_id/event_id. | Tech Lead + SA | `05`, `08` |
| P1-14 | Idempotency store | Dùng `integration_events` hoặc unique source external id/idempotency key; raw payload không thay thế idempotency. | SA + Tech Lead | `05`, `06`, `08` |
| P1-15 | Raw payload storage | Lưu sanitized/raw ngắn hạn 30-90 ngày, payload hash lâu hơn, mask/encrypt PII. | SA + Security | `04`, `06`, `08` |
| P1-16 | Integration/Webhook log | Có `integration_events` để debug processed/failed/duplicate, raw payload masked. | SA + Tech Lead + UX | `06`, `07`, `08`, `brief` |
| P1-17 | Lead source required | Marketing lead bắt buộc source; manual/import cũ warning và set `UNKNOWN_SOURCE`. | PO + Marketing + Tech Lead | `08`, `brief` |
| P1-18 | GraphQL hay REST | Không dùng GraphQL cho R1A; giữ REST để dễ contract và QA. | Tech Lead | `05`, `08` |
| P1-19 | Soft delete/inactive | Config entity dùng `INACTIVE`; history/audit append-only; không hard delete nghiệp vụ R1A. | SA | `06`, `08`, `11` |
| P1-20 | Audit/message partition về sau | Chưa bắt buộc R1A, nhưng schema không được cản partition/retention sau này. | SA + DB owner | `06`, `08` |

## 5. P1/P2 - UX/FE decisions cần chốt

| ID | Vấn đề cần chốt | Đề xuất Tech Lead/SA | Owner chốt | Nguồn file |
|---|---|---|---|---|
| UX-01 | Admin setup route/layout | Chốt route và layout trước FE Sprint 1; dùng form/list đơn giản, ưu tiên testability. | FE Lead + UX | `07`, `10`, `14` |
| UX-02 | Permission matrix edit UX | Dùng grid edit nhanh, nhưng guardrail phải disable rõ và API vẫn enforce. | FE Lead + QA + Tech Lead | `10`, `12` |
| UX-03 | Catalog UI | Nếu effort thấp dùng tree Language -> Program -> Course; nếu không dùng tabs/list filters. | FE Lead + UX + PO | `07`, `10` |
| UX-04 | Seed button trên UI | Không làm seed button trong product UI; seed chạy backend/migration/fixture. | Tech Lead + FE Lead | `10` |
| UX-05 | Invite email | Không làm trong Sprint 1; để auth/user onboarding riêng. | PO + Tech Lead | `10` |
| UX-06 | Import wizard save mapping template | Should-have, không block R1A. | PO + UX + Tech Lead | `07`, `08` |
| UX-07 | Advisor inbox table hay kanban | Dùng table/list để scan SLA; kanban để sau. | PO + UX | `07`, `08`, `09` |
| UX-08 | Pipeline stage R1B/R1C trên UI | Có thể hiển thị nhưng disable/limited và label rõ phần chưa support. | PO + UX + Tech Lead | `07`, `08` |
| UX-09 | Duplicate review bulk ignore | Không làm bulk ignore trong R1A để tránh sai dữ liệu hàng loạt. | PO + QA | `07`, `08` |
| UX-10 | Contact info masking | Admin/advisor-own full theo quyền; raw payload luôn masked nếu thiếu quyền. | PO + Security + SA | `07`, `08` |
| UX-11 | Activity Log placement | Hiển thị trong Opportunity Detail và có checklist riêng WF-15. | UX + QA + PO | `07`, `08` |
| UX-12 | Integration Log retry | Mặc định view/debug; chỉ bật retry/mark resolved khi Tech Lead xác nhận idempotency an toàn. | Tech Lead + UX + QA | `07`, `08` |
| UX-13 | Working Hours/SLA Settings | Có WF-17 riêng, link từ Tenant Setup và Assignment Rule. | UX + Admin + Sales Lead | `07`, `08` |

## 6. Backlog readiness / blocked items cần xử lý

| ID | Item | Tình trạng | Đề xuất Tech Lead/SA | Nguồn file |
|---|---|---|---|---|
| BL-01 | R1A backlog tổng | Draft backlog, not yet sprint-ready | Sau sign-off, split Sprint 1 thành tickets có estimate/owner/DoR. | `09` |
| BL-02 | FE tenant/branch/user/permission screens | Needs wireframe confirmation | Chốt route/layout và permission states trước khi FE commit. | `09`, `10`, `07` |
| BL-03 | Catalog UI | Needs UX confirmation | Chốt tree vs tabs/list filters. | `09`, `10`, `07` |
| BL-04 | Dedup/opportunity model | Blocked until dedup and opportunity model ready | Không đưa vào Sprint 1; chốt CustomerIdentity/DuplicateCase trước R1A-S4. | `09`, `08`, `06` |
| BL-05 | Integration/raw payload | Blocked until raw payload/idempotency decision | Chốt `integration_events`, retention, masking trước webhook stories. | `09`, `08`, `06` |
| BL-06 | Meta integration | Needs Meta integration detail | Không block Sprint 1; cần chi tiết trước R1A realtime intake. | `09` |
| BL-07 | Unmerge permission | Needs unmerge permission confirm | Admin-only unmerge cho P0. | `09`, `08` |
| BL-08 | Advisor inbox | Needs final inbox table/list decision | Chọn table/list. | `09`, `07`, `08` |
| BL-09 | UX hardening | Needs UX confirmation | PO/UX chốt screen priority và empty/error/loading states. | `09`, `07` |

## 7. QA/release decisions cần chốt

| ID | Vấn đề cần chốt | Đề xuất Tech Lead/SA | Owner chốt | Nguồn file |
|---|---|---|---|---|
| QA-01 | QA/staging environment | Có fresh migration, seed runner, test users, Tenant A/B data. | DevOps + QA + BE | `12`, `14`, `15` |
| QA-02 | Tenant A/B test data | Bắt buộc để test list/detail cross-tenant access. | QA + BE | `12` |
| QA-03 | Import fixture/API | Dùng fixture script Sprint 1; ghi rõ QA không test create batch API nếu không có draft API. | BE + QA | `12`, `14` |
| QA-04 | Actual result vs pass condition | Meeting chỉ accept pass condition; actual Pass/Fail sau implementation. | QA Lead + BA | `14` |
| QA-05 | API bypass guardrail | QA phải test direct API bypass cho Advisor export, Marketing payment write, non-admin matrix update. | QA + BE | `12` |
| QA-06 | Error envelope tests | Validation/permission/conflict phải có code/message/details/request_id. | QA + Tech Lead | `12`, `14` |
| QA-07 | Final release gate | Không accept Sprint 1 nếu auth, tenant isolation, permission, AuditLog hoặc import testability fail. | QA + PO + Tech Lead | `12`, `13`, `14` |

## 8. Business/PO decisions không nên mở lại trừ khi scope đổi

Các decision `DEC-01` đến `DEC-23` trong `01-ba-clarification-questions.md` đã là baseline cho Epic Brief. Không cần mở lại trong Sprint 1, trừ khi PO đổi phạm vi MVP.

Nếu có đổi, bắt buộc update traceability trong:

- `brief.md`
- `03-r1-story-specs.md`
- `09-r1a-dev-backlog.md`
- `10-r1a-sprint-1-ready-package.md`
- `13-r1a-sprint-1-signoff-decision-log.md`

## 9. Thứ tự chốt đề xuất

1. Chốt P0-01 đến P0-15 trong một buổi Tech/SA/QA/PO sign-off.
2. Sau P0, cập nhật `13` và `14` thành Go/Conditional Go/No-Go.
3. Nếu Go, Tech Lead mở sprint planning và estimate lại `09`/`10`.
4. Chỉ sau đó mới tạo technical design chi tiết/OpenAPI/DDL/migration.
5. P1/P2 decisions được chốt theo từng slice R1A trước khi kéo vào sprint tương ứng.

## 10. Kết luận Tech Lead/SA

Đề xuất hiện tại: **Go for backend bootstrap/foundation implementation planning**.

Lý do: các P0 technical decisions đã được freeze: Spring Boot ecosystem, PostgreSQL 17, Docker Compose local/QA, Flyway, tenant isolation, API-level permission, `audit_events`, error envelope, enum convention và import fixture testability.

Sprint 1 có thể chuyển sang development với phạm vi foundation setup, không claim các feature R1A nghiệp vụ đầy đủ. Actual release gate chỉ được pass sau khi có migration/code/test evidence.
