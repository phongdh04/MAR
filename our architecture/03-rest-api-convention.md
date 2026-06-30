# REST API CONVENTION - QUY TẮC THIẾT KẾ API MAR

**Ngày tạo:** 30/06/2026  
**Phiên bản:** MAR-CONV-1.0  
**Tác giả:** Tech Lead / Solution Architect  
**Trạng thái:** Locked for Sprint 1 technical kickoff  
**Stack:** Java 21, Spring Boot 3.5.x, Spring Web, Spring Validation, Spring Security  
**Tham chiếu:**
- `D:\Documents-for-Expert-Design-Database\MAR\architecture\api_security_convention.md` - Pattern API/security OASIS
- `D:\Documents-for-Expert-Design-Database\MAR\architecture\exception_convention.md` - Pattern error envelope
- `D:\Documents-for-Expert-Design-Database\MAR\architecture\search_convention.md` - Pattern search/filter/pagination
- `D:\Documents-for-Expert-Design-Database\MAR\architecture\message_convention.md` - Pattern message/i18n
- `D:\Documents-for-Expert-Design-Database\MAR\our architecture\02-coding-package-convention.md` - Coding/package convention MAR

## 1. TỔNG QUAN & MỤC ĐÍCH

Tài liệu này chuẩn hóa cách thiết kế REST API cho backend MAR Lead-to-Enrollment MVP.

Mục đích:

- Tạo API contract nhất quán giữa backend, frontend, QA và BA.
- Chuẩn hóa base path, resource naming, request/response, error envelope, pagination và search.
- Đảm bảo permission và tenant isolation được thể hiện rõ trong API design.
- Tránh phát sinh endpoint tự phát làm lệch contract Sprint 1.

Nguyên tắc ghi nhớ:

> **"API là hợp đồng; hợp đồng phải ổn định, rõ nghĩa và test được."**

## 2. PHẠM VI ÁP DỤNG

Áp dụng cho:

- REST API JSON của backend MAR.
- API controller, request DTO, response DTO, pagination DTO, error response.
- Endpoint thuộc Sprint 1: tenant, branch, user, role/permission, catalog, lead import, audit.
- API test bằng MockMvc/WebTestClient hoặc integration test.
- OpenAPI/spec should-have cho Sprint 1 endpoints sau khi contract freeze.

Không áp dụng cho:

- UI routing của frontend.
- Server-side rendered page.
- External integration/webhook chưa vào scope Sprint 1.
- Internal Java method contract không exposed qua HTTP.

## 3. NGUYÊN TẮC CHUNG

1. **REST JSON only:** backend MAR cung cấp API JSON, không render HTML trong server.
2. **Resource-first:** path dùng danh từ tài nguyên, không dùng động từ khi HTTP method đã đủ nghĩa.
3. **Versioned API:** mọi endpoint Sprint 1 nằm dưới `/api/v1`.
4. **DTO only:** không trả JPA entity ra API.
5. **Stable error envelope:** mọi lỗi trả về cùng cấu trúc.
6. **Explicit pagination:** list API phải có phân trang.
7. **Whitelisted sort/filter:** không truyền thẳng sort/filter tùy ý vào repository.
8. **Permission documented:** endpoint được bảo vệ phải ghi rõ permission.
9. **Tenant isolation enforced:** endpoint tenant-scoped phải chặn cross-tenant access.
10. **Backward compatible by default:** thêm field optional được; đổi tên/xóa field là breaking change.

## 4. QUY TẮC ĐẶT TÊN

### 4.1. Base path

```text
/api/v1
```

Không tạo endpoint ngoài `/api/v1` nếu không có quyết định kiến trúc riêng.

### 4.2. Resource path

| Resource | Endpoint chuẩn |
|---|---|
| Tenant | `/api/v1/tenants` |
| Branch | `/api/v1/branches` |
| User | `/api/v1/users` |
| Role | `/api/v1/roles` |
| Permission matrix | `/api/v1/permissions/matrix` |
| Language | `/api/v1/languages` |
| Program | `/api/v1/programs` |
| Course | `/api/v1/courses` |
| Lead import batch | `/api/v1/imports/leads` |
| Lead import row error | `/api/v1/imports/leads/{batch_id}/errors` |
| Audit event | `/api/v1/audit-events` |

Rules:

