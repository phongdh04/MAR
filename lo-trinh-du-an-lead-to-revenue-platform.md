# Lộ trình dự án: Công cụ MVP tự động hóa tuyển sinh đa ngôn ngữ

## 1. Định vị sản phẩm

Định vị mới:

> Công cụ MVP tự động hóa tuyển sinh và đo doanh thu cho trung tâm ngoại ngữ/đơn vị đào tạo ngôn ngữ đa ngôn ngữ.

Tên nội bộ có thể dùng:

- **Language Enrollment Automation MVP**
- **Công cụ MVP tự động hóa tuyển sinh cho trung tâm ngoại ngữ**

Thông điệp ngắn:

> Từ quảng cáo đến học viên đóng phí — đo được, chăm sóc được, tối ưu được cho mọi chương trình ngôn ngữ.

Lưu ý ranh giới sản phẩm:

> Đây không phải nền tảng để học, không phải LMS và không phải app học ngôn ngữ. Đây là công cụ MVP phục vụ tuyển sinh, tư vấn, chăm sóc lead, ghi nhận enrollment/payment và đo doanh thu thật.

Sản phẩm không hard-code cho tiếng Anh. Tiếng Anh, Nhật, Trung, Hàn, Đức, Pháp hoặc ngôn ngữ khác chỉ là **template/ngữ cảnh cấu hình** bên trong một core hệ thống chung.

## 2. Sản phẩm này là gì?

Sản phẩm là lớp nằm giữa marketing, tuyển sinh, tư vấn, chăm sóc và doanh thu:

```text
Ads / Website / Landing Page / Zalo / Inbox / Form
-> Lead
-> Chống trùng
-> Phân loại ngôn ngữ/chương trình
-> Phân tư vấn viên
-> Gọi/nhắn/chăm sóc
-> Lịch hẹn tuyển sinh: tư vấn, đánh giá trình độ hoặc demo nếu trung tâm có dùng
-> Đóng cọc / đóng học phí / mua gói học
-> Gắn doanh thu về campaign
-> Dashboard tuyển sinh
-> Tự động chăm sóc lại / upsell / renewal
```

Khác biệt so với CRM thường:

- CRM thường lưu khách hàng và trạng thái.
- Công cụ này đo được lead nào thành học viên thật, campaign nào tạo doanh thu thật, tư vấn viên nào chốt tốt và lead rơi ở bước nào.

## 3. Sản phẩm này không phải là gì?

| Không phải | Vì sao |
|---|---|
| Nền tảng/app học ngôn ngữ | MVP không phải nơi học bài, làm bài, luyện nói hay theo dõi tiến độ học tập sâu |
| LMS dạy học online đầy đủ | MVP không tập trung vào bài giảng, bài tập, chấm điểm học tập |
| Phần mềm quản lý lớp học toàn diện | Không làm sâu lịch giáo viên, điểm danh, giáo án, học vụ ở MVP |
| CRM tổng quát cho mọi ngành | Chỉ tập trung ngành giáo dục/ngôn ngữ |
| Công cụ chạy ads thay Meta/Google/TikTok | Ads automation là module riêng, không phải lõi MVP |
| Chatbot thay tư vấn viên | MVP hỗ trợ tư vấn viên, không thay con người |
| Phần mềm kế toán học phí | Chỉ cần ghi nhận enrollment/payment để đo doanh thu |

## 4. Kiến trúc sản phẩm

Không xây mỗi ngôn ngữ thành một sản phẩm riêng. Xây core hệ thống chung + template ngôn ngữ:

```text
Core hệ thống chung
|
+-- Template tiếng Anh
+-- Template tiếng Nhật
+-- Template tiếng Trung
+-- Template tiếng Hàn
+-- Template tiếng Đức
+-- Template tiếng Pháp
+-- Template tùy chỉnh
```

### Core hệ thống chung

