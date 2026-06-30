# Draft phạm vi Epic - MAR Lead-to-Enrollment MVP

> Phiên bản cập nhật: `v2.1 - Architecture version/convention lock - 2026-06-29`.
> Baseline kỹ thuật hiện hành: `Java + Spring Boot ecosystem`, `PostgreSQL`, `Flyway`, `Spring Data JPA/Hibernate`.
> Ghi chú: file này đã được đồng bộ theo quyết định chọn Spring Boot và MAR-ARCH-1.0; development commitment vẫn phụ thuộc sign-off `SP1-D01` đến `SP1-D10`.
## 1. Mục tiêu

Tài liệu này là draft scope ở cấp Epic để User/PM duyệt trước khi BA viết Epic Brief chi tiết. Bản này đã được chỉnh theo review BA: bổ sung Messaging vào Release 1, thêm Ad Cost Import, import validation, pipeline governance, consent detail, duplicate handling và ranh giới Payment/Revenue.

## 2. R1 Goal

Release 1 cần chứng minh được vòng khép kín:

```text
Lead quảng cáo/form/import
-> Tư vấn xử lý đúng SLA
-> Đặt lịch tuyển sinh
-> Ghi nhận enrollment/payment
-> Đo revenue, cost per enrollment, ROAS theo source/campaign/language/program
```

## 3. Danh sách Epic nháp

| Epic ID | Epic Name | Statement | Business Goal | Main Actors |
|---|---|---|---|---|
| EPIC-01 | Tenant, User, Role & Permission Setup | Cho phép mỗi trung tâm cấu hình đơn vị, cơ sở, người dùng, vai trò và quyền cơ bản | Mỗi pilot tenant có dữ liệu tách biệt và vận hành đúng quyền | Admin, CEO |
| EPIC-02 | Language, Program, Course Configuration | Cho phép cấu hình ngôn ngữ, chương trình, khóa học, level framework mà không hard-code | Hỗ trợ đa ngôn ngữ và nhiều mô hình tuyển sinh | Admin, Marketing, Sales Lead |
| EPIC-03 | Lead Capture, Import, Data Quality & Dedup | Gom lead từ file/Sheet/form/Meta, chuẩn hóa, validate import và chống trùng | Không mất lead, không trùng lead, dữ liệu đầu vào đủ tin cậy | Marketing, Admin |
| EPIC-04 | Admission Pipeline & Lead Management | Quản lý lead/opportunity theo pipeline tuyển sinh cấu hình được, có transition rule | Tư vấn viên biết lead đang ở đâu và cần làm gì tiếp | Sales Lead, Advisor |
| EPIC-05 | Assignment, SLA & Task Reminder | Phân lead cho tư vấn viên và nhắc xử lý đúng SLA | Giảm thời gian phản hồi, tránh bỏ sót lead | Sales Lead, Advisor |
| EPIC-06 | Appointment Management | Đặt lịch hẹn tuyển sinh theo loại lịch, nhắc lịch và cập nhật Done/No-show | Đo được bước trung gian từ lead đến enrollment | Advisor, CSKH |
| EPIC-07 | Enrollment, Deposit, Payment & Revenue Tracking | Ghi nhận/import đặt cọc, enrollment, payment và phân biệt revenue metrics | Gắn doanh thu thật về lead/source/campaign | Finance/Học vụ, Advisor |
| EPIC-08 | Attribution, Ad Cost & Revenue Matching | Gắn enrollment/payment và ad cost về source/campaign/ad | Biết campaign nào tạo học viên đóng phí và ROAS thật | CEO, Marketing |
| EPIC-09 | Messaging & Follow-up Automation | Gửi Zalo/SMS/email template cho xác nhận, nhắc lịch, follow-up và lưu message log | Tăng tỷ lệ tham gia lịch hẹn và đóng phí | CSKH, Advisor |
| EPIC-10 | Dashboard & Reporting | Hiển thị funnel, marketing, sales, revenue, ngôn ngữ/chương trình | Ra quyết định dựa trên doanh thu thật, không chỉ CPL | CEO, Marketing, Sales Lead |
| EPIC-11 | Consent, Opt-out & Audit Log | Lưu consent theo mục đích/kênh, chặn gửi tin khi opt-out và audit thao tác quan trọng | Giảm rủi ro vận hành và compliance | Admin, CSKH |

