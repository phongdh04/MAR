# R1A Wireframe and Grooming Checklist

> Phiên bản cập nhật: `v2.1 - Architecture version/convention lock - 2026-06-29`.
> Baseline kỹ thuật hiện hành: `Java + Spring Boot ecosystem`, `PostgreSQL`, `Flyway`, `Spring Data JPA/Hibernate`.
> Ghi chú: file này đã được đồng bộ theo quyết định chọn Spring Boot và MAR-ARCH-1.0; development commitment vẫn phụ thuộc sign-off `SP1-D01` đến `SP1-D10`.
## 1. Trạng thái tài liệu

| Thuộc tính | Giá trị |
|---|---|
| Tên tài liệu | R1A Wireframe and Grooming Checklist |
| Vai trò tài liệu | Checklist màn hình, state và UX flow cho PO/UX/QA grooming |
| Nguồn baseline | `04-r1a-technical-ba-spec.md` |
| Trạng thái | Draft for grooming, not yet approved for development |
| Phạm vi | R1A Lead & Pipeline Core screens |
| Ngày lập | 2026-06-29 |

Tài liệu này không phải thiết kế UI final. Mục tiêu là giúp PO/UX/Tech/QA biết mỗi màn hình R1A cần có gì, trạng thái nào cần xử lý và tiêu chí nào phải chốt trước khi dev.

## 2. R1A Screen Map

| Screen ID | Màn hình | Actor chính | Mục tiêu |
|---|---|---|---|
| WF-01 | Tenant setup | Admin | Tạo/sửa tenant và cấu hình timezone/currency |
| WF-02 | Branch management | Admin | Quản lý cơ sở |
| WF-03 | User and role management | Admin | Quản lý nhân sự và role |
| WF-04 | Permission matrix | Admin | Bật/tắt quyền P0 |
| WF-05 | Language/program/course catalog | Admin | Cấu hình ngôn ngữ, chương trình, khóa học |
| WF-06 | Lead import wizard | Admin/Marketing/Sales Lead | Import lead từ CSV/Sheet |
| WF-07 | Import history | Admin/Marketing/Sales Lead | Xem batch import và lỗi |
| WF-08 | Duplicate review | Admin/Sales Lead | Xử lý possible duplicate |
| WF-09 | Advisor inbox | Advisor | Xem lead/opportunity cần xử lý |
| WF-10 | Opportunity detail/pipeline | Advisor/Sales Lead/Admin | Cập nhật stage và thông tin opportunity |
| WF-11 | Stage history timeline | Sales Lead/Admin/Advisor own | Xem lịch sử stage |
| WF-12 | Assignment rule setup | Admin/Sales Lead | Cấu hình rule giao lead |
| WF-13 | SLA task and overdue view | Advisor/Sales Lead/Admin | Xử lý task và quá hạn |
| WF-14 | Unassigned queue | Sales Lead/Admin | Xử lý lead chưa có owner |
| WF-15 | Activity / Interaction Log | Advisor/Sales Lead/Admin | Ghi và xem lịch sử tương tác thật |
| WF-16 | Integration / Webhook Log | Admin/Marketing | Debug webhook/form/Meta và trạng thái xử lý |
| WF-17 | Working Hours & SLA Settings | Admin/Sales Lead | Cấu hình giờ làm việc và SLA pilot |

## 3. Global UX Rules

| Rule | Áp dụng |
|---|---|
| Scope theo role | Advisor chỉ thấy own opportunity; Sales Lead thấy branch/owner scope; Team scope tạm disabled/map theo Branch nếu R1A chưa có team entity |
| Không thao tác nếu không quyền | Button/action không hiển thị hoặc disabled có tooltip |
| Validation rõ ràng | Lỗi phải chỉ field và lý do |
| Không mất dữ liệu import | Preview trước confirm; hủy preview không tạo lead chính thức |
| Stage transition có guard | Chỉ hiển thị transition hợp lệ hoặc báo lỗi nếu gọi API sai |
| Audit-sensitive actions | Permission change, merge/unmerge, reassign, import confirm phải yêu cầu reason nếu cần |
| Empty state có action | Ví dụ chưa có branch thì cho Admin tạo branch |
| Loading state rõ | Import/webhook/assignment có thể mất thời gian |
| PII masking rõ | Phone/email/raw payload phải mask nếu role không có quyền xem đầy đủ |
| KPI không suy từ stage | SLA hit/contact success phải dựa trên activity result, không chỉ dựa vào `CONTACTING`/`CONTACTED` |

