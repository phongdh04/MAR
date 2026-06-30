# Epic Brief - MAR Lead-to-Enrollment MVP

> Phiên bản cập nhật: `v2.1 - Architecture version/convention lock - 2026-06-29`.
> Baseline kỹ thuật hiện hành: `Java + Spring Boot ecosystem`, `PostgreSQL`, `Flyway`, `Spring Data JPA/Hibernate`.
> Ghi chú: file này đã được đồng bộ theo quyết định chọn Spring Boot và MAR-ARCH-1.0; development commitment vẫn phụ thuộc sign-off `SP1-D01` đến `SP1-D10`.
## 1. Trạng thái tài liệu

| Hạng mục | Giá trị |
|---|---|
| Tên tài liệu | Epic Brief - MAR Lead-to-Enrollment MVP |
| Vai trò soạn | Senior Business Analyst |
| Trạng thái | Approved for Epic Brief; Sprint implementation details must follow signed-off downstream docs |
| Ngày baseline | 29/06/2026 |
| Nguồn baseline | Decision Log đã chốt DEC-01 đến DEC-23 |

Tài liệu này là brief chính thức để chuyển từ giai đoạn hiểu yêu cầu sang giai đoạn tách Story Specs. Tài liệu này chưa đủ để dev code ngay. Trước development cần tiếp tục có field list chi tiết, business rules, validation rules, state transition, permission rules, API/integration specs và acceptance criteria đầy đủ cho từng story.

### 1.1. Document lineage

Brief này là nguồn định hướng sản phẩm và phạm vi MVP. Các tài liệu triển khai chi tiết phát sinh sau brief và được dùng để grooming/sign-off gồm:

- `03-r1-story-specs.md`
- `09-r1a-dev-backlog.md`
- `10-r1a-sprint-1-ready-package.md`
- `11-r1a-sprint-1-technical-handoff.md`
- `12-r1a-sprint-1-qa-acceptance-pack.md`
- `13-r1a-sprint-1-signoff-decision-log.md`

Nếu có khác biệt giữa brief và các tài liệu Sprint đã sign-off, BA phải cập nhật traceability và ghi rõ quyết định nào đang supersede nội dung cũ trong brief.

## 2. Định vị sản phẩm

MAR là công cụ tự động hóa tuyển sinh và đo doanh thu thật cho trung tâm ngoại ngữ / đơn vị đào tạo ngôn ngữ đa ngôn ngữ.

MAR không phải:

- CRM tổng quát cho mọi ngành.
- CDP enterprise.
- LMS hoặc app học ngôn ngữ.
- Phần mềm quản lý trung tâm đầy đủ.
- Phần mềm kế toán học phí đầy đủ.
- AI chatbot thay tư vấn viên.
- Ads automation nâng cao.

Thông điệp sản phẩm:

> Từ quảng cáo đến học viên đóng phí: gom lead đa nguồn, phân tư vấn viên, nhắc xử lý đúng SLA, quản lý lịch hẹn tuyển sinh, ghi nhận enrollment/payment và đo doanh thu thật theo campaign, ngôn ngữ, chương trình.

## 3. Mục tiêu MVP

MVP cần chứng minh được vòng khép kín:

```text
Lead quảng cáo/form/import
-> Dedup
-> Customer Profile
-> Admission Opportunity
-> Assignment/SLA
-> Appointment
-> Deposit/Enrollment
-> Payment
-> Revenue Attribution
-> Dashboard
```

Mục tiêu kinh doanh:

| Mục tiêu | Ý nghĩa |
|---|---|
| Không bỏ sót lead | Lead từ nhiều nguồn được gom về hệ thống và có owner |
| Giảm trùng dữ liệu | Phone/email/Zalo ID được chuẩn hóa và phát hiện duplicate |
| Tăng tốc tư vấn | Lead có SLA, task và cảnh báo quá hạn |
| Đo được enrollment thật | Payment/enrollment được nối với customer/opportunity |
| Đo hiệu quả marketing thật | Có ad cost, revenue attribution, CPL, cost per enrollment, ROAS |
| Kiểm soát consent | Message phải kiểm tra consultation/marketing consent và opt-out |

## 4. Phạm vi pilot đã chốt

