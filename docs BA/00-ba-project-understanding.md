# Ghi nhận hiểu biết BA về dự án MAR

> Phiên bản cập nhật: `v2.1 - Architecture version/convention lock - 2026-06-29`.
> Baseline kỹ thuật hiện hành: `Java + Spring Boot ecosystem`, `PostgreSQL`, `Flyway`, `Spring Data JPA/Hibernate`.
> Ghi chú: file này đã được đồng bộ theo quyết định chọn Spring Boot và MAR-ARCH-1.0; development commitment vẫn phụ thuộc sign-off `SP1-D01` đến `SP1-D10`.
## 1. Vai trò BA hiện tại

Tài liệu này tổng hợp hiểu biết BA về dự án MAR sau khi đọc tài liệu gốc và tiếp thu feedback review BA của User.

Nguồn đã đọc:

- `deep-research-report.md`
- `lo-trinh-du-an-lead-to-revenue-platform.md`
- `overview-mvp-language-enrollment-automation.md`
- Feedback review BA của User ngày 29/06/2026

## 2. Định vị sản phẩm

MAR không phải CRM tổng quát, CDP enterprise, LMS, app học ngôn ngữ, phần mềm kế toán học phí, hoặc phần mềm quản lý trung tâm đầy đủ.

Định vị đúng cho MVP:

> Công cụ tự động hóa tuyển sinh và đo doanh thu thật cho trung tâm ngoại ngữ / đơn vị đào tạo ngôn ngữ đa ngôn ngữ.

Giá trị cốt lõi:

> Từ quảng cáo đến học viên đóng phí: gom lead, chống trùng, phân tư vấn viên, chăm sóc đúng SLA, quản lý lịch hẹn tuyển sinh, ghi nhận enrollment/payment và đo doanh thu thật theo campaign, ngôn ngữ, chương trình, tư vấn viên.

## 3. Trọng tâm pilot đề xuất

| Hạng mục | Đề xuất BA |
|---|---|
| Mô hình pilot | Trung tâm ngoại ngữ offline/hybrid trước |
| Quy mô pilot | 3-5 trung tâm hoặc 3-5 mô hình chương trình khác nhau |
| Ngôn ngữ pilot | Anh, Nhật, Trung; cho phép cấu hình custom |
| Nguồn lead P0 | CSV/Google Sheet, website form webhook, Meta Lead Ads |
| Kênh message P0 | Zalo + SMS fallback, email optional |
| Payment P0 | Manual entry + CSV/Sheet import, chưa cần payment gateway realtime |

Online subscription, LMS, quản lý học vụ sâu và mobile app riêng nên để phase sau hoặc pilot riêng.

## 4. Mục tiêu kinh doanh MVP

| Mục tiêu | Ý nghĩa nghiệp vụ |
|---|---|
| Không bỏ sót lead | Lead từ nhiều nguồn được gom về một nơi và xử lý đúng SLA |
| Không bị trùng lead | Lead được chống trùng theo số điện thoại, email, Zalo ID hoặc platform ID |
| Tăng tốc độ tư vấn | Tư vấn viên nhận lead, có task/SLA, có nhắc việc |
| Đo được doanh thu thật | Enrollment/payment được gắn về source/campaign/ad |
| Tối ưu tuyển sinh | Biết campaign, ngôn ngữ, chương trình, tư vấn viên nào tạo học viên đóng phí |

## 5. Người dùng chính

| Actor | Nhu cầu chính |
|---|---|
| Chủ trung tâm / CEO | Xem doanh thu thật, ROAS, CAC, hiệu quả theo campaign, ngôn ngữ, chương trình |
| Marketing | Biết kênh nào tạo lead, appointment, enrollment và doanh thu thật |
| Trưởng nhóm tuyển sinh | Theo dõi SLA, chia lead, hiệu quả tư vấn viên |
| Tư vấn viên | Nhận lead, gọi/nhắn, cập nhật stage, đặt lịch hẹn tuyển sinh, follow-up |
| CSKH / Giáo vụ | Nhắc lịch, xác nhận ghi danh, chăm sóc sau ghi danh |
| Kế toán / Học vụ | Import/xác nhận enrollment, deposit, payment để đo doanh thu |
| Admin trung tâm | Cấu hình branch, user, role, language, program, pipeline, integration |