## 4. WF-01 Tenant Setup

### User goal

Admin tạo tenant pilot với timezone, currency và trạng thái.

### Required elements

| Element | Type | Rule |
|---|---|---|
| Tenant name | Text input | Required |
| Timezone | Select | Default `Asia/Ho_Chi_Minh` |
| Default currency | Select | Default `VND` |
| Status | Toggle/select | Active/Inactive |
| Save | Button | Enabled khi form valid |

### States

| State | Expected |
|---|---|
| Empty form | Default timezone/currency có sẵn |
| Missing tenant name | Inline error |
| Save success | Toast + tenant detail |
| Tenant inactive | Warning rằng tenant không nhận lead active |

### Acceptance checklist

- Admin tạo được tenant active.
- Tenant inactive không nhận lead active.
- Không có hard delete trong UI R1A.

## 5. WF-02 Branch Management

### Required elements

| Element | Type | Rule |
|---|---|---|
| Branch list | Table | Name, city, status, actions |
| Add branch | Button | Admin only |
| Branch name | Text input | Required |
| City | Text/select | Optional |
| Address | Textarea | Optional |
| Status | Toggle/select | Active/Inactive |

### Edge states

- Chưa có branch: empty state có nút tạo branch.
- Branch inactive: không chọn được trong lead/opportunity mới.
- Branch có dữ liệu: không cho delete, chỉ inactive.

## 6. WF-03 User and Role Management

### Required elements

| Element | Type | Rule |
|---|---|---|
| User list | Table | Name, email, phone, role, branch, status |
| Role | Select | Required |
| Branch assignment | Multi-select | Optional theo tenant |
| Status | Toggle/select | Active/Inactive |

### Role behavior

| Role | UI expectation |
|---|---|
| Admin | Có quyền cấu hình |
| Marketing | Thấy import lead và lead marketing scope |
| Sales Lead | Thấy team/branch lead, assignment, duplicate theo scope |
| Advisor | Thấy inbox own |
| Finance | R1A view limited, payment ở R1B |

### Acceptance checklist

- User inactive không nhận assignment.
- Advisor không thấy toàn tenant.
- Email trùng trong tenant bị chặn.

## 7. WF-04 Permission Matrix

### Layout

Ma trận role x function.

Columns:

- Function.
- CEO.
- Admin.
- Marketing.
- Sales Lead.
- Advisor.
- CSKH.
- Finance.

Controls:

- Access level dropdown: None/View/Create/Update/Manage.
- Scope dropdown: Tenant/Branch/Team/Own/None.
- Save changes.
- Reason modal nếu thay đổi quyền nhạy cảm.

### Guardrails

- Advisor export data luôn disabled.
- Marketing ghi payment disabled.
- Permission change ghi AuditLog.

## 8. WF-05 Language/Program/Course Catalog

### Required views

| View | Purpose |
|---|---|
| Language list | Tạo Anh, Nhật, Trung, custom |
| Program list | Gắn program vào language |
| Course list | Gắn course vào program |

### Field checklist

Language:

- Name.
- Code.
- Status.

Program:

- Language.
- Program name.
- Exam track.
- Status.

Course:

- Program.
- Course name.
- Level.
- Tuition gross.
- Currency.
- Status.

### Acceptance checklist

- Program không được tạo dưới language inactive.
- Course tuition âm bị chặn.
- Không hard-code IELTS/JLPT/HSK vào UI logic.

## 9. WF-06 Lead Import Wizard

### Step 1 - Select source