| Hạng mục | Chốt |
|---|---|
| Mô hình pilot | Trung tâm ngoại ngữ offline/hybrid |
| Quy mô pilot | 3-5 trung tâm |
| Quy mô tư vấn viên | 2-20 tư vấn viên/trung tâm |
| Ngôn ngữ pilot | Anh, Nhật, Trung + custom language |
| Nguồn lead P0 | CSV/Google Sheet, website form webhook, Meta Lead Ads |
| Lead realtime | Form webhook và Meta realtime; CSV/Sheet batch |
| Message P0 | Zalo ưu tiên + SMS fallback, email optional |
| Payment P0 | Manual entry + CSV/Sheet import |
| Attribution P0 | Lưu first-touch và last-touch; dashboard mặc định dùng last-touch |
| Retention P0 | Audit log và message log tối thiểu 24 tháng |

Không chọn online-only subscription làm pilot đầu vì sẽ kéo thêm signup, activation, trial-to-paid, churn, renewal và subscription billing.

## 5. Người dùng chính

| Actor | Nhu cầu chính |
|---|---|
| CEO / Chủ trung tâm | Xem doanh thu thật, ROAS, cost per enrollment, hiệu quả theo campaign/ngôn ngữ/chương trình |
| Marketing | Biết campaign nào tạo lead, appointment, enrollment và revenue |
| Sales Lead / Trưởng nhóm tuyển sinh | Theo dõi assignment, SLA, pipeline, hiệu quả advisor |
| Advisor / Tư vấn viên | Nhận lead, liên hệ, cập nhật stage, đặt appointment, follow-up |
| CSKH / Giáo vụ | Nhắc lịch, hỗ trợ follow-up, gửi tin đúng consent |
| Finance / Học vụ | Ghi nhận/import deposit, enrollment, payment |
| Admin | Cấu hình tenant, branch, user, role, permission, language, program, course, template |

## 6. Định nghĩa nghiệp vụ

| Khái niệm | Định nghĩa |
|---|---|
| Lead | Inbound signal/source record từ form, ads, Zalo, inbox hoặc import |
| Customer Profile | Hồ sơ người liên hệ đã hợp nhất sau dedup |
| CustomerIdentity | Phone/email/Zalo ID/Facebook ID/platform ID/cookie ID thuộc về Customer Profile; một Customer Profile có thể có nhiều identity |
| Learner | Người học thực tế |
| Guardian/Parent | Phụ huynh/người đại diện nếu người liên hệ không phải người học |
| Admission Opportunity | Cơ hội tuyển sinh theo language/program/course |
| Activity/InteractionLog | Lịch sử call attempt, call connected, no answer, Zalo/SMS/email interaction, note và next action |
| WorkingHoursConfig/SlaPolicy | Cấu hình giờ làm việc, ngày nghỉ, timezone và rule tính due_at/SLA theo tenant |
| IntegrationEvent/WebhookEvent | Log sự kiện import/webhook/raw payload/idempotency để debug và chống xử lý trùng |
| Appointment | Lịch hẹn tuyển sinh: tư vấn, test, demo, phỏng vấn, kiểm tra hồ sơ, hẹn đóng phí |
| Enrollment | Ghi danh hợp lệ vào course/program |
| Deposit Paid | Có đặt cọc hợp lệ; là conversion trung gian |
| Payment | Giao dịch đặt cọc, thanh toán, hoàn phí hoặc công nợ cơ bản |
| Revenue Gross | Tổng học phí/giá trị khóa học trước giảm trừ |
| Revenue Collected | Tiền thực thu; metric chính của dashboard P0 |
| Revenue Net | Tiền thực thu sau refund/discount nếu có dữ liệu |
| Unknown Revenue | Revenue không match được source/campaign đáng tin cậy |
| Unmatched Revenue | Payment chưa match được customer/opportunity |

Ranh giới dữ liệu:

```text
Lead = inbound signal / source record
Customer Profile = hồ sơ người liên hệ đã hợp nhất
Admission Opportunity = cơ hội tuyển sinh thực sự
```

Một khách có thể để lại nhiều lead/touchpoint. Sau dedup, lead được gắn vào customer. Nếu khách quan tâm chương trình/ngôn ngữ khác, hệ thống tạo `Admission Opportunity` mới, không tạo customer mới.

