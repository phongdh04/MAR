# R1A API Contract Baseline - Lead & Pipeline Core

> Phiên bản cập nhật: `v2.1 - Architecture version/convention lock - 2026-06-29`.
> Baseline kỹ thuật hiện hành: `Java + Spring Boot ecosystem`, `PostgreSQL`, `Flyway`, `Spring Data JPA/Hibernate`.
> Ghi chú: file này đã được đồng bộ theo quyết định chọn Spring Boot và MAR-ARCH-1.0; development commitment vẫn phụ thuộc sign-off `SP1-D01` đến `SP1-D10`.
## 1. Trạng thái tài liệu

| Thuộc tính | Giá trị |
|---|---|
| Tên tài liệu | R1A API Contract Baseline |
| Vai trò tài liệu | API contract baseline cho PO/Tech Lead grooming |
| Nguồn baseline | `04-r1a-technical-ba-spec.md` |
| Trạng thái | Draft for grooming, not yet approved for development |
| Phạm vi | Tenant/config, lead import, webhook, dedup, opportunity, pipeline, assignment, SLA |
| Ngày lập | 2026-06-29 |

Tài liệu này chưa phải OpenAPI final. Mục tiêu là chốt behavioral contract: endpoint nào cần có, request/response tối thiểu, validation, permission, event và lỗi chuẩn.

## 2. API Design Principles

| Nguyên tắc | Chốt R1A |
|---|---|
| Tenant isolation | Mọi request nghiệp vụ phải chạy trong tenant context |
| API authorization | Permission phải enforce ở API, không chỉ ở UI |
| Idempotency | Webhook và confirm import cần chống tạo trùng |
| Auditability | Import confirm, permission change, merge/unmerge, reassign và stage change quan trọng phải audit |
| Preview before commit | Import lead phải preview trước, confirm sau |
| Validation transparency | Lỗi cần trả field, code, message và row number nếu là import |
| Event-driven ready | Các action lõi cần phát event để R1B/R1C nối tiếp |

## 3. Common API Conventions

### 3.1. Base path và versioning

```text
/api/v1
```

Version có thể đổi theo chuẩn kỹ thuật sau này. BA khuyến nghị version ngay từ đầu vì R1B/R1C sẽ mở rộng payment, appointment và attribution.

### 3.2. Tenant context

Mỗi request dùng một trong các cách sau, Tech Lead chốt final:

| Cách | Dùng cho |
|---|---|
| JWT claim `tenant_id` | User API sau login |
| Header `X-Tenant-Id` + auth token | Admin/internal API nếu cần |
| Integration key/signature | Website/Meta webhook |

Quy tắc:

- User không được truy cập tenant khác.
- Webhook key chỉ được ghi dữ liệu vào tenant đã cấu hình.
- Cross-tenant merge, assignment hoặc stage update phải bị chặn.

### 3.3. Standard response envelope

Success:

```json
{
  "data": {},
  "meta": {
    "request_id": "req_123",
    "timestamp": "2026-06-29T08:00:00Z"
  }
}
```

List:

```json
{
  "data": [],
  "pagination": {
    "page": 1,
    "page_size": 50,
    "total": 125
  },
  "meta": {
    "request_id": "req_123"
  }
}
```

Error:

```json
{
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "Dữ liệu không hợp lệ.",
    "details": [
      {
        "field": "phone",
        "reason": "Lead phải có ít nhất phone, email hoặc zalo_id."
      }
    ]
  },
  "meta": {
    "request_id": "req_123"
  }
}
```

### 3.4. Standard HTTP status

| Status | Khi dùng |
|---|---|
| 200 | GET/PATCH/POST xử lý đồng bộ thành công |
| 201 | Tạo resource mới thành công |
| 202 | Webhook/import accepted để xử lý bất đồng bộ nếu có queue |
| 400 | Validation lỗi |
| 401 | Chưa xác thực |
| 403 | Không có quyền |
| 404 | Không tìm thấy resource trong tenant scope |
| 409 | Conflict, duplicate idempotency, transition không hợp lệ |
| 422 | Business rule không đạt |
| 429 | Rate limit webhook/API |
| 500 | Lỗi hệ thống |

### 3.5. Common audit fields

Các response entity nên có:

| Field | Ý nghĩa |
|---|---|
| created_at | Thời điểm tạo |
| updated_at | Thời điểm cập nhật |
| created_by | User tạo nếu có |
| updated_by | User cập nhật nếu có |

Webhook/system action có thể dùng actor `system`.

### 3.6. Enum key convention

API/DB phải dùng enum key ổn định dạng `UPPER_SNAKE_CASE`; UI label được xử lý riêng theo ngôn ngữ.

