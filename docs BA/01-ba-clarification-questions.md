# Bộ câu hỏi và Decision Log BA - MAR

> Phiên bản cập nhật: `v2.1 - Architecture version/convention lock - 2026-06-29`.
> Baseline kỹ thuật hiện hành: `Java + Spring Boot ecosystem`, `PostgreSQL`, `Flyway`, `Spring Data JPA/Hibernate`.
> Ghi chú: file này đã được đồng bộ theo quyết định chọn Spring Boot và MAR-ARCH-1.0; development commitment vẫn phụ thuộc sign-off `SP1-D01` đến `SP1-D10`.
## 1. Mục tiêu

Tài liệu này gom các điểm cần User/PM chốt trước khi BA viết Epic Brief chính thức. Sau review, tài liệu được chuyển từ danh sách câu hỏi thuần túy sang dạng Decision Log để PM/User có thể duyệt nhanh.

## 2. Decision Log đề xuất

Trạng thái hiện tại: `Approved for Epic Brief, not yet approved for development`. Các decision dưới đây là baseline đã chốt để BA viết `brief.md`.

| Decision ID | Vấn đề | Final decision | Trạng thái | Ghi chú chốt |
|---|---|---|---|---|
| DEC-01 | Mô hình pilot | Pilot ưu tiên trung tâm ngoại ngữ offline/hybrid trước | Approved | Không chọn online subscription làm pilot đầu vì kéo thêm signup, activation, churn, subscription billing |
| DEC-02 | Ngôn ngữ pilot | Anh, Nhật, Trung + custom language | Approved | Không hard-code IELTS/JLPT/HSK vào core |
| DEC-03 | Nguồn lead P0 | CSV/Google Sheet, website form webhook, Meta Lead Ads | Approved | Zalo/inbox có thể ghi nhận thủ công hoặc bán tự động ở P0 |
| DEC-04 | Lead realtime | Form webhook và Meta realtime; CSV/Sheet batch | Approved | Ads/form cần realtime để đảm bảo SLA; dữ liệu cũ dùng batch import |
| DEC-05 | Message P0 | Zalo ưu tiên + SMS fallback, email optional | Approved with scope control | Chỉ làm template, reminder, message log; không làm journey builder phức tạp |
| DEC-06 | Payment P0 | Manual entry + CSV/Sheet import | Approved | Chưa cần payment gateway realtime |
| DEC-07 | Enrollment success | Tách Deposit Paid và Enrolled | Approved with clarification | Deposit Paid là conversion trung gian; Enrolled khi trung tâm xác nhận ghi danh và có deposit/payment hợp lệ theo pilot |
| DEC-08 | Revenue metric | Revenue tách Gross, Collected, Net | Approved | Dashboard P0 ưu tiên Revenue Collected; Gross/Net hiển thị nếu có dữ liệu |
| DEC-09 | Pipeline | Dedup/Merge không nằm trong pipeline business | Approved | Dedup/Merge là data processing status |
| DEC-10 | Opportunity model | Thêm Admission Opportunity giữa Customer và Enrollment | Approved | Cần để xử lý khách quan tâm nhiều ngôn ngữ/chương trình |
| DEC-11 | Duplicate handling | Exact phone auto-link; email/near match vào possible duplicate; có merge/unmerge | Approved | Không auto-merge case không chắc chắn |
| DEC-12 | Ad cost P0 | Import ad cost theo date/source/campaign/adset/ad | Approved | Bắt buộc để tính CPL, cost per appointment, cost per enrollment, ROAS |
| DEC-13 | Assignment P0 | Rule-based + fallback round-robin | Approved | Chưa cần AI routing |
| DEC-14 | SLA P0 | Lead nóng 5-15 phút, thường 30-60 phút, ngoài giờ auto message + task hôm sau | Approved | Có thể cấu hình theo tenant sau |
| DEC-15 | Consent P0 | Tách consultation consent và marketing consent, có opt-out theo channel | Approved | Bắt buộc cho Zalo/SMS/email/call |
| DEC-16 | Permission P0 | Dùng role matrix cơ bản | Approved | Chưa làm field-level permission sâu |
| DEC-17 | Dashboard P0 | CEO overview, campaign dashboard, sales SLA dashboard, funnel by language/program | Approved | 4 dashboard bắt buộc |
| DEC-18 | Lead scoring | Rule-based scoring là P1/Should-have; ML defer | Approved | MVP chưa cần scoring để launch, nhưng data model nên sẵn sàng |
| DEC-19 | Attribution model P0 | Lưu first-touch và last-touch; dashboard mặc định dùng last-touch | Changed / Approved | First-touch dùng để tham khảo |
| DEC-20 | Payment matching khi nhiều opportunity | Đưa vào review queue nếu customer có nhiều active opportunity | Approved | Không auto-match bừa |
| DEC-21 | Message trigger P0 | Auto appointment reminder; follow-up chủ yếu task/template; không journey builder | Approved | Giữ messaging đúng phạm vi MVP |
| DEC-22 | Data retention/audit retention | Audit log và message log tối thiểu 24 tháng | Changed / Approved | Có thể cấu hình dài hơn theo tenant |
| DEC-23 | Pilot success criteria | Data quality, SLA, appointment, attribution, adoption | Approved | Dùng làm điều kiện nghiệm thu pilot |