Dedup exact phone/email/Zalo phải tra qua `CustomerIdentity` thay vì chỉ tra field primary trên `Customer Profile`. SLA và contact success phải dựa trên `Activity/InteractionLog`, không suy luận trực tiếp từ stage.

## 7. Source of truth MVP

| Dữ liệu | Source of truth |
|---|---|
| Lead | MAR sau khi import/webhook |
| Customer Profile | MAR sau dedup/merge |
| CustomerIdentity | MAR sau normalize/dedup identity |
| Opportunity stage | MAR |
| Activity/InteractionLog | MAR sau advisor/system ghi nhận interaction |
| WorkingHoursConfig/SlaPolicy | MAR theo cấu hình tenant |
| IntegrationEvent/WebhookEvent | MAR sau khi nhận import/webhook/raw payload |
| Appointment | MAR |
| Payment/Enrollment | Manual entry hoặc import từ Finance/Học vụ |
| Ad Cost | CSV/Google Sheet import |
| Campaign metadata | UTM/import/platform export |
| Consent | MAR sau capture/import/manual update |
| Message log | MAR sau gửi hoặc đồng bộ trạng thái gửi |

## 8. Phạm vi chức năng MVP

### Must-have

| Nhóm | Phạm vi |
|---|---|
| Setup | Tenant, branch, user, role, permission matrix cơ bản |
| Config | Language, program, exam, course, level framework |
| Lead capture | CSV/Sheet import, website form webhook, Meta Lead Ads, integration/webhook event log |
| Data quality | Mapping, validation, CustomerIdentity dedup, duplicate preview, import/integration history |
| Dedup | Exact identity auto-link, email/near match possible duplicate, merge/unmerge |
| Pipeline | Business stage, Activity/InteractionLog, stage history, lost reason, transition rule |
| Assignment/SLA | Rule-based assignment, fallback round-robin, WorkingHours/SLA policy, first response task, overdue alert |
| Appointment | Appointment type, reminder, Done/No-show, follow-up task |
| Messaging | Zalo/SMS/email template, appointment reminder, message log, consent check |
| Payment/Enrollment | Manual entry, CSV/Sheet import, payment/enrollment matching |
| Attribution | First-touch, last-touch, unknown/unmatched revenue, ad cost import |
| Dashboard | CEO overview, campaign dashboard, sales SLA dashboard, funnel by language/program |
| Compliance | Consultation consent, marketing consent, opt-out by channel, audit log, integration event trace |

### Out of scope MVP

- Online-only subscription.
- LMS/app học ngôn ngữ.
- Full school management.
- Payment gateway realtime.
- Full journey builder.
- Full Zalo chatbot.
- AI/ML lead scoring.
- Multi-touch attribution nâng cao.
- Field-level permission nâng cao.
- Native mobile app.

## 9. Release 1 delivery slices

| Slice | Mục tiêu | Epic chính |
|---|---|---|
| R1A - Lead & Pipeline Core | Có tenant, config, import lead, CustomerIdentity/dedup, ActivityLog, pipeline, assignment, WorkingHours/SLA | EPIC-01 đến EPIC-05 |
| R1B - Appointment & Payment Core | Có appointment, reminder, enrollment/payment import | EPIC-06, EPIC-07, EPIC-09 basic |
| R1C - Attribution & Dashboard | Có ad cost import, revenue matching, dashboard, consent/audit | EPIC-08, EPIC-10, EPIC-11 |

## 10. Epic list

| Epic ID | Epic Name | Business Goal |
|---|---|---|
| EPIC-01 | Tenant, User, Role & Permission Setup | Mỗi pilot tenant có dữ liệu tách biệt và vận hành đúng quyền |
| EPIC-02 | Language, Program, Course Configuration | Hỗ trợ đa ngôn ngữ, nhiều program/course, không hard-code |
| EPIC-03 | Lead Capture, Import, Data Quality & Dedup | Không mất lead, không trùng lead, dữ liệu đầu vào đáng tin cậy |
| EPIC-04 | Admission Pipeline & Lead Management | Advisor biết opportunity đang ở đâu và cần làm gì tiếp |
| EPIC-05 | Assignment, SLA & Task Reminder | Giảm thời gian phản hồi và tránh bỏ sót lead |
| EPIC-06 | Appointment Management | Đo được bước trung gian từ lead đến enrollment |
| EPIC-07 | Enrollment, Deposit, Payment & Revenue Tracking | Ghi nhận doanh thu thật theo customer/opportunity |
| EPIC-08 | Attribution, Ad Cost & Revenue Matching | Đo campaign tạo học viên đóng phí và ROAS thật |
| EPIC-09 | Messaging & Follow-up Automation | Nhắc lịch/follow-up đúng scope, đúng consent |
| EPIC-10 | Dashboard & Reporting | CEO/Marketing/Sales Lead ra quyết định dựa trên dữ liệu |
| EPIC-11 | Consent, Opt-out & Audit Log | Kiểm soát consent, opt-out và truy vết thao tác quan trọng |

