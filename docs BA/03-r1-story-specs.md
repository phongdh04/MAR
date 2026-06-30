# Release 1 Story Specs - Lead-to-Revenue Platform

> Phiên bản cập nhật: `v2.1 - Architecture version/convention lock - 2026-06-29`.
> Baseline kỹ thuật hiện hành: `Java + Spring Boot ecosystem`, `PostgreSQL`, `Flyway`, `Spring Data JPA/Hibernate`.
> Ghi chú: file này đã được đồng bộ theo quyết định chọn Spring Boot và MAR-ARCH-1.0; development commitment vẫn phụ thuộc sign-off `SP1-D01` đến `SP1-D10`.
## 1. Trạng thái tài liệu

| Thuộc tính | Giá trị |
|---|---|
| Vai trò tài liệu | Story Specs baseline cho Release 1 |
| Nguồn baseline | `brief.md` đã được chốt ở trạng thái Approved for Epic Brief |
| Trạng thái | Ready for PO/Tech grooming, not yet approved for development |
| Phạm vi | MVP pilot cho trung tâm ngoại ngữ offline/hybrid |
| Ngày lập | 2026-06-29 |

Tài liệu này chuyển Epic Brief thành story spec ở mức BA. Trước khi dev code, từng story vẫn cần được PO/Tech Lead xác nhận thêm API contract, UI wireframe, data migration, security detail và effort estimate.

## 2. Release 1 Scope

| Slice | Mục tiêu nghiệm thu | Epic |
|---|---|---|
| R1A - Lead & Pipeline Core | Có tenant, cấu hình cơ bản, import/capture lead, dedup, opportunity, pipeline, assignment và SLA | EPIC-01 đến EPIC-05 |
| R1B - Appointment & Payment Core | Có appointment, reminder, enrollment/payment import, payment matching và message log cơ bản | EPIC-06, EPIC-07, EPIC-09 basic |
| R1C - Attribution & Dashboard | Có ad cost import, first/last touch, revenue attribution, dashboard P0, consent/opt-out và audit log | EPIC-08, EPIC-10, EPIC-11 |

Out of scope trong Release 1:

- AI/ML lead scoring.
- Full journey builder.
- Payment gateway realtime.
- Native mobile app.
- Multi-touch attribution nâng cao.
- Field-level permission phức tạp.
- CAC đầy đủ bao gồm lương, hoa hồng và chi phí vận hành.

## 3. Data Model Baseline

### 3.1. Tenant, user và cấu hình

| Entity | Field P0 | Ghi chú |
|---|---|---|
| Tenant | tenant_id, tenant_name, timezone, default_currency, status, created_at, updated_at | Timezone mặc định `Asia/Ho_Chi_Minh` |
| Branch | branch_id, tenant_id, branch_name, address, city, status | Dùng cho assignment và dashboard |
| User | user_id, tenant_id, full_name, email, phone, role, branch_ids, status | Một user có thể thuộc nhiều branch nếu tenant cho phép |
| PermissionProfile | role, function_code, access_level, scope | Scope có thể là tenant, branch, team hoặc own |
| WorkingHoursConfig | working_hours_id, tenant_id, branch_id, weekday, start_time, end_time, timezone, is_working_day | Dùng tính SLA trong/ngoài giờ; default pilot 08:00-18:00 Monday-Saturday |
| Language | language_id, tenant_id, name, code, status | Có Anh, Nhật, Trung và custom |
| Program | program_id, tenant_id, language_id, name, exam_track, status | Không hard-code IELTS/JLPT/HSK vào logic lõi |
| Course | course_id, tenant_id, program_id, course_name, level, tuition_gross, currency, status | Dùng cho enrollment/payment |

### 3.2. Lead, customer và opportunity

| Entity | Field P0 | Ghi chú |
|---|---|---|
| Lead | lead_id, tenant_id, full_name, phone_raw, phone_normalized, email, zalo_id, source_type, source, source_created_at, language_id, program_id, branch_id, campaign, adset, ad, utm_source, utm_medium, utm_campaign, consent_consultation, consent_marketing, contactability, lead_temperature, temperature_reason, import_batch_id, raw_payload_id, lead_status, created_at | Lead là inbound signal, không phải deal |
| CustomerProfile | customer_id, tenant_id, full_name, primary_phone, primary_email, zalo_id, guardian_name, guardian_phone, preferred_channel, consent_summary, created_at, updated_at | Một customer có nhiều lead/touchpoint; primary fields là shortcut, identity đầy đủ nằm ở CustomerIdentity |
| CustomerIdentity | identity_id, tenant_id, customer_id, identity_type, raw_value, normalized_value, is_primary, verified_status, source, created_at | P0-lite để lưu nhiều phone/email/Zalo/Facebook/platform ID |
| AdmissionOpportunity | opportunity_id, tenant_id, customer_id, source_lead_id, language_id, program_id, course_id, branch_id, owner_id, current_stage, qualification_status, lead_temperature, sla_policy_id, lost_reason, lost_note, first_touch_id, last_touch_id, created_at, updated_at | Opportunity là cơ hội tuyển sinh |
| Touchpoint | touchpoint_id, tenant_id, customer_id, lead_id, opportunity_id, source, campaign, adset, ad, utm_source, utm_medium, utm_campaign, touch_time, touch_type | Lưu first-touch và last-touch |
| ActivityLog / InteractionLog | activity_id, tenant_id, opportunity_id, customer_id, actor_id, activity_type, activity_result, occurred_at, note, source, created_at | Nguồn đo first response, contact success, note/follow-up |
| DuplicateCase | duplicate_case_id, tenant_id, lead_id, customer_id, match_type, confidence, status, reviewed_by, reviewed_at, resolution_note | Possible duplicate cần review |
| MergeHistory | merge_id, tenant_id, source_customer_id, target_customer_id, merged_by, merged_at, reason, can_unmerge | Admin tối thiểu được unmerge |

### 3.3. Import, pipeline, assignment và SLA

| Entity | Field P0 | Ghi chú |
|---|---|---|
| ImportBatch | import_batch_id, tenant_id, import_type, source_file_name, source_uri, mapping_config, total_rows, created_count, updated_count, skipped_count, error_count, duplicate_count, status, imported_by, imported_at, completed_at, error_report_uri | Áp dụng cho lead, payment, ad cost |
| StageHistory | stage_history_id, tenant_id, opportunity_id, from_stage, to_stage, changed_by, changed_at, reason, duration_in_previous_stage | Bắt buộc để đo bottleneck |
| AssignmentRule | rule_id, tenant_id, priority, language_id, program_id, branch_id, shift, advisor_ids, is_active | Thứ tự ưu tiên: language, program, branch, shift, workload |
| AssignmentPoolState | pool_state_id, tenant_id, pool_key, last_assigned_user_id, updated_at | Lưu con trỏ round-robin để fallback ổn định |
| SlaTask | task_id, tenant_id, opportunity_id, owner_id, task_type, due_at, status, completed_at, overdue_level, escalated_to | Quá hạn lần 1 nhắc Advisor, quá hạn tiếp báo Sales Lead |

### 3.4. Appointment, payment, attribution, message và audit

| Entity | Field P0 | Ghi chú |
|---|---|---|
| Appointment | appointment_id, tenant_id, opportunity_id, appointment_type, scheduled_at, channel, location, status, result_note, created_by, created_at, updated_at | Type gồm consultation, placement test, trial/demo, interview, document check, payment appointment |
| Enrollment | enrollment_id, tenant_id, opportunity_id, customer_id, course_id, enrollment_status, enrollment_date, confirmed_by, created_at | Enrolled cần ghi danh hợp lệ và deposit/payment hợp lệ theo pilot |
| Payment | payment_id, tenant_id, customer_id, opportunity_id, enrollment_id, payment_date, payment_type, amount_paid, revenue_gross, discount_amount, refund_amount, revenue_net, outstanding_amount, currency, transaction_ref, source, match_status, created_by, created_at | Payment import/manual là source of truth P0 |
| AdCost | ad_cost_id, tenant_id, cost_date, source, campaign, adset, ad, cost, currency, impressions, clicks, leads, import_batch_id | Tối thiểu theo date + source + campaign |
| RevenueAttribution | attribution_id, tenant_id, payment_id, opportunity_id, touchpoint_id, attribution_type, attributed_amount, source, campaign, calculated_at | Dashboard mặc định dùng last-touch |
| MessageTemplate | template_id, tenant_id, template_type, channel, title, body, status, updated_by, updated_at | Admin chỉnh được template |
| MessageLog | message_id, tenant_id, customer_id, opportunity_id, channel, template_id, status, sent_at, suppression_reason, provider_message_id | Lưu tối thiểu 24 tháng |
| ConsentRecord | consent_id, tenant_id, customer_id, consent_type, channel, status, source, captured_at, updated_by | Tách consultation consent và marketing consent |
| AuditLog | audit_id, tenant_id, actor_id, action, entity_type, entity_id, before_value, after_value, reason, ip_address, created_at | Lưu tối thiểu 24 tháng |
| WebhookEvent / IntegrationLog | event_id, tenant_id, source_type, external_id, payload_hash, status, error_code, error_message, received_at, processed_at, raw_payload_uri | Debug webhook/form/Meta và chống trùng payload |