Elements:

- Source type: CSV, Google Sheet.
- Upload file or Sheet URL.
- Default source.
- Default language/program/branch optional.

Validation:

- File required for CSV.
- Sheet URL/id required for Google Sheet if enabled.

### Step 2 - Mapping

Elements:

- Source column list.
- System field dropdown.
- Required/Recommended marker.
- Save mapping template optional.

Required mapping logic:

- At least one of phone, email, Zalo ID must be mapped or supplied.
- Source recommended/required for marketing lead.

### Step 3 - Preview

Summary cards:

- Total rows.
- Valid rows.
- Error rows.
- Duplicate candidates.
- Rows to create.
- Rows to update/link.

Tables:

- Error preview: row number, field, reason, raw value.
- Duplicate preview: row number, match type, candidate customer, recommended action.
- Valid sample preview.

Actions:

- Back to mapping.
- Download error report.
- Confirm import.
- Cancel import.

### Step 4 - Confirm result

Result:

- Created.
- Updated/linked.
- Skipped.
- Error.
- Duplicate cases created.
- Link to import history.

### Edge states

| State | Expected |
|---|---|
| File empty | Error |
| Unknown headers | User can map manually |
| Too many errors | Allow cancel, show warning before confirm |
| Duplicate found | Show duplicate preview |
| Confirm clicked twice | Idempotency prevents duplicate |

## 10. WF-07 Import History

### Required elements

Filters:

- Date range.
- Status.
- Source type.
- Imported by.

Table columns:

- Batch ID.
- Source file/name.
- Status.
- Total.
- Created.
- Updated.
- Skipped.
- Error.
- Duplicate.
- Imported by.
- Imported at.

Detail panel:

- Mapping config.
- Error report.
- Duplicate cases.
- Created lead IDs sample.

Acceptance:

- Confirm import must be traceable.
- User can inspect failed rows.

## 11. WF-08 Duplicate Review

### Required elements

List filters:

- Status.
- Match type.
- Source.
- Created date.
- Confidence.

Comparison panel:

| Left | Right |
|---|---|
| New lead data | Candidate customer profile |
| Phone/email/Zalo | Existing identifiers |
| Source/campaign | Existing touchpoints |
| Language/program interest | Existing active opportunities |

Actions:

- Merge.
- Link.
- Ignore.
- View customer.
- View lead.

Required modal:

- Action selected.
- Target customer if needed.
- Reason/note required.
- Confirmation warning for merge.

Acceptance:

- Possible duplicate not auto-merged.
- Merge requires reason.
- Advisor has no merge action.
- Admin can unmerge if allowed.

## 12. WF-09 Advisor Inbox

### Required elements

Header KPIs:

- Open assigned opportunities.
- SLA due soon.
- Overdue.
- New today.

Filters:

- Stage.
- SLA status.
- Source.
- Program.
- Branch if user has scope.

Table/card fields:

- Customer name.
- Contact identifiers.
- Source/campaign.
- Language/program.
- Current stage.
- SLA due time.
- Owner.
- Next action.

Quick actions:

- Add call/Zalo/SMS/email attempt.
- Mark contact success when activity result is `CONNECTED`/`REPLIED`.
- Mark Contacting stage if advisor starts handling but has no outbound activity yet.
- Move to Lost.
- Move to Nurturing.
- Open detail.

States:

- Empty inbox.
- Overdue highlight.
- Permission-limited contact info if needed.

Acceptance:

- Advisor only sees own opportunities.
- SLA status is visible.
- Quick actions obey transition matrix.
- First response SLA hit requires a valid outbound activity, not only stage change.

## 13. WF-10 Opportunity Detail and Pipeline

### Required sections

| Section | Content |
|---|---|
| Customer summary | Name, phone, email, Zalo, preferred channel |
| Lead/source summary | Source, campaign, source_created_at, touchpoint |
| Opportunity info | Language, program, course, branch, owner |
| Pipeline stage | Current stage and allowed next stages |
| Qualification | Need, timeframe, note, qualification status |
| Activity timeline | Call/Zalo/SMS/email/note history with result and occurred_at |
| Actions | Change stage, reassign if allowed, add activity/note |