- Dùng danh từ số nhiều: `/users`, `/branches`.
- Dùng lowercase path.
- Dùng hyphen cho cụm tài nguyên dài nếu cần: `/audit-events`.
- Path variable dùng snake_case trong tài liệu API: `{tenant_id}`, `{batch_id}`.
- Không dùng action verb cho CRUD thường: tránh `/createTenant`, `/getUsers`.

### 4.3. HTTP method

| Method | Dùng cho | Ví dụ |
|---|---|---|
| `GET` | List/detail/read-only | `GET /api/v1/users` |
| `POST` | Create hoặc command có side effect | `POST /api/v1/imports/leads` |
| `PATCH` | Partial update | `PATCH /api/v1/users/{user_id}` |
| `PUT` | Full replace, dùng rất hạn chế | `PUT /api/v1/catalog/courses/{course_id}` |
| `DELETE` | Future only, khi có decision riêng | Không dùng cho Sprint 1 config entities |

Sprint 1 không expose `DELETE` cho tenant/branch/user/catalog config entities. Dùng `PATCH` để chuyển `status = INACTIVE`. `DELETE` chỉ được thêm sau khi có architecture decision rõ về audit, permission, recovery và API contract.

### 4.4. HTTP status

| Case | Status | Ghi chú |
|---|---|---|
| Create thành công | `201 Created` | Nên trả response detail |
| Read/update thành công | `200 OK` | Có response body |
| Command thành công không body | `204 No Content` | Chỉ khi thật sự không cần body |
| Validation lỗi syntax | `400 Bad Request` | Sprint 1 default |
| Business rule không hợp lệ | `422 Unprocessable Entity` | Dùng cho rule đúng syntax nhưng sai nghiệp vụ |
| Chưa đăng nhập/token sai | `401 Unauthorized` | Không có identity hợp lệ |
| Không đủ quyền | `403 Forbidden` | Có identity nhưng thiếu permission |
| Không tìm thấy | `404 Not Found` | Không lộ resource cross-tenant |
| Trùng dữ liệu/conflict | `409 Conflict` | Duplicate code/name, state conflict |
| Rate limit | `429 Too Many Requests` | Khi bật rate limit |
| Lỗi hệ thống | `500 Internal Server Error` | Không lộ stack trace |

Rule cụ thể:

| Case | Status nên dùng |
|---|---|
| JSON malformed | `400` |
| Bean Validation required/format/min/max | `400` |
| Missing `tenant_name` | `400` |
| Negative tuition nếu validate bằng Bean Validation | `400` |
| Duplicate active branch/user email/code | `409` |
| State already exists/idempotency conflict | `409` |
| Program thuộc inactive language | `422` |
| Permission guardrail violation | `422` |
| Permission denied | `403` |
| Direct-id cross-tenant detail | `404` mặc định theo security policy |

### 4.5. JSON field naming

JSON field dùng `snake_case`:

```json
{
  "tenant_id": "00000000-0000-0000-0000-000000000001",
  "created_at": "2026-06-30T10:00:00Z"
}
```

Java field vẫn dùng `camelCase`:

```java
private UUID tenantId;
private Instant createdAt;
```

Project phải chọn một approach và dùng nhất quán:

- Cấu hình Jackson `PropertyNamingStrategies.SNAKE_CASE`, hoặc
- Annotate rõ bằng `@JsonProperty`.

Ưu tiên cấu hình global để giảm lặp.

## 5. CẤU TRÚC FILE & PACKAGE

### 5.1. Controller package

```text
vn.mar.<module>.controller
```

Ví dụ:

```text
vn.mar.tenant.controller.TenantController
vn.mar.user.controller.UserController
vn.mar.leadimport.controller.LeadImportController
```

### 5.2. DTO package

```text
vn.mar.<module>.dto.request
vn.mar.<module>.dto.response
```

Ví dụ:

```text
vn.mar.user.dto.request.CreateUserRequest
vn.mar.user.dto.request.UserSearchRequest
vn.mar.user.dto.response.UserDetailResponse
vn.mar.user.dto.response.UserSummaryResponse
```

### 5.3. Common API package

```text
vn.mar.common.dto
vn.mar.common.error
vn.mar.common.pagination
vn.mar.common.search
```

Chỉ đặt object dùng chung thật sự ở common:

- `ApiResponse<T>`
- `ErrorResponse`
- `FieldErrorResponse`
- `PageResponse<T>`
- `SortRequest`
- `SearchRequest`

Không đặt DTO nghiệp vụ như `TenantResponse`, `CourseResponse`, `ImportBatchResponse` vào common.

### 5.4. Controller method order

Trong controller, method nên sắp xếp:

1. `GET /resources` list/search.
2. `GET /resources/{id}` detail.
3. `POST /resources` create.
4. `PATCH /resources/{id}` update.
5. `PATCH /resources/{id}` status change/deactivate nếu Sprint 1 config entity cần inactive.
6. Command endpoint riêng nếu có.

## 6. CÁC PATTERN BẮT BUỘC

### 6.1. Standard success envelope

MAR dùng envelope nhất quán để dễ trace request và mở rộng meta:

```java
public record ApiResponse<T>(
        T data,
        ApiMeta meta
) {
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(data, ApiMeta.current());
    }
}
```

Response:

```json
{
  "data": {
    "tenant_id": "00000000-0000-0000-0000-000000000001",
    "tenant_name": "ABC Language Center"
  },
  "meta": {
    "request_id": "req_20260630_0001"
  }
}
```

### 6.2. Standard list response

```java
public record PageResponse<T>(
        List<T> items,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}
```

Response:

```json
{
  "data": {
    "items": [],
    "page": 0,
    "size": 20,
    "total_elements": 0,
    "total_pages": 0
  },
  "meta": {
    "request_id": "req_20260630_0002"
  }
}
```

Nếu dùng Jackson global snake_case thì Java record vẫn viết `totalElements`, `totalPages`.

Đây là source of truth cho list response:

```text
ApiResponse<PageResponse<T>>
data.items
data.page
data.size
data.total_elements
data.total_pages
```

Không dùng song song format cũ kiểu:

```json
{
  "data": [],
  "pagination": {}
}
```

### 6.3. Standard error envelope

```json
{
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "Validation failed",
    "details": [
      {
        "field": "tenant_name",
        "code": "REQUIRED",
        "message": "Tenant name is required"
      }
    ]
  },
  "meta": {
    "request_id": "req_20260630_0003"
  }
}
```

Rules:

- `code` là stable machine-readable code.
- `message` là safe human-readable message.
- `details` dùng cho field validation hoặc row-level import error.
- Không trả stack trace, class name nội bộ, SQL raw hoặc token.
- Error details không được leak full phone/email/token/raw SQL/raw payload.
- Với import row errors, được trả `row_number`, `field`, `code`, `message`; `raw_value` phải mask hoặc bỏ qua nếu chứa PII.

### 6.4. Request DTO pattern

```java
public record CreateTenantRequest(
        @NotBlank
        @Size(max = 255)
        String tenantName,

        @NotBlank
        @Size(max = 50)
        String timezone,

        @NotNull
        TenantStatus status
) {
}
```

Rules:

- Bean Validation cho syntactic validation.
- Không dùng entity làm request.
- Không dùng `Map<String, Object>` nếu contract đã biết.
- Không nhận `tenantId` trong body của normal tenant user endpoint.

### 6.5. Response DTO pattern

```java
public record TenantDetailResponse(
        UUID tenantId,
        String tenantName,
        String timezone,
        TenantStatus status,
        Instant createdAt,
        Instant updatedAt
) {
}
```

Rules:

- Response không chứa field nhạy cảm: password hash, token, secret key.
- Field thời gian dùng ISO-8601.
- Enum trả bằng key ổn định, không trả label hiển thị.

### 6.6. Pagination pattern

Query:

```text
GET /api/v1/users?page=0&size=20&sort=created_at,desc
```

Rules:

- `page` zero-based.
- Default `size = 20`.
- Max `size = 100`.
- This document is the source of truth for pagination.
- New APIs use `page` + `size`.
- Do not use `page_size` in new APIs.
- Nếu `size` vượt max, trả validation error hoặc clamp theo quyết định API contract; Sprint 1 ưu tiên trả lỗi rõ.
- Sort field phải nằm trong whitelist của resource.

### 6.7. Search/filter pattern

Sprint 1 dùng query parameter rõ nghĩa:

```text
GET /api/v1/users?keyword=anh&role_code=ADVISOR&status=ACTIVE&page=0&size=20
```