## 4. Global Business Rules

### 4.1. Lead validity

- Lead hợp lệ khi có ít nhất một định danh liên hệ: `phone`, `email` hoặc `zalo_id`.
- Nếu thiếu cả ba định danh liên hệ, record không được tạo lead hợp lệ và phải nằm trong import error hoặc invalid lead queue.
- Phone phải được normalize trước khi dedup.
- Email exact nhưng phone khác không được auto-link; phải vào possible duplicate.
- Zalo ID exact chỉ auto-link khi Zalo ID đã được xác thực.
- Chỉ trùng tên không đủ điều kiện merge.

### 4.2. Duplicate và customer profile

| Case | Xử lý |
|---|---|
| Phone exact sau chuẩn hóa | Auto-link lead/touchpoint vào CustomerProfile hiện có |
| Email exact nhưng phone khác | Tạo DuplicateCase trạng thái `Needs Review` |
| Zalo ID exact đã xác thực | Auto-link nếu customer tồn tại |
| Họ tên gần giống, phone/email gần giống | Tạo DuplicateCase trạng thái `Needs Review` |
| Trùng customer nhưng quan tâm program khác | Tạo AdmissionOpportunity mới |
| Merge sai | Admin được unmerge, Sales Lead có quyền theo cấu hình |

CustomerIdentity rule:

- CustomerProfile có thể có nhiều CustomerIdentity cho phone, email, Zalo ID, Facebook ID, cookie/platform ID, guardian phone và learner phone.
- Phone/email/Zalo trên CustomerProfile chỉ là primary snapshot để UI/search nhanh, không phải toàn bộ định danh.
- Không merge mù khi guardian/learner dùng chung phone/email nếu chưa đủ tín hiệu xác nhận.

### 4.3. Pipeline transition mặc định

| From | To allowed |
|---|---|
| New | Contacting, Lost |
| Contacting | Contacted, Lost, Nurturing |
| Contacted | Qualified, Lost, Nurturing |
| Qualified | Program Selected, Appointment Booked, Consulting, Lost |
| Program Selected | Appointment Booked, Consulting, Lost |
| Appointment Booked | Appointment Done, No-show, Cancelled |
| Appointment Done | Consulting, Deposit Paid, Enrolled, Lost |
| No-show | Contacting, Nurturing, Lost |
| Consulting | Deposit Paid, Enrolled, Lost, Nurturing |
| Deposit Paid | Enrolled, Lost, Refunded |
| Enrolled | Chỉ Admin/Finance được chỉnh ngược nếu có lý do |
| Lost | Sales Lead/Admin được reopen |
| Nurturing | Contacting, Qualified, Lost |

Khi chuyển sang `Lost`, hệ thống bắt buộc chọn lost reason. Khi chuyển stage, hệ thống phải tạo StageHistory.

### 4.4. Assignment và SLA

- Assignment P0 chạy theo thứ tự: Language -> Program -> Branch -> Shift/working hour -> Workload -> fallback round-robin.
- Hot lead SLA mặc định: 5 đến 15 phút.
- Normal lead SLA mặc định: 30 đến 60 phút.
- Lead ngoài giờ: gửi auto message xác nhận nếu consent hợp lệ và tạo task đầu ca hôm sau dựa trên WorkingHoursConfig.
- First response SLA hit = opportunity/lead có outbound ActivityLog hợp lệ đầu tiên trong SLA.
- Contact success = khách thật sự nghe máy, trả lời, xác nhận hoặc phản hồi; không đồng nhất với first response SLA hit.
- Quá hạn lần 1: nhắc Advisor.
- Quá hạn tiếp: báo Sales Lead.
- P0 chưa tự động đổi owner khi quá hạn.
- Fallback round-robin phải có AssignmentPoolState hoặc cơ chế tương đương để lưu con trỏ phân lead.

### 4.4A. Activity, working hours và integration log

- Advisor note, call attempt, Zalo/SMS/email attempt, contact connected/replied phải ghi vào ActivityLog/InteractionLog.
- Stage `Contacting` không được tính là contact success nếu không có activity result connected/replied.
- WorkingHoursConfig default cho pilot: Monday-Saturday, 08:00-18:00, timezone tenant.
- Webhook/form/Meta payload phải ghi WebhookEvent/IntegrationLog với idempotency key hoặc payload hash.
- Raw payload/error report có PII phải mask hoặc giới hạn quyền truy cập; retention raw payload nên ngắn hơn audit/message log.

### 4.4B. Enum key convention

- API/DB dùng enum key dạng `UPPER_SNAKE_CASE`.
- UI label có thể là tiếng Việt/Anh và không được dùng làm key.
- Ví dụ: `PROGRAM_SELECTED`, `APPOINTMENT_BOOKED`, `TUITION_TOO_HIGH`, `UNREACHABLE`, `SALES_LEAD`, `META_LEAD_ADS`.

### 4.5. Payment và revenue

- `Deposit Paid` là conversion trung gian.
- `Enrolled` là khi trung tâm xác nhận ghi danh và có deposit/payment hợp lệ theo quy tắc pilot.
- Dashboard P0 mặc định dùng `Revenue Collected`.
- ROAS basic = `Revenue Collected Attributed / Ad Cost`.
- Nếu một customer có nhiều active opportunity, payment matching phải đưa vào review queue, không auto-match mù.
- Payment không match customer/opportunity là `Unmatched Revenue`.
- Revenue không có source/campaign đáng tin là `Unknown Revenue`.

### 4.6. Attribution

- Hệ thống lưu cả first-touch và last-touch.
- Dashboard mặc định dùng last-touch attribution.
- First-touch được hiển thị để tham khảo, không phải default KPI.
- Ad cost P0 tối thiểu theo `date + source + campaign`.

### 4.7. Messaging và consent

- P0 chỉ gồm template, reminder, task, message log và consent check.
- Không làm full journey builder.
- Message engine phải kiểm tra opt-out theo channel trước khi gửi.
- Nếu bị opt-out, không gửi và phải ghi suppression reason vào MessageLog.

## 5. Permission Rules P0

| Function | CEO | Admin | Marketing | Sales Lead | Advisor | CSKH | Finance |
|---|---|---|---|---|---|---|---|
| Xem dashboard revenue | Có | Có | Giới hạn | Giới hạn | Không/giới hạn | Không | Có |
| Import lead | Không | Có | Có | Có thể | Không | Không | Không |
| Xem lead toàn tenant | Có | Có | Có | Có | Chỉ lead được giao | Giới hạn | Không |
| Reassign lead | Không | Có | Không | Có | Không | Không | Không |
| Ghi payment | Không | Có | Không | Không | Giới hạn | Không | Có |
| Export data | Có | Có | Giới hạn | Giới hạn | Không | Không | Giới hạn |
| Sửa consent | Không | Có | Không | Không | Giới hạn | Giới hạn | Không |
| Merge/unmerge | Không | Có | Không | Có | Không | Không | Không |

Mọi thao tác export, merge/unmerge, reassign, payment change, enrollment change, consent change, delete/update dữ liệu quan trọng và import confirm phải ghi AuditLog.

## 6. Story Specs - R1A Lead & Pipeline Core

### US-01.01 - Tạo tenant profile