| Thành phần | Dùng để làm gì |
|---|---|
| Lead capture | Nhận lead từ ads, form, Zalo, website, inbox, import |
| Lead dedup | Chống trùng lead theo số điện thoại, email, Zalo ID |
| CRM pipeline | Quản lý trạng thái tuyển sinh |
| Sales assignment | Phân lead cho tư vấn viên |
| SLA | Nhắc tư vấn viên xử lý lead đúng thời gian |
| Activity log | Lưu gọi, nhắn, ghi chú, lịch hẹn |
| Appointment booking | Quản lý lịch hẹn tuyển sinh: tư vấn, đánh giá trình độ hoặc demo nếu có |
| Enrollment/payment | Ghi nhận học viên đóng phí |
| Attribution | Gắn doanh thu về nguồn/campaign |
| Zalo/SMS/email workflow | Tự động nhắc lịch, chăm sóc lead |
| Dashboard | Đo CPL, cost per appointment, cost per enrollment, ROAS |
| Consent/audit log | Lưu đồng ý nhận tư vấn, lịch sử gửi tin, lịch sử thao tác |

### Template theo ngôn ngữ

| Thành phần | Ví dụ |
|---|---|
| Loại khóa | IELTS, TOEIC, JLPT, HSK, TOPIK, giao tiếp, du học |
| Mục tiêu học | IELTS 6.5, JLPT N3, HSK 4, giao tiếp công việc |
| Cấp độ | Beginner, Elementary, N5, N4, HSK1, HSK2 |
| Hình thức đánh giá đầu vào | Tư vấn trình độ, kiểm tra trình độ hoặc speaking check nếu trung tâm có |
| Pipeline phụ | Có tư vấn, có phỏng vấn, có đánh giá đầu vào hoặc demo tùy mô hình |
| Nội dung chăm sóc | Tin nhắn Zalo/SMS/email theo từng khóa |
| KPI phụ | Cost per appointment, cost per consultation, cost per enrollment |

## 5. ICP

### ICP chính

Chủ trung tâm ngoại ngữ hoặc chuỗi trung tâm có nhiều nguồn lead, có đội tư vấn tuyển sinh, có quy trình tư vấn/đặt lịch/ghi danh, đang chạy quảng cáo đều nhưng chưa đo được doanh thu thật theo campaign, theo ngôn ngữ và theo tư vấn viên.

### ICP phụ

Chủ đơn vị đào tạo ngôn ngữ online/hybrid có signup, gói dùng thử, paid subscription hoặc khóa trả phí, cần đo từ campaign -> signup -> dùng thử -> paid user -> LTV.

### Tiêu chí ICP chi tiết

| Tiêu chí | Mô tả |
|---|---|
| Loại khách hàng | Trung tâm ngoại ngữ, chuỗi trung tâm, đơn vị đào tạo ngôn ngữ online/hybrid |
| Ngôn ngữ | Anh, Nhật, Trung, Hàn, Đức, Pháp hoặc nhiều ngôn ngữ cùng lúc |
| Quy mô phù hợp | Có ít nhất 2-20 tư vấn viên/telesales |
| Nguồn lead | Meta, Google, TikTok, Zalo, website, landing page, inbox, form |
| Quy trình tuyển sinh | Có tư vấn, đặt lịch, báo học phí, đặt cọc/ghi danh |
| Công cụ hiện tại | Excel, Google Sheet, inbox, CRM rời rạc, phần mềm học viên nhưng không đo attribution |
| Vấn đề chính | Không biết tiền quảng cáo tạo ra bao nhiêu học viên thật |
| Người quyết định mua | Chủ trung tâm, founder, CEO, giám đốc tuyển sinh |
| Người dùng hằng ngày | Marketing, tư vấn tuyển sinh, trưởng nhóm sales, CSKH, kế toán/học vụ |

## 6. Phân khúc pilot

Vì sản phẩm là công cụ MVP dùng chung, không chọn một ngôn ngữ duy nhất để hard-code. Tuy nhiên vẫn nên pilot trong phạm vi hẹp:

> Trung tâm ngoại ngữ có từ 1-3 nhóm chương trình ngôn ngữ, có đội tư vấn tuyển sinh, có quảng cáo đều, đang quản lý lead rời rạc và cần đo doanh thu theo campaign.