## 11. Business rules trọng yếu

### 11.1. Lead và duplicate

| Case | Xử lý |
|---|---|
| Có phone exact sau chuẩn hóa | Tra `CustomerIdentity`; auto-link vào Customer Profile hiện có |
| Email exact nhưng phone khác | Tra `CustomerIdentity`; possible duplicate nếu chưa đủ chắc chắn |
| Zalo ID exact đã xác thực | Tra `CustomerIdentity`; auto-link nếu đã có customer tương ứng |
| Chỉ trùng tên | Không merge |
| Lead trùng customer nhưng quan tâm program khác | Tạo Admission Opportunity mới |
| Case không chắc chắn | Đưa vào duplicate review, Admin/Sales Lead xử lý |
| Merge sai | Admin có quyền unmerge |

Một Customer Profile có thể có nhiều `CustomerIdentity`. Identity được normalize theo type/source/value, và dedup exact phải ưu tiên identity đã xác thực thay vì chỉ nhìn vào một trường primary trên customer.

### 11.2. Qualified

Opportunity được xem là Qualified khi có tối thiểu:

- Thông tin liên hệ hợp lệ.
- Đã liên hệ được.
- Có nhu cầu học rõ.
- Có timeframe.
- Không phải spam/test/sai đối tượng.

### 11.3. Payment và revenue

| Rule | Chốt |
|---|---|
| Deposit Paid | Conversion trung gian |
| Enrolled | Trung tâm xác nhận ghi danh và có deposit/payment hợp lệ theo pilot |
| Dashboard revenue mặc định | Revenue Collected |
| ROAS MVP | Revenue Collected Attributed / Ad Cost |
| Customer có nhiều active opportunity | Không auto-match payment; đưa vào review queue |

### 11.4. Attribution

| Case | Xử lý P0 |
|---|---|
| Lead có UTM campaign | Gắn campaign từ UTM |
| Lead từ Meta Lead Ads | Source=Meta; lấy campaign/adset/ad nếu có |
| Nhiều touchpoint | Lưu first-touch và last-touch |
| Dashboard mặc định | Last-touch attribution |
| Không có source/campaign đáng tin | Unknown Source |
| Payment không match customer/opportunity | Unmatched Revenue |

### 11.5. Messaging

| Trigger | P0 xử lý |
|---|---|
| Lead mới ngoài giờ | Auto message xác nhận + task đầu ca hôm sau |
| Appointment booked | Auto reminder qua Zalo, SMS fallback |
| Appointment no-show | Tạo follow-up task; message template nếu bật |
| Sau appointment done chưa đóng phí | Tạo task follow-up; advisor gửi template thủ công/bán tự động |
| Enrolled | Gửi tin xác nhận nếu consent hợp lệ |
| Opt-out | Không gửi qua channel đã opt-out, ghi suppression log |

### 11.6. SLA và contact measurement

| Rule | Chốt |
|---|---|
| First response SLA hit | Opportunity có first valid outbound activity trong SLA sau khi assigned |
| Contact success | Opportunity có activity result `CONNECTED`, `REPLIED` hoặc trạng thái tương đương được xác nhận |
| Contacting stage | Chỉ là trạng thái đang xử lý, không tự động được tính là SLA hit hoặc contact success |
| Contacted stage | Chỉ được chuyển khi có bằng chứng activity thành công |
| SLA clock | Tính theo `WorkingHoursConfig/SlaPolicy` của tenant, mặc định `Asia/Ho_Chi_Minh` |
| Ngoài giờ làm việc | Tạo task đầu ca kế tiếp và tính due_at theo policy |