- Actor: Admin.
- Business context: Mỗi trung tâm pilot cần dữ liệu, timezone, currency và cấu hình tách biệt.
- Pre-condition: Admin có quyền tạo tenant.
- Main flow: Admin tạo tenant, nhập tên trung tâm, timezone, currency, trạng thái và lưu.
- Validation: Tenant name bắt buộc; timezone mặc định `Asia/Ho_Chi_Minh`; tenant đang active mới được nhận lead.
- Permission: Chỉ Admin.
- Acceptance criteria:
  - Khi Admin tạo tenant hợp lệ, hệ thống lưu tenant và tạo cấu hình mặc định.
  - Khi thiếu tenant name, hệ thống báo lỗi và không tạo tenant.
  - Khi tenant inactive, webhook/import không được tạo lead active cho tenant đó.

### US-01.02 - Quản lý user và role

- Actor: Admin.
- Business context: Nhân sự trong trung tâm cần truy cập theo vai trò.
- Main flow: Admin tạo user, chọn role, gán branch/team và active user.
- Validation: Email hoặc phone user không được trùng trong cùng tenant; role bắt buộc.
- Permission: Chỉ Admin tạo/sửa user; CEO chỉ xem.
- Acceptance criteria:
  - User được gán role thì chỉ thấy chức năng trong permission matrix.
  - Advisor chỉ xem lead/opportunity được giao.
  - User inactive không đăng nhập hoặc nhận assignment mới.

### US-01.03 - Cấu hình branch

- Actor: Admin.
- Business context: Assignment và dashboard cần biết lead thuộc cơ sở nào.
- Main flow: Admin tạo branch, nhập tên, thành phố, địa chỉ và trạng thái.
- Validation: Branch name bắt buộc; branch inactive không xuất hiện trong rule mới.
- Permission: Admin quản lý; Sales Lead xem branch liên quan.
- Acceptance criteria:
  - Branch active được dùng trong lead, opportunity, assignment rule và dashboard.
  - Không cho xóa branch đã có dữ liệu; chỉ cho inactive.
  - Lead không có branch vẫn được nhận nhưng sẽ đi qua rule fallback.

### US-01.04 - Cấu hình permission matrix cơ bản

- Actor: Admin.
- Business context: Dữ liệu lead, revenue, payment và export cần kiểm soát quyền.
- Main flow: Admin xem permission matrix mặc định và bật/tắt các quyền P0 theo role nếu tenant cho phép.
- Validation: Không cho Advisor bật quyền export; không cho Marketing ghi payment; mọi thay đổi quyền phải audit.
- Permission: Chỉ Admin.
- Acceptance criteria:
  - Khi role không có quyền, UI/API từ chối thao tác tương ứng.
  - Khi Admin đổi permission, AuditLog ghi before_value và after_value.
  - Permission mặc định khớp bảng Permission Rules P0.

### US-02.01 - Cấu hình ngôn ngữ

- Actor: Admin.
- Business context: Pilot hỗ trợ Anh, Nhật, Trung và custom language.
- Main flow: Admin tạo/sửa language và active/inactive.
- Validation: Name bắt buộc; không trùng language name trong tenant.
- Permission: Admin quản lý; Sales Lead/Marketing xem.
- Acceptance criteria:
  - Tenant mới có thể cấu hình Anh, Nhật, Trung.
  - Admin tạo được custom language.
  - Language inactive không được chọn cho lead/opportunity mới.

### US-02.02 - Cấu hình program và exam track

- Actor: Admin.
- Business context: Lead cần phân loại theo IELTS, JLPT, HSK hoặc track khác.
- Main flow: Admin tạo program dưới language, nhập exam track nếu có.
- Validation: Program phải thuộc một language active; không hard-code logic theo tên exam.
- Permission: Admin quản lý.
- Acceptance criteria:
  - Program được dùng trong lead import, assignment và pipeline.
  - Một language có nhiều program.
  - Program inactive không được chọn cho opportunity mới.

### US-02.03 - Cấu hình course, học phí và level framework

- Actor: Admin.
- Business context: Enrollment/payment cần gắn với course cụ thể.
- Main flow: Admin tạo course, nhập level, tuition gross, currency và status.
- Validation: Tuition gross không âm; currency mặc định theo tenant; course phải thuộc program active.
- Permission: Admin quản lý; Finance xem khi ghi payment.
- Acceptance criteria:
  - Course active được chọn trong enrollment.
  - Tuition gross được dùng làm revenue gross nếu payment import không cung cấp gross.
  - Không cho xóa course đã có enrollment; chỉ cho inactive.

### US-03.01 - Import lead từ CSV/Google Sheet

- Actor: Marketing, Admin, Sales Lead nếu được cấp quyền.
- Business context: Trung tâm thường có lead cũ trong sheet và file export.
- Main flow: User chọn nguồn, upload file hoặc kết nối Sheet, map cột, preview lỗi/duplicate, confirm import.
- Validation: Mỗi row phải có phone/email/Zalo ID; source bắt buộc với lead marketing; phone normalize trước khi dedup.
- Permission: Admin/Marketing được import; Sales Lead có thể được cấp quyền.
- Acceptance criteria:
  - Import không ghi dữ liệu vào hệ thống trước bước confirm.
  - Row thiếu cả phone, email và Zalo ID nằm trong error preview.
  - Sau confirm, ImportBatch hiển thị total, created, updated, skipped, error và duplicate.

### US-03.02 - Nhận lead từ website form/webhook

- Actor: System.
- Business context: Lead form cần vào hệ thống realtime để bảo đảm SLA.
- Main flow: Website gửi payload, hệ thống validate, normalize, tạo lead/touchpoint, chạy dedup và assignment.
- Validation: Webhook phải idempotent theo external_id hoặc payload hash; payload thiếu contact identifier bị reject.
- Permission: Integration key theo tenant.
- Acceptance criteria:
  - Payload hợp lệ tạo lead trong đúng tenant.
  - Gửi lại cùng external_id không tạo lead trùng.
  - Payload lỗi trả response lỗi có lý do đủ để debug.

### US-03.03 - Capture lead từ Meta Lead Ads

- Actor: System, Marketing.
- Business context: Lead ads cần vào nhanh để sales phản hồi.
- Main flow: Meta webhook nhận lead, lấy campaign/adset/ad nếu có, map field và tạo lead.
- Validation: Source mặc định `Meta`; campaign/adset/ad lưu nếu payload có.
- Permission: Marketing/Admin cấu hình kết nối; System ghi lead.
- Acceptance criteria:
  - Lead từ Meta có source, campaign, adset/ad nếu Meta cung cấp.
  - Lead được đưa vào SLA realtime như website form.
  - Lỗi kết nối hoặc payload lỗi được ghi integration log/import error tương ứng.

### US-03.04 - Chuẩn hóa và phát hiện duplicate

- Actor: System.
- Business context: Cùng một học viên/phụ huynh không được biến thành nhiều customer sai.
- Main flow: Hệ thống normalize phone/email/Zalo ID, tìm customer hiện có, auto-link hoặc tạo DuplicateCase.
- Validation: Phone exact auto-link; email exact nhưng phone khác là possible duplicate; chỉ trùng tên không merge.
- Permission: System xử lý tự động; Admin/Sales Lead review possible duplicate.
- Acceptance criteria:
  - Given lead có phone normalized trùng customer, when import/capture, then lead được link vào customer đó và tạo touchpoint mới.
  - Given email trùng nhưng phone khác, when import/capture, then hệ thống tạo DuplicateCase và không auto-merge.
  - Given chỉ trùng full_name, when import/capture, then hệ thống không merge và không tạo duplicate case bắt buộc.

### US-03.05 - Map cột CSV/Sheet vào field hệ thống

- Actor: Admin, Marketing.
- Business context: Mỗi trung tâm có template sheet khác nhau.
- Main flow: User upload file, hệ thống đọc header, user map field nguồn sang field hệ thống, lưu mapping nếu cần.
- Validation: Không cho confirm nếu field bắt buộc không có mapping hoặc rule thay thế hợp lệ.
- Permission: Người có quyền import.
- Acceptance criteria:
  - User map được cột phone/email/Zalo ID, source, campaign, language, program.
  - Mapping config được lưu trong ImportBatch.
  - Hệ thống báo lỗi nếu mapping khiến mọi row thiếu contact identifier.

