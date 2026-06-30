# Overview module MVP: Công cụ tự động hóa tuyển sinh đa ngôn ngữ

## 1. Mục tiêu MVP

MVP cần chứng minh một giá trị cốt lõi:

> Trung tâm ngoại ngữ biết chính xác mỗi đồng quảng cáo tạo ra bao nhiêu học viên đóng phí, lead rơi ở bước nào và tư vấn viên nào xử lý/chốt tốt.

MVP không cố làm đủ mọi chức năng quản lý trung tâm. Trọng tâm là:

```text
Lead -> Tư vấn -> Lịch hẹn tuyển sinh -> Enrollment -> Doanh thu thật
```

Ranh giới quan trọng:

> Đây không phải nền tảng để học, không phải LMS và không phải app học ngôn ngữ. Đây chỉ là công cụ MVP của chúng ta để kiểm chứng nghiệp vụ tuyển sinh, chăm sóc lead và đo doanh thu thật.

## 2. Người dùng chính

| Vai trò | Việc họ cần làm |
|---|---|
| Chủ trung tâm/CEO | Xem doanh thu thật theo campaign, ngôn ngữ, chương trình, tư vấn viên |
| Marketing | Biết nguồn nào tạo lead, lịch hẹn, enrollment, ROAS thật |
| Trưởng nhóm tuyển sinh | Theo dõi SLA, phân lead, hiệu quả tư vấn viên |
| Tư vấn viên | Nhận lead, gọi/nhắn, cập nhật trạng thái, đặt lịch hẹn tuyển sinh |
| CSKH/giáo vụ | Gửi nhắc lịch, xác nhận ghi danh, chăm sóc sau ghi danh |
| Kế toán/học vụ | Import/xác nhận payment/enrollment để đo doanh thu |

## 3. Phạm vi MVP

### Must-have

| Nhóm | Chức năng |
|---|---|
| Cấu hình | Tenant, user, role, branch, language, program, course |
| Lead | Lead capture, import, dedup, UTM tracking |
| Pipeline | Pipeline tuyển sinh cấu hình được theo ngôn ngữ/chương trình |
| Assignment | Phân lead theo ngôn ngữ, chương trình, cơ sở, tải công việc |
| SLA | Nhắc tư vấn viên xử lý lead, cảnh báo quá hạn |
| Activity | Call/note/message log thủ công |
| Appointment | Đặt lịch hẹn tuyển sinh, nhắc lịch, cập nhật Done/No-show |
| Enrollment | Import/ghi nhận enrollment, deposit, payment |
| Attribution | Gắn doanh thu về source/campaign/ad |
| Messaging | Zalo/SMS/email nhắc lịch cơ bản |
| Dashboard | Funnel, marketing, sales, revenue, language/program |
| Compliance | Consent status, audit log cơ bản |

### Should-have

- Template tiếng Anh, tiếng Nhật, tiếng Trung.
- Lead scoring cấu hình được.
- Lost reason analytics.
- Sales leaderboard.
- Google Ads cost import/API.
- Workflow nurture lead chưa đóng phí.
- Zalo OA user mapping.

### Ngoài phạm vi MVP

- Nền tảng/app học ngôn ngữ.
- LMS đầy đủ.
- Quản lý giáo viên/lớp học/học vụ toàn diện.
- Kế toán học phí đầy đủ.
- AI chatbot thay tư vấn viên.
- Ads automation nâng cao.
- Mobile app riêng.
- Multi-touch attribution phức tạp.

## 4. Workflow MVP

### Workflow 1: Lead mới

```text
Lead mới
-> Chuẩn hóa số điện thoại/email
-> Kiểm tra trùng
-> Gắn UTM/source/campaign
-> Xác định language/program
-> Chấm điểm lead
-> Phân tư vấn viên
-> Tạo task SLA
-> Gửi Zalo/SMS xác nhận cho lead
```

### Workflow 2: Tư vấn viên xử lý lead

```text
Lead assigned
-> Tư vấn viên gọi/nhắn
-> Cập nhật Contacted/Qualified/Lost
-> Chọn language/program/goal/current level
-> Đặt lịch hẹn tuyển sinh nếu phù hợp
-> Ghi chú nhu cầu và next action
```

### Workflow 3: Lịch hẹn tuyển sinh

```text
Appointment Booked
-> Gửi xác nhận
-> Nhắc trước 24h
-> Nhắc trước 2h
-> Sau lịch yêu cầu cập nhật Done/No-show
-> Nếu Done: chuyển sang Consulting
-> Nếu No-show: tạo workflow follow-up
```