### Stage change behavior

- Only allowed transitions shown.
- If API rejects transition, show error.
- Lost opens lost reason modal.
- Lost reason `Khác` requires note.
- Stage change success updates timeline.

### Acceptance

- StageHistory created for every stage change.
- ActivityLog created for note/contact attempt/contact success.
- New -> Enrolled blocked.
- Contacted -> Lost requires reason.

## 14. WF-11 Stage History Timeline

### Required elements

- From stage.
- To stage.
- Changed by.
- Changed at.
- Duration in previous stage.
- Reason if any.

States:

- New opportunity with only initial history.
- Long duration highlight optional.

Acceptance:

- Timeline is append-only.
- Advisor cannot edit history.

## 15. WF-12 Assignment Rule Setup

### Required elements

Rule list:

- Priority.
- Language.
- Program.
- Branch.
- Shift.
- Advisor pool.
- Strategy.
- Active.

Rule form:

- Priority.
- Conditions.
- Advisor selector.
- Strategy selector.
- Active toggle.

Test panel:

- Select sample language/program/branch/source/time.
- Show matched rule.
- Show eligible advisors.
- Show selected advisor and reason.

Acceptance:

- Inactive advisors not selectable or flagged.
- Rule priority conflict handled.
- Rule change audit if policy requires.

## 16. WF-13 SLA Task and Overdue View

### Advisor view

Fields:

- Task type.
- Opportunity.
- Customer.
- Due time.
- Status.
- Completion action.

Actions:

- Complete first response task by adding a valid outbound activity.
- Open opportunity.
- Add activity/note.

### Sales Lead view

Fields:

- Advisor.
- Opportunity.
- Due time.
- Overdue level.
- Current stage.

Actions:

- Alert/review.
- Reassign if allowed.
- Open opportunity.

Acceptance:

- Hot lead due 5-15 minutes.
- Normal lead due 30-60 minutes.
- Overdue level escalates Advisor then Sales Lead.
- R1A does not auto-change owner on overdue.
- Contact success is shown separately from first response SLA hit.

## 17. WF-14 Unassigned Queue

### User goal

Sales Lead/Admin xử lý opportunity không tìm được owner.

### Required elements

- Opportunity list.
- Reason: no rule match, no active advisor, branch restricted, config missing.
- Suggested action.
- Assign owner manually.
- Edit assignment rule shortcut.

Acceptance:

- No eligible advisor creates unassigned queue item.
- Manual assignment requires reason.
- Assignment event created after owner selected.

## 18. WF-15 Activity / Interaction Log

### User goal

Advisor ghi nhận tương tác thật với khách; Sales Lead/Admin xem timeline để kiểm tra SLA, contact success và follow-up.

### Required elements

| Element | Purpose |
|---|---|
| Activity type | CALL, ZALO, SMS, EMAIL, NOTE, MEETING |
| Activity result | ATTEMPTED, CONNECTED, REPLIED, NO_ANSWER, FAILED, SENT |
| Occurred at | Thời điểm tương tác thực tế |
| Note | Nội dung tư vấn hoặc ghi chú follow-up |
| Next action | Việc tiếp theo nếu cần |
| Create follow-up task | Tạo nhắc việc nếu có ngày hẹn |
| Activity timeline | Lịch sử tương tác theo thời gian |

States:

- Empty activity timeline.
- Activity save success.
- Missing activity result for outbound attempt.
- Permission-limited note/contact detail.

Acceptance:

- Advisor ghi call attempt được.
- Call/Zalo/SMS/email attempt trong SLA có thể hoàn thành first response SLA.
- Contact success chỉ tính khi result là `CONNECTED` hoặc `REPLIED`.
- Stage `CONTACTING` không tự động tính SLA hit nếu không có activity.
- Activity không bị sửa/xóa tùy tiện; chỉnh sửa nếu có phải có audit.
- Sales Lead/Admin xem activity theo scope.