Mục tiêu của SLA là đo advisor có phản hồi đúng hạn hay không. Mục tiêu của contact success là đo khách có thực sự được kết nối/trả lời hay không. Hai KPI này không được gộp làm một.

## 12. Data import baseline

### 12.1. Lead import P0

Lead hợp lệ phải có ít nhất một định danh liên hệ: `phone`, `email`, hoặc `zalo_id`.

| Field | Bắt buộc? |
|---|---|
| full_name | Nên có |
| phone | Bắt buộc nếu không có email/Zalo ID |
| email | Optional |
| zalo_id | Optional |
| language | Nên có |
| program | Nên có |
| source | Bắt buộc nếu import từ marketing |
| campaign | Nên có |
| created_at/source_created_at | Nên có |
| consent_consultation | Nên có |
| consent_marketing | Optional |

### 12.2. Ad cost import P0

| Field | Bắt buộc? |
|---|---|
| date | Bắt buộc |
| source | Bắt buộc |
| campaign | Bắt buộc |
| adset | Optional |
| ad | Optional |
| cost | Bắt buộc |
| currency | Bắt buộc |
| impressions/clicks/leads | Optional |

### 12.3. Payment import P0

| Field | Bắt buộc? |
|---|---|
| payment_date | Bắt buộc |
| phone/email/customer_code | Bắt buộc một trong các field |
| amount_paid | Bắt buộc |
| course/program | Nên có |
| payment_type | deposit/full/refund |
| revenue_gross | Optional |
| discount | Optional |
| refund_amount | Optional |
| transaction_ref | Optional |

## 13. Pipeline mặc định

```text
New
-> Contacting
-> Contacted
-> Qualified
-> Program Selected
-> Appointment Booked
-> Appointment Done / No-show
-> Consulting
-> Deposit Paid
-> Enrolled
-> Lost
-> Nurturing
```

Pipeline có thể bật/tắt stage theo program, nhưng pipeline mặc định phải đủ các mốc để đo lead-to-enrollment.

Stage là trạng thái nghiệp vụ để điều phối pipeline. KPI SLA/contact phải lấy từ `Activity/InteractionLog`, không lấy trực tiếp từ tên stage.

## 14. KPI P0

| KPI | Công thức |
|---|---|
| Lead count | Tổng lead mới |
| Duplicate rate | Duplicate leads / total leads |
| First response SLA hit rate | Opportunities with first valid outbound activity within SLA / assigned opportunities |
| Contact success rate | Opportunities with connected/replied/contacted activity / assigned opportunities |
| Speed to first response | Median time from assigned_at to first valid outbound activity |
| Appointment booked | Số appointment booked |
| Appointment show-up rate | Appointment Done / Appointment Booked |
| Enrollment count | Số enrolled |
| Revenue collected | Tổng paid amount |
| CPL | Ad cost / leads |
| Cost per appointment | Ad cost / appointment booked hoặc done |
| Cost per enrollment | Ad cost / enrolled |
| ROAS basic | Revenue attributed / ad cost |
| Unknown source revenue | Revenue chưa match được source/campaign |

## 15. Definition of Done cho MVP

MVP được xem là pass khi:

- Import được lead/payment/ad cost từ file mẫu.
- Nhận được lead từ form webhook hoặc Meta.
- Dedup qua CustomerIdentity chạy đúng case cơ bản cho phone/email/Zalo ID.
- Tạo được Customer Profile và Admission Opportunity.
- Ghi được Activity/InteractionLog cho call attempt, connected/replied, no answer và follow-up note.
- Phân lead cho advisor, tạo SLA task và đo first response SLA theo WorkingHours/SLA policy.
- Đặt được appointment và ghi Done/No-show.
- Ghi nhận được deposit/enrollment/payment.
- Match được revenue với source/campaign ở mức P0.
- Dashboard hiển thị lead, appointment, enrollment, revenue, CPL, cost per enrollment, ROAS basic.
- Consent/opt-out chặn được message.
- IntegrationEvent/WebhookEvent lưu được raw event, trạng thái xử lý và idempotency key ở mức P0.
- Audit log ghi được thao tác quan trọng.

## 16. Pilot Success Criteria