Với search phức tạp sau Sprint 1, dùng request object:

```json
{
  "keyword": "anh",
  "filters": [
    {
      "field": "role_code",
      "operator": "EQ",
      "value": "ADVISOR"
    }
  ],
  "page": 0,
  "size": 20,
  "sort": [
    {
      "field": "created_at",
      "direction": "DESC"
    }
  ]
}
```

Không cho frontend gửi arbitrary field/operator nếu backend chưa whitelist.

### 6.8. Permission annotation pattern

Endpoint được bảo vệ phải có permission rõ:

```java
@PreAuthorize("@authz.hasPermission(authentication, 'tenant.manage')")
@PostMapping
public ResponseEntity<ApiResponse<TenantDetailResponse>> createTenant(
        @Valid @RequestBody CreateTenantRequest request) {
    TenantDetailResponse response = tenantService.createTenant(request);
    return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
}
```

Permission phải map với permission matrix trong BA docs.

### 6.9. Tenant isolation pattern

Detail/update/delete endpoint tenant-scoped phải load bằng resource id + tenant id:

```java
@Transactional(readOnly = true)
public BranchDetailResponse getBranch(UUID branchId) {
    UUID tenantId = TenantContext.currentTenantId();
    Branch branch = branchRepository.findByIdAndTenantId(branchId, tenantId)
            .orElseThrow(() -> new ResourceNotFoundException("BRANCH_NOT_FOUND"));
    return branchMapper.toDetailResponse(branch);
}
```

Không load bằng id đơn rồi check tenant sau nếu dễ quên ở code path khác.

### 6.10. Versioning pattern

Breaking change phải tạo version mới:

- Xóa field.
- Đổi tên field.
- Đổi type field.
- Đổi enum key.
- Đổi error envelope.
- Đổi meaning của status code.

Backward-compatible change:

- Thêm optional response field.
- Thêm optional request field có default.
- Thêm endpoint mới.
- Thêm enum mới nếu client đã được thiết kế tolerate.

Deprecation rule:

- Deprecated field/endpoint phải được document.
- Phải có replacement rõ.
- Phải có removal plan hoặc điều kiện xóa.
- Không remove field/endpoint đã publish trong cùng Sprint nếu FE/QA đang phụ thuộc mà chưa có migration plan.

### 6.11. Request id pattern

Mọi request phải có `request_id` xuyên suốt API response, error response và log:

```text
Incoming header: X-Request-Id
Response header: X-Request-Id
Error meta: meta.request_id
Log MDC: requestId
```

Rules:

- `RequestIdFilter` generate request id nếu client không gửi.
- Nếu client gửi request id hợp lệ, backend propagate lại.
- `ApiMeta.current()` đọc request id từ request context/MDC.
- Error response `meta.request_id` phải trace được trong application log.

### 6.12. OpenAPI pattern

OpenAPI spec should be generated or maintained for Sprint 1 endpoints after contract freeze.

Every published endpoint must document:

- Method/path.
- Request DTO.
- Response DTO.
- Error envelope.
- Required permission.
- Tenant/platform context.
- Pagination/sort/filter nếu có.

## 7. QUY TẮC RIÊNG CỦA MAR API

### 7.1. Sprint 1 endpoint priority

Ưu tiên thiết kế và test contract cho:

1. Tenant setup.
2. Branch management.
3. User/role/permission foundation.
4. Catalog language/program/course.
5. Lead import batch và import error.
6. Audit event read-only.

### 7.2. Platform-level vs tenant-level endpoint

Platform-level:

- Có thể nhận `tenant_id` để admin quản trị nhiều tenant.
- Phải có permission platform rõ, ví dụ `platform.tenant.manage`.
- `POST /api/v1/tenants` là platform-level; request chạy bằng platform context, không lấy tenant context từ tenant hiện tại.
- Tenant create/update by platform admin must use platform permission, không dùng quyền tenant admin thông thường.

Tenant-level:

- Không nhận `tenant_id` từ body nếu đã có context.
- Chỉ truy cập dữ liệu thuộc tenant hiện tại.
- Tenant-level APIs resolve `tenant_id` từ auth/session context.
- Normal tenant-scoped endpoints must not accept `tenant_id` from request body.

### 7.3. Import API