Nên pilot với 3 nhóm đầu tiên:

| Nhóm | Vì sao nên có trong pilot |
|---|---|
| Tiếng Anh | Thị trường lớn, quy trình IELTS/TOEIC/giao tiếp rõ |
| Tiếng Nhật | Có JLPT, du học, xuất khẩu lao động, nhu cầu tư vấn cao |
| Tiếng Trung | Có HSK, giao tiếp, thương mại, du học/làm việc |

Mục tiêu pilot không phải xây 3 sản phẩm, mà kiểm tra core hệ thống có đủ linh hoạt cho nhiều ngôn ngữ không.

## 7. Quy trình lead-to-enrollment chuẩn

```text
Lead phát sinh từ ads/form/Zalo/inbox/import
-> Hệ thống chuẩn hóa và chống trùng
-> Xác định ngôn ngữ/chương trình quan tâm
-> Chấm điểm lead
-> Phân tư vấn viên theo ngôn ngữ/chương trình/cơ sở
-> SLA gọi/nhắn lead
-> Tư vấn mục tiêu học
-> Đặt lịch hẹn tuyển sinh
-> Nhắc lịch qua Zalo/SMS/email
-> Cập nhật kết quả lịch hẹn
-> Tư vấn học phí
-> Đặt cọc/đóng học phí
-> Ghi danh
-> Gắn doanh thu về campaign/source
-> Dashboard marketing-sales-revenue
-> Chăm sóc sau ghi danh/upsell/renewal
```

### Pipeline chuẩn cho trung tâm ngoại ngữ

| Stage | Ý nghĩa |
|---|---|
| New Lead | Lead mới vào hệ thống |
| Deduplicated / Merged | Đã kiểm tra trùng |
| Assigned | Đã phân cho tư vấn viên |
| Contacting | Đang gọi/nhắn lần đầu |
| Contacted | Đã liên hệ được |
| Qualified | Có nhu cầu thật |
| Program Selected | Đã xác định ngôn ngữ/khóa quan tâm |
| Appointment Booked | Đã đặt lịch hẹn tuyển sinh |
| Appointment Done | Đã hoàn thành lịch hẹn |
| No-show | Không đến/không tham gia lịch hẹn |
| Consulting | Đang tư vấn lộ trình/học phí |
| Deposit Paid | Đã đặt cọc |
| Enrolled | Đã đóng học phí/ghi danh |
| Lost | Rớt lead |
| Nurturing | Đưa vào chăm sóc lại |

Ghi chú: `Appointment` là lịch hẹn tuyển sinh, không phải tính năng học. Tùy trung tâm, appointment có thể là tư vấn, đánh giá trình độ, demo khóa học, kiểm tra hồ sơ hoặc phỏng vấn. Các stage có thể bật/tắt theo mô hình:

| Mô hình | Stage nên bật |
|---|---|
| IELTS/TOEIC | Appointment Booked, Appointment Done, Consulting, Enrolled |
| JLPT/HSK | Appointment Booked, Level Consultation, Enrolled |
| Giao tiếp | Appointment Booked, Appointment Done, Consulting, Enrolled |
| Du học/XKLĐ | Consulting, Document Check, Interview, Enrolled |
| Online subscription | Signup, Activated, Paid, Renewed |
| Khóa 1-1 | Consultation, Schedule Matching, Payment, Enrolled |

## 8. Cấu hình đa ngôn ngữ

Không hard-code kiểu:

```text
ielts_score
toeic_score
english_level
```

Nên thiết kế chung:

```text
language
program_type
target_exam
current_level
target_level
placement_result
course_recommendation
```

Ví dụ cấu hình:

| Ngôn ngữ | target_exam | current_level | target_level |
|---|---|---|---|
| Tiếng Anh | IELTS | 4.5 | 6.5 |
| Tiếng Anh | TOEIC | 450 | 750 |
| Tiếng Nhật | JLPT | N5 | N3 |
| Tiếng Trung | HSK | HSK2 | HSK4 |
| Tiếng Hàn | TOPIK | TOPIK I | TOPIK II |
| Tiếng Đức | Goethe/TestDaF | A1 | B1/B2 |
| Tiếng Pháp | DELF/DALF | A1 | B2 |

Program type chung:

| Program type | Ví dụ |
|---|---|
| Exam prep | IELTS, TOEIC, JLPT, HSK, TOPIK |
| Communication | Giao tiếp công việc, giao tiếp du lịch |
| Kids/Teen | Trẻ em, học sinh |
| Academic | Tiếng Anh học thuật, du học |
| Business | Tiếng Trung thương mại, tiếng Anh công sở |
| Study abroad | Du học Nhật, Đức, Hàn |
| Labor export | Nhật, Hàn, Đài Loan |
| 1-on-1 tutoring | Gia sư/ngôn ngữ cá nhân hóa |
| Subscription | App/web học ngôn ngữ trả phí |

## 9. Data model lõi

### Entity đa ngôn ngữ cần có

| Entity | Ý nghĩa |
|---|---|
| Language | Ngôn ngữ: Anh, Nhật, Trung, Hàn, Đức, Pháp |
| Program | Chương trình học cụ thể |
| Program Type | Luyện thi, giao tiếp, du học, trẻ em, business |
| Exam Type | IELTS, TOEIC, JLPT, HSK, TOPIK |
| Level Framework | CEFR, JLPT, HSK, TOPIK, custom level |
| Admission Appointment | Lịch hẹn tuyển sinh: tư vấn, đánh giá trình độ, demo hoặc phỏng vấn |
| Course | Khóa học cụ thể |
| Enrollment | Ghi danh vào khóa |
| Learning Goal | Mục tiêu học của lead |
| Campus/Branch | Cơ sở nếu có |
| Advisor Team | Team tư vấn theo ngôn ngữ/chương trình |

### Entity cốt lõi vẫn giữ

| Entity | Ý nghĩa |
|---|---|
| Tenant | Trung tâm/đơn vị sử dụng hệ thống |
| User | Nhân viên |
| Role | Quyền |
| Lead | Khách tiềm năng |
| Customer/Learner | Học viên/phụ huynh |
| Campaign | Chiến dịch marketing |
| Touchpoint | Điểm chạm |
| Activity | Gọi, nhắn, ghi chú |
| Task | Việc cần xử lý |
| Payment | Thanh toán |
| Message | Tin Zalo/SMS/email |
| Consent | Đồng ý nhận tư vấn/tin nhắn |
| Audit Log | Lịch sử thao tác |

### Lead fields nên có

| Nhóm | Trường |
|---|---|
| Thông tin | Họ tên, số điện thoại, email, tuổi, khu vực |
| Vai trò | Học viên tự đăng ký, phụ huynh, doanh nghiệp |
| Ngôn ngữ quan tâm | English, Japanese, Chinese, Korean... |
| Chương trình quan tâm | IELTS, JLPT, HSK, giao tiếp, du học |
| Mục tiêu | IELTS 6.5, JLPT N3, HSK4, giao tiếp công việc |
| Level hiện tại | Chưa biết, beginner, A2, N5, HSK1... |
| Thời gian muốn học | Học ngay, 1 tháng nữa, 3 tháng nữa |
| Ngân sách | Khoảng học phí phù hợp |
| Nguồn | Source, medium, campaign, adset, ad |
| Tracking | UTM, gclid, fbclid, ttclid |
| Sales owner | Tư vấn viên phụ trách |
| Pipeline stage | Trạng thái tuyển sinh |
| Kết quả | Appointment done/no-show, deposit, enrolled, lost |
| Consent | Đồng ý nhận tư vấn/marketing |

## 10. Lead scoring

MVP nên có lead scoring đơn giản nhưng cấu hình được:

| Điều kiện | Điểm gợi ý |
|---|---|
| Có số điện thoại hợp lệ | +30 |
| Chọn rõ ngôn ngữ muốn học | +10 |
| Chọn rõ mục tiêu học | +15 |
| Chọn rõ kỳ thi hoặc chương trình | +15 |
| Muốn học trong 30 ngày | +20 |
| Có ngân sách phù hợp | +15 |
| Đăng ký lịch hẹn tuyển sinh | +25 |
| Nhắn hỏi học phí | +15 |
| Đã từng tương tác nhiều lần | +10 |
| Không nghe máy sau nhiều lần | -15 |
| Sai khu vực/sai đối tượng | -30 |

Phân loại:

| Điểm | Nhóm |
|---|---|
| 80-100 | Lead nóng |
| 50-79 | Lead ấm |
| 20-49 | Lead lạnh |
| Dưới 20 | Cần nurture hoặc lọc |

## 11. Sales assignment và SLA

Hệ thống cần phân lead theo nhiều điều kiện:

| Rule | Ví dụ |
|---|---|
| Theo ngôn ngữ | Lead tiếng Nhật giao team Nhật |
| Theo chương trình | IELTS giao team IELTS, JLPT giao team JLPT |
| Theo cơ sở | Lead ở Đà Nẵng giao cơ sở Đà Nẵng |
| Theo ca trực | Lead ngoài giờ giao sales trực |
| Theo tải công việc | Sales ít lead hơn nhận lead mới |
| Theo kinh nghiệm | Lead nóng hoặc khóa giá cao giao sales senior |
| Theo nguồn | Lead Google Search có intent cao, ưu tiên xử lý nhanh |
| Theo khách cũ | Lead từng học trước đây giao lại tư vấn viên cũ |

SLA đề xuất:

| Loại lead | SLA |
|---|---|
| Lead nóng | Liên hệ trong 5-15 phút |
| Lead thường | Liên hệ trong 30-60 phút |
| Lead ngoài giờ | Gửi Zalo/SMS xác nhận tự động, tạo task đầu ngày hôm sau |
| Lead không nghe máy | Tạo chuỗi gọi lại và nhắn Zalo/SMS |
| Lead đã đặt lịch hẹn | Nhắc trước lịch 24h và 2h |
| Lead sau lịch hẹn | Follow-up trong 24h |
| Lead chưa đóng phí | Đưa vào workflow nurture |

## 12. Workflow tự động bản MVP

| Workflow | Luồng chính |
|---|---|
| Lead mới | Kiểm tra trùng -> gắn nguồn/campaign -> gắn ngôn ngữ/chương trình -> chấm điểm -> phân tư vấn viên -> gửi thông báo |
| Sales chưa xử lý lead | Sau X phút chưa có activity -> nhắc tư vấn viên -> sau Y phút báo trưởng nhóm |
| Đặt lịch hẹn tuyển sinh | Xác nhận -> nhắc trước 24h -> nhắc trước 2h -> sau lịch yêu cầu cập nhật Done/No-show |
| Lead chưa đóng học phí | Sau 1 ngày nhắc follow-up -> sau 3 ngày gửi lợi ích khóa học -> sau 7 ngày gửi ưu đãi/tư vấn lại -> sau 14 ngày nurture |
| Học viên đã ghi danh | Xác nhận -> gửi hướng dẫn nhập học -> dừng remarketing lead -> chuyển sang chăm sóc học viên -> gợi ý upsell/renewal |

## 13. Tích hợp bắt buộc trong MVP

| Tích hợp | Bắt buộc? | Mục đích |
|---|---|---|
| Excel/CSV import | Có | Nhập dữ liệu cũ |
| Google Sheet import/sync | Có | Phù hợp cách vận hành hiện tại |
| Website/landing page form webhook | Có | Nhận lead realtime |
| Meta Lead Ads | Có | Nguồn lead phổ biến |
| UTM tracking | Có | Đo attribution |
| Google Ads cost import | Có | Tính CPL/ROAS |
| Zalo OA/ZNS cơ bản | Có | Nhắc lịch/chăm sóc |
| SMS gateway | Có | Dự phòng |
| Email gateway | Có | Gửi tài liệu/nurture |
| Enrollment/payment import | Có | Đo doanh thu thật |
| Activity/call log thủ công | Có | Sales cập nhật nhanh |
| TikTok Lead/API | Sau MVP | Khi pilot dùng nhiều |
| Tổng đài | Sau MVP | Khi cần ghi âm/call tracking |
| LMS/phần mềm học viên | Sau MVP | Tích hợp tùy khách |
| Payment gateway | Sau MVP | Ưu tiên cho mô hình online/subscription |