### US-03.06 - Preview lỗi import trước khi xác nhận

- Actor: Admin, Marketing.
- Business context: Import thẳng vào DB dễ làm bẩn dữ liệu.
- Main flow: Sau mapping, hệ thống validate và hiển thị preview lỗi, duplicate và số lượng dự kiến.
- Validation: Preview phải phân loại error, duplicate, valid, skipped.
- Permission: Người có quyền import.
- Acceptance criteria:
  - Preview hiển thị row number và lý do lỗi.
  - User tải được error report hoặc xem danh sách lỗi.
  - Chỉ sau confirm hệ thống mới tạo/cập nhật lead.

### US-03.07 - Xem lịch sử import

- Actor: Admin, Marketing, Sales Lead nếu có quyền.
- Business context: Cần truy vết batch nào đã tạo dữ liệu nào.
- Main flow: User mở import history, lọc theo type/status/date/user, xem số liệu và report.
- Validation: ImportBatch không được mất mapping_config và summary count.
- Permission: Theo quyền import/xem dữ liệu.
- Acceptance criteria:
  - Import history hiển thị total, created, updated, skipped, error và duplicate.
  - User xem được mapping config và file/report liên quan.
  - Confirm import phải ghi AuditLog.

### US-03.08 - Review possible duplicate, merge và unmerge

- Actor: Admin, Sales Lead.
- Business context: Duplicate không chắc chắn cần người có thẩm quyền xử lý.
- Main flow: User xem DuplicateCase, so sánh dữ liệu, chọn merge/link/ignore, nhập note.
- Validation: Merge cần reason; unmerge chỉ cho Admin tối thiểu; không merge customer khác tenant.
- Permission: Admin merge/unmerge; Sales Lead merge/unmerge theo team hoặc cấu hình tenant.
- Acceptance criteria:
  - Possible duplicate không tự merge trước khi review.
  - Merge ghi MergeHistory và AuditLog.
  - Admin có thể unmerge nếu phát hiện merge sai.

### US-03.09 - Tạo opportunity mới khi customer quan tâm program khác

- Actor: System.
- Business context: Một customer có thể quan tâm nhiều ngôn ngữ/chương trình.
- Main flow: Khi lead auto-link vào customer hiện có, hệ thống kiểm tra active opportunity cùng program; nếu khác program thì tạo opportunity mới.
- Validation: Không tạo customer mới khi đã xác định cùng customer; không tạo opportunity trùng nếu cùng program còn active.
- Permission: System tự động; Advisor/Sales Lead xem.
- Acceptance criteria:
  - Lead mới của customer hiện có nhưng program khác tạo AdmissionOpportunity mới.
  - Lead mới cùng program với active opportunity được gắn vào opportunity hiện tại hoặc touchpoint theo rule tenant.
  - Opportunity mới giữ source_lead_id và touchpoint để attribution.

### US-03.10 - Lưu CustomerIdentity và WebhookEvent

- Actor: System.
- Business context: Lead đa nguồn cần lưu nhiều định danh và log tích hợp để dedup/debug chính xác.
- Main flow: Khi lead vào từ import/webhook/Meta, hệ thống tạo CustomerIdentity phù hợp và ghi WebhookEvent/IntegrationLog nếu nguồn là integration.
- Validation: Identity phải normalize; webhook duplicate theo external_id/payload_hash không tạo lead trùng; raw payload có PII phải mask/giới hạn quyền.
- Permission: System ghi; Admin/Marketing xem integration log theo quyền.
- Acceptance criteria:
  - Customer có thể có nhiều phone/email/Zalo/Facebook/platform identity.
  - Webhook duplicate được ghi nhận là duplicate/ignored, không tạo lead trùng.
  - Payload lỗi có error_code/error_message để debug.

### US-04.01 - Lead inbox của Advisor

- Actor: Advisor.
- Business context: Advisor cần biết lead/opportunity nào cần xử lý hôm nay.
- Main flow: Advisor mở inbox, xem lead được giao, SLA task, stage, contact info và action tiếp theo.
- Validation: Advisor chỉ thấy dữ liệu được giao; thông tin nhạy cảm theo permission.
- Permission: Advisor own scope; Sales Lead xem team; Admin xem tenant.
- Acceptance criteria:
  - Advisor chỉ thấy lead/opportunity của mình.
  - Inbox hiển thị SLA due_at, trạng thái quá hạn và stage hiện tại.
  - Advisor có thể mở chi tiết opportunity từ inbox.

### US-04.02 - Cập nhật stage và thông tin lead/opportunity

- Actor: Advisor, Sales Lead.
- Business context: Pipeline cần phản ánh đúng tiến độ tuyển sinh.
- Main flow: User mở opportunity, cập nhật stage, qualification info, next action và ghi note/activity nếu có tương tác.
- Validation: Stage transition phải thuộc allowed transition; chuyển Lost cần lost reason; mọi stage change tạo StageHistory; note nghiệp vụ phải lưu thành ActivityLog type `NOTE`.
- Permission: Advisor cập nhật opportunity được giao; Sales Lead cập nhật team; Admin toàn tenant.
- Acceptance criteria:
  - Transition sai bị chặn và hiển thị lý do.
  - Chuyển Lost không có lost reason bị chặn.
  - StageHistory ghi from_stage, to_stage, changed_by, changed_at và duration.
  - Advisor note/call/message attempt được lưu vào ActivityLog, không chỉ là text tự do trên Opportunity.

### US-04.03 - Cấu hình pipeline stages

- Actor: Sales Lead, Admin.
- Business context: Một số program có bước placement test, trial/demo hoặc document check khác nhau.
- Main flow: User xem pipeline mặc định và cấu hình stage active/inactive theo program.
- Validation: Không cho xóa các stage lõi đang có dữ liệu; chỉ cho inactive nếu không phá vỡ transition.
- Permission: Admin/Sales Lead.
- Acceptance criteria:
  - Program có thể bật/tắt stage theo cấu hình P0.
  - Pipeline mặc định vẫn đủ mốc để đo lead-to-enrollment.
  - Thay đổi cấu hình pipeline ghi AuditLog.

### US-04.04 - Cấu hình stage bắt buộc/tùy chọn theo program

- Actor: Sales Lead, Admin.
- Business context: Không ép mọi chương trình đi qua cùng một funnel.
- Main flow: User đánh dấu stage required/optional theo program.
- Validation: Required stage không được bỏ qua trong transition nếu program áp dụng.
- Permission: Admin/Sales Lead.
- Acceptance criteria:
  - Program có required stage thì opportunity phải đi qua hoặc có bypass reason được phép.
  - Optional stage có thể bỏ qua theo allowed transition.
  - Cấu hình stage được áp dụng cho opportunity mới sau thời điểm thay đổi.

### US-04.05 - Kiểm soát allowed transition

- Actor: System.
- Business context: Funnel sai nếu user nhảy stage tùy ý.
- Main flow: Khi user đổi stage, hệ thống kiểm tra transition matrix và quyền.
- Validation: Enrolled chỉ chỉnh ngược bởi Admin/Finance có reason; Lost chỉ reopen bởi Sales Lead/Admin.
- Permission: System enforce.
- Acceptance criteria:
  - Không cho nhảy từ New trực tiếp sang Enrolled.
  - Lost có thể reopen bởi Sales Lead/Admin theo transition.
  - Enrolled bị chỉnh ngược phải có reason và AuditLog.

### US-04.06 - Bắt buộc lost reason

- Actor: Advisor, Sales Lead.
- Business context: Dashboard lý do rớt lead phải đáng tin cậy.
- Main flow: Khi chuyển Lost, user chọn lost reason và nhập note nếu chọn Khác.
- Validation: Lost reason bắt buộc; reason Khác bắt buộc note.
- Permission: Người có quyền đổi stage opportunity.
- Acceptance criteria:
  - Không thể chuyển Lost nếu chưa chọn reason.
  - Reason Khác thiếu note bị chặn.
  - Lost reason xuất hiện trong dashboard và export nếu có quyền.

### US-04.07 - Xem stage history