Lead import là workflow có nhiều trạng thái:

```text
UPLOADED -> VALIDATING -> VALIDATED -> IMPORTING -> IMPORTED
UPLOADED -> VALIDATING -> FAILED
```

Endpoint theo sprint:

| Method | Endpoint | Sprint | Mục đích |
|---|---|---|---|
| `GET` | `/api/v1/imports/leads` | Sprint 1 | List import batch/history |
| `GET` | `/api/v1/imports/leads/{batch_id}` | Sprint 1 | Detail import batch |
| `GET` | `/api/v1/imports/leads/{batch_id}/errors` | Sprint 1 | List row errors |
| `POST` | `/api/v1/imports/leads` | Sprint 2 hoặc explicit Sprint 1 ticket | Upload/tạo import batch thật |
| `POST` | `/api/v1/imports/leads/{batch_id}/confirm` | Sprint 2 | Confirm import |

Command endpoint có thể dùng verb cuối path khi không phải CRUD resource thường.

Nếu Sprint 1 cần testability trước parser production, dùng internal/test-only fixture API theo `11-import-file-storage-convention.md`; không public production như import upload hoàn chỉnh.

### 7.4. Audit API

Audit API mặc định read-only:

- `GET /api/v1/audit-events`
- `GET /api/v1/audit-events/{event_id}`

Không tạo endpoint update/delete audit event.

### 7.5. Idempotency

Sprint 1 chưa bắt buộc idempotency cho CRUD thường.

Cân nhắc `Idempotency-Key` khi có:

- Payment.
- Webhook.
- Lead ads integration.
- Import command retry có nguy cơ duplicate.

## 8. VÍ DỤ CODE MẪU

### 8.1. Good example - create endpoint

```java
@RestController
@RequestMapping("/api/v1/branches")
@RequiredArgsConstructor
public class BranchController {

    private final BranchService branchService;

    @PreAuthorize("@authz.hasPermission(authentication, 'branch.manage')")
    @PostMapping
    public ResponseEntity<ApiResponse<BranchDetailResponse>> createBranch(
            @Valid @RequestBody CreateBranchRequest request) {
        BranchDetailResponse response = branchService.createBranch(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }
}
```

### 8.2. Good example - search endpoint

```java
@GetMapping
public ApiResponse<PageResponse<UserSummaryResponse>> searchUsers(
        @Valid UserSearchRequest request) {
    return ApiResponse.success(userService.searchUsers(request));
}
```

```java
public record UserSearchRequest(
        String keyword,
        String roleCode,
        UserStatus status,
        @Min(0) Integer page,
        @Min(1) @Max(100) Integer size,
        String sort
) {
}
```

### 8.3. Bad example - API contract yếu

```java
@PostMapping("/create-user")
public Object create(@RequestBody Map<String, Object> body) {
    return userRepository.save(new User());
}
```

Vấn đề:

- Path dùng verb và không nằm dưới resource chuẩn.
- Request dùng `Map`, không có validation.
- Controller gọi repository.
- Response trả entity/object tùy tiện.
- Không có permission.
- Không có tenant isolation.
- Không có error envelope.

## 9. ANTI-PATTERNS CẦN TRÁNH

Không được:

- Tạo endpoint không version: `/users`.
- Tạo endpoint theo action CRUD: `/createUser`, `/updateTenant`.
- Dùng status `200 OK` cho mọi trường hợp lỗi.
- Trả `RuntimeException.getMessage()` trực tiếp ra client.
- Trả stack trace hoặc SQL detail.
- Trả JPA entity ra API.
- Dùng request/response `Map` khi contract đã biết.
- Dùng `tenant_id` từ body cho tenant-level endpoint.
- Cho sort/filter field chạy thẳng vào query.
- Dùng enum label hiển thị làm enum key API.
- Đổi tên field API mà không versioning hoặc migration plan.
- Dùng `page_size` trong API mới.
- Dùng list response dạng `data + pagination` song song với `data.items`.
- Tạo `DELETE` endpoint cho Sprint 1 config entity khi chưa có decision.
- Trả raw value nhạy cảm trong error details.
- Tạo endpoint update/delete cho audit event.

## 10. TESTING CONVENTIONS

### 10.1. Controller contract test

Mỗi controller chính cần test:

- Valid request trả đúng status và response envelope.
- Invalid request trả `VALIDATION_ERROR`.
- Missing authentication trả `401`.
- Missing permission trả `403`.
- Cross-tenant access trả `404` hoặc `403` theo policy đã chốt.
- Pagination max size được enforce.
- Pagination dùng `page` + `size`, không dùng `page_size`.
- List response dùng `ApiResponse<PageResponse<T>>`.
- Sort field ngoài whitelist bị reject.
- `X-Request-Id`/`meta.request_id` tồn tại và trace được.
- OpenAPI/spec cập nhật sau khi contract freeze.

### 10.2. MockMvc example

```java
@WebMvcTest(BranchController.class)
class BranchControllerTest {

    @Test
    void createBranch_whenRequestValid_shouldReturnCreated() throws Exception {
        mockMvc.perform(post("/api/v1/branches")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "code": "HN01",
                                  "name": "Ha Noi Branch"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.branch_id").exists())
                .andExpect(jsonPath("$.meta.request_id").exists());
    }
}
```

### 10.3. API snapshot/contract

Khi endpoint đã publish cho frontend:

- Không đổi response field tùy tiện.
- Nếu cần đổi, cập nhật OpenAPI/spec và migration note.
- Test phải bắt được breaking change quan trọng.

### 10.4. Import API test

Lead import endpoint cần test thêm:

- Sprint 1 history endpoints: list/detail/errors.
- Batch không thuộc tenant hiện tại.
- Row error response có pagination theo `page` + `size`.
- Upload/confirm tests chỉ bắt buộc khi endpoint thuộc Sprint 2 hoặc có explicit Sprint 1 ticket.
- Error details không leak raw phone/email/raw row.

## 11. CODE REVIEW CHECKLIST

- [ ] Endpoint nằm dưới `/api/v1`.
- [ ] Path dùng resource noun số nhiều.
- [ ] HTTP method đúng ý nghĩa.
- [ ] Status code đúng convention.
- [ ] Request/response dùng DTO.
- [ ] Response không trả entity.
- [ ] Error response theo envelope chuẩn.
- [ ] Permission được document và enforce.
- [ ] Tenant isolation được enforce.
- [ ] List API có pagination.
- [ ] Pagination dùng `page` + `size`, không dùng `page_size`.
- [ ] List response dùng `ApiResponse<PageResponse<T>>` với `data.items`.
- [ ] Sort/filter field được whitelist.
- [ ] Validation dùng Bean Validation và service validator đúng chỗ.
- [ ] Không leak secret/token/password/hash.
- [ ] Error details không leak raw PII/raw payload.
- [ ] Platform-level API và tenant-level API được phân biệt rõ.
- [ ] Import endpoints được tag đúng Sprint 1/Sprint 2.
- [ ] Sprint 1 config entities không expose `DELETE`.
- [ ] OpenAPI/spec được cập nhật nếu endpoint đã freeze/publish.
- [ ] `X-Request-Id` và `meta.request_id` khớp logging convention.
- [ ] API change không phá backward compatibility.
- [ ] Test có case success, validation, unauthorized, forbidden.

## 12. TÀI LIỆU LIÊN QUAN

- `02-coding-package-convention.md` - DTO/controller/service package.
- `04-database-flyway-convention.md` - Entity/repository/database mapping.
- `05-security-auth-authz-convention.md` - Auth/authz/API protection.
- `06-exception-error-i18n-convention.md` - Error envelope/error code.
- `09-testing-quality-convention.md` - API test quality gate.
- OASIS reference: `api_security_convention.md`, `exception_convention.md`, `search_convention.md`, `message_convention.md`.

## 13. LỊCH SỬ CẬP NHẬT

| Phiên bản | Ngày | Người cập nhật | Nội dung |
|---|---|---|---|
| MAR-CONV-1.0 | 30/06/2026 | Tech Lead / Solution Architect | Đồng bộ API format: `page/size`, `ApiResponse<PageResponse<T>>`, tag Import API theo sprint, làm rõ platform/tenant context, status 400/409/422, no DELETE Sprint 1 và request id/OpenAPI rule. |
| MAR-CONV-1.0 | 30/06/2026 | Tech Lead / Solution Architect | Chuẩn hóa REST API convention theo pattern OASIS, chuyển sang REST JSON-only cho MAR. |