## 14. KPI

| Nhóm | KPI |
|---|---|
| Marketing | Lead, CPL, qualified CPL, cost per appointment, cost per enrollment |
| Sales | Speed to lead, contact rate, appointment booking rate, enrollment rate |
| Revenue | Doanh thu theo campaign, doanh thu theo ngôn ngữ, ROAS thật, CAC |
| Program | Enrollment theo khóa, cost per enrollment theo khóa, doanh thu theo chương trình |
| Language | Lead/enrollment/revenue/LTV theo Anh, Nhật, Trung, Hàn... |
| Funnel | Lead -> contacted -> qualified -> appointment -> enrolled |
| Retention | Renewal, upsell, repeat course, referral |
| Mô hình online/subscription | Signup, activated trial, trial-to-paid, CAC, LTV, churn |

## 15. Dashboard MVP

### Dashboard 1: Tổng quan tuyển sinh

| Chỉ số | Có lọc theo |
|---|---|
| Lead | Thời gian, nguồn, ngôn ngữ, chương trình |
| CPL | Source, campaign, ngôn ngữ |
| Appointment booked/done/no-show | Ngôn ngữ, chương trình, tư vấn viên |
| Enrollment | Ngôn ngữ, khóa, campaign |
| Doanh thu | Campaign, sales, ngôn ngữ |
| Cost per enrollment | Campaign, ngôn ngữ |
| ROAS thật | Campaign, source |
| Funnel drop-off | Theo từng stage |

### Dashboard 2: Hiệu quả marketing

- Kênh nào tạo nhiều lead nhất?
- Kênh nào tạo học viên thật?
- Campaign nào lead rẻ nhưng không chốt?
- Campaign nào CPL cao nhưng doanh thu tốt?
- Ngôn ngữ nào đang tuyển sinh tốt nhất?
- Khóa nào có cost per enrollment thấp nhất?
- Nguồn nào tạo LTV cao?

### Dashboard 3: Hiệu quả tư vấn viên

- Lead assigned.
- First response time.
- Contact rate.
- Appointment booking rate.
- Enrollment rate.
- Revenue closed.
- Follow-up overdue.
- Lost reason.

### Dashboard 4: Funnel theo ngôn ngữ/chương trình

Điểm quan trọng: funnel phải cấu hình được theo từng program, không cố định một pipeline duy nhất.

## 16. MoSCoW cho MVP

### Must-have

| Tính năng | Lý do |
|---|---|
| Multi-language configuration | Công cụ MVP phải dùng được cho nhiều ngôn ngữ |
| Program/course configuration | Mỗi trung tâm có khóa khác nhau |
| Lead capture | Nhận lead từ form/import/Meta |
| Lead dedup | Tránh trùng dữ liệu |
| UTM tracking | Attribution cơ bản |
| Pipeline tuyển sinh cấu hình được | Mỗi ngôn ngữ/chương trình có quy trình khác |
| Sales assignment | Phân lead cho tư vấn viên |
| SLA/reminder | Không bỏ sót lead |
| Activity log | Lưu gọi/nhắn/ghi chú |
| Appointment booking | Quản lý lịch hẹn tuyển sinh, không quản lý hoạt động học |
| Enrollment/payment import | Đo doanh thu thật |
| Dashboard lead-to-enrollment | Giá trị cốt lõi |
| Zalo/SMS/email reminder cơ bản | Nhắc lịch và chăm sóc |
| Consent status | Cần cho gửi tin |
| Audit log cơ bản | Cần cho quản trị |