## 6. Định nghĩa nghiệp vụ cốt lõi

| Khái niệm | Định nghĩa BA |
|---|---|
| Lead | Một yêu cầu/tín hiệu quan tâm đến khóa học, sinh ra từ form, ads, Zalo, inbox hoặc import |
| Customer Profile | Hồ sơ người liên hệ đã được hợp nhất từ nhiều lead/touchpoint |
| Learner | Người học thực tế |
| Guardian/Parent | Phụ huynh/người đại diện, áp dụng khi người liên hệ không phải người học |
| Admission Opportunity | Cơ hội tuyển sinh cụ thể cho một language/program/course; một customer có thể có nhiều opportunity |
| Enrollment | Kết quả ghi danh vào course/program |
| Payment | Giao dịch đặt cọc, thanh toán, hoàn phí hoặc công nợ cơ bản |
| Revenue Gross | Học phí/gia trị khóa học trước giảm trừ |
| Revenue Collected | Tiền thực thu |
| Revenue Net | Tiền thực thu sau refund/discount nếu dữ liệu pilot có đủ |
| Unknown Revenue | Doanh thu chưa đủ dữ liệu để gắn source/campaign đáng tin cậy |

Ghi chú BA: với ngành ngoại ngữ, cần tách Customer Profile, Learner và Guardian vì nhiều lead là phụ huynh đăng ký cho con. Cũng cần có Admission Opportunity vì một người có thể quan tâm nhiều chương trình như IELTS, JLPT N5, HSK hoặc giao tiếp.

### 6.1. Ranh giới Lead / Customer / Opportunity

Để tránh dev thiết kế `Lead` như một CRM deal rồi bị trùng vai trò với `Admission Opportunity`, BA chốt ranh giới như sau:

```text
Lead = inbound signal / source record
Customer Profile = hồ sơ người liên hệ đã hợp nhất sau dedup
Admission Opportunity = cơ hội tuyển sinh thực sự theo language/program/course
```

Mỗi lần khách để lại thông tin từ form/ads/import có thể tạo một `Lead` hoặc `Touchpoint` mới. Hệ thống không nhất thiết tạo `Customer Profile` mới. Sau khi dedup, lead được gắn vào customer hiện có hoặc tạo customer mới nếu chưa có hồ sơ phù hợp. Nếu khách quan tâm một chương trình/ngôn ngữ mới, hệ thống tạo `Admission Opportunity` mới dưới cùng customer đó.

### 6.2. Định nghĩa Qualified P0

Một opportunity được xem là `Qualified` khi thỏa điều kiện tối thiểu:

| Điều kiện | Gợi ý P0 |
|---|---|
| Có thông tin liên hệ hợp lệ | Phone/email/Zalo ID |
| Liên hệ được | Call answered, message replied hoặc confirmed |
| Có nhu cầu học rõ | Có language/program/goal |
| Có timeframe | Học ngay, trong 1 tháng, trong 3 tháng hoặc timeframe tùy chọn |
| Không sai đối tượng | Không phải spam/test/ngoài khu vực/không có nhu cầu |

P0 có thể cho Sales Lead cấu hình field bắt buộc để chuyển stage sang `Qualified`, nhưng dashboard phải hiểu đây là mốc xác nhận lead có nhu cầu thật, không chỉ là lead có đủ thông tin liên hệ.

## 7. Phạm vi MVP đề xuất

### Must-have

| Nhóm | Chức năng |
|---|---|
| Cấu hình | Tenant, branch, user, role, language, program, course, level framework |
| Lead | Capture/import lead, dedup, UTM/campaign tracking |
| Data quality | Import mapping, validation, duplicate handling, import history |
| Pipeline | Pipeline tuyển sinh cấu hình được theo ngôn ngữ/chương trình |
| Assignment | Phân lead theo ngôn ngữ, chương trình, cơ sở, ca trực, tải công việc |
| SLA | Nhắc việc, cảnh báo quá hạn, escalation cơ bản |
| Activity | Call log, message log, ghi chú, next action |
| Appointment | Đặt lịch hẹn tuyển sinh, appointment type, nhắc lịch, Done/No-show |
| Enrollment | Ghi nhận/import enrollment, đặt cọc, payment |
| Attribution | Gắn doanh thu về source/campaign/ad và ghi nhận unknown revenue |
| Ad cost | Import chi phí quảng cáo theo ngày/source/campaign/adset/ad ở mức P0 |
| Messaging | Zalo/SMS/email template tối thiểu, nhắc lịch và follow-up cơ bản |
| Dashboard | Funnel, marketing, sales, revenue, ngôn ngữ/chương trình |
| Compliance | Consultation consent, marketing consent, opt-out, audit log cơ bản |