- Actor: Sales Lead, Admin.
- Business context: Cần đo bottleneck và thời gian nằm ở từng stage.
- Main flow: User mở opportunity và xem timeline stage history.
- Validation: StageHistory không được sửa tay; chỉ tạo từ event đổi stage.
- Permission: Sales Lead xem team; Admin xem tenant; Advisor có thể xem own opportunity nếu tenant cho phép.
- Acceptance criteria:
  - Timeline hiển thị stage cũ, stage mới, người đổi, thời điểm và duration.
  - Sales Lead lọc được opportunity nằm lâu ở một stage.
  - StageHistory vẫn giữ khi opportunity chuyển owner.

### US-04.08 - Ghi Activity/InteractionLog

- Actor: Advisor, System.
- Business context: SLA, contact rate và follow-up cần dựa trên hoạt động thật thay vì suy luận từ stage.
- Main flow: Advisor/System ghi call, Zalo, SMS, email, note hoặc system activity vào opportunity.
- Validation: Activity phải có type, result, occurred_at; first response SLA chỉ tính outbound attempt hợp lệ; contact success chỉ tính connected/replied.
- Permission: Advisor ghi activity cho own opportunity; Sales Lead/Admin xem theo scope.
- Acceptance criteria:
  - Advisor ghi được call attempted/no_answer/connected và note.
  - First outbound activity trong SLA được dùng cho first response SLA hit.
  - Activity result connected/replied được dùng cho contact success.
  - Activity timeline hiển thị trong opportunity detail.

### US-05.01 - Định nghĩa assignment rule

- Actor: Sales Lead, Admin.
- Business context: Lead cần đến đúng team/advisor theo ngôn ngữ, chương trình, branch, ca làm và workload.
- Main flow: User tạo rule, chọn điều kiện, advisor/team, priority và active.
- Validation: Priority không trùng trong cùng rule set; advisor phải active; rule inactive không chạy.
- Permission: Admin/Sales Lead.
- Acceptance criteria:
  - Rule match theo thứ tự ưu tiên đã chốt.
  - User có thể kiểm tra lead mẫu sẽ match rule nào.
  - Thay đổi rule ghi AuditLog.

### US-05.02 - Tạo SLA task sau khi assign

- Actor: System.
- Business context: Assignment phải đi kèm hạn phản hồi để không bỏ sót lead.
- Main flow: Khi opportunity được assign owner, hệ thống xác định SLA type, tạo SlaTask và due_at.
- Validation: Hot lead 5 đến 15 phút; normal lead 30 đến 60 phút; ngoài giờ tạo task đầu ca hôm sau theo WorkingHoursConfig.
- Permission: System tạo; owner xem task.
- Acceptance criteria:
  - Lead được assign có SlaTask tương ứng.
  - Lead ngoài giờ có task đầu ca hôm sau và message xác nhận nếu consent hợp lệ.
  - SLA due_at dùng timezone tenant.
  - Nếu thiếu WorkingHoursConfig, hệ thống dùng default pilot 08:00-18:00 Monday-Saturday và ghi warning cấu hình.

### US-05.03 - Cảnh báo SLA quá hạn

- Actor: System, Sales Lead.
- Business context: Sales Lead cần can thiệp khi Advisor phản hồi chậm.
- Main flow: System kiểm tra task quá hạn, nhắc Advisor, sau ngưỡng tiếp theo báo Sales Lead.
- Validation: Không auto đổi owner trong P0.
- Permission: System gửi alert; Sales Lead xem team.
- Acceptance criteria:
  - Quá hạn lần 1 tạo alert cho Advisor.
  - Quá hạn tiếp tạo alert cho Sales Lead.
  - SLA hit/miss dựa trên first valid outbound ActivityLog trong SLA.
  - Contact success được tính riêng từ activity result connected/replied.

### US-05.04 - Fallback round-robin

- Actor: System.
- Business context: Không lead nào được bỏ lại chỉ vì không match rule.
- Main flow: Nếu không rule nào match, hệ thống chọn advisor active theo round-robin trong pool mặc định.
- Validation: Không assign cho user inactive; không assign ngoài branch nếu tenant cấm.
- Permission: System.
- Acceptance criteria:
  - Lead không match rule vẫn có owner nếu còn advisor active.
  - Nếu không có advisor active, lead vào unassigned queue và báo Sales Lead/Admin.
  - Assignment event ghi owner.assigned.
  - Round-robin fallback cập nhật AssignmentPoolState để lần phân tiếp theo ổn định.

### US-05.05 - Cấu hình WorkingHoursConfig cho SLA

- Actor: Admin, Sales Lead.
- Business context: Lead ngoài giờ cần tính đúng task đầu ca hôm sau theo lịch làm việc của tenant/branch.
- Main flow: User cấu hình ngày làm việc, giờ bắt đầu/kết thúc và timezone cho tenant hoặc branch.
- Validation: Nếu chưa cấu hình, hệ thống dùng default pilot Monday-Saturday 08:00-18:00 theo timezone tenant.
- Permission: Admin quản lý; Sales Lead xem hoặc đề xuất theo cấu hình tenant.
- Acceptance criteria:
  - SLA due_at dùng WorkingHoursConfig.
  - Lead ngoài giờ tạo task đầu ca làm việc tiếp theo.
  - Thiếu config không làm hỏng SLA; hệ thống dùng default và ghi warning.

## 7. Story Specs - R1B Appointment & Payment Core

### US-06.01 - Đặt lịch hẹn tuyển sinh

- Actor: Advisor.
- Business context: Appointment là bước trung gian quan trọng giữa lead và enrollment.
- Main flow: Advisor chọn opportunity, tạo appointment, nhập type, thời gian, channel/location và note.
- Validation: scheduled_at bắt buộc; không được đặt lịch ở quá khứ; opportunity phải active.
- Permission: Advisor với own opportunity; Sales Lead/Admin theo scope.
- Acceptance criteria:
  - Appointment Booked được tạo và liên kết opportunity.
  - Opportunity có thể chuyển sang Appointment Booked theo transition.
  - Hệ thống tạo reminder task/message theo rule P0.

### US-06.02 - Chọn appointment type

- Actor: Advisor.
- Business context: Cần phân biệt consultation, placement test, trial/demo, interview, document check, payment appointment.
- Main flow: Advisor chọn appointment type từ danh mục P0.
- Validation: Type bắt buộc; tenant có thể inactive type không dùng.
- Permission: Advisor tạo appointment; Admin cấu hình danh mục nếu cần.
- Acceptance criteria:
  - Appointment luôn có type.
  - Dashboard phân biệt booked/done/no-show theo type.
  - Type inactive không được chọn cho appointment mới.

### US-06.03 - Gửi nhắc lịch hẹn

- Actor: System.
- Business context: Reminder giúp giảm no-show.
- Main flow: Khi appointment booked, hệ thống lên lịch reminder 24h và 2h qua Zalo, SMS fallback, email optional.
- Validation: Phải kiểm tra consent và opt-out trước khi gửi; nếu bị chặn phải ghi MessageLog.
- Permission: System gửi; Admin quản lý template.
- Acceptance criteria:
  - Appointment hợp lệ tạo reminder theo template.
  - Opt-out channel không bị gửi message và có suppression reason.
  - MessageLog ghi channel, template, status và sent_at/suppression_reason.

### US-06.04 - Đánh dấu Done hoặc No-show

- Actor: Advisor.
- Business context: Funnel metrics cần biết học viên có tham gia lịch hay không.
- Main flow: Advisor mở appointment, chọn Done, No-show hoặc Cancelled và nhập result note nếu cần.
- Validation: Không cập nhật result cho appointment chưa đến giờ nếu tenant không cho phép; Done/No-show cập nhật opportunity stage hợp lệ.
- Permission: Advisor own appointment; Sales Lead/Admin theo scope.
- Acceptance criteria:
  - Appointment Done có thể chuyển opportunity sang Appointment Done.
  - No-show chuyển opportunity sang No-show hoặc tạo follow-up theo rule.
  - Result update ghi AuditLog nếu thay đổi sau khi đã chốt.

### US-06.05 - No-show tự tạo follow-up task