| Loại | API/DB key | UI label ví dụ |
|---|---|---|
| Role | `SALES_LEAD` | Sales Lead / Trưởng nhóm tuyển sinh |
| Stage | `APPOINTMENT_BOOKED` | Appointment Booked / Đã đặt lịch |
| Lost reason | `TUITION_TOO_HIGH` | Học phí cao |
| Source type | `META_LEAD_ADS` | Meta Lead Ads |
| Consent status | `GRANTED` | Đã đồng ý |

Consent status dùng enum, không dùng boolean:

```text
GRANTED
DENIED
UNKNOWN
REVOKED
```

## 4. Permission Codes Baseline

| Function | Permission code | Roles mặc định |
|---|---|---|
| Tenant setup | `tenant.manage` | Admin |
| Branch setup | `branch.manage` | Admin |
| User setup | `user.manage` | Admin |
| Permission setup | `permission.manage` | Admin |
| Language/program/course setup | `catalog.manage` | Admin |
| Import lead | `lead.import` | Admin, Marketing, Sales Lead nếu bật |
| View lead | `lead.view` | Admin, CEO, Marketing, Sales Lead, Advisor own |
| Duplicate review | `duplicate.manage` | Admin, Sales Lead |
| Merge/unmerge | `customer.merge` | Admin, Sales Lead nếu bật |
| Customer view/update | `customer.view`, `customer.update` | Admin, Sales Lead, Advisor own |
| Customer identity | `customer.identity.view` | Admin, Sales Lead, Advisor own |
| View inbox | `inbox.view` | Advisor, Sales Lead, Admin |
| Update opportunity | `opportunity.update` | Advisor own, Sales Lead team, Admin |
| Activity log | `activity.create`, `activity.view` | Advisor own, Sales Lead team, Admin |
| Reassign opportunity | `opportunity.reassign` | Admin, Sales Lead |
| Assignment rule | `assignment.manage` | Admin, Sales Lead |
| SLA task | `sla.view_update` | Advisor own, Sales Lead team, Admin |
| Working hours | `working_hours.manage` | Admin, Sales Lead nếu bật |
| Integration log | `integration_log.view` | Admin, Marketing |

## 5. Data DTO Baseline

### 5.1. TenantDTO

```json
{
  "tenant_id": "uuid",
  "tenant_name": "ABC Language Center",
  "timezone": "Asia/Ho_Chi_Minh",
  "default_currency": "VND",
  "status": "ACTIVE",
  "created_at": "2026-06-29T08:00:00Z",
  "updated_at": "2026-06-29T08:00:00Z"
}
```

### 5.2. LeadDTO

```json
{
  "lead_id": "uuid",
  "tenant_id": "uuid",
  "external_id": "meta_123",
  "full_name": "Nguyễn Minh A",
  "phone_raw": "0912 345 678",
  "phone_normalized": "+84912345678",
  "email": "a@example.com",
  "zalo_id": "zalo_123",
  "source_type": "META_LEAD_ADS",
  "source": "Meta",
  "source_created_at": "2026-06-29T08:00:00Z",
  "language_id": "uuid",
  "program_id": "uuid",
  "branch_id": "uuid",
  "campaign": "IELTS_June",
  "adset": "HN_Parents",
  "ad": "Creative_A",
  "utm_source": "meta",
  "utm_medium": "paid_social",
  "utm_campaign": "IELTS_June",
  "consent_consultation": "GRANTED",
  "consent_marketing": "UNKNOWN",
  "contactability": "HIGH",
  "lead_temperature": "HOT",
  "temperature_reason": "META_FORM_IMMEDIATE_NEED",
  "lead_status": "LINKED",
  "customer_id": "uuid",
  "opportunity_id": "uuid"
}
```

### 5.3. OpportunityDTO

```json
{
  "opportunity_id": "uuid",
  "customer_id": "uuid",
  "source_lead_id": "uuid",
  "language_id": "uuid",
  "program_id": "uuid",
  "course_id": "uuid",
  "branch_id": "uuid",
  "owner_id": "uuid",
  "current_stage": "NEW",
  "qualification_status": "UNKNOWN",
  "lost_reason": null,
  "lost_note": null,
  "first_touch_id": "uuid",
  "last_touch_id": "uuid",
  "created_at": "2026-06-29T08:00:00Z",
  "updated_at": "2026-06-29T08:00:00Z"
}
```

## 6. Tenant and Config APIs

### 6.1. Create tenant

```text
POST /api/v1/tenants
Permission: tenant.manage
```

Request:

```json
{
  "tenant_name": "ABC Language Center",
  "timezone": "Asia/Ho_Chi_Minh",
  "default_currency": "VND",
  "status": "ACTIVE"
}
```

Response `201`:

```json
{
  "data": {
    "tenant_id": "uuid",
    "tenant_name": "ABC Language Center",
    "timezone": "Asia/Ho_Chi_Minh",
    "default_currency": "VND",
    "status": "ACTIVE"
  }
}
```

Validation:

- `tenant_name` required.
- `timezone` default `Asia/Ho_Chi_Minh`.
- `default_currency` default `VND`.

Events:

- `tenant.created`.

### 6.2. Update tenant

```text
PATCH /api/v1/tenants/{tenant_id}
Permission: tenant.manage
```

Request:

```json
{
  "tenant_name": "ABC Language Center",
  "timezone": "Asia/Ho_Chi_Minh",
  "default_currency": "VND",
  "status": "INACTIVE",
  "reason": "Pilot ended"
}
```

Rules:

- Inactive tenant không nhận lead active.
- Không hard delete tenant trong R1A.
- Status change cần AuditLog.

### 6.3. Create branch

```text
POST /api/v1/branches
Permission: branch.manage
```

Request:

```json
{
  "branch_name": "Hà Nội - Cầu Giấy",
  "city": "Hà Nội",
  "address": "Cầu Giấy",
  "status": "ACTIVE"
}
```

Validation:

- `branch_name` required.
- Active branch name unique trong tenant.

### 6.4. Create user

```text
POST /api/v1/users
Permission: user.manage
```

Request:

```json
{
  "full_name": "Trần Thị B",
  "email": "advisor@example.com",
  "phone": "0987654321",
  "role": "ADVISOR",
  "branch_ids": ["uuid"],
  "status": "ACTIVE"
}
```

Validation:

- `full_name` required.
- `role` required.
- Email unique trong tenant nếu có.
- User inactive không nhận assignment mới.

### 6.5. Update permission matrix

```text
PATCH /api/v1/permissions/matrix
Permission: permission.manage
```

Request:

```json
{
  "changes": [
    {
      "role": "SALES_LEAD",
      "function_code": "lead.import",
      "access_level": "CREATE",
      "scope": "TEAM"
    }
  ],
  "reason": "Cho Sales Lead import lead cho team pilot"
}
```

Validation:

- Advisor không được bật export data.
- Marketing không được ghi payment.
- Permission change phải có AuditLog.

Events:

- `permission.updated`.

### 6.6. Catalog APIs

```text
POST /api/v1/languages
POST /api/v1/programs
POST /api/v1/courses
Permission: catalog.manage
```

Create language request:

```json
{
  "name": "Japanese",
  "code": "JA",
  "status": "ACTIVE"
}
```

Create program request:

```json
{
  "language_id": "uuid",
  "program_name": "JLPT N5",
  "exam_track": "JLPT",
  "status": "ACTIVE"
}
```

Create course request:

```json
{
  "program_id": "uuid",
  "course_name": "JLPT N5 Foundation",
  "level": "N5",
  "tuition_gross": 4500000,
  "currency": "VND",
  "status": "ACTIVE"
}
```

Rules:

- Program phải thuộc language active.
- Course phải thuộc program active.
- Tuition không âm.
- Không hard-code IELTS/JLPT/HSK vào business logic.

### 6.7. Required list/detail/update APIs for UI

Các endpoint dưới đây cần có trước khi FE admin setup/catalog hoạt động đầy đủ:

| Method | Endpoint | Permission | Mục đích |
|---|---|---|---|
| GET | `/api/v1/tenants/{tenant_id}` | tenant.manage hoặc tenant.view | Load tenant detail |
| GET | `/api/v1/branches` | branch.manage/lead.view theo scope | List branch |
| PATCH | `/api/v1/branches/{branch_id}` | branch.manage | Update branch/status |
| GET | `/api/v1/users` | user.manage theo scope | List users |
| GET | `/api/v1/users/{user_id}` | user.manage theo scope | User detail |
| GET | `/api/v1/languages` | catalog.manage hoặc lead.view | List language |
| PATCH | `/api/v1/languages/{language_id}` | catalog.manage | Update language/status |
| GET | `/api/v1/programs` | catalog.manage hoặc lead.view | List program |
| PATCH | `/api/v1/programs/{program_id}` | catalog.manage | Update program/status |
| GET | `/api/v1/courses` | catalog.manage hoặc lead.view | List course |
| PATCH | `/api/v1/courses/{course_id}` | catalog.manage | Update course/status |

Inactive config entity vẫn query được nhưng không được chọn cho dữ liệu mới nếu business rule không cho phép.

## 7. Lead Import APIs

### 7.1. Preview lead import

```text
POST /api/v1/imports/leads/preview
Permission: lead.import
Content-Type: multipart/form-data hoặc application/json cho Google Sheet
```

Request for file upload must be `multipart/form-data`, not pure JSON:

| Field | Type | Required | Notes |
|---|---|---|---|
| source_file | File | Yes for CSV upload | CSV/XLSX nếu Tech Lead cho phép |
| mapping_config | JSON string | Yes | Source column -> system field |
| options | JSON string | Optional | first_row_is_header, default_source, default_language_id |

Example logical payload:

```json
{
  "source_type": "CSV",
  "source_file": "multipart-file",
  "mapping_config": {
    "Full Name": "full_name",
    "Phone": "phone_raw",
    "Email": "email",
    "Language": "language_name",
    "Program": "program_name",
    "Source": "source",
    "Campaign": "campaign"
  },
  "options": {
    "first_row_is_header": true,
    "default_source": "Offline Import",
    "default_language_id": "uuid"
  }
}
```

Response `200`:

```json
{
  "data": {
    "batch_id": "uuid",
    "status": "PREVIEWED",
    "total_rows": 100,
    "valid_count": 92,
    "error_count": 5,
    "duplicate_count": 3,
    "errors": [
      {
        "row_number": 12,
        "field": "phone/email/zalo_id",
        "code": "CONTACT_IDENTIFIER_REQUIRED",
        "message": "Lead phải có ít nhất phone, email hoặc zalo_id."
      }
    ],
    "duplicate_candidates": [
      {
        "row_number": 31,
        "match_type": "EMAIL_EXACT_PHONE_DIFFERENT",
        "lead_preview": {
          "full_name": "Nguyễn Minh A",
          "email": "a@example.com",
          "phone_raw": "0900000000"
        },
        "candidate_customer": {
          "customer_id": "uuid",
          "full_name": "Nguyễn Minh A",
          "primary_email": "a@example.com",
          "primary_phone": "+84912345678"
        },
        "recommended_action": "NEEDS_REVIEW"
      }
    ]
  }
}
```

Validation:

- Mapping phải có ít nhất một contact field hoặc default contact source hợp lệ.
- Preview không tạo Lead chính thức.
- Error row cần giữ row number và reason.

Events:

- `lead.import.previewed`.

### 7.2. Confirm lead import

```text
POST /api/v1/imports/leads/{batch_id}/confirm
Permission: lead.import
Idempotency-Key: required
```

Request:

```json
{
  "confirm_errors_skipped": true,
  "confirm_duplicate_handling": "create_duplicate_cases",
  "reason": "Import lead tháng 06"
}
```

Response `200`:

```json
{
  "data": {
    "batch_id": "uuid",
    "status": "COMPLETED",
    "total_rows": 100,
    "created_count": 75,
    "updated_count": 12,
    "skipped_count": 5,
    "error_count": 5,
    "duplicate_count": 3,
    "sample_created_lead_ids": ["uuid"],
    "sample_created_opportunity_ids": ["uuid"],
    "sample_duplicate_case_ids": ["uuid"],
    "links": {
      "created_records": "/api/v1/imports/leads/uuid/created-records",
      "duplicate_cases": "/api/v1/duplicates?import_batch_id=uuid"
    }
  }
}
```

Không trả toàn bộ list ID nếu batch lớn. Response chỉ trả count, link phân trang và sample nhỏ nếu cần.

Business rules:

- Chỉ batch `PREVIEWED` mới được confirm.
- Confirm cùng Idempotency-Key không được tạo trùng.
- Valid rows tạo hoặc link Lead, CustomerProfile, AdmissionOpportunity.
- Duplicate uncertain tạo DuplicateCase.
- Confirm import phải ghi AuditLog.

Events:

- `lead.import.confirmed`.
- `lead.created`.
- `customer.created` hoặc `customer.linked`.
- `duplicate.detected` nếu có.
- `opportunity.created`.
- `owner.assigned` nếu assignment thành công.
- `sla.task_created` nếu có owner.

### 7.3. Import history

```text
GET /api/v1/imports/leads?page=1&page_size=50&status=COMPLETED&from=2026-06-01&to=2026-06-30
Permission: lead.import hoặc lead.view theo scope
```

Response includes:

- `batch_id`.
- `source_file_name`.
- `status`.
- `total_rows`.
- `created_count`.
- `updated_count`.
- `skipped_count`.
- `error_count`.
- `duplicate_count`.
- `imported_by`.
- `imported_at`.
- `completed_at`.

### 7.4. Import errors

```text
GET /api/v1/imports/leads/{batch_id}/errors
Permission: lead.import
```

Response:

```json
{
  "data": [
    {
      "row_number": 12,
      "field": "phone/email/zalo_id",
      "code": "CONTACT_IDENTIFIER_REQUIRED",
      "raw_row": {},
      "message": "Lead phải có ít nhất phone, email hoặc zalo_id."
    }
  ]
}
```

PII rule:

- `raw_row` phải được mask theo quyền người dùng.
- Export/download error report phải ghi AuditLog.
- Raw row/raw payload nên có retention ngắn hơn audit log, khuyến nghị 30-90 ngày nếu không có yêu cầu khác.

Created records pagination:

```text
GET /api/v1/imports/leads/{batch_id}/created-records?page=1&page_size=50
Permission: lead.import hoặc lead.view theo scope
```

## 8. Webhook APIs

### 8.1. Website form webhook

```text
POST /api/v1/webhooks/leads/website
Auth: tenant integration key/signature
Idempotency: external_id or payload_hash
```

Request:

```json
{
  "external_id": "webform_20260629_001",
  "submitted_at": "2026-06-29T09:00:00+07:00",
  "full_name": "Nguyễn Minh A",
  "phone": "0912345678",
  "email": "a@example.com",
  "zalo_id": null,
  "language": "English",
  "program": "IELTS",
  "branch": "Hà Nội - Cầu Giấy",
  "source": "Website Form",
  "campaign": "IELTS_June",
  "utm": {
    "source": "google",
    "medium": "cpc",
    "campaign": "IELTS_June"
  },
  "consent": {
    "consultation": "GRANTED",
    "marketing": "DENIED",
    "source": "website_form"
  },
  "raw_payload": {}
}
```

R1A pilot recommendation: process synchronously if volume is small, return `200` when lead/customer/opportunity are created or linked.

Response `200` for sync processing:

```json
{
  "data": {
    "status": "PROCESSED",
    "lead_id": "uuid",
    "customer_id": "uuid",
    "opportunity_id": "uuid",
    "owner_id": "uuid",
    "sla_task_id": "uuid",
    "duplicate_case_id": null
  }
}
```

If Tech Lead chooses async processing, response `202` must not promise resource IDs that may not exist yet:

```json
{
  "data": {
    "status": "ACCEPTED",
    "correlation_id": "uuid",
    "webhook_event_id": "uuid"
  }
}
```

Async status lookup, if async is selected:

```text
GET /api/v1/integrations/webhook-events/{event_id}
Permission: integration_log.view
```

Validation:

- Có ít nhất phone/email/zalo_id.
- Tenant key hợp lệ.
- Idempotency không tạo trùng.

Error example:

```json
{
  "error": {
    "code": "CONTACT_IDENTIFIER_REQUIRED",
    "message": "Webhook payload thiếu phone, email và zalo_id."
  }
}
```

### 8.2. Meta Lead Ads webhook

```text
POST /api/v1/webhooks/leads/meta
Auth: Meta app verification + tenant mapping
Idempotency: meta_lead_id
```

Request normalized by adapter:

```json
{
  "meta_lead_id": "123456789",
  "form_id": "form_001",
  "page_id": "page_001",
  "created_time": "2026-06-29T09:00:00+07:00",
  "full_name": "Nguyễn Minh A",
  "phone": "0912345678",
  "email": "a@example.com",
  "campaign": "IELTS_June",
  "adset": "HN_Parents",
  "ad": "Creative_A",
  "field_data": {},
  "consent": {
    "consultation": "GRANTED",
    "marketing": "UNKNOWN"
  }
}
```

Rules:

- API/DB key: `source_type = META_LEAD_ADS`.
- `source = Meta`.
- Campaign/adset/ad lưu nếu có.
- Meta lead có thể là hot lead theo tenant config.

### 8.3. Webhook/integration log APIs

```text
GET /api/v1/integrations/webhook-events
GET /api/v1/integrations/webhook-events/{event_id}
Permission: integration_log.view
```

Response item:

```json
{
  "event_id": "uuid",
  "source_type": "WEBSITE_FORM",
  "external_id": "webform_20260629_001",
  "payload_hash": "sha256...",
  "status": "PROCESSED",
  "error_code": null,
  "error_message": null,
  "received_at": "2026-06-29T09:00:00+07:00",
  "processed_at": "2026-06-29T09:00:02+07:00",
  "raw_payload_uri": "internal://masked-payload/uuid"
}
```

PII rule:

- Raw payload access requires permission and masking.
- Payload can be stored encrypted or as internal URI.
- Retention recommended 30-90 days for raw payload, while audit/message logs follow retention policy.

## 9. Customer, Activity and Duplicate APIs

### 9.1. Customer APIs

```text
GET /api/v1/customers?query=nguyen&page=1&page_size=50
GET /api/v1/customers/{customer_id}
PATCH /api/v1/customers/{customer_id}
GET /api/v1/customers/{customer_id}/identities
Permission: customer.view/customer.update/customer.identity.view theo scope
```

CustomerIdentity item:

```json
{
  "identity_id": "uuid",
  "identity_type": "PHONE",
  "raw_value": "0912 345 678",
  "normalized_value": "+84912345678",
  "is_primary": true,
  "verified_status": "UNKNOWN",
  "source": "IMPORT",
  "created_at": "2026-06-29T08:00:00Z"
}
```

Rules:

- CustomerProfile primary phone/email/zalo are shortcuts; full identity list is returned by identities endpoint.
- Raw/normalized identity values may be masked by permission.
- No blind merge based only on shared guardian/learner contact.

### 9.2. Opportunity activities

```text
POST /api/v1/opportunities/{id}/activities
GET /api/v1/opportunities/{id}/activities
Permission: activity.create/activity.view theo scope
```

Create activity request:

```json
{
  "activity_type": "CALL",
  "activity_result": "ATTEMPTED",
  "occurred_at": "2026-06-29T09:05:00+07:00",
  "note": "Gọi lần 1, khách chưa nghe máy",
  "source": "MANUAL"
}
```

Rules:

- First response SLA hit is based on the first valid outbound activity within SLA.
- Contact success requires `activity_result` such as `CONNECTED` or `REPLIED`.
- Stage `CONTACTING` alone does not mean contact success.

### 9.3. List duplicate cases

```text
GET /api/v1/duplicates?status=NEEDS_REVIEW&match_type=EMAIL_EXACT_PHONE_DIFFERENT&page=1&page_size=50
Permission: duplicate.manage
```

Response:

```json
{
  "data": [
    {
      "duplicate_case_id": "uuid",
      "lead_id": "uuid",
      "candidate_customer_id": "uuid",
      "match_type": "EMAIL_EXACT_PHONE_DIFFERENT",
      "confidence": "MEDIUM",
      "status": "NEEDS_REVIEW",
      "created_at": "2026-06-29T08:00:00Z"
    }
  ]
}
```

### 9.4. Resolve duplicate

```text
POST /api/v1/duplicates/{case_id}/resolve
Permission: duplicate.manage, customer.merge for merge
```

Request:

```json
{
  "action": "MERGE",
  "target_customer_id": "uuid",
  "reason": "Cùng email và phụ huynh xác nhận cùng người học"
}
```

Allowed actions:

| Action | Ý nghĩa |
|---|---|
| merge | Gộp source customer/lead vào target customer |
| link | Link lead vào customer nhưng không merge toàn bộ profile |
| ignore | Bỏ qua case |

Validation:

- Reason required.
- Không merge khác tenant.
- Không merge customer vào chính nó.
- Advisor không được resolve.

Events:

- `customer.merged` nếu merge.
- `customer.linked` nếu link.

### 9.5. Unmerge

```text
POST /api/v1/customers/{customer_id}/unmerge
Permission: customer.merge, Admin tối thiểu
```

Request:

```json
{
  "merge_id": "uuid",
  "reason": "Merge nhầm phụ huynh cùng email công ty"
}
```

Rules:

- Chỉ unmerge nếu `can_unmerge = true`.
- Unmerge phải AuditLog.
- Nếu đã có dữ liệu downstream không thể tách an toàn, API trả `422 UNMERGE_NOT_ALLOWED`.

## 10. Opportunity APIs

### 10.1. Advisor inbox

```text
GET /api/v1/advisor/inbox?stage=New&overdue=true&page=1&page_size=50
Permission: inbox.view
Scope: Advisor own, Sales Lead team, Admin tenant
```

Response item:

```json
{
  "opportunity_id": "uuid",
  "customer": {
    "customer_id": "uuid",
    "full_name": "Nguyễn Minh A",
    "primary_phone": "+84912345678",
    "primary_email": "a@example.com"
  },
  "lead": {
    "lead_id": "uuid",
    "source": "Meta",
    "campaign": "IELTS_June"
  },
  "current_stage": "New",
  "owner_id": "uuid",
  "sla": {
    "task_id": "uuid",
    "due_at": "2026-06-29T09:15:00+07:00",
    "status": "Open",
    "overdue_level": "None"
  },
  "next_actions": ["Contacting", "Lost", "Nurturing"]
}
```

### 10.2. List opportunities

```text
GET /api/v1/opportunities?owner_id=uuid&stage=Contacted&language_id=uuid&program_id=uuid
Permission: lead.view or opportunity.update
```

Rules:

- Advisor only own.
- Sales Lead team/branch.
- Admin tenant.
- Marketing view scope theo cấu hình, không update stage.

### 10.3. Update opportunity fields

```text
PATCH /api/v1/opportunities/{id}
Permission: opportunity.update
```

Request:

```json
{
  "language_id": "uuid",
  "program_id": "uuid",
  "course_id": "uuid",
  "branch_id": "uuid",
  "qualification_status": "Qualified",
  "note": "Muốn học IELTS trong 3 tháng tới"
}
```

Validation:

- Language/program/course phải active nếu đổi sang giá trị mới.
- Advisor chỉ cập nhật own opportunity.
- Field thay đổi quan trọng nên AuditLog theo policy.

### 10.4. Change stage

```text
POST /api/v1/opportunities/{id}/stage
Permission: opportunity.update
```

Request:

```json
{
  "to_stage": "Lost",
  "lost_reason": "Học phí cao",
  "lost_note": null,
  "reason": "Khách báo chưa phù hợp ngân sách"
}
```

Response:

```json
{
  "data": {
    "opportunity_id": "uuid",
    "from_stage": "Contacted",
    "to_stage": "Lost",
    "stage_history_id": "uuid",
    "changed_at": "2026-06-29T08:00:00Z"
  }
}
```

Validation:

- Transition phải nằm trong matrix.
- `Lost` required `lost_reason`.
- `lost_reason = Khác` required `lost_note`.
- `Enrolled` chỉnh ngược chỉ Admin/Finance ở R1B, R1A có thể block.

Events:

- `opportunity.stage_changed`.

### 10.5. Stage history

```text
GET /api/v1/opportunities/{id}/stage-history
Permission: lead.view/opportunity.update theo scope
```

Response:

```json
{
  "data": [
    {
      "stage_history_id": "uuid",
      "from_stage": "New",
      "to_stage": "Contacting",
      "changed_by": "uuid",
      "changed_at": "2026-06-29T08:00:00Z",
      "reason": null,
      "duration_in_previous_stage_seconds": 3600
    }
  ]
}
```

### 10.6. Reassign opportunity

```text
POST /api/v1/opportunities/{id}/reassign
Permission: opportunity.reassign
```

Request:

```json
{
  "new_owner_id": "uuid",
  "reason": "Advisor nghỉ phép"
}
```

Validation:

- New owner active.
- New owner thuộc scope hợp lệ.
- Reason required.

Events:

- `owner.assigned`.

Audit:

- Required.

## 11. Assignment and SLA APIs

### 11.1. Create assignment rule

```text
POST /api/v1/assignment-rules
Permission: assignment.manage
```

List/update/inactive:

```text
GET /api/v1/assignment-rules
PATCH /api/v1/assignment-rules/{rule_id}
PATCH /api/v1/assignment-rules/{rule_id}/inactive
Permission: assignment.manage
```

Request:

```json
{
  "priority": 10,
  "language_id": "uuid",
  "program_id": "uuid",
  "branch_id": "uuid",
  "shift": "WorkingHours",
  "advisor_ids": ["uuid-1", "uuid-2"],
  "strategy": "LeastWorkloadThenRoundRobin",
  "is_active": true
}
```

Validation:

- Advisor active.
- Priority hợp lệ trong tenant.
- Rule inactive không chạy.
- Priority/pool conflict trả `ASSIGNMENT_RULE_CONFLICT`.
- Round-robin fallback phải cập nhật AssignmentPoolState hoặc cơ chế tương đương.

### 11.2. Test assignment rule

```text
POST /api/v1/assignment-rules/test
Permission: assignment.manage
```

Request:

```json
{
  "lead_preview": {
    "language_id": "uuid",
    "program_id": "uuid",
    "branch_id": "uuid",
    "source": "Meta",
    "submitted_at": "2026-06-29T09:00:00+07:00"
  }
}
```

Response:

```json
{
  "data": {
    "matched_rule_id": "uuid",
    "eligible_advisor_ids": ["uuid-1", "uuid-2"],
    "selected_owner_id": "uuid-1",
    "selection_reason": "Lowest active workload"
  }
}
```

### 11.3. SLA tasks

```text
GET /api/v1/sla/tasks?status=Open&overdue=true
Permission: sla.view_update
```

Response item:

```json
{
  "task_id": "uuid",
  "opportunity_id": "uuid",
  "owner_id": "uuid",
  "task_type": "FirstContact",
  "due_at": "2026-06-29T09:15:00+07:00",
  "status": "Open",
  "overdue_level": "None"
}
```

Complete task:

```text
POST /api/v1/sla/tasks/{task_id}/complete
Permission: sla.view_update
```

Request:

```json
{
  "completion_type": "Contacted",
  "note": "Đã gọi và tư vấn lịch test"
}
```

Rules:

- Advisor chỉ complete own task.
- Sales Lead/Admin có thể complete/cancel theo scope nếu có reason.
- SLA hit/miss cần lưu để R1C dashboard dùng.
- SLA hit dựa trên first valid outbound ActivityLog trong SLA.
- Contact success là KPI riêng, dựa trên activity result connected/replied.

### 11.4. Working hours APIs

```text
GET /api/v1/working-hours
PATCH /api/v1/working-hours
Permission: working_hours.manage
```