## 19. WF-16 Integration / Webhook Log

### User goal

Admin/Marketing theo dõi webhook website/Meta, biết payload nào processed/failed/duplicate và link được sang lead/opportunity nếu tạo thành công.

### Required elements

| Element | Purpose |
|---|---|
| Event list | Danh sách integration events |
| Source filter | WEBSITE_FORM, META_LEAD_ADS, CSV/GOOGLE_SHEET nếu exposed |
| Status filter | RECEIVED, ACCEPTED, PROCESSED, FAILED, DUPLICATE |
| External ID | ID nguồn như meta_lead_id/form_id |
| Error code/message | Lý do reject/map lỗi |
| Created records | Link lead/customer/opportunity nếu có |
| Raw/sanitized payload view | Chỉ hiển thị nếu có quyền |
| Retry/mark resolved | Chỉ bật nếu Tech Lead cho phép trong R1A |

States:

- No webhook event.
- Failed event with error.
- Duplicate ignored.
- Raw payload masked.
- Access denied for raw payload.

Acceptance:

- Marketing/Admin filter được theo source/status/date.
- Event detail hiển thị correlation/idempotency key nếu có.
- Raw payload bị mask theo quyền và ghi audit khi export/download.
- Async webhook nếu có chỉ trả correlation/event id; màn này dùng để xem kết quả xử lý.

## 20. WF-17 Working Hours & SLA Settings

### User goal

Admin/Sales Lead cấu hình giờ làm việc và SLA pilot để due time/after-hours được tính nhất quán.

### Required elements

| Element | Example |
|---|---|
| Working days | Monday-Saturday |
| Working time | 08:00-18:00 |
| Timezone | Tenant timezone |
| Hot lead SLA | 15 minutes |
| Normal lead SLA | 60 minutes |
| Escalation window | Alert Sales Lead after N minutes overdue |
| After-hours behavior | Auto message + task next working shift |
| Branch override | Optional branch-specific working hours |

States:

- Default pilot config.
- Branch override missing.
- Invalid time range.
- No working day selected.

Acceptance:

- Nếu thiếu config, UI hiển thị default pilot và warning cấu hình.
- SLA due time dùng working hours, không hard-code rải rác.
- Admin cập nhật được config; Sales Lead xem được và chỉ sửa nếu được cấp quyền.
- QA test được lead trong giờ, ngoài giờ và cuối tuần.

## 21. Notification and Message Copy Baseline

R1A chưa làm full messaging, nhưng cần alert/in-app notification cho SLA.

| Trigger | Copy gợi ý | Receiver |
|---|---|---|
| New assigned lead | Bạn có lead mới cần xử lý. | Advisor |
| SLA due soon | Lead sắp quá hạn SLA. | Advisor |
| SLA overdue | Lead đã quá hạn SLA. | Advisor |
| SLA escalated | Lead của advisor đã quá hạn SLA. | Sales Lead |
| Unassigned opportunity | Có lead chưa được giao owner. | Sales Lead/Admin |

## 22. QA Checklist by Flow

### Import flow

- Upload file hợp lệ.
- Upload file rỗng.
- Mapping thiếu contact.
- Row thiếu contact.
- Duplicate phone exact.
- Duplicate email phone khác.
- Confirm import một lần.
- Confirm import double click.
- Cancel preview.

### Dedup flow

- Phone exact auto-link.
- Email exact phone khác tạo DuplicateCase.
- Chỉ trùng tên không merge.
- Merge có reason.
- Ignore có reason.
- Unmerge Admin.
- Advisor không thấy action merge.

### Pipeline flow

- Allowed transition success.
- Invalid transition blocked.
- Lost reason required.
- Lost reason Khác note required.
- StageHistory append.
- ActivityLog append for note/contact attempt/contact success.
- Advisor scope enforced.

### Assignment/SLA flow