### Workflow 4: Ghi danh và doanh thu

```text
Consulting
-> Deposit Paid hoặc Enrolled
-> Ghi nhận course/program/language/payment
-> Gắn revenue về lead/campaign/source
-> Cập nhật dashboard ROAS/CAC/cost per enrollment
-> Chuyển học viên sang chăm sóc sau ghi danh
```

### Workflow 5: Lead chưa đóng phí

```text
Appointment Done nhưng chưa paid
-> Sau 1 ngày: nhắc tư vấn viên follow-up
-> Sau 3 ngày: gửi nội dung lợi ích/lộ trình
-> Sau 7 ngày: gửi ưu đãi hoặc mời tư vấn lại
-> Sau 14 ngày: chuyển sang Nurturing
```

## 5. Data model MVP

| Entity | Trường/ý nghĩa chính |
|---|---|
| Tenant | Trung tâm/đơn vị sử dụng hệ thống |
| Branch | Cơ sở/chi nhánh |
| User | Nhân viên |
| Role | Quyền |
| Language | Anh, Nhật, Trung, Hàn, Đức, Pháp, custom |
| Program | IELTS, TOEIC, JLPT, HSK, TOPIK, giao tiếp, du học |
| Program Type | Exam prep, communication, kids, business, study abroad, subscription |
| Course | Khóa học cụ thể, học phí, thời lượng |
| Level Framework | CEFR, JLPT, HSK, TOPIK, custom |
| Lead | Thông tin liên hệ, nhu cầu, source, stage, owner |
| Customer/Learner | Học viên/phụ huynh |
| Activity | Gọi, nhắn, ghi chú, lịch hẹn |
| Task | Việc cần xử lý, deadline, SLA |
| Admission Appointment | Lịch hẹn tuyển sinh, loại lịch hẹn, trạng thái, kết quả |
| Enrollment | Ghi danh vào khóa/chương trình |
| Payment | Đặt cọc, thanh toán, hoàn phí, công nợ cơ bản |
| Campaign | Source, medium, campaign, adset, ad |
| Touchpoint | UTM, gclid, fbclid, ttclid |
| Message | Zalo/SMS/email đã gửi |
| Consent | Đồng ý nhận tư vấn/marketing |
| Audit Log | Lịch sử thao tác |

## 6. Trường lead tối thiểu

| Nhóm | Trường |
|---|---|
| Thông tin | Họ tên, số điện thoại, email, tuổi, khu vực |
| Vai trò | Học viên tự đăng ký, phụ huynh, doanh nghiệp |
| Nhu cầu | Ngôn ngữ, chương trình, mục tiêu, level hiện tại, target level |
| Thời gian | Muốn học ngay, 1 tháng nữa, 3 tháng nữa |
| Ngân sách | Khoảng học phí phù hợp |
| Tracking | Source, medium, campaign, adset, ad, UTM, gclid, fbclid, ttclid |
| Sales | Owner, stage, next action, SLA due |
| Kết quả | Appointment done/no-show, deposit, enrolled, lost reason |
| Consent | Đồng ý nhận tư vấn/marketing |

## 7. Tích hợp MVP

| Tích hợp | Ưu tiên | Mục đích |
|---|---|---|
| Excel/CSV import | P0 | Nhập dữ liệu cũ |
| Google Sheet import/sync | P0 | Phù hợp vận hành thực tế |
| Website/landing page webhook | P0 | Nhận lead realtime |
| Meta Lead Ads | P0 | Nguồn lead phổ biến |
| UTM tracking | P0 | Attribution cơ bản |
| Google Ads cost import | P0 | Tính CPL/ROAS |
| Zalo OA/ZNS cơ bản | P0 | Xác nhận, nhắc lịch, chăm sóc |
| SMS gateway | P1 | Dự phòng khi Zalo không hiệu quả |
| Email gateway | P1 | Gửi tài liệu/lộ trình/nurture |
| Enrollment/payment import | P0 | Đo doanh thu thật |
| Manual activity/call log | P0 | Tư vấn viên cập nhật nhanh |
| TikTok Lead/API | P2 | Sau MVP nếu pilot dùng nhiều |
| Tổng đài | P2 | Sau MVP khi cần ghi âm/call tracking |
| LMS/phần mềm học viên | P2 | Sau MVP, tích hợp tùy khách |

