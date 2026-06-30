# R1A Grooming Review - Lead & Pipeline Core

> Phiên bản cập nhật: `v2.1 - Architecture version/convention lock - 2026-06-29`.
> Baseline kỹ thuật hiện hành: `Java + Spring Boot ecosystem`, `PostgreSQL`, `Flyway`, `Spring Data JPA/Hibernate`.
> Ghi chú: file này đã được đồng bộ theo quyết định chọn Spring Boot và MAR-ARCH-1.0; development commitment vẫn phụ thuộc sign-off `SP1-D01` đến `SP1-D10`.
## 1. Trạng thái tài liệu

| Thuộc tính | Giá trị |
|---|---|
| Tên tài liệu | R1A Grooming Review |
| Vai trò tài liệu | Tài liệu review/grooming để PO, Tech Lead, SA, QA chốt trước khi dev |
| Nguồn baseline | `04-r1a-technical-ba-spec.md`, `05-r1a-api-contract.md`, `06-r1a-db-schema-erd.md`, `07-r1a-wireframe-checklist.md` |
| Trạng thái | Draft for grooming, not yet approved for development |
| Ngày lập | 2026-06-29 |

Tài liệu này gom toàn bộ open questions và quyết định đề xuất cho R1A. Các dòng có trạng thái `Proposed` là khuyến nghị BA, chưa được xem là final cho development cho đến khi PO/Tech Lead/SA xác nhận.

## 2. Mục tiêu buổi grooming R1A

Buổi grooming cần chốt đủ để chuyển R1A từ tài liệu BA sang backlog dev:

- Phạm vi R1A có giữ đúng Tenant -> Lead -> Dedup -> Opportunity -> Assignment/SLA -> Pipeline không.
- DB schema có đủ nền cho R1B/R1C mà không over-engineer.
- API contract đủ rõ cho FE/BE/QA.
- Wireframe checklist đủ trạng thái màn hình và permission behavior.
- Các câu hỏi còn mở được gắn owner và quyết định.
- Story nào đạt Definition of Ready, story nào còn blocked.

## 3. Input review

| File | Vai trò |
|---|---|
| `04-r1a-technical-ba-spec.md` | Spec BA kỹ thuật tổng hợp cho R1A |
| `05-r1a-api-contract.md` | API baseline, DTO, endpoint, error code |
| `06-r1a-db-schema-erd.md` | ERD kỹ thuật, table specs, index, migration order |
| `07-r1a-wireframe-checklist.md` | Screen map, UX state, QA checklist |

## 4. Recommended Decisions

### 4.1. Business/BA decisions

| ID | Câu hỏi | BA recommendation | Owner confirm | Impact | Trạng thái |
|---|---|---|---|---|---|
| OQ-R1A-001 | Email exact nhưng phone khác có tạo opportunity pending không? | Không đưa vào pipeline chính trước review; tạo DuplicateCase `NEEDS_REVIEW`. Sau khi link/merge mới tạo hoặc gắn opportunity. | PO + Tech Lead | Cao, ảnh hưởng data quality và sales workflow | Proposed |
| OQ-R1A-002 | SLA hit và contact success phân biệt thế nào? | SLA hit = first valid outbound activity/contact attempt trong SLA. `CONTACTED`/connected/replied = contact success. `CONTACTING` chỉ là bắt đầu xử lý. | PO + Sales Lead | Cao, ảnh hưởng KPI SLA | Proposed |
| OQ-R1A-003 | Sales Lead có được unmerge không? | P0 chỉ Admin được unmerge. Sales Lead được merge/link/ignore trong scope nếu tenant bật, nhưng unmerge để Admin xử lý. | PO + Admin representative | Trung bình, giảm rủi ro dữ liệu | Proposed |
| OQ-R1A-004 | Google Sheet import là upload export hay kết nối live sheet? | R1A must-have: CSV upload. Google Sheet URL/import là should-have nếu effort cho phép; không làm live sync liên tục trong R1A. | PO + Tech Lead | Trung bình, ảnh hưởng effort | Proposed |
| OQ-R1A-005 | Có cần team entity riêng cho Sales Lead không? | R1A dùng branch + owner scope trước. Thêm Team entity chỉ nếu pilot xác nhận có nhiều team trong cùng branch. | SA + Tech Lead | Trung bình, ảnh hưởng schema | Proposed |
| OQ-R1A-006 | Lead source bắt buộc đến mức nào? | Bắt buộc với marketing lead; warning với manual/import cũ. Nếu thiếu source thì gắn key `UNKNOWN_SOURCE` và UI render label riêng. | PO + Marketing | Cao, ảnh hưởng attribution R1C | Proposed |
| OQ-R1A-007 | Có lưu raw payload không? | Lưu sanitized raw payload hoặc raw payload có retention ngắn cho webhook/import debug; luôn lưu payload hash để idempotency. | SA + Security/Tech Lead | Cao, ảnh hưởng debug và PII | Proposed |
| OQ-R1A-008 | R1A có thêm CustomerIdentity không? | Có, P0-lite cho PHONE/EMAIL/ZALO_ID; Facebook/cookie/platform có thể mở sau. | PO + SA + Tech Lead | Cao, ảnh hưởng dedup/merge | Proposed |
| OQ-R1A-009 | R1A có thêm Activity/InteractionLog không? | Có, bắt buộc để đo first response SLA, contact success, note và follow-up. | PO + Sales Lead + Tech Lead | Cao, ảnh hưởng KPI/vận hành | Proposed |
| OQ-R1A-010 | SLA hit tính thế nào? | First valid outbound activity trong SLA; Contacted/connected/replied là contact success riêng. | PO + Sales Lead + QA | Cao, ảnh hưởng dashboard R1C | Proposed |
| OQ-R1A-011 | Working hours lấy từ đâu? | Có `working_hours_configs` tối thiểu và seed default pilot 08:00-18:00 Monday-Saturday theo tenant timezone. | SA + Tech Lead + PO | Cao, ảnh hưởng due_at | Proposed |
| OQ-R1A-012 | Webhook sync hay async? | R1A pilot sync nếu volume nhỏ; nếu async thì trả correlation/event id, không trả lead_id ngay. | Tech Lead + SA | Trung bình, ảnh hưởng API contract | Proposed |
| OQ-R1A-013 | Team scope xử lý thế nào? | R1A dùng Branch/Own; Team scope disabled hoặc map tạm theo Branch nếu chưa có team entity. | PO + SA | Trung bình, ảnh hưởng permission | Proposed |
| OQ-R1A-014 | Raw payload retention thế nào? | Lưu sanitized/raw ngắn hạn 30-90 ngày, payload hash lâu hơn, mask/encrypt PII và audit khi export. | Security + SA | Cao, ảnh hưởng PII | Proposed |