## 4. Release 1 Must Scope sau review

| Epic | Must trong Release 1 |
|---|---|
| EPIC-01 | Tenant, branch, user, role, permission matrix cơ bản |
| EPIC-02 | Language/program/course config cho Anh/Nhật/Trung + custom |
| EPIC-03 | CSV/Sheet import, form webhook, Meta Lead Ads, UTM, dedup, import mapping/validation/history |
| EPIC-04 | Lead inbox, pipeline chuẩn, stage update, lost reason, stage history, allowed transition cơ bản |
| EPIC-05 | Assignment rule-based + fallback round-robin, SLA task, overdue alert |
| EPIC-06 | Appointment booking, appointment type, reminder, Done/No-show |
| EPIC-07 | Manual enrollment/payment + import CSV/Sheet, deposit/enrolled/payment matching |
| EPIC-08 | Source/campaign/ad tracking, ad cost import, first/last touch, unknown revenue |
| EPIC-09 | Zalo/SMS/email template tối thiểu, appointment reminder, follow-up reminder, message log |
| EPIC-10 | CEO overview, campaign dashboard, sales SLA dashboard, funnel by language/program |
| EPIC-11 | Consultation consent, marketing consent, opt-out, audit log thao tác quan trọng |

## 5. Release 1 Defer Scope

- TikTok integration.
- Tổng đài/call recording.
- LMS/phụ huynh/học vụ sau ghi danh.
- Payment gateway realtime.
- AI/ML lead scoring.
- Multi-touch attribution nâng cao.
- Full journey builder kéo-thả.
- Full Zalo chatbot.
- Mobile app riêng.
- Field-level permission nâng cao.

## 6. Chia nhỏ delivery cho Release 1

R1 vẫn giữ mục tiêu vòng khép kín, nhưng không nên giao toàn bộ 11 epic cùng lúc. BA đề xuất chia delivery thành 3 lát mỏng:

| Lát | Mục tiêu | Epic chính |
|---|---|---|
| R1A - Lead & Pipeline Core | Có tenant, config, import lead, dedup, pipeline, assignment, SLA | EPIC-01 đến EPIC-05 |
| R1B - Appointment & Payment Core | Có appointment, reminder, enrollment/payment import | EPIC-06, EPIC-07, EPIC-09 basic |
| R1C - Attribution & Dashboard | Có ad cost import, revenue matching, dashboard, consent/audit | EPIC-08, EPIC-10, EPIC-11 |

Ghi chú BA: cách chia này giúp dev giao hàng theo increment nhưng vẫn bảo toàn mục tiêu MVP cuối cùng.

## 7. Draft Story Breakdown cho Release 1

### EPIC-01: Tenant, User, Role & Permission Setup

| Story | User Story |
|---|---|
| US-01.01 | Là Admin, tôi muốn tạo tenant profile để mỗi trung tâm có cấu hình và dữ liệu tách biệt. |
| US-01.02 | Là Admin, tôi muốn quản lý user và role để mỗi nhân viên chỉ truy cập chức năng phù hợp. |
| US-01.03 | Là Admin, tôi muốn cấu hình branch để lead có thể được phân theo cơ sở. |
| US-01.04 | Là Admin, tôi muốn cấu hình permission matrix cơ bản để kiểm soát quyền xem dashboard, import, reassign, payment, export và merge. |

### EPIC-02: Language, Program, Course Configuration

| Story | User Story |
|---|---|
| US-02.01 | Là Admin, tôi muốn cấu hình ngôn ngữ để trung tâm hỗ trợ tiếng Anh, Nhật, Trung và ngôn ngữ custom. |
| US-02.02 | Là Admin, tôi muốn cấu hình program và exam để lead được phân loại theo IELTS, JLPT, HSK hoặc track khác. |
| US-02.03 | Là Admin, tôi muốn cấu hình course, học phí và level framework để enrollment/payment được gắn với khóa học. |

### EPIC-03: Lead Capture, Import, Data Quality & Dedup