- Actor: System.
- Business context: No-show vẫn có thể cứu lại nếu follow-up đúng lúc.
- Main flow: Khi appointment đổi sang No-show, hệ thống tạo task follow-up trong 24 giờ.
- Validation: Không tạo task trùng nếu đã có task no-show đang open.
- Permission: System tạo; owner xử lý.
- Acceptance criteria:
  - No-show tạo task cho owner hiện tại.
  - Task có due_at trong 24 giờ theo timezone tenant.
  - Advisor thấy task trong inbox.

### US-07.01 - Import enrollment/payment data

- Actor: Finance, Admin.
- Business context: Payment import/manual là source of truth P0 cho doanh thu.
- Main flow: User upload payment/enrollment file, map cột, preview lỗi, preview match, confirm import.
- Validation: Mỗi row cần payment_date, amount_paid và một định danh customer: phone/email/customer_code; amount_paid không âm trừ refund.
- Permission: Finance/Admin.
- Acceptance criteria:
  - Import payment có preview trước confirm.
  - Row không match customer/opportunity được đưa vào unmatched/review queue.
  - Confirm import tạo Payment/Enrollment theo rule và ghi AuditLog.

### US-07.02 - Ghi nhận deposit hoặc enrollment thủ công

- Actor: Finance, Admin, Advisor giới hạn nếu tenant cho phép.
- Business context: Pilot có thể cần ghi nhận nhanh deposit/enrollment chưa có file.
- Main flow: User chọn opportunity, nhập payment/enrollment info, chọn payment_type và lưu.
- Validation: Deposit Paid và Enrolled tách biệt; Enrolled cần payment/deposit hợp lệ theo pilot.
- Permission: Finance/Admin full; Advisor chỉ đề xuất hoặc ghi giới hạn.
- Acceptance criteria:
  - Deposit tạo trạng thái Deposit Paid nhưng chưa tự động coi là Enrolled.
  - Enrolled cần enrollment_date và payment/deposit hợp lệ.
  - Manual payment change ghi AuditLog.

### US-07.03 - Match payment/enrollment với customer/opportunity

- Actor: System, Finance.
- Business context: Doanh thu phải nối đúng customer/opportunity để attribution đáng tin cậy.
- Main flow: Hệ thống match theo phone/email/customer_code, kiểm tra active opportunity, auto-match hoặc đưa review queue.
- Validation: Nhiều active opportunity thì không auto-match; phải review. Không match thì `Unmatched Revenue`.
- Permission: System đề xuất; Finance/Admin xác nhận review.
- Acceptance criteria:
  - Given một customer có đúng một active opportunity, when payment match phone/email, then payment được gắn vào opportunity đó.
  - Given một customer có nhiều active opportunity, when payment match customer, then payment vào review queue và không auto-match.
  - Given không tìm thấy customer/opportunity, when import payment, then payment được ghi là Unmatched Revenue nếu tenant cho phép ghi unmatched.

### US-07.04 - Tách deposit, paid, refund và outstanding

- Actor: Finance, CEO xem dashboard.
- Business context: Dashboard không được dùng một số revenue mơ hồ.
- Main flow: Finance nhập/import amount_paid, revenue_gross, discount, refund và outstanding nếu có.
- Validation: Revenue collected = tổng amount_paid hợp lệ; revenue net = collected trừ refund/discount nếu dữ liệu đủ.
- Permission: Finance/Admin ghi; CEO xem.
- Acceptance criteria:
  - Dashboard hiển thị deposit count, enrollment count, paid amount và outstanding nếu có dữ liệu.
  - Refund không được cộng vào Revenue Collected.
  - Unknown hoặc thiếu dữ liệu net không làm sai Revenue Collected.

### US-07.05 - Phân loại Revenue Gross, Collected, Net

- Actor: CEO, Finance.
- Business context: Chủ trung tâm cần hiểu doanh thu cam kết, thực thu và net nếu đủ dữ liệu.
- Main flow: Hệ thống tính/hiển thị Gross, Collected, Net theo dữ liệu payment/enrollment.
- Validation: Dashboard mặc định dùng Revenue Collected; Gross/Net có nhãn rõ.
- Permission: CEO/Admin/Finance xem full; Marketing/Sales Lead xem giới hạn.
- Acceptance criteria:
  - Revenue Collected là metric mặc định trong dashboard P0.
  - Gross và Net không thay thế Collected nếu chưa được cấu hình.
  - Report ghi rõ công thức revenue đang dùng.

### US-09.01 - Gửi Zalo/SMS/email nhắc lịch

- Actor: System, CSKH.
- Business context: Reminder ưu tiên Zalo, SMS fallback, email optional.
- Main flow: Hệ thống gửi theo template khi appointment booked hoặc trước lịch.
- Validation: Check opt-out theo channel; fallback chỉ chạy nếu channel chính không gửi được và consent hợp lệ.
- Permission: System gửi; CSKH xem log/hỗ trợ theo quyền.
- Acceptance criteria:
  - Zalo được ưu tiên nếu channel hợp lệ.
  - SMS fallback khi Zalo không khả dụng và SMS consent hợp lệ.
  - Không gửi qua channel đã opt-out.

### US-09.02 - Nhắc follow-up cho lead chưa đóng phí

- Actor: System, Advisor.
- Business context: Sau appointment done chưa đóng phí cần follow-up trong 24 giờ.
- Main flow: Khi Appointment Done mà chưa có deposit/payment, hệ thống tạo follow-up task.
- Validation: Không tạo task trùng; due_at theo timezone tenant.
- Permission: System tạo; Advisor xử lý own task.
- Acceptance criteria:
  - Appointment Done chưa payment tạo task follow-up.
  - Advisor thấy task trong inbox.
  - Task completed khi Advisor ghi activity hoặc chuyển stage phù hợp.

### US-09.03 - Quản lý message templates

- Actor: Admin.
- Business context: Trung tâm cần template chuẩn cho confirmation, reminder, no-show, deposit và enrolled.
- Main flow: Admin xem template mặc định, chỉnh nội dung, active/inactive template.
- Validation: Template phải có channel, type, body; biến động như tên học viên/lịch hẹn phải validate.
- Permission: Admin.
- Acceptance criteria:
  - Có template mặc định cho các trigger P0.
  - Admin chỉnh template và lưu version/update metadata.
  - Template inactive không được dùng cho message tự động mới.

### US-09.04 - Lưu message log

- Actor: System.
- Business context: Cần biết tin nào đã gửi, thất bại hoặc bị chặn.
- Main flow: Mỗi lần gửi hoặc suppress message, hệ thống tạo MessageLog.
- Validation: MessageLog có customer/opportunity nếu xác định được; status bắt buộc.
- Permission: Admin/CSKH xem theo scope; Advisor xem own customer nếu tenant cho phép.
- Acceptance criteria:
  - Sent/failed/suppressed đều có log.
  - Suppressed log có suppression_reason.
  - MessageLog lưu tối thiểu 24 tháng.

### US-09.05 - Chặn gửi message nếu opt-out

- Actor: System.
- Business context: Hệ thống phải tôn trọng opt-out theo channel.
- Main flow: Trước khi gửi, message engine kiểm tra ConsentRecord và opt-out status.
- Validation: Opt-out channel nào chặn channel đó; opt-out marketing không chặn message phục vụ tư vấn nếu consultation consent còn hợp lệ.
- Permission: System enforce.
- Acceptance criteria:
  - Customer opt-out SMS không nhận SMS.
  - Customer opt-out toàn bộ không nhận outbound message tự động.
  - Mọi lần chặn gửi có MessageLog suppression.

## 8. Story Specs - R1C Attribution & Dashboard

### US-08.01 - Lưu touchpoint source/campaign/ad

- Actor: System, Marketing.
- Business context: Attribution cần dữ liệu touchpoint từ lead source.
- Main flow: Khi lead vào hệ thống, lưu source, campaign, adset/ad, UTM và thời điểm touch.
- Validation: Source bắt buộc với marketing lead; nếu thiếu thì gắn Unknown Source.
- Permission: System ghi; Marketing xem.
- Acceptance criteria:
  - Lead từ form/Meta/import có Touchpoint.
  - Touchpoint lưu campaign/adset/ad nếu payload có.
  - First-touch và last-touch được xác định theo thời gian touch.

### US-08.02 - Match revenue về campaign/source