## 3. Message trigger P0 đề xuất

| Trigger | P0 nên làm | Ghi chú BA |
|---|---|---|
| Lead mới ngoài giờ | Auto message xác nhận + tạo task hôm sau | Phải check consent/channel |
| Appointment booked | Auto reminder | Kênh ưu tiên Zalo, SMS fallback |
| Appointment no-show | Tạo follow-up task, message tùy cấu hình | Không spam nurturing tự động |
| Sau appointment done chưa đóng phí | Tạo task follow-up, có template gửi thủ công hoặc bán tự động | Không làm journey builder phức tạp |
| Enrolled | Tin xác nhận nếu consent hợp lệ | Có message log |

## 4. Câu hỏi còn cần User/PM xác nhận

### 4.1. Pilot và vận hành thực tế

| # | Câu hỏi | Default BA đề xuất |
|---|---|---|
| 1 | Pilot đầu tiên nhắm vào trung tâm offline, online/hybrid, hay cả hai? | Offline/hybrid trước |
| 2 | Quy mô pilot mong muốn: bao nhiêu trung tâm, bao nhiêu tư vấn viên mỗi trung tâm? | 3-5 trung tâm, mỗi trung tâm 2-20 tư vấn viên |
| 3 | Pilot bắt buộc hỗ trợ ngôn ngữ nào? | Anh, Nhật, Trung |
| 4 | Trung tâm pilot hiện quản lý lead bằng gì? | Ưu tiên hỗ trợ CSV/Google Sheet trước |

### 4.2. Lead, import và duplicate

| # | Câu hỏi | Default BA đề xuất |
|---|---|---|
| 5 | Lead tối thiểu cần field nào? | Phone hoặc email hoặc Zalo ID; khuyến nghị có language/program/source |
| 6 | Lead thiếu phone nhưng có Zalo ID/email thì có tạo không? | Có, nhưng contactability thấp hơn và cần duplicate rule riêng |
| 7 | Trùng chắc chắn thì auto-merge hay cảnh báo? | Phone exact normalized: auto-link; case còn lại: possible duplicate |
| 8 | Trùng gần giống thì ai duyệt merge? | Admin hoặc Sales Lead |
| 9 | Có cho unmerge không? | Có, tối thiểu cho Admin |
| 10 | Nếu lead trùng customer nhưng quan tâm chương trình khác thì xử lý thế nào? | Merge customer, tạo Admission Opportunity mới |
| 11 | Import CSV/Sheet có cần mapping cột, preview lỗi, duplicate preview không? | Có, bắt buộc cho P0 |
| 12 | Import history cần lưu gì? | Created/updated/skipped/error, người import, thời điểm, file/source |

### 4.3. Pipeline và appointment

| # | Câu hỏi | Default BA đề xuất |
|---|---|---|
| 13 | Pipeline mặc định có bắt buộc Appointment Booked/Done/No-show không? | Có, vì MVP đo lead-to-enrollment |
| 14 | Appointment trong MVP gồm loại nào? | Consultation, Placement Test, Trial/Demo, Interview, Document Check, Payment Appointment |
| 15 | Pipeline có cần khác nhau theo language/program không? | Có cấu hình bật/tắt stage theo program |
| 16 | Lost reason cần chuẩn hóa không? | Có, bắt buộc khi chuyển Lost |
| 17 | Có cần allowed transition giữa các stage không? | Có, để tránh nhảy stage sai |
| 18 | Có cần stage history không? | Có, để đo funnel time và bottleneck |

### 4.4. Assignment và SLA

| # | Câu hỏi | Default BA đề xuất |
|---|---|---|
| 19 | Rule phân lead P0 ưu tiên theo gì? | Ngôn ngữ, chương trình, branch, ca trực, workload, fallback round-robin |
| 20 | SLA mặc định cho lead nóng/thường/ngoài giờ là bao lâu? | Nóng 5-15 phút, thường 30-60 phút, ngoài giờ auto message + task hôm sau |
| 21 | Quá hạn SLA thì làm gì? | Nhắc advisor, sau đó báo Sales Lead; chưa tự động đổi owner ở P0 |