| Story | User Story |
|---|---|
| US-03.01 | Là Marketing, tôi muốn import lead từ CSV/Google Sheet để dữ liệu cũ và lead offline có trong hệ thống. |
| US-03.02 | Là Marketing, tôi muốn lead từ website form/webhook tự động vào hệ thống để không bỏ sót inbound lead. |
| US-03.03 | Là Marketing, tôi muốn capture lead từ Meta Lead Ads để sales nhận lead quảng cáo nhanh. |
| US-03.04 | Là System, tôi muốn chuẩn hóa phone/email và phát hiện trùng để cùng một học viên không bị xử lý thành nhiều customer. |
| US-03.05 | Là Admin, tôi muốn map cột CSV/Sheet vào field hệ thống để import dữ liệu từ nhiều template khác nhau. |
| US-03.06 | Là Admin, tôi muốn xem preview lỗi import trước khi xác nhận để tránh đưa dữ liệu sai vào hệ thống. |
| US-03.07 | Là Admin, tôi muốn xem lịch sử import và số bản ghi created/updated/skipped/error để kiểm soát dữ liệu. |
| US-03.08 | Là Sales Lead/Admin, tôi muốn xử lý possible duplicate và có thể merge/unmerge để sửa trường hợp ghép nhầm. |
| US-03.09 | Là System, tôi muốn khi lead trùng customer nhưng quan tâm chương trình khác thì tạo Admission Opportunity mới thay vì tạo customer mới. |

### EPIC-04: Admission Pipeline & Lead Management

| Story | User Story |
|---|---|
| US-04.01 | Là Advisor, tôi muốn xem lead inbox của mình để biết lead nào cần xử lý. |
| US-04.02 | Là Advisor, tôi muốn cập nhật stage và thông tin lead/opportunity để pipeline phản ánh đúng tiến độ tuyển sinh. |
| US-04.03 | Là Sales Lead, tôi muốn cấu hình pipeline stages để mỗi ngôn ngữ/chương trình khớp với quy trình thật. |
| US-04.04 | Là Sales Lead, tôi muốn cấu hình stage bắt buộc/tùy chọn theo program để không ép mọi chương trình dùng cùng một funnel. |
| US-04.05 | Là System, tôi muốn kiểm soát allowed transition giữa các stage để tránh nhảy stage sai logic. |
| US-04.06 | Là System, tôi muốn bắt buộc chọn lost reason khi chuyển sang Lost để dashboard lý do rớt lead đáng tin cậy. |
| US-04.07 | Là Sales Lead, tôi muốn xem stage history để đo thời gian nằm ở từng stage và phát hiện bottleneck. |

### EPIC-05: Assignment, SLA & Task Reminder

| Story | User Story |
|---|---|
| US-05.01 | Là Sales Lead, tôi muốn định nghĩa assignment rule để lead đi đến đúng tư vấn viên/team. |
| US-05.02 | Là System, tôi muốn tạo SLA task sau khi assign để tư vấn viên follow-up đúng hạn. |
| US-05.03 | Là Sales Lead, tôi muốn nhận cảnh báo SLA quá hạn để can thiệp trước khi lead bị mất. |
| US-05.04 | Là System, tôi muốn fallback round-robin khi không rule nào match để lead vẫn được giao owner. |

### EPIC-06: Appointment Management

| Story | User Story |
|---|---|
| US-06.01 | Là Advisor, tôi muốn đặt lịch hẹn tuyển sinh để học viên có thể tham gia tư vấn, placement test hoặc demo. |
| US-06.02 | Là Advisor, tôi muốn chọn appointment type để phân biệt consultation, placement test, trial/demo, interview, document check hoặc payment appointment. |
| US-06.03 | Là System, tôi muốn gửi nhắc lịch hẹn để giảm tỷ lệ no-show. |
| US-06.04 | Là Advisor, tôi muốn đánh dấu appointment là Done hoặc No-show để funnel metrics chính xác. |
| US-06.05 | Là System, tôi muốn No-show tự tạo follow-up task để tư vấn viên xử lý tiếp. |

### EPIC-07: Enrollment, Deposit, Payment & Revenue Tracking