### Should-have

- Template tiếng Anh/Nhật/Trung.
- Lead scoring.
- Lost reason analytics.
- Sales leaderboard.
- Google Ads cost import/API.
- Zalo OA mapping.
- Workflow nurture lead chưa mua.
- Dashboard theo ngôn ngữ/chương trình.

### Could-have

- TikTok Lead integration.
- Tổng đài.
- AI gợi ý nội dung follow-up.
- AI phân loại ghi chú sales.
- Multi-touch attribution nâng cao.
- Mobile app cho sales.

### Won't-have trong MVP

- LMS đầy đủ.
- Quản lý giáo viên/lớp học đầy đủ.
- Kế toán học phí đầy đủ.
- AI chatbot thay tư vấn viên.
- Ads automation nâng cao.
- Full CDP enterprise.

## 17. Roadmap triển khai

### Giai đoạn 0: Chốt định vị và pilot

| Mục tiêu | Nội dung |
|---|---|
| Chốt sản phẩm | Công cụ MVP tự động hóa tuyển sinh cho trung tâm/đơn vị đào tạo ngôn ngữ |
| Chọn pilot | 3-5 đơn vị thuộc Anh, Nhật, Trung hoặc trung tâm đa ngôn ngữ |
| Chốt ICP | Chủ trung tâm ngoại ngữ có đội tư vấn và chạy ads đều |
| Chốt thông điệp | Từ quảng cáo đến học viên đóng phí — đo được, chăm sóc được, tối ưu được |
| Chốt quy trình | Lead-to-enrollment chung, pipeline cấu hình được |
| Chốt tích hợp | Import, form, Meta, UTM, Google cost, Zalo, SMS/email, payment import |
| Chốt KPI | CPL, cost per appointment, cost per enrollment, ROAS, CAC, LTV theo ngôn ngữ/chương trình |

### Giai đoạn 1: Core MVP

- Xây tenant/user/role.
- Xây language/program/course/level framework.
- Xây lead/customer/learner.
- Xây pipeline cấu hình được.
- Xây lead capture/import/dedup.
- Xây sales assignment/SLA/activity log.
- Xây appointment booking cho tuyển sinh.
- Xây enrollment/payment import.
- Xây attribution và dashboard MVP.

### Giai đoạn 2: Pilot vận hành

- Tích hợp Meta Lead Ads, website form, Google Sheet/import, Zalo OA/ZNS cơ bản.
- Chạy pilot với 3-5 đơn vị.
- Đo data match rate, speed to lead, cost per enrollment, ROAS thật.
- Kiểm chứng template Anh/Nhật/Trung và khả năng tùy chỉnh pipeline.

### Giai đoạn 3: Mở rộng module

- Zalo nurture nâng cao.
- Ads-to-enrollment conversion feedback.
- Renewal/upsell/referral.
- Compliance claim/consent/content approval.
- Template Hàn/Đức/Pháp/custom.

## 18. Kết luận định hướng

Chốt hướng sản phẩm:

> Xây công cụ MVP dùng chung cho nghiệp vụ tuyển sinh ngành ngôn ngữ, trong đó mọi thứ được cấu hình theo ngôn ngữ, chương trình, kỳ thi, cấp độ, pipeline và nội dung chăm sóc.

Chiến lược MVP:

- Không hard-code tiếng Anh.
- Không làm CRM chung chung.
- Không làm nền tảng học.
- Không làm LMS.
- Không làm phần mềm quản lý trung tâm đầy đủ.
- Tập trung vào: **Lead -> Tư vấn -> Lịch hẹn tuyển sinh -> Enrollment -> Doanh thu thật**.

Câu chốt định vị:

> Công cụ MVP tự động hóa tuyển sinh cho trung tâm ngoại ngữ: gom lead đa kênh, phân tư vấn viên, nhắc follow-up, quản lý lịch hẹn tuyển sinh, ghi nhận học phí và đo chính xác doanh thu theo từng chiến dịch, từng ngôn ngữ, từng khóa học.