| Nhóm | Điều kiện pass |
|---|---|
| Data quality | Ít nhất 80% lead mới có source rõ |
| Lead capture | Import và webhook hoạt động ổn định |
| Dedup | Phát hiện duplicate theo CustomerIdentity phone/email/Zalo ID |
| Assignment | Ít nhất 90% lead mới có owner |
| SLA | Đo được first response SLA hit/miss từ Activity/InteractionLog |
| Appointment | Đo được booked/done/no-show |
| Payment | Import/manual payment hoạt động |
| Attribution | Match được revenue với customer/opportunity/source/campaign ở mức P0 |
| Dashboard | CEO xem được lead, appointment, enrollment, revenue, CPL, cost per enrollment, ROAS |
| Adoption | Sales/Marketing dùng hệ thống cho quy trình pilot, không chỉ dùng sheet song song |
| Insight | Phát hiện được campaign/kênh/advisor hiệu quả hoặc lãng phí rõ ràng |

## 17. NFR P0

| NFR | Chốt |
|---|---|
| Multi-tenant isolation | Dữ liệu tenant tách biệt tuyệt đối |
| Security | Role-based access, export control |
| Audit | Ghi log thao tác quan trọng |
| Import reliability | Import có preview, error report, retry và ImportBatch/ImportRow trace |
| Webhook reliability | Webhook có IntegrationEvent/WebhookEvent, retry và idempotency |
| Performance | Dashboard P0 tải được trong mức chấp nhận với dữ liệu pilot |
| Backup | Có backup DB định kỳ |
| PII handling | Phone/email/Zalo ID là dữ liệu nhạy cảm, cần kiểm soát quyền |
| Timezone | Tenant timezone mặc định Asia/Ho_Chi_Minh |
| Log retention | Audit log, activity/message log và integration event log tối thiểu 24 tháng |

## 18. Event assumptions

| Event | Khi nào tạo |
|---|---|
| lead.created | Lead mới từ import/form/Meta |
| integration_event.received | Nhận raw import/webhook event |
| customer.merged | Merge customer |
| opportunity.created | Tạo cơ hội tuyển sinh |
| owner.assigned | Giao advisor |
| activity.logged | Advisor/system ghi nhận call/message/note/follow-up |
| first_response.logged | Ghi nhận first valid outbound activity cho SLA |
| sla.overdue | Quá hạn xử lý |
| appointment.booked | Đặt lịch |
| appointment.done | Hoàn thành lịch |
| appointment.no_show | Không tham gia |
| payment.created | Ghi nhận thanh toán |
| enrollment.created | Ghi danh |
| message.sent | Gửi tin |
| consent.updated | Đổi consent |
| revenue.attributed | Match doanh thu |

## 19. Rủi ro BA còn lại

| Rủi ro | Cách kiểm soát |
|---|---|
| R1 quá rộng | Chia R1A/R1B/R1C |
| Dashboard gây tranh cãi | Chốt last-touch mặc định, hiển thị first-touch tham khảo, tách Unknown Revenue |
| SLA gây tranh cãi | Tách first response SLA hit khỏi contact success; tính từ Activity/InteractionLog |
| Payment match sai khi có nhiều opportunity | Đưa vào review queue |
| Messaging vượt scope | P0 chỉ template, reminder, task, log, consent check |
| Permission còn khung | Chuyển thành permission rules trong Story Specs |
| Dev hiểu nhầm Lead là deal | Khóa định nghĩa Lead là inbound signal, Opportunity là cơ hội tuyển sinh |

## 20. Bước tiếp theo

Sau brief này, BA cần tách Story Specs cho Release 1 theo thứ tự:

1. R1A - Lead & Pipeline Core.
2. R1B - Appointment & Payment Core.
3. R1C - Attribution & Dashboard.

Mỗi Story Spec phải có:

- User Story.
- Business context.
- Pre-condition.
- Main flow.
- Alternative flow.
- Field list.
- Validation rules.
- Permission rules.
- State transition rules nếu có.
- Acceptance criteria.
- Edge cases.
- Event/log cần ghi.

Tại thời điểm Sprint 1 grooming, các tài liệu R1A/Sprint 1 chi tiết đã được tạo và phải được dùng cùng brief khi ra quyết định: dev backlog, ready package, technical handoff, QA acceptance pack và sign-off decision log. Brief giữ vai trò baseline sản phẩm; tài liệu Sprint giữ vai trò baseline triển khai gần nhất.