| Story | User Story |
|---|---|
| US-07.01 | Là Finance/Học vụ, tôi muốn import enrollment/payment data để doanh thu được đo lường. |
| US-07.02 | Là Advisor, tôi muốn ghi nhận kết quả deposit hoặc enrollment để hiệu quả campaign được cập nhật. |
| US-07.03 | Là System, tôi muốn match payment/enrollment với customer/opportunity bằng phone/email để doanh thu được nối với nguồn lead. |
| US-07.04 | Là Finance, tôi muốn tách deposit amount, paid amount, refund amount và outstanding amount để dashboard không dùng một số revenue mơ hồ. |
| US-07.05 | Là CEO, tôi muốn revenue được phân loại Gross, Collected, Net nếu có dữ liệu để hiểu đúng hiệu quả kinh doanh. |

### EPIC-08: Attribution, Ad Cost & Revenue Matching

| Story | User Story |
|---|---|
| US-08.01 | Là Marketing, tôi muốn touchpoint của lead lưu source/campaign/ad để attribution khả thi. |
| US-08.02 | Là Marketing, tôi muốn revenue được match về campaign/source để đo cost per enrollment và ROAS. |
| US-08.03 | Là CEO, tôi muốn unknown revenue được hiển thị rõ để dashboard không tạo niềm tin ảo. |
| US-08.04 | Là Marketing, tôi muốn import ad cost theo ngày/source/campaign/adset/ad để hệ thống tính CPL, cost per appointment, cost per enrollment và ROAS. |
| US-08.05 | Là Marketing, tôi muốn mapping campaign name/UTM campaign với ad cost để báo cáo không bị lệch nguồn. |
| US-08.06 | Là Marketing, tôi muốn xem matched revenue, unmatched revenue và unknown source riêng biệt để biết chất lượng attribution. |
| US-08.07 | Là CEO, tôi muốn dashboard hiển thị ROAS basic dựa trên revenue attributed và ad cost để đánh giá campaign. |

### EPIC-09: Messaging & Follow-up Automation

| Story | User Story |
|---|---|
| US-09.01 | Là CSKH, tôi muốn gửi Zalo/SMS/email nhắc lịch để học viên tham gia appointment. |
| US-09.02 | Là Advisor, tôi muốn có nhắc follow-up cho lead chưa đóng phí để không quên lead đủ điều kiện. |
| US-09.03 | Là Admin, tôi muốn quản lý message templates để giao tiếp nhất quán. |
| US-09.04 | Là System, tôi muốn lưu message log để biết tin nào đã gửi, gửi qua kênh nào và trạng thái ra sao. |
| US-09.05 | Là System, tôi muốn chặn gửi message nếu customer đã opt-out ở kênh tương ứng. |

### EPIC-10: Dashboard & Reporting

| Story | User Story |
|---|---|
| US-10.01 | Là CEO, tôi muốn dashboard tổng quan để xem lead, appointment, enrollment, revenue và ROAS. |
| US-10.02 | Là Marketing, tôi muốn campaign dashboard để so sánh CPL, appointment, enrollment và revenue theo source. |
| US-10.03 | Là Sales Lead, tôi muốn dashboard hiệu quả advisor để theo dõi SLA, contact rate và enrollment rate. |
| US-10.04 | Là Manager, tôi muốn funnel theo language/program để biết chương trình nào chuyển đổi tốt nhất. |
| US-10.05 | Là CEO, tôi muốn xem unknown source revenue và duplicate rate để đánh giá độ tin cậy dữ liệu. |

### EPIC-11: Consent, Opt-out & Audit Log

| Story | User Story |
|---|---|
| US-11.01 | Là Admin, tôi muốn lưu consultation consent để biết ai đồng ý được liên hệ tư vấn. |
| US-11.02 | Là Admin, tôi muốn lưu marketing consent để biết ai đồng ý nhận nội dung marketing/ưu đãi. |
| US-11.03 | Là System, tôi muốn ghi consent source/timestamp để truy vết nguồn đồng ý. |
| US-11.04 | Là System, tôi muốn chặn gửi message nếu customer opt-out ở kênh tương ứng. |
| US-11.05 | Là Admin, tôi muốn audit log cho export data, merge/unmerge, change owner, change payment, change consent và thao tác dữ liệu quan trọng. |