- Rule match language/program/branch.
- Fallback round-robin.
- No advisor active -> unassigned queue.
- Hot lead SLA.
- Normal lead SLA.
- After-hours SLA.
- First response SLA hit from valid outbound activity.
- Contact success from connected/replied activity.
- Advisor overdue alert.
- Sales Lead escalation.

### Integration/debug flow

- Webhook processed creates/links records.
- Webhook duplicate ignored safely.
- Webhook failed shows error code/message.
- Raw payload masked for unauthorized role.

### Working hours flow

- Default pilot config visible.
- In-hours due time calculated.
- After-hours due time moves to next working shift.
- Invalid working hours blocked.

## 23. Grooming Output Checklist

Sau buổi grooming R1A, mỗi màn hình cần chốt:

- Owner PO/UX/Tech.
- Wireframe final hoặc đủ để dev.
- Field list visible/editable.
- Empty/loading/error states.
- Permission behavior.
- API endpoint dùng.
- Validation message.
- Audit/event requirement.
- QA scenario.
- Story mapping.

## 24. Story to Screen Mapping

| Story | Screen |
|---|---|
| US-01.01 | WF-01 |
| US-01.02 | WF-03 |
| US-01.03 | WF-02 |
| US-01.04 | WF-04 |
| US-02.01 | WF-05 |
| US-02.02 | WF-05 |
| US-02.03 | WF-05 |
| US-03.01 | WF-06, WF-07 |
| US-03.02 | WF-07 for import logs/history |
| US-03.03 | WF-16 for integration/webhook status |
| US-03.04 | WF-08 |
| US-03.05 | WF-06 |
| US-03.06 | WF-06 |
| US-03.07 | WF-07 |
| US-03.08 | WF-08 |
| US-03.09 | WF-10 |
| US-04.01 | WF-09 |
| US-04.02 | WF-10, WF-15 |
| US-04.03 | WF-10 or admin pipeline config later |
| US-04.04 | Pipeline config, may be lightweight in R1A |
| US-04.05 | WF-10 |
| US-04.06 | WF-10 |
| US-04.07 | WF-11 |
| US-05.01 | WF-12 |
| US-05.02 | WF-09, WF-13, WF-17 |
| US-05.03 | WF-13, WF-15 |
| US-05.04 | WF-14 |
| US-05.05 | WF-17 |

## 25. Open UX Questions

| ID | Câu hỏi | Khuyến nghị BA |
|---|---|---|
| UX-Q01 | Import wizard có cần save mapping template không? | Should-have nếu nhiều file lặp lại |
| UX-Q02 | Advisor inbox dùng table hay kanban? | Table/list tốt hơn cho SLA scanning; kanban có thể để sau |
| UX-Q03 | Pipeline stage có hiển thị cả stage R1B/R1C không? | Có thể hiển thị nhưng disable/limited nếu chưa triển khai |
| UX-Q04 | Duplicate review có cho bulk ignore không? | Chưa nên trong R1A nếu chưa có rule an toàn |
| UX-Q05 | Contact info có mask với role nào không? | Advisor own/Admin full theo scope; Marketing/Sales Lead theo policy; raw payload luôn mask nếu thiếu quyền |
| UX-Q06 | SLA completion hiển thị theo activity hay stage? | Không dùng stage đơn lẻ để tính hit; first valid outbound activity trong SLA là SLA hit, connected/replied là contact success |
| UX-Q07 | Activity Log nằm trong Opportunity Detail hay màn riêng? | R1A hiển thị section trong WF-10 và có checklist riêng WF-15 |
| UX-Q08 | Integration/Webhook Log có cho retry không? | Chỉ bật retry/mark resolved nếu Tech Lead xác nhận an toàn; mặc định view/debug |
| UX-Q09 | Working Hours/SLA Settings đặt ở đâu? | Có WF-17 riêng; có thể link từ Tenant Setup và Assignment Rule |
| UX-Q10 | Team scope xử lý thế nào khi chưa có Team entity? | R1A dùng Branch/Own; Team scope disabled hoặc map tạm theo Branch |