### 4.5. Payment, revenue và ad cost

| # | Câu hỏi | Default BA đề xuất |
|---|---|---|
| 22 | Source of truth cho payment là gì? | Manual entry + CSV/Sheet import trong MVP |
| 23 | Enrollment thành công khi đặt cọc hay đóng đủ học phí? | Tách Deposit Paid và Enrolled; quy tắc Enrolled chốt theo pilot |
| 24 | Revenue tính Gross, Collected hay Net? | Dashboard hiển thị Collected là chính; Gross/Net nếu có đủ dữ liệu |
| 25 | Ad cost P0 lấy bằng gì? | Import CSV/Sheet theo date/source/campaign/adset/ad |
| 26 | Cost phân bổ ở cấp nào? | Tối thiểu campaign/date; nếu có thì adset/ad |
| 27 | Có nhập chi phí sales/hoa hồng để tính CAC không? | Không trong P0; P0 chỉ tính marketing CAC/ROAS cơ bản |
| 28 | Múi giờ/ngày ghi nhận cost/revenue theo gì? | Theo timezone tenant, mặc định Asia/Ho_Chi_Minh |

### 4.6. Dashboard và KPI

| # | Câu hỏi | Default BA đề xuất |
|---|---|---|
| 29 | Dashboard P0 quan trọng nhất cho ai? | CEO, Marketing, Sales Lead |
| 30 | KPI bắt buộc launch là gì? | Lead count, SLA hit rate, appointment, enrollment, revenue collected, CPL, cost per appointment, cost per enrollment, ROAS basic, unknown revenue |
| 31 | Có cần dashboard theo tư vấn viên ngay MVP không? | Có ở mức Sales SLA/performance cơ bản |

### 4.7. Compliance, message và permission

| # | Câu hỏi | Default BA đề xuất |
|---|---|---|
| 32 | Consent cần tách tư vấn và marketing không? | Có |
| 33 | Có cần opt-out theo từng kênh không? | Có: Zalo/SMS/email/call |
| 34 | Nội dung tin nhắn do hệ thống tạo template hay trung tâm tự tạo? | Hệ thống cung cấp template mặc định, Admin có thể chỉnh |
| 35 | Có cần chặn gửi message khi opt-out không? | Có, bắt buộc |
| 36 | Role nào được export data? | CEO/Admin, Marketing/Finance giới hạn theo quyền |
| 37 | Những thao tác nào cần audit log? | Export, merge/unmerge, change owner, change payment, change consent, delete/update critical data |

## 4. Định nghĩa mặc định đề xuất cho Payment/Revenue

| Trạng thái/Metric | Cách hiểu đề xuất |
|---|---|
| Deposit Paid | Có đặt cọc hợp lệ; là conversion trung gian |
| Enrolled | Có ghi danh hợp lệ và/hoặc payment/deposit hợp lệ theo quy tắc pilot |
| Revenue Gross | Tổng học phí/giá trị khóa học trước giảm trừ |
| Revenue Collected | Tiền thực thu |
| Revenue Net | Thực thu trừ refund/discount nếu có dữ liệu |
| ROAS MVP | Revenue attributed / Ad cost; dashboard phải ghi rõ dùng Gross, Collected hay Net |
| Unknown Revenue | Revenue không match được source/campaign đáng tin cậy |

## 5. Permission matrix P0 đề xuất

| Function | CEO | Admin | Marketing | Sales Lead | Advisor | CSKH | Finance |
|---|---|---|---|---|---|---|---|
| Xem dashboard revenue | Có | Có | Có giới hạn | Có giới hạn | Không/giới hạn | Không | Có |
| Import lead | Không | Có | Có | Có thể | Không | Không | Không |
| Xem lead toàn tenant | Có | Có | Có | Có | Chỉ lead được giao | Có giới hạn | Không |
| Reassign lead | Không | Có | Không | Có | Không | Không | Không |
| Ghi payment | Không | Có | Không | Không | Có giới hạn | Không | Có |
| Export data | Có | Có | Có giới hạn | Có giới hạn | Không | Không | Có giới hạn |
| Sửa consent | Không | Có | Không | Không | Có giới hạn | Có giới hạn | Không |
| Merge/unmerge | Không | Có | Không | Có | Không | Không | Không |

## 6. Kết luận BA

Các decision trên đủ để chuyển sang viết `brief.md` sau khi User/PM duyệt. Nếu không có phản hồi khác, BA đề xuất dùng toàn bộ giá trị ở cột Default/Khuyến nghị làm baseline cho Epic Brief chính thức.