## 8. Field list P0 cần bổ sung khi viết Story Specs

### 8.1. Lead import P0

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
| branch | Optional |
| advisor_owner | Optional |
| created_at/source_created_at | Nên có |
| consent_consultation | Nên có |
| consent_marketing | Optional |

### 8.2. Ad cost import P0

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

### 8.3. Payment import P0

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

## 9. Pipeline transition rule P0

| From | To allowed | Rule |
|---|---|---|
| New | Contacting, Lost | Lost phải có reason |
| Contacting | Contacted, Lost, Nurturing | Nếu không liên hệ được nhiều lần có thể chuyển Nurturing |
| Contacted | Qualified, Lost | Qualified cần có nhu cầu rõ |
| Qualified | Appointment Booked, Consulting, Lost | Tùy program |
| Appointment Booked | Appointment Done, No-show, Cancelled | Cần appointment record |
| Appointment Done | Consulting, Lost | Cần kết quả appointment |
| Consulting | Deposit Paid, Enrolled, Lost, Nurturing | Lost bắt buộc reason |
| Deposit Paid | Enrolled, Refunded/Lost | Cần payment/deposit |
| Enrolled | Không chuyển ngược nếu không có quyền Admin/Finance | Tránh sửa sai doanh thu |

## 10. Attribution rule P0

| Trường hợp | Cách xử lý P0 |
|---|---|
| Lead có UTM campaign | Gắn campaign từ UTM |
| Lead từ Meta Lead Ads | Gắn source=Meta, campaign/adset/ad nếu API/import có |
| Payment match customer có 1 active opportunity | Gắn payment vào opportunity đó |
| Payment match customer có nhiều active opportunity | Đưa vào review queue hoặc chọn opportunity gần nhất theo rule được duyệt |
| Payment không match phone/email | Unmatched revenue |
| Revenue có customer nhưng không có campaign | Unknown source revenue |
| Có nhiều touchpoint | Lưu first-touch và last-touch; dashboard mặc định dùng model đã chốt trong Decision Log |

## 11. Messaging automation boundary P0

| Chức năng | P0 nên làm |
|---|---|
| Template | Admin tạo/sửa template cơ bản |
| Manual send | Advisor/CSKH gửi từ lead/opportunity |
| Auto reminder | Appointment reminder tự động |
| Follow-up task | Tạo task follow-up, không nhất thiết auto message |
| Fallback | Zalo fail thì SMS nếu có consent |
| Message log | Lưu channel, template, receiver, status, sent_at |
| Consent check | Chặn gửi nếu opt-out channel tương ứng |

Điểm cần tránh: không biến EPIC-09 thành journey builder phức tạp trong P0.

## 12. Acceptance Criteria mẫu cần áp dụng khi viết Story Specs

### US-03.04 - Dedup lead

```gherkin
Given một lead mới có số điện thoại trùng với customer hiện có
When lead được tạo từ import/form/Meta
Then hệ thống không tạo customer mới
And tạo lead/touchpoint mới dưới customer hiện có
And ghi duplicate status = matched_by_phone

Given một lead mới có email trùng nhưng phone khác
When lead được nhập
Then hệ thống đánh dấu possible duplicate
And yêu cầu Admin hoặc Sales Lead xác nhận merge hoặc keep separate

Given phone nhập dạng +84901234567 và 0901234567
When hệ thống chuẩn hóa
Then hai số được nhận diện là cùng một phone chuẩn
```

### US-07.03 - Match payment/enrollment với customer/opportunity

```gherkin
Given payment import có phone/email trùng với customer
When import được xác nhận
Then hệ thống tạo payment record
And match payment với customer tương ứng
And nếu customer có active opportunity thì gắn payment vào opportunity đó
And revenue dashboard cập nhật trong kỳ báo cáo

Given payment không match được phone/email nào
When import được xác nhận
Then payment được đưa vào unmatched revenue
And CEO dashboard hiển thị unknown/unmatched revenue riêng
```

### US-08.03 - Unknown revenue

```gherkin
Given một enrollment/payment không có source/campaign đáng tin cậy
When dashboard revenue được tính
Then revenue đó không được gán bừa cho campaign bất kỳ
And được hiển thị trong nhóm Unknown Source
And tỷ lệ Unknown Revenue được hiển thị trên dashboard
```