### 4.2. API decisions

| ID | Câu hỏi | BA recommendation | Owner confirm | Impact | Trạng thái |
|---|---|---|---|---|---|
| API-Q01 | Import preview xử lý sync hay async? | File nhỏ xử lý sync; file lớn dùng async job + polling. R1A UI phải support trạng thái processing. | Tech Lead | Trung bình | Proposed |
| API-Q02 | Webhook response nên 200 hay 202? | R1A pilot khuyến nghị sync `200` nếu volume nhỏ và trả resource IDs khi đã tạo/link xong. Nếu dùng async/queue thì trả `202 Accepted` với correlation_id/event_id, không hứa có lead_id ngay. | Tech Lead | Trung bình | Proposed |
| API-Q03 | Idempotency lưu trong bảng riêng hay raw payload? | Có bảng/idempotency store riêng hoặc unique key trên source external id; raw payload không thay thế idempotency. | SA + Tech Lead | Cao | Proposed |
| API-Q04 | Có cần GraphQL cho inbox/filter không? | Không. R1A dùng REST để nhanh, dễ test và dễ contract. | Tech Lead | Thấp | Proposed |
| API-Q05 | Stage transition enforce ở service nào? | Centralize trong Opportunity/Pipeline service; UI chỉ phản ánh allowed actions, API vẫn là nguồn enforce chính. | SA + Tech Lead | Cao | Proposed |
| API-Q06 | Assignment workload tính realtime hay snapshot? | R1A dùng active open opportunities/task count realtime mức đơn giản. Sau này tối ưu bằng snapshot nếu chậm. | Tech Lead | Trung bình | Proposed |

### 4.3. DB/SA decisions