## 8. Dashboard MVP

### Tổng quan tuyển sinh

- Lead mới.
- Qualified lead.
- Appointment booked/done/no-show.
- Enrolled.
- Doanh thu.
- CPL.
- Cost per appointment.
- Cost per enrollment.
- ROAS thật.

### Hiệu quả marketing

- Lead by source/campaign.
- Enrollment by source/campaign.
- Revenue by campaign.
- Low CPL, low enrollment.
- High CPL, high ROAS.
- ROAS by language/program.

### Hiệu quả tư vấn viên

- Lead assigned.
- First response time.
- Contact rate.
- Appointment booking rate.
- Enrollment rate.
- Revenue closed.
- Follow-up overdue.
- Lost reason.

### Funnel theo ngôn ngữ/chương trình

- Tiếng Anh/IELTS: Lead -> Appointment -> Consulting -> Enrolled.
- Tiếng Nhật/JLPT: Lead -> Appointment -> Consulting -> Deposit -> Enrolled.
- Tiếng Trung/HSK: Lead -> Appointment -> Consulting -> Enrolled.
- Online subscription: Signup -> Trial -> Activated -> Paid -> Renewed.

## 9. KPI thành công của MVP

| KPI | Mục tiêu kiểm chứng |
|---|---|
| Time to first value | Trung tâm thấy dashboard có giá trị trong 7-14 ngày sau onboarding |
| Data match rate | Tỷ lệ lead ghép được với enrollment/payment đủ cao để tin dashboard |
| Speed to lead | Thời gian phản hồi lead giảm |
| Contact rate | Tỷ lệ liên hệ được tăng |
| Appointment booking rate | Đo và cải thiện được |
| Enrollment rate | Đo theo source/campaign/tư vấn viên |
| Cost per enrollment | Đo được thay vì chỉ đo CPL |
| Revenue by campaign | Gắn được doanh thu thật về campaign |
| ROAS thật | Tính được theo học phí thật |
| Workflow success rate | Nhắc lịch/follow-up chạy đúng |

## 10. Tiêu chí hoàn thành MVP

MVP được xem là hoàn thành khi:

- Có thể cấu hình ít nhất 3 ngôn ngữ/chương trình khác nhau mà không sửa code lõi.
- Nhận/import lead từ ít nhất 3 nguồn: file/Sheet, form/webhook, Meta Lead Ads.
- Gộp trùng lead theo SĐT/email/Zalo ID.
- Phân lead cho tư vấn viên và có SLA nhắc xử lý.
- Quản lý được pipeline tuyển sinh có appointment/enrollment.
- Ghi nhận hoặc import được payment/enrollment.
- Dashboard trả lời được: campaign nào tạo học viên đóng phí và doanh thu thật.
- Có Zalo/SMS/email nhắc lịch cơ bản.
- Có consent và audit log cơ bản.
- Chạy pilot thực tế với 3-5 đơn vị hoặc ít nhất 3 mô hình chương trình khác nhau.

## 11. Rủi ro MVP

| Rủi ro | Cách giảm |
|---|---|
| Mỗi trung tâm có pipeline khác nhau | Pipeline/stage phải cấu hình được |
| Dữ liệu cũ bẩn | Có import mapping, dedup, chuẩn hóa SĐT/email |
| Tư vấn viên ngại cập nhật | UI cực nhanh, ít trường bắt buộc, có task rõ |
| Không ghép được lead với học phí | Chuẩn hóa SĐT/email/payment import ngay từ đầu |
| Zalo/ZNS giới hạn template | Có SMS/email fallback |
| Dashboard bị nghi ngờ | Hiển thị matched/unknown revenue minh bạch |
| MVP bị kéo sang nền tảng học/LMS/quản lý lớp | Giữ ranh giới: tuyển sinh và doanh thu trước |

## 12. Kết luận MVP

Module MVP nên được đóng gói là:

> Lead-to-Enrollment MVP cho trung tâm/đơn vị đào tạo ngôn ngữ đa ngôn ngữ.

Giá trị chính:

- Không bỏ sót lead.
- Không chậm follow-up.
- Không đo quảng cáo bằng CPL đơn thuần.
- Biết chiến dịch nào tạo học viên đóng phí.
- Biết ngôn ngữ/chương trình nào có hiệu quả tuyển sinh tốt.
- Tạo nền dữ liệu để sau này mở rộng sang Zalo nurture, retention, compliance và ads automation.