## 13. KPI P0 cho Release 1

| KPI | Công thức |
|---|---|
| Lead count | Tổng lead mới |
| Duplicate rate | Duplicate leads / total leads |
| Contact rate | Contacted leads / assigned leads |
| SLA hit rate | Leads contacted within SLA / assigned leads |
| Appointment booked | Số appointment booked |
| Appointment show-up rate | Appointment Done / Appointment Booked |
| Enrollment count | Số enrolled |
| Revenue collected | Tổng paid amount |
| CPL | Ad cost / leads |
| Cost per appointment | Ad cost / appointment booked hoặc done |
| Cost per enrollment | Ad cost / enrolled |
| ROAS basic | Revenue attributed / ad cost |
| Unknown source revenue | Revenue chưa match được source/campaign |

## 14. Definition of Done cho MVP

MVP được xem là pass khi:

- Import được lead/payment/ad cost từ file mẫu.
- Nhận được lead từ form webhook hoặc Meta.
- Dedup phone/email chạy đúng case cơ bản.
- Tạo được Customer Profile và Admission Opportunity.
- Phân lead cho advisor và tạo SLA task.
- Đặt được appointment và ghi Done/No-show.
- Ghi nhận được deposit/enrollment/payment.
- Match được revenue với source/campaign ở mức P0.
- Dashboard hiển thị lead, appointment, enrollment, revenue, CPL, cost per enrollment, ROAS basic.
- Consent/opt-out chặn được message.
- Audit log ghi được thao tác quan trọng.

## 15. Pilot Success Criteria

| Nhóm | Tiêu chí pass |
|---|---|
| Data | 80%+ lead mới có source rõ |
| Dedup | Giảm lead trùng hoặc phát hiện duplicate rõ |
| Sales | 90%+ lead mới có owner |
| SLA | Đo được SLA hit/miss |
| Appointment | Đo được booked/done/no-show |
| Revenue | Match được payment/enrollment với customer/opportunity |
| Attribution | Có dashboard cost per enrollment/ROAS basic |
| User adoption | Sales/Marketing dùng hệ thống thay vì chỉ dùng sheet |
| Feedback | Chủ trung tâm hiểu dashboard và ra quyết định được |

## 16. MVP API/Event assumptions

| Event | Khi nào tạo |
|---|---|
| lead.created | Lead mới từ import/form/Meta |
| customer.merged | Merge customer |
| opportunity.created | Tạo cơ hội tuyển sinh |
| owner.assigned | Giao advisor |
| sla.overdue | Quá hạn xử lý |
| appointment.booked | Đặt lịch |
| appointment.done | Hoàn thành lịch |
| appointment.no_show | Không tham gia |
| payment.created | Ghi nhận thanh toán |
| enrollment.created | Ghi danh |
| message.sent | Gửi tin |
| consent.updated | Đổi consent |
| revenue.attributed | Match doanh thu |

## 17. Non-functional Requirements P0

| NFR | Gợi ý |
|---|---|
| Multi-tenant isolation | Dữ liệu tenant tách biệt tuyệt đối |
| Security | Role-based access, export control |
| Audit | Ghi log thao tác quan trọng |
| Import reliability | Import có preview, error report, retry |
| Webhook reliability | Webhook có retry/idempotency |
| Performance | Dashboard P0 tải được trong mức chấp nhận với dữ liệu pilot |
| Backup | Có backup DB định kỳ |
| PII handling | Phone/email/Zalo ID là dữ liệu nhạy cảm, cần kiểm soát quyền |
| Timezone | Tenant timezone mặc định Asia/Ho_Chi_Minh |

## 18. Khuyến nghị BA

Bản scope này đã đủ tốt để chuyển sang viết `brief.md` chính thức sau khi User/PM duyệt Decision Log. Trạng thái BA khuyến nghị:

> Approved for Epic Brief, not yet approved for development.

Trước khi dev, bắt buộc bổ sung Story Specs chi tiết cho từng story, bao gồm acceptance criteria, field list, state transition, validation rule, permission rule, attribution rule, event/log cần ghi và edge cases.