| ID | Câu hỏi | BA recommendation | Owner confirm | Impact | Trạng thái |
|---|---|---|---|---|---|
| DB-Q01 | Xác nhận PostgreSQL JSONB cho mapping/raw row | PostgreSQL đã chọn; `mapping_config`, `raw_row`, `normalized_row` dùng JSONB. | SA + DB owner | Trung bình | User-selected, pending technical sign-off |
| DB-Q02 | Có cần bảng team riêng không? | R1A chưa bắt buộc team entity. Dùng Branch/Own; Team scope disabled hoặc map tạm theo Branch nếu pilot chưa có team thật ngoài branch. | SA + PO | Trung bình | Proposed |
| DB-Q03 | Có lưu raw payload dài hạn không? | Không lưu dài hạn mặc định. Lưu sanitized/raw có retention ngắn; audit giữ metadata. | Security + SA | Cao | Proposed |
| DB-Q04 | Có dùng soft delete không? | Config entity dùng `INACTIVE`; audit/history append-only; không hard delete dữ liệu nghiệp vụ R1A. | SA | Trung bình | Proposed |
| DB-Q05 | Có cần partition audit_logs/message sau này không? | Chưa bắt buộc R1A, nhưng schema nên không cản partition sau này vì retention tối thiểu 24 tháng. | SA/DB owner | Thấp | Proposed |
| DB-Q06 | Unique phone/email customer có nên strict không? | Không strict tuyệt đối ở customer vì phụ huynh/người học có thể dùng chung phone/email. Dùng index hỗ trợ dedup, service quyết định merge. | SA + PO | Cao | Proposed |
| DB-Q07 | Có thêm `customer_identities` vào R1A không? | Có. R1A P0-lite cần CustomerIdentity cho phone/email/Zalo ID; Facebook/cookie/platform mở sau. | SA + Tech Lead + PO | Cao | Proposed |
| DB-Q08 | Có cần `activities`/`interaction_logs` trong R1A không? | Có. Bắt buộc để đo first response SLA, contact success, note và follow-up. | SA + Tech Lead + Sales Lead | Cao | Proposed |
| DB-Q09 | Working hours/SLA policy cấu hình hay hard-code? | Có `working_hours_configs` và `sla_policies` tối thiểu; nếu thiếu config dùng default pilot và ghi warning. | SA + Tech Lead | Cao | Proposed |
| DB-Q10 | Integration log/idempotency dùng bảng nào? | Dùng `integration_events` làm log/idempotency chính; raw payload là optional/retention ngắn, không thay thế payload hash. | SA + Security | Cao | Proposed |
| DB-Q11 | Có cần assignment history/pool state không? | Có `assignment_history` để audit reassign và `assignment_pool_state` để round-robin ổn định. | SA + Tech Lead | Cao | Proposed |
| DB-Q12 | Enum DB/API chuẩn gì? | Dùng `UPPER_SNAKE_CASE`; UI render display label riêng. | SA + Tech Lead + UX | Trung bình | Proposed |

### 4.4. UX decisions

| ID | Câu hỏi | BA recommendation | Owner confirm | Impact | Trạng thái |
|---|---|---|---|---|---|
| UX-Q01 | Import wizard có cần save mapping template không? | Should-have, không block R1A. Nếu làm nhanh được thì hỗ trợ saved mapping theo tenant/source. | PO + UX | Thấp | Proposed |
| UX-Q02 | Advisor inbox dùng table hay kanban? | R1A dùng table/list để scan SLA tốt hơn. Kanban để sau nếu cần pipeline view trực quan. | PO + UX | Trung bình | Proposed |
| UX-Q03 | Pipeline stage có hiển thị stage R1B/R1C không? | Có thể hiển thị toàn bộ stage nhưng các action chưa support sâu phải disabled/limited và ghi nhãn rõ. | PO + UX + Tech Lead | Trung bình | Proposed |
| UX-Q04 | Duplicate review có cho bulk ignore không? | Không trong R1A. Tránh xử lý hàng loạt khi rule duplicate chưa đủ trưởng thành. | PO + QA | Thấp | Proposed |
| UX-Q05 | Contact info có mask với role nào không? | Advisor own/Admin full theo scope; Sales Lead/Marketing theo policy; raw payload luôn mask nếu thiếu quyền. | PO + Security | Cao | Proposed |
| UX-Q06 | SLA completion hiển thị theo activity hay stage? | Đồng bộ với OQ-R1A-002: không dùng stage đơn lẻ; first valid outbound activity là SLA hit, connected/replied là contact success. | PO + Sales Lead | Cao | Proposed |
| UX-Q07 | Activity Log nằm ở đâu? | Hiển thị trong Opportunity Detail và có checklist riêng WF-15 để UX/QA test activity type/result/timeline. | UX + PO + QA | Cao | Proposed |
| UX-Q08 | Webhook/Integration Log có cần màn riêng không? | Có WF-16 cho Admin/Marketing debug processed/failed/duplicate và raw payload masked. | UX + Marketing + Tech Lead | Cao | Proposed |
| UX-Q09 | Working Hours/SLA Settings đặt ở đâu? | Có WF-17 riêng; link từ Tenant Setup và Assignment Rule Setup. | UX + Admin + Sales Lead | Cao | Proposed |

## 5. Scope Confirmation

### 5.1. R1A stays in scope