### Ngoài phạm vi MVP

- LMS đầy đủ.
- App học ngôn ngữ.
- Quản lý giáo viên/lớp học/toàn bộ học vụ.
- Kế toán học phí đầy đủ.
- AI chatbot thay tư vấn viên.
- Ads automation nâng cao.
- Multi-touch attribution phức tạp.
- Full journey builder kéo-thả.
- Full Zalo chatbot.
- Payment gateway realtime.
- Native mobile app riêng cho sales.

## 8. Quy trình nghiệp vụ lõi

```text
Lead phát sinh từ ads/form/Zalo/inbox/import
-> Chuẩn hóa phone/email/identity
-> Kiểm tra duplicate status
-> Gắn source/campaign/UTM/ad identifiers
-> Tạo/cập nhật Customer Profile
-> Tạo Admission Opportunity theo language/program/course
-> Chấm điểm lead bằng rule-based scoring nếu bật
-> Phân tư vấn viên
-> Tạo task SLA
-> Tư vấn viên liên hệ
-> Qualified / Lost / Appointment Booked
-> Appointment Done / No-show
-> Consulting
-> Deposit Paid / Enrolled
-> Ghi nhận payment/enrollment
-> Match revenue và ad cost về campaign/source
-> Dashboard marketing-sales-revenue
-> Chăm sóc sau ghi danh / upsell / renewal
```

## 9. Pipeline tuyển sinh chuẩn

Pipeline business không chứa trạng thái xử lý dữ liệu như `Deduplicated/Merged`. Các trạng thái đó được lưu ở duplicate/data processing status.

| Stage | Ý nghĩa |
|---|---|
| New | Lead/opportunity mới vào hệ thống |
| Contacting | Đang liên hệ lần đầu |
| Contacted | Đã liên hệ được |
| Qualified | Có nhu cầu thật |
| Program Selected | Đã chọn ngôn ngữ/chương trình quan tâm |
| Appointment Booked | Đã đặt lịch hẹn tuyển sinh |
| Appointment Done | Đã hoàn thành lịch hẹn |
| No-show | Không tham gia lịch hẹn |
| Consulting | Đang tư vấn lộ trình/học phí |
| Deposit Paid | Đã đặt cọc |
| Enrolled | Đã đóng phí/ghi danh hợp lệ |
| Lost | Rớt lead/opportunity |
| Nurturing | Đưa vào chăm sóc lại |

### Trạng thái không thuộc pipeline chính

| Loại trạng thái | Ví dụ | Nơi lưu |
|---|---|---|
| Data processing status | Deduplicated, Merged, Possible Duplicate, Source Matched | Lead/Customer duplicate status |
| Ownership/task status | Assigned, SLA Overdue, Reassigned | Task/Assignment status |
| Import status | Created, Updated, Skipped, Error | ImportBatch/ImportResult |

## 10. Appointment type đề xuất

| Appointment type | Ví dụ sử dụng |
|---|---|
| Consultation | Tư vấn lộ trình/học phí |
| Placement Test | Test đầu vào |
| Trial/Demo Class | Học thử/demo |
| Interview | Phỏng vấn đầu vào/du học |
| Document Check | Kiểm tra hồ sơ, dùng cho du học/xuất khẩu lao động |
| Payment Appointment | Hẹn đóng phí/đặt cọc |

## 11. Data entities lõi