- Actor: System.
- Business context: Marketing cần biết campaign nào tạo enrollment/payment.
- Main flow: Khi payment match opportunity, hệ thống tính attribution theo last-touch và lưu RevenueAttribution.
- Validation: Nếu không có source/campaign đáng tin, đưa vào Unknown Revenue.
- Permission: System ghi; Marketing/CEO xem theo quyền.
- Acceptance criteria:
  - Payment matched opportunity có attributed source/campaign nếu touchpoint tồn tại.
  - Dashboard mặc định dùng last-touch.
  - Revenue không có source/campaign đáng tin hiển thị riêng là Unknown Revenue.

### US-08.03 - Hiển thị Unknown Revenue

- Actor: CEO, Marketing.
- Business context: Dashboard không được tạo niềm tin ảo khi attribution thiếu dữ liệu.
- Main flow: Dashboard tách revenue attributed, unknown source revenue và unmatched revenue.
- Validation: Unknown Source khác Unmatched Revenue.
- Permission: CEO/Admin xem full; Marketing xem theo scope.
- Acceptance criteria:
  - Revenue có payment nhưng không có source/campaign đáng tin nằm trong Unknown Revenue.
  - Payment chưa match customer/opportunity nằm trong Unmatched Revenue.
  - ROAS không âm thầm cộng Unknown Revenue vào attributed revenue nếu rule không cho phép.

### US-08.04 - Import ad cost

- Actor: Marketing, Admin.
- Business context: Cần cost để tính CPL, cost per appointment, cost per enrollment và ROAS.
- Main flow: User import CSV/Sheet ad cost, map cột, preview lỗi, confirm import.
- Validation: date, source, campaign, cost, currency bắt buộc; cost không âm.
- Permission: Marketing/Admin.
- Acceptance criteria:
  - Ad cost import có preview trước confirm.
  - Row thiếu date/source/campaign/cost/currency bị lỗi.
  - Cost được lưu tối thiểu ở cấp date + source + campaign.

### US-08.05 - Mapping campaign name/UTM campaign với ad cost

- Actor: Marketing.
- Business context: Tên campaign trong lead và file cost có thể lệch nhau.
- Main flow: Marketing tạo mapping giữa UTM/campaign name và campaign cost name.
- Validation: Mapping không được trùng conflict trong cùng tenant/source/date range.
- Permission: Marketing/Admin.
- Acceptance criteria:
  - Dashboard dùng mapping để nối lead/revenue với cost.
  - Mapping thay đổi ghi AuditLog.
  - Nếu không mapping được, report hiển thị cost/revenue unmatched rõ ràng.

### US-08.06 - Xem matched, unmatched và unknown revenue

- Actor: Marketing, CEO.
- Business context: Cần đánh giá chất lượng attribution trước khi ra quyết định.
- Main flow: User mở attribution view, xem revenue matched campaign, unknown source, unmatched payment.
- Validation: Các nhóm phải loại trừ nhau để không double-count.
- Permission: CEO/Admin full; Marketing giới hạn theo quyền.
- Acceptance criteria:
  - Tổng revenue breakdown không double-count.
  - User lọc được theo source/campaign/date/language/program.
  - Unknown và unmatched có giải thích rõ trong label/report.

### US-08.07 - Dashboard ROAS basic

- Actor: CEO, Marketing.
- Business context: Cần đánh giá campaign dựa trên revenue collected attributed và ad cost.
- Main flow: Dashboard tính ROAS basic theo campaign/source/date range.
- Validation: ROAS = Revenue Collected Attributed / Ad Cost; nếu ad cost bằng 0 thì hiển thị N/A hoặc theo rule sản phẩm.
- Permission: CEO/Admin full; Marketing theo scope.
- Acceptance criteria:
  - ROAS basic dùng Revenue Collected Attributed.
  - Dashboard ghi rõ attribution default là last-touch.
  - First-touch có thể xem tham khảo nhưng không thay default KPI.

### US-10.01 - CEO overview dashboard

- Actor: CEO.
- Business context: CEO cần một màn hình tổng quan cho lead, appointment, enrollment, revenue và ROAS.
- Main flow: CEO chọn date range và xem KPI P0.
- Validation: Dashboard dùng timezone tenant; số liệu revenue mặc định là Revenue Collected.
- Permission: CEO/Admin.
- Acceptance criteria:
  - Dashboard có lead count, appointment booked/done/no-show, enrollment count, revenue collected, CPL, cost per enrollment, ROAS.
  - Unknown source revenue hiển thị riêng.
  - Date filter áp dụng nhất quán theo rule ngày đã chốt.

### US-10.02 - Campaign dashboard

- Actor: Marketing.
- Business context: Marketing cần so sánh hiệu quả source/campaign.
- Main flow: User lọc theo source/campaign/date và xem lead, CPL, appointment, enrollment, revenue, ROAS.
- Validation: Cost và revenue phải cùng date range và attribution rule.
- Permission: Marketing/Admin; CEO xem.
- Acceptance criteria:
  - Dashboard hiển thị CPL, cost per appointment, cost per enrollment và ROAS.
  - Campaign thiếu cost vẫn hiển thị lead/revenue nhưng cost metrics là N/A.
  - Campaign có unknown revenue không được cộng sai vào attributed revenue.

### US-10.03 - Sales SLA dashboard

- Actor: Sales Lead.
- Business context: Sales Lead cần theo dõi phản hồi và hiệu quả advisor.
- Main flow: Sales Lead xem lead assigned, first response time, first response SLA hit/miss, contact success rate, appointment, enrollment và lost reason theo advisor.
- Validation: Sales Lead chỉ xem team/branch được cấp quyền.
- Permission: Sales Lead/Admin; Advisor không xem toàn team.
- Acceptance criteria:
  - Dashboard hiển thị first response SLA hit rate theo advisor.
  - Dashboard hiển thị contact success rate riêng, không gộp với SLA hit.
  - Có lọc theo branch, program, language và date range.
  - Lead quá hạn SLA được liệt kê để Sales Lead xử lý.

### US-10.04 - Funnel theo language/program

- Actor: CEO, Sales Lead, Marketing.
- Business context: Cần biết chương trình nào chuyển đổi tốt nhất.
- Main flow: User chọn language/program và xem funnel stage conversion.
- Validation: Funnel dùng StageHistory và current_stage; pipeline config theo program.
- Permission: Theo role dashboard.
- Acceptance criteria:
  - Funnel hiển thị số lượng ở từng stage.
  - Có conversion từ lead sang appointment, deposit và enrolled.
  - Program có stage inactive không làm sai funnel mặc định.

### US-10.05 - Data quality dashboard

- Actor: CEO, Admin, Marketing.
- Business context: Cần biết dữ liệu có đáng tin không.
- Main flow: Dashboard hiển thị duplicate rate, unknown source revenue, unmatched revenue, source completeness.
- Validation: Duplicate rate = duplicate leads / total leads; source completeness tối thiểu đo % lead có source rõ.
- Permission: CEO/Admin full; Marketing xem phần marketing.
- Acceptance criteria:
  - Dashboard hiển thị % lead có source rõ.
  - Duplicate rate hiển thị theo date range.
  - Unknown/unmatched revenue có drill-down nếu có quyền.

### US-11.01 - Lưu consultation consent

- Actor: Admin, Advisor giới hạn, System.
- Business context: Cần biết khách đồng ý được liên hệ tư vấn hay không.
- Main flow: Consent được nhập từ form/import/manual và lưu vào ConsentRecord.
- Validation: Có status, source và captured_at.
- Permission: Admin sửa; Advisor/CSKH sửa giới hạn theo quyền; System ghi từ form.
- Acceptance criteria:
  - Consultation consent lưu riêng với marketing consent.
  - Lead import có thể map consultation consent.
  - Thay đổi consent ghi AuditLog.

### US-11.02 - Lưu marketing consent

- Actor: Admin, System.
- Business context: Marketing message cần consent riêng.
- Main flow: Hệ thống lưu marketing consent theo source/channel.
- Validation: Marketing consent không mặc định true nếu không có dữ liệu.
- Permission: Admin sửa; System ghi từ form/import.
- Acceptance criteria:
  - Marketing consent được phân biệt với consultation consent.
  - Customer không có marketing consent không nhận message marketing tự động.
  - Consent status hiển thị trong customer profile.