| Area | Confirmed direction |
|---|---|
| Tenant/config | Cần trước mọi nghiệp vụ lead |
| Lead import | CSV must-have, Google Sheet should-have |
| Webhook | Website form và Meta realtime baseline |
| Integration log | Webhook/form/Meta có integration_events để debug/idempotency |
| Dedup | Phone exact auto-link, uncertain case review |
| Customer identity | CustomerIdentity P0-lite cho phone/email/Zalo ID |
| Opportunity | Entity chính của sales pipeline |
| Activity log | Ghi contact attempt, contact success, note và follow-up |
| Pipeline | Stage transition và stage history bắt buộc |
| Assignment | Rule-based + fallback round-robin |
| Working hours | Default pilot và config tối thiểu để tính due_at |
| SLA | First response task, SLA hit theo activity và overdue alert |

### 5.2. R1A must not expand into

| Area | Reason |
|---|---|
| Appointment module đầy đủ | Thuộc R1B |
| Payment/enrollment | Thuộc R1B |
| Attribution dashboard/ROAS | Thuộc R1C |
| Journey builder | Ngoài MVP |
| AI scoring | P1/Should-have |

## 6. Risk Review

| Rủi ro | Mức | Dấu hiệu | Cách kiểm soát |
|---|---|---|---|
| Dedup sai gây gộp nhầm customer | Cao | Email trùng nhưng phone khác, phụ huynh dùng chung email/phone | CustomerIdentity, không strict unique customer; possible duplicate review; unmerge Admin |
| API/UI lệch transition | Cao | UI cho đổi stage nhưng API chặn hoặc ngược lại | Centralize transition service; FE lấy allowed transitions từ API |
| Import dirty data | Cao | Nhiều row thiếu contact/source | Preview, error report, confirm step, warning source |
| SLA gây tranh cãi KPI | Cao | Advisor chuyển Contacting để tính hit | Chốt SLA hit = first valid outbound activity; contact success đo riêng |
| Thiếu ActivityLog | Cao | Không biết advisor đã gọi/nhắn/ghi chú lúc nào | Bắt buộc WF-15 và bảng activities |
| Working hours sai | Cao | Lead ngoài giờ bị tính due_at sai | Seed default 08:00-18:00 Monday-Saturday và WF-17 config |
| Webhook khó debug | Cao | Payload failed/duplicate không truy vết được | `integration_events`, error_code, masked raw payload |
| Scope creep Google Sheet/live sync | Trung bình | Muốn sync 2 chiều/live | R1A chỉ CSV must-have; Sheet import nếu effort cho phép |
| Team/branch model thiếu | Trung bình | Pilot có nhiều team trong một branch | R1A dùng Branch/Own; Team defer nếu pilot chưa cần |
| PII/raw payload retention | Cao | Lưu payload dài hạn không kiểm soát | Sanitized payload, retention, audit metadata |

## 7. Grooming Agenda đề xuất

| Thứ tự | Nội dung | Người chốt | Output |
|---|---|---|---|
| 1 | Confirm R1A scope/out-of-scope | PO | Scope locked |
| 2 | Review entity model và Team decision | SA + Tech Lead + PO | DB model direction |
| 3 | Review CustomerIdentity và dedup rules | PO + Tech Lead + Sales Lead | Dedup decision final |
| 4 | Review ActivityLog và SLA/contact KPI | PO + Sales Lead + QA | KPI/SLA rule final |
| 5 | Review import/webhook flow/API | Tech Lead + QA + PO | API contract ready |
| 6 | Review working hours/SLA config | SA + Tech Lead + Sales Lead | Due time rule final |
| 7 | Review wireframe states WF-15/16/17 | UX + PO + QA | Screen acceptance baseline |
| 8 | Mark backlog Ready/Blocked | PO + Tech Lead | Sprint candidates |

## 8. Sign-off Checklist

Trước khi chuyển R1A sang development, cần tick:

- R1A scope locked.
- Team entity decision closed.
- CustomerIdentity decision closed.
- ActivityLog/SLA/contact success decision closed.
- Working hours/SLA policy decision closed.
- IntegrationEvent/webhook log decision closed.
- Raw payload/idempotency decision closed.
- Dedup uncertain case behavior closed.
- SLA hit definition closed.
- Import CSV/Google Sheet scope closed.
- Permission matrix accepted.
- Stage transition matrix accepted.
- API error code catalog accepted.
- DB index/constraint baseline accepted.
- Wireframe states accepted.
- PII masking/raw payload access accepted.
- QA scenarios mapped to stories.

## 9. Proposed Go/No-Go

| Hạng mục | BA recommendation |
|---|---|
| Go to technical design | Yes, with proposed decisions reviewed |
| Go to development immediately | No, cần split/update dev backlog và handoff theo decisions mới |
| Next artifact | Update/split `09-r1a-dev-backlog.md` theo CustomerIdentity, ActivityLog, WorkingHours, IntegrationEvent |
| Required meeting | R1A grooming review with PO, Tech Lead, SA, QA, UX |