Request:

```json
{
  "branch_id": null,
  "timezone": "Asia/Ho_Chi_Minh",
  "weekly_hours": [
    {
      "weekday": "MONDAY",
      "is_working_day": true,
      "start_time": "08:00",
      "end_time": "18:00"
    }
  ]
}
```

Rules:

- Default pilot if missing: Monday-Saturday, 08:00-18:00, tenant timezone.
- Branch-specific config can override tenant default if implemented.
- If SLA calculation requires working hours but none exists, use default and emit config warning.

## 12. Error Code Catalog

| Code | HTTP | Ý nghĩa |
|---|---|---|
| `TENANT_INACTIVE` | 422 | Tenant không active |
| `PERMISSION_DENIED` | 403 | Không có quyền |
| `CONTACT_IDENTIFIER_REQUIRED` | 400/422 | Lead thiếu phone/email/Zalo ID |
| `IMPORT_BATCH_NOT_PREVIEWED` | 409 | Batch chưa ở trạng thái `PREVIEWED` |
| `IMPORT_ALREADY_CONFIRMED` | 409 | Batch đã confirm |
| `MAPPING_INVALID` | 400 | Mapping thiếu hoặc sai |
| `DUPLICATE_REVIEW_REQUIRED` | 422 | Case cần review trước khi merge/link |
| `CROSS_TENANT_OPERATION_BLOCKED` | 403 | Thao tác khác tenant |
| `INVALID_STAGE_TRANSITION` | 409 | Transition không hợp lệ |
| `LOST_REASON_REQUIRED` | 422 | Chuyển Lost thiếu reason |
| `OWNER_INACTIVE` | 422 | Owner mới inactive |
| `NO_ELIGIBLE_ADVISOR` | 422 | Không có advisor phù hợp |
| `IDEMPOTENCY_CONFLICT` | 409 | Idempotency key/payload conflict |
| `UNMERGE_NOT_ALLOWED` | 422 | Không thể unmerge an toàn |
| `WEBHOOK_SIGNATURE_INVALID` | 401 | Chữ ký webhook không hợp lệ |
| `WEBHOOK_DUPLICATE_IGNORED` | 200/202 | Webhook trùng idempotency key đã được bỏ qua an toàn |
| `IMPORT_FILE_TOO_LARGE` | 413 | File import vượt giới hạn dung lượng |
| `IMPORT_PREVIEW_EXPIRED` | 410 | Bản preview import đã hết hạn, cần preview lại |
| `RAW_PAYLOAD_ACCESS_DENIED` | 403 | Không có quyền xem raw payload/error report chứa PII |
| `ASSIGNMENT_RULE_CONFLICT` | 409 | Rule assignment bị trùng priority/scope/pool hoặc xung đột điều kiện |
| `WORKING_HOURS_NOT_CONFIGURED` | 422 | Thiếu working hours khi policy yêu cầu cấu hình tường minh |

## 13. Grooming Questions for Tech Lead

| ID | Câu hỏi | Quyết định cần chốt |
|---|---|---|
| API-Q01 | Import preview xử lý sync hay async? | R1A cho phép sync với file nhỏ; file lớn dùng async job + polling |
| API-Q02 | Webhook response nên `200` hay `202`? | R1A pilot khuyến nghị sync `200`; chỉ dùng `202 Accepted` nếu đã có queue/worker và status lookup |
| API-Q03 | Idempotency lưu trong bảng riêng hay raw payload? | Cần chống duplicate webhook/import confirm |
| API-Q04 | Có cần GraphQL cho inbox/filter không? | R1A khuyến nghị REST để nhanh |
| API-Q05 | Stage transition enforce ở service nào? | Nên centralize trong Opportunity service |
| API-Q06 | Assignment workload tính realtime hay snapshot? | R1A có thể dùng active open opportunities/task count |
| API-Q07 | Enum catalog nằm ở API config hay hard-code FE? | API/DB dùng `UPPER_SNAKE_CASE`; FE chỉ render display label |
| API-Q08 | CustomerIdentity có đưa vào P0-lite không? | Có, để chống duplicate và hỗ trợ phone/email/Zalo đa định danh |
| API-Q09 | SLA hit lấy từ stage hay activity? | Lấy từ first valid outbound `ActivityLog`; contact success đo riêng |
| API-Q10 | WorkingHoursConfig mặc định thế nào? | Default `08:00-18:00`, thứ Hai-thứ Bảy, theo timezone tenant |
| API-Q11 | Raw payload/webhook log lưu và che PII ra sao? | Mask/encrypt, phân quyền xem, audit khi export, retention 30-90 ngày |
| API-Q12 | Import confirm trả danh sách ID đầy đủ không? | Không; trả count, link phân trang và sample ID nhỏ |