### US-11.03 - Ghi consent source và timestamp

- Actor: System.
- Business context: Consent cần truy vết nguồn và thời điểm.
- Main flow: Khi tạo/cập nhật consent, hệ thống lưu source, channel, captured_at và updated_by.
- Validation: captured_at không được rỗng; source là form/import/manual/Meta/Zalo hoặc custom allowed.
- Permission: System enforce.
- Acceptance criteria:
  - Mỗi consent record có source và timestamp.
  - Consent update giữ lịch sử đủ để audit.
  - Import consent thiếu timestamp dùng import time và ghi chú nguồn.

### US-11.04 - Chặn message khi opt-out

- Actor: System.
- Business context: Opt-out là rule bắt buộc cho Zalo/SMS/email/call.
- Main flow: Trước khi gửi, hệ thống kiểm tra channel opt-out.
- Validation: Opt-out theo channel; opt-out toàn bộ chặn tất cả outbound automated message.
- Permission: System enforce; Admin/CSKH cập nhật opt-out theo quyền.
- Acceptance criteria:
  - Opt-out SMS chặn SMS nhưng không tự động chặn call nếu call consent còn hợp lệ.
  - Opt-out toàn bộ chặn outbound message tự động.
  - Mọi suppression được ghi MessageLog.

### US-11.05 - Audit log thao tác quan trọng

- Actor: System.
- Business context: Dữ liệu lead, payment, consent và export cần truy vết.
- Main flow: Hệ thống ghi AuditLog khi user thực hiện thao tác quan trọng.
- Validation: AuditLog có who, what, when, before_value, after_value, reason/note nếu có.
- Permission: Admin xem audit; CEO xem theo policy; user thường không sửa/xóa audit.
- Acceptance criteria:
  - Export data, merge/unmerge, reassign, payment change, enrollment change, consent change và import confirm đều có audit.
  - Audit log không thể sửa từ UI thường.
  - Audit log và message log lưu tối thiểu 24 tháng.

## 9. API và Integration Specs P0

Các endpoint dưới đây là baseline BA để Tech Lead thiết kế API contract chi tiết. Tên endpoint chỉ là đề xuất, chưa phải contract cuối.

| Nhóm | Endpoint/Event đề xuất | Mục đích | Ghi chú |
|---|---|---|---|
| Lead import | `POST /imports/leads/preview` | Upload/map/validate lead import | Không ghi dữ liệu chính |
| Lead import | `POST /imports/leads/{batch_id}/confirm` | Confirm import | Ghi AuditLog |
| Website form | `POST /webhooks/leads/website` | Nhận lead realtime từ form | Cần idempotency key |
| Meta Lead Ads | `POST /webhooks/leads/meta` | Nhận lead Meta realtime | Source mặc định Meta |
| Integration log | `GET /integrations/webhook-events` | Xem webhook/form/Meta events | Debug và idempotency |
| Integration log | `GET /integrations/webhook-events/{event_id}` | Xem chi tiết event | Raw payload phải mask/giới hạn quyền |
| Customer | `GET /customers`, `GET /customers/{id}`, `PATCH /customers/{id}` | Tìm/xem/sửa customer theo scope | Không merge mù identity |
| Customer identity | `GET /customers/{id}/identities` | Xem định danh customer | Phone/email/Zalo/Facebook/platform |
| Duplicate | `POST /duplicates/{case_id}/resolve` | Merge/link/ignore duplicate | Admin/Sales Lead |
| Pipeline | `POST /opportunities/{id}/stage` | Đổi stage | Enforce transition matrix |
| Activity | `POST /opportunities/{id}/activities` | Ghi call/Zalo/SMS/email/note | Nguồn đo SLA/contact |
| Activity | `GET /opportunities/{id}/activities` | Xem activity timeline | Advisor own/Sales Lead team/Admin |
| Assignment | `POST /assignment-rules` | Tạo/sửa assignment rule | Admin/Sales Lead |
| Assignment | `GET /assignment-rules`, `PATCH /assignment-rules/{id}` | Xem/sửa/inactive rule | Cần AssignmentPoolState cho round-robin |
| Working hours | `GET /working-hours`, `PATCH /working-hours` | Xem/sửa lịch làm việc | Tính after-hours SLA |
| Appointment | `POST /appointments` | Tạo appointment | Tạo reminder |
| Payment import | `POST /imports/payments/preview` | Preview payment import | Có match preview |
| Payment import | `POST /imports/payments/{batch_id}/confirm` | Confirm payment import | Finance/Admin |
| Ad cost import | `POST /imports/ad-costs/preview` | Preview ad cost import | Marketing/Admin |
| Message | `POST /messages/send-template` | Gửi template thủ công/bán tự động | Check consent |
| Consent | `POST /customers/{id}/consents` | Cập nhật consent/opt-out | Ghi AuditLog |
| Dashboard | `GET /dashboards/overview` | CEO overview | Date range theo tenant timezone |

Event baseline:

| Event | Khi tạo |
|---|---|
| webhook.received | Nhận payload webhook/form/Meta |
| webhook.processed | Payload webhook xử lý thành công |
| webhook.failed | Payload webhook lỗi |
| lead.created | Lead mới từ import/form/Meta |
| customer.linked | Lead được auto-link vào customer |
| customer.identity.created | Thêm CustomerIdentity mới |
| duplicate.detected | Tạo possible duplicate case |
| opportunity.created | Tạo AdmissionOpportunity |
| activity.created | Ghi call/Zalo/SMS/email/note/system activity |
| owner.assigned | Giao advisor |
| sla.overdue | Quá hạn SLA |
| appointment.booked | Đặt lịch |
| appointment.done | Hoàn thành lịch |
| appointment.no_show | Không tham gia lịch |
| payment.created | Ghi nhận payment |
| enrollment.created | Ghi danh |
| message.sent | Gửi tin |
| message.suppressed | Chặn gửi do opt-out/consent |
| consent.updated | Cập nhật consent |
| revenue.attributed | Match doanh thu với touchpoint |

## 10. Validation Checklist cho PO/Tech Grooming

Trước khi chuyển một story sang Ready for Development, cần xác nhận:

- UI entry point và màn hình liên quan.
- API contract request/response.
- DB schema hoặc migration.
- Error message và edge case.
- Permission scope.
- Audit log cần ghi hay không.
- Event cần phát hay không.
- CustomerIdentity/ActivityLog/WebhookEvent có liên quan hay không.
- WorkingHoursConfig/SLA rule có bị ảnh hưởng hay không.
- Enum key API/DB đã dùng UPPER_SNAKE_CASE hay chưa.
- Raw payload/error report có PII masking và retention hay chưa.
- Test data mẫu.
- Acceptance criteria được PO xác nhận.

## 11. Definition of Ready cho Development

Một story chỉ được đưa vào sprint dev khi đạt đủ:

- Có actor, business context, pre-condition và main flow.
- Có field list liên quan.
- Có validation rule và business rule.
- Có permission rule.
- Có acceptance criteria testable.
- Có API/integration assumption hoặc xác nhận không cần API mới.
- Có enum key và display label nếu story dùng status/stage/reason/role.
- Có cách đo Activity/SLA nếu story ảnh hưởng first response/contact success.
- Không còn conflict với `brief.md`.

## 12. Ưu tiên triển khai đề xuất

| Thứ tự | Nhóm story | Lý do |
|---|---|---|
| 1 | US-01, US-02 | Cần tenant/config trước khi nhận lead |
| 2 | US-03 | Lead import/capture/dedup là lõi dữ liệu |
| 3 | US-04 | Opportunity, pipeline và ActivityLog để vận hành sales/contact thật |
| 4 | US-05 | Assignment, WorkingHoursConfig, AssignmentPoolState và SLA |
| 5 | US-06, US-09 basic | Appointment và reminder để đo bước trung gian |
| 6 | US-07 | Payment/enrollment để có revenue |
| 7 | US-08 | Attribution/ad cost để nối marketing với revenue |
| 8 | US-10 | Dashboard sau khi có data foundation |
| 9 | US-11 | Consent/audit phải đi cùng message/export/payment trước pilot |