| Entity | Mô tả |
|---|---|
| Tenant | Trung tâm/đơn vị sử dụng hệ thống |
| Branch | Cơ sở/chi nhánh |
| User | Nhân viên hệ thống |
| Role | Quyền người dùng |
| Language | Anh, Nhật, Trung, Hàn, Đức, Pháp, custom |
| Program | IELTS, TOEIC, JLPT, HSK, TOPIK, giao tiếp, du học... |
| Course | Khóa học cụ thể, học phí, thời lượng |
| Customer Profile | Hồ sơ người liên hệ/học viên/phụ huynh đã hợp nhất |
| CustomerIdentity | Phone, email, Zalo ID, Facebook ID, platform ID, cookie/session ID |
| Lead | Tín hiệu quan tâm/touch đầu vào |
| AdmissionOpportunity | Cơ hội tuyển sinh theo language/program/course |
| Activity | Gọi, nhắn, ghi chú, lịch hẹn |
| Task | Việc cần xử lý và deadline SLA |
| Admission Appointment | Lịch hẹn tuyển sinh |
| Enrollment | Ghi danh vào khóa/chương trình |
| Payment | Đặt cọc, thanh toán, hoàn phí, công nợ cơ bản |
| Campaign | Source, medium, campaign, adset, ad |
| AdCost | Chi phí quảng cáo theo ngày/source/campaign/adset/ad |
| Touchpoint | UTM, gclid, fbclid, ttclid |
| RevenueAttribution | Kết quả match revenue với source/campaign/touchpoint |
| MessageTemplate | Template Zalo/SMS/email |
| Message | Tin Zalo/SMS/email đã gửi |
| ConsentEvent | Lịch sử đồng ý/từ chối nhận tư vấn/marketing |
| StageHistory | Lịch sử chuyển stage và thời gian nằm ở từng stage |
| AssignmentHistory | Lịch sử giao/chuyển owner |
| ImportBatch | Lịch sử import file/sheet và kết quả xử lý |
| DuplicateCandidate | Bản ghi trùng chắc chắn hoặc nghi ngờ cần xử lý |
| LostReason | Danh mục lý do rớt lead/opportunity |
| Audit Log | Lịch sử thao tác quan trọng |

## 12. Quan hệ dữ liệu đề xuất

```text
Tenant
  -> Branch
  -> User
  -> Language
  -> Program
  -> Course
  -> Campaign
  -> Customer Profile
       -> CustomerIdentity
       -> Lead
       -> AdmissionOpportunity
            -> Activity
            -> Task
            -> Admission Appointment
            -> Enrollment
            -> Payment
            -> StageHistory
       -> ConsentEvent
       -> Message
```

## 13. Source of truth dữ liệu MVP

| Dữ liệu | Source of truth MVP |
|---|---|
| Lead | MAR sau khi import/webhook |
| Customer Profile | MAR sau khi dedup/merge |
| Admission Opportunity stage | MAR |
| Appointment | MAR |
| Payment/Enrollment | Manual entry hoặc import từ kế toán/học vụ |
| Ad Cost | Import CSV/Sheet |
| Campaign metadata | UTM/import/platform export |
| Consent | MAR sau khi capture/import/manual update |
| Message log | MAR sau khi gửi hoặc đồng bộ trạng thái gửi |

Ghi chú BA: bảng này cần được giữ trong Epic Brief để tránh tranh luận sau này về "số nào là số đúng" khi dữ liệu từ Sheet, Ads platform, kế toán và MAR khác nhau.

## 14. KPI P0 cần chốt cho MVP

| KPI | Công thức đề xuất |
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
| Cost per appointment | Ad cost / appointment booked hoặc appointment done |
| Cost per enrollment | Ad cost / enrolled |
| ROAS basic | Revenue attributed / ad cost |
| Unknown source revenue | Revenue chưa match được source/campaign |

KPI như CAC đầy đủ, LTV, multi-touch attribution, lead score conversion và advisor weighted performance nên để P1 vì cần dữ liệu sạch hơn.

## 15. Nhận định BA sau review

Bộ tài liệu đã đúng hướng sản phẩm, nhưng trước khi viết `brief.md` cần chốt và phản ánh rõ các điểm sau:

1. Messaging là P0 trong Release 1, nhưng scope chỉ gồm template/reminder/message log, không làm journey builder phức tạp.
2. ROAS/CPL bắt buộc cần Ad Cost Import, nếu không dashboard marketing chưa đủ tin cậy.
3. Dedup/Merge không phải pipeline stage, chỉ là trạng thái dữ liệu.
4. Cần Admission Opportunity để xử lý một customer quan tâm nhiều ngôn ngữ/chương trình.
5. Consent phải tách consultation consent và marketing consent, có opt-out và audit.
6. Payment, revenue, enrollment phải có định nghĩa riêng, không dùng một cột revenue chung chung.
