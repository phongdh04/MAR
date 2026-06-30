# EXCEPTION, ERROR & I18N CONVENTION - QUY TẮC LỖI MAR

**Ngày tạo:** 30/06/2026  
**Phiên bản:** MAR-CONV-1.1  
**Tác giả:** Tech Lead / Solution Architect  
**Trạng thái:** Locked for Sprint 1 technical kickoff  
**Stack:** Spring Web, Spring Validation, Spring Security, Jackson, MessageSource optional  
**Tham chiếu:**
- `D:\Documents-for-Expert-Design-Database\MAR\architecture\exception_convention.md` - Pattern exception OASIS
- `D:\Documents-for-Expert-Design-Database\MAR\architecture\message_convention.md` - Pattern message/i18n
- `D:\Documents-for-Expert-Design-Database\MAR\architecture\api_security_convention.md` - Pattern security error
- `D:\Documents-for-Expert-Design-Database\MAR\our architecture\03-rest-api-convention.md` - REST API envelope MAR

## 1. TỔNG QUAN & MỤC ĐÍCH

Tài liệu này chuẩn hóa exception, error code, error response và i18n strategy cho MAR REST API.

Mục đích:

- Đảm bảo frontend/QA xử lý lỗi bằng contract ổn định.
- Mọi lỗi có `error.code` machine-readable.
- Mọi response lỗi có `request_id` để trace log.
- Không expose stack trace, SQL, token, password hoặc thông tin nội bộ ra client.
- Tách rõ lỗi validation, business rule, conflict, authentication, authorization và unexpected error.

Nguyên tắc ghi nhớ:

> **"Client xử lý bằng code; con người đọc message; kỹ sư debug bằng request_id."**

## 2. PHẠM VI ÁP DỤNG

Áp dụng cho:

- Exception class trong backend MAR.
- Global exception handler.
- Error code enum/catalog.
- Error response DTO.
- Bean Validation field error.
- Security error `401/403`.
- Import row error response.
- Message key/i18n chuẩn bị cho frontend.

Không áp dụng cho:

- UI toast/modal copywriting chi tiết.
- Email/SMS template.
- Log format chi tiết, trừ rule liên quan exception.
- Database constraint naming, trừ mapping lỗi constraint sang error code.

## 3. NGUYÊN TẮC CHUNG

1. **One error shape:** mọi API error trả cùng envelope.
2. **Error code required:** không trả free-text-only error.
3. **Global handler owns mapping:** controller không tự try-catch để build lỗi thường.
4. **Expected vs unexpected:** lỗi nghiệp vụ log khác lỗi hệ thống.
5. **No sensitive leak:** message không chứa password, token, secret, raw SQL, stack trace.
6. **Stable codes:** đổi message được, đổi code là breaking change.
7. **Field details for validation:** validation lỗi field phải có `details`.
8. **Security ambiguity:** cross-tenant direct-id nên trả not found theo policy.
9. **I18n-ready:** error code/message key phải đủ ổn định để frontend localize.
10. **Request id everywhere:** error response phải có `meta.request_id`.

## 4. QUY TẮC ĐẶT TÊN

### 4.1. Class naming

| Loại | Convention | Ví dụ |
|---|---|---|
| Base exception | `MarException` | `MarException` |
| Validation exception | `ValidationException` | `ValidationException` |
| Not found exception | `ResourceNotFoundException` | `ResourceNotFoundException` |
| Conflict exception | `ConflictException` | `ConflictException` |
| Business exception | `BusinessException` | `BusinessException` |
| Error code enum | `ErrorCode` | `ErrorCode` |
| Error response DTO | `ErrorResponse` | `ErrorResponse` |
| Field error DTO | `FieldErrorDetail` | `FieldErrorDetail` |
| Handler | `GlobalExceptionHandler` | `GlobalExceptionHandler` |

### 4.2. Error code naming

Error code dùng UPPER_SNAKE_CASE:

```text
VALIDATION_ERROR
RESOURCE_NOT_FOUND
DUPLICATE_RESOURCE
PERMISSION_DENIED
```

Rules:

- Không dùng camelCase/kebab-case.
- Không dùng message tiếng Việt/Anh làm code.
- Không nhúng id cụ thể vào code.
- Không tạo code quá vụn nếu field detail đã đủ.

### 4.3. Message key naming

Pattern:

```text
<module>.<context>.<code_or_field>
```

Ví dụ:

```text
tenant.error.inactive
branch.error.duplicate_active_code
user.validation.email_required
permission.error.guardrail_violation
import.error.batch_not_found
```

### 4.4. Field path naming

Field trong validation detail dùng JSON field naming:

```text
tenant_name
email
branches[0].branch_name
rows[15].phone_number
```

Không trả Java internal field nếu API đã dùng snake_case.

## 5. CẤU TRÚC FILE & PACKAGE

### 5.1. Common exception package

```text
vn.mar.common.exception
├── MarException.java
├── ResourceNotFoundException.java
├── ValidationException.java
├── ConflictException.java
├── BusinessException.java
└── ExternalServiceException.java
```

### 5.2. Common error package

```text
vn.mar.common.error
├── ErrorCode.java
├── ErrorResponse.java
├── ErrorBody.java
├── ErrorDetail.java
├── ApiMeta.java
├── ErrorResponseFactory.java
└── GlobalExceptionHandler.java
```

### 5.3. Message resources

Nếu backend resolve localized message:

```text
src/main/resources/messages.properties
src/main/resources/messages_vi.properties
src/main/resources/messages_en.properties
```

Sprint 1 default: backend trả default message ổn định; frontend có thể localize bằng `error.code`.

### 5.4. Module-specific error factory

Module có nhiều business rule có thể có factory/helper:

```text
vn.mar.branch.exception.BranchErrors
vn.mar.imports.exception.ImportErrors
```

Không tạo exception class mới cho mọi rule nhỏ nếu `BusinessException` + `ErrorCode` đủ rõ.

## 6. CÁC PATTERN BẮT BUỘC

### 6.1. Error envelope pattern

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
    "request_id": "req_20260630_0001"
  }
}
```

Rules:

- `error.code` bắt buộc.
- `error.message` bắt buộc, safe để hiển thị.
- `error.details` optional, nhưng nếu có thì là array.
- `meta.request_id` bắt buộc.
- `meta.request_id` được tạo/propagate bởi `RequestIdFilter`, không sinh rải rác trong handler.
- `ApiMeta.current()` chỉ đọc request id từ request context/MDC đã được filter set trước đó.

### 6.2. Error response DTO pattern

```java
public record ErrorResponse(
        ErrorBody error,
        ApiMeta meta
) {
    public static ErrorResponse of(ErrorCode code, String message, List<ErrorDetail> details) {
        return new ErrorResponse(
                new ErrorBody(code.code(), message, details),
                ApiMeta.current()
        );
    }
}
```

```java
public record ErrorBody(
        String code,
        String message,
        List<ErrorDetail> details
) {
}
```

```java
public record ErrorDetail(
        String field,
        String code,
        String message
) {
}
```

Rules:

- `ErrorDetail` không có field `reason`.
- Với lỗi field-level, client đọc `details[].code` và `details[].message`.
- Với lỗi top-level, client đọc `error.code` và `error.message`.
- Không tạo hai khái niệm song song `reason` và `code`; `code` là machine-readable contract duy nhất.

### 6.2.1. Error response factory pattern

Nếu cần dùng chung giữa MVC handler và Spring Security filter-chain, tạo một factory nhỏ:

```java
@Component
public class ErrorResponseFactory {

    public ErrorResponse create(ErrorCode code, String message, List<ErrorDetail> details) {
        return ErrorResponse.of(code, message, details);
    }

    public ErrorResponse create(ErrorCode code) {
        return ErrorResponse.of(code, code.defaultMessage(), List.of());
    }
}
```

Rules:

- `GlobalExceptionHandler`, `ApiAuthenticationEntryPoint` và `ApiAccessDeniedHandler` phải dùng cùng DTO/factory này.
- Không build JSON thủ công trong security handler.
- Nếu chưa tách factory riêng ở Sprint 1, cả ba handler vẫn phải gọi chung `ErrorResponse.of(...)` để giữ cùng envelope.

### 6.3. ErrorCode enum pattern

```java
public enum ErrorCode {

    UNAUTHENTICATED("UNAUTHENTICATED", HttpStatus.UNAUTHORIZED, "Authentication is required"),
    PERMISSION_DENIED("PERMISSION_DENIED", HttpStatus.FORBIDDEN, "Permission denied"),
    VALIDATION_ERROR("VALIDATION_ERROR", HttpStatus.BAD_REQUEST, "Validation failed"),
    RESOURCE_NOT_FOUND("RESOURCE_NOT_FOUND", HttpStatus.NOT_FOUND, "Resource not found"),
    DUPLICATE_RESOURCE("DUPLICATE_RESOURCE", HttpStatus.CONFLICT, "Resource already exists"),
    CONFLICT("CONFLICT", HttpStatus.CONFLICT, "State conflict"),
    TENANT_INACTIVE("TENANT_INACTIVE", HttpStatus.CONFLICT, "Tenant is inactive"),
    DUPLICATE_ACTIVE_BRANCH("DUPLICATE_ACTIVE_BRANCH", HttpStatus.CONFLICT, "Active branch code already exists"),
    DUPLICATE_USER_EMAIL("DUPLICATE_USER_EMAIL", HttpStatus.CONFLICT, "User email already exists"),
    NEGATIVE_TUITION("NEGATIVE_TUITION", HttpStatus.BAD_REQUEST, "Tuition must be non-negative"),
    BUSINESS_RULE_VIOLATION("BUSINESS_RULE_VIOLATION", HttpStatus.UNPROCESSABLE_ENTITY, "Business rule violation"),
    INTERNAL_ERROR("INTERNAL_ERROR", HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error");

    private final String code;
    private final HttpStatus httpStatus;
    private final String defaultMessage;
}
```

Rules:

- `ErrorCode` là catalog tập trung.
- Không hard-code string code rải rác nếu đã có enum.
- Khi thêm code mới, cập nhật test và tài liệu API liên quan.

### 6.4. Base exception pattern

```java
@Getter
public abstract class MarException extends RuntimeException {

    private final ErrorCode errorCode;
    private final List<ErrorDetail> details;

    protected MarException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = Objects.requireNonNull(errorCode);
        this.details = List.of();
    }

    protected MarException(ErrorCode errorCode, String message, List<ErrorDetail> details) {
        super(message);
        this.errorCode = Objects.requireNonNull(errorCode);
        this.details = List.copyOf(details);
    }
}
```

Rules:

- Constructor validate code/message không rỗng.
- Không nhúng PII vào message.
- Exception expected không cần stack trace tùy optimization sau, nhưng Sprint 1 giữ đơn giản.

### 6.5. Concrete exception pattern

```java
public class ResourceNotFoundException extends MarException {

    public ResourceNotFoundException(String message) {
        super(ErrorCode.RESOURCE_NOT_FOUND, message);
    }
}
```

```java
public class BusinessException extends MarException {

    public BusinessException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}
```

Factory method cho rule lặp lại:

```java
public final class BranchErrors {

    private BranchErrors() {
    }

    public static BusinessException duplicateActiveCode() {
        return new BusinessException(
                ErrorCode.DUPLICATE_RESOURCE,
                "Active branch code already exists"
        );
    }
}
```

### 6.6. GlobalExceptionHandler pattern

```java
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    @ExceptionHandler(MarException.class)
    public ResponseEntity<ErrorResponse> handleMarException(MarException ex) {
        ErrorCode code = ex.getErrorCode();
        ErrorResponse response = ErrorResponse.of(code, ex.getMessage(), ex.getDetails());
        return ResponseEntity.status(code.httpStatus()).body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        List<ErrorDetail> details = toFieldErrors(ex);
        ErrorResponse response = ErrorResponse.of(
                ErrorCode.VALIDATION_ERROR,
                ErrorCode.VALIDATION_ERROR.defaultMessage(),
                details
        );
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex) {
        log.error("Unexpected error requestId={}", ApiMeta.current().requestId(), ex);
        ErrorResponse response = ErrorResponse.of(
                ErrorCode.INTERNAL_ERROR,
                ErrorCode.INTERNAL_ERROR.defaultMessage(),
                List.of()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
```

Rules:

- Chỉ có một global handler chính.
- `GlobalExceptionHandler` chỉ xử lý exception sau khi request đã vào MVC dispatch.
- `GlobalExceptionHandler` không phải owner duy nhất của `401/403` phát sinh trong Spring Security filter-chain.
- Unexpected error log stack trace ở server nhưng response generic.
- Controller không catch exception chỉ để format error.
- Với `DataIntegrityViolationException`, handler phải gọi `ConstraintErrorMapper` trước khi quyết định error code.

### 6.7. Bean Validation mapping pattern

Bean Validation:

```java
public record CreateUserRequest(
        @NotBlank
        @Email
        String email,

        @NotBlank
        @Size(max = 255)
        String fullName
) {
}
```

Response:

```json
{
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "Validation failed",
    "details": [
      {
        "field": "email",
        "code": "INVALID_EMAIL",
        "message": "Email format is invalid"
      }
    ]
  },
  "meta": {
    "request_id": "req_20260630_0002"
  }
}
```

### 6.8. Security exception mapping

Spring Security filter-chain có lifecycle riêng, nên `401/403` không được kỳ vọng chỉ đi qua `GlobalExceptionHandler`.

| Lỗi | Owner | Response |
|---|---|---|
| Chưa xác thực, token thiếu/sai/hết hạn | `ApiAuthenticationEntryPoint` | `401 UNAUTHENTICATED` |
| Đã xác thực nhưng thiếu permission | `ApiAccessDeniedHandler` | `403 PERMISSION_DENIED` |
| Exception nghiệp vụ sau khi vào controller/service | `GlobalExceptionHandler` | Theo `ErrorCode` |

Rules:

- `ApiAuthenticationEntryPoint` và `ApiAccessDeniedHandler` phải trả cùng `ErrorResponse` envelope với `GlobalExceptionHandler`.
- Cả security handler và global handler phải có `meta.request_id`.
- Security handler không trả raw exception message từ Spring Security ra client.
- Security audit/log phải ghi được request id để đối soát với `08-audit-convention.md`.

Authentication failure:

```json
{
  "error": {
    "code": "UNAUTHENTICATED",
    "message": "Authentication is required",
    "details": []
  },
  "meta": {
    "request_id": "req_20260630_0003"
  }
}
```

Authorization failure:

```json
{
  "error": {
    "code": "PERMISSION_DENIED",
    "message": "You do not have permission to perform this action",
    "details": []
  },
  "meta": {
    "request_id": "req_20260630_0004"
  }
}
```

Không reveal resource thuộc tenant khác trong message.

### 6.9. ConstraintErrorMapper pattern

Database constraint là lớp bảo vệ cuối cùng, nhưng client không được thấy raw SQL hoặc raw constraint detail. Backend phải map constraint đã biết sang error code ổn định:

```java
@Component
public class ConstraintErrorMapper {

    private static final Map<String, ErrorCode> KNOWN_CONSTRAINTS = Map.of(
            "ux_branches__tenant_code_active", ErrorCode.DUPLICATE_ACTIVE_BRANCH,
            "ux_users__tenant_email", ErrorCode.DUPLICATE_USER_EMAIL,
            "ck_courses__tuition_non_negative", ErrorCode.NEGATIVE_TUITION
    );

    public Optional<ErrorCode> map(String constraintName) {
        return Optional.ofNullable(KNOWN_CONSTRAINTS.get(constraintName));
    }
}
```

Sprint 1 known mapping:

| DB constraint | Error code | HTTP |
|---|---|---:|
| `ux_branches__tenant_code_active` | `DUPLICATE_ACTIVE_BRANCH` | 409 |
| `ux_users__tenant_email` | `DUPLICATE_USER_EMAIL` | 409 |
| `ck_courses__tuition_non_negative` | `NEGATIVE_TUITION` | 400 |

Rules:

- Constraint name phải khớp với `04-database-flyway-convention.md`.
- Unknown unique/check constraint trả lỗi generic an toàn, thường là `CONFLICT` hoặc `INTERNAL_ERROR` tùy ngữ cảnh đã xác minh được hay chưa.
- Không trả raw SQL, table name nội bộ, stack trace hoặc database error detail cho client.
- Khi thêm constraint business-critical, phải cập nhật cả Flyway, mapper, error catalog và test.

## 7. QUY TẮC RIÊNG CỦA MAR ERROR/I18N

### 7.1. Sprint 1 error code catalog

| Error code | HTTP | Dùng khi |
|---|---:|---|
| `UNAUTHENTICATED` | 401 | Chưa login/token sai/hết hạn |
| `PERMISSION_DENIED` | 403 | Thiếu permission |
| `VALIDATION_ERROR` | 400 | Bean Validation/request syntax lỗi |
| `RESOURCE_NOT_FOUND` | 404 | Không thấy resource hoặc cross-tenant direct-id |
| `DUPLICATE_RESOURCE` | 409 | Trùng code/email/name theo unique rule |
| `CONFLICT` | 409 | State conflict chung |
| `BUSINESS_RULE_VIOLATION` | 422 | Rule nghiệp vụ không hợp lệ |
| `TENANT_INACTIVE` | 409 | Tenant inactive không cho thao tác |
| `DUPLICATE_ACTIVE_BRANCH` | 409 | Trùng active branch trong tenant |
| `DUPLICATE_USER_EMAIL` | 409 | Email user đã tồn tại theo rule |
| `INVALID_PARENT_STATUS` | 422 | Parent catalog inactive không cho tạo child |
| `NEGATIVE_TUITION` | 400 | Tuition âm, ưu tiên nằm trong validation detail của `VALIDATION_ERROR` |
| `INVALID_PERMISSION_GUARDRAIL` | 422 | Permission matrix vi phạm guardrail |
| `IMPORT_BATCH_NOT_FOUND` | 404 | Import batch không tồn tại/khác tenant |
| `IMPORT_FILE_INVALID` | 400 | File import sai format |
| `IMPORT_ROW_VALIDATION_ERROR` | 422 | Row import lỗi nghiệp vụ |
| `INTERNAL_ERROR` | 500 | Lỗi hệ thống |

Rules:

- Dùng code cụ thể khi frontend/QA/caller cần xử lý khác nhau.
- Dùng code generic khi không có hành vi client khác biệt hoặc rule chưa ổn định.
- Nếu catalog tăng lớn, tách thêm `ERROR_CODE_REGISTRY.md` hoặc bảng registry trong tài liệu API để tránh trùng nghĩa.

### 7.2. Khi nào dùng exception nào

| Tình huống | Exception | Status |
|---|---|---:|
| Request thiếu field/sai format | Bean Validation -> handler | 400 |
| Entity không tồn tại | `ResourceNotFoundException` | 404 |
| Resource khác tenant | `ResourceNotFoundException` | 404 |
| Trùng unique business key | `ConflictException` | 409 |
| State không cho phép | `BusinessException` | 422 |
| Chưa authenticated | Security entry point | 401 |
| Thiếu permission | Access denied handler | 403 |
| Constraint DB do duplicate | Handler map `DataIntegrityViolationException` | 409 |
| Constraint DB đã biết | `ConstraintErrorMapper` | Theo mapped `ErrorCode` |
| Tenant hiện tại inactive | `BusinessException(ErrorCode.TENANT_INACTIVE)` | 409 |
| Tuition âm do Bean Validation bắt được | `VALIDATION_ERROR` + detail code `NEGATIVE_TUITION` | 400 |
| Tuition âm do business rule bắt sau này | `BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION)` hoặc code cụ thể đã chốt | 422 |
| Lỗi ngoài dự kiến | `Exception` handler | 500 |

### 7.3. I18n strategy

Sprint 1:

- Backend trả `error.code` và default English message.
- Frontend có thể map `error.code` sang tiếng Việt.
- Field label thuộc frontend.

Sau Sprint 1 nếu backend cần localized message:

- Dùng `Accept-Language`.
- Dùng `MessageSource`.
- Message key map từ `ErrorCode`.
- Nếu không có bản dịch, fallback default English message.

### 7.4. Message safety rule

Không đưa vào message:

- Password/token/OTP.
- Full import row raw data.
- SQL statement.
- Stack trace.
- Internal host/path.
- Email/số điện thoại đầy đủ nếu không cần.

Nên dùng:

```text
User not found
Active branch code already exists
Import file format is invalid
```

Không dùng:

```text
User phong@example.com not found in tenants table using SQL ...
```

### 7.5. Import row error rule

Import row error có thể dùng `details` nhiều dòng:

```json
{
  "field": "rows[15].phone_number",
  "code": "INVALID_PHONE_NUMBER",
  "message": "Phone number format is invalid"
}
```

Với lỗi row-level lớn, lưu chi tiết vào `import_rows.error_details` và API trả phân trang.

### 7.6. TENANT_INACTIVE usage rule

`TENANT_INACTIVE` dùng khi user đã authenticated, tenant hiện tại xác định được, nhưng trạng thái tenant chặn thao tác.

Không dùng `TENANT_INACTIVE` cho:

- Tenant id không tồn tại.
- Direct-id thuộc tenant khác.
- User chưa authenticated.

Các trường hợp đó lần lượt dùng `RESOURCE_NOT_FOUND` hoặc `UNAUTHENTICATED` theo security policy.

### 7.7. NEGATIVE_TUITION usage rule

Sprint 1 mặc định:

- Request tạo/sửa course có tuition âm phải được Bean Validation bắt sớm.
- Response top-level là `VALIDATION_ERROR` HTTP 400.
- `details[].field` là `tuition`.
- `details[].code` là `NEGATIVE_TUITION`.

Sau Sprint 1, nếu tuition âm chỉ phát hiện trong rule nghiệp vụ phức tạp, có thể dùng `BUSINESS_RULE_VIOLATION` HTTP 422 hoặc code cụ thể mới, nhưng phải cập nhật catalog/test trước.

### 7.8. Request id ownership rule

`RequestIdFilter` là owner của request id:

- Đọc `X-Request-Id` từ request nếu client gửi và format hợp lệ.
- Nếu client không gửi, backend sinh request id mới.
- Ghi lại vào response header `X-Request-Id`.
- Đưa vào MDC/request context để `ApiMeta.current()` đọc.

Handler không tự sinh request id riêng vì sẽ làm lệch giữa response, log và audit.

### 7.9. Error code granularity rule

Trước khi thêm error code mới, trả lời ba câu hỏi:

1. Client/QA có cần assert hoặc xử lý riêng không?
2. Code này có ổn định về nghiệp vụ qua nhiều sprint không?
3. Field detail có đủ diễn đạt lỗi này chưa?

Nếu câu trả lời là không, ưu tiên dùng code generic như `VALIDATION_ERROR`, `CONFLICT` hoặc `BUSINESS_RULE_VIOLATION` kèm detail cụ thể.

## 8. VÍ DỤ CODE MẪU

### 8.1. Good example - service throw business exception

```java
@Transactional
public BranchDetailResponse createBranch(CreateBranchRequest request) {
    UUID tenantId = currentUserContext.currentTenantId();

    if (branchRepository.existsByTenantIdAndCodeIgnoreCase(tenantId, request.code())) {
        throw new ConflictException(
                ErrorCode.DUPLICATE_ACTIVE_BRANCH,
                "Active branch code already exists"
        );
    }

    Branch branch = Branch.create(tenantId, request.code(), request.name());
    return branchMapper.toDetailResponse(branchRepository.save(branch));
}
```

### 8.2. Good example - controller không try-catch

```java
@PostMapping
public ResponseEntity<ApiResponse<BranchDetailResponse>> createBranch(
        @Valid @RequestBody CreateBranchRequest request) {
    BranchDetailResponse response = branchService.createBranch(request);
    return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
}
```

### 8.3. Bad example - inconsistent error

```java
@PostMapping
public ResponseEntity<?> createUser(@RequestBody CreateUserRequest request) {
    try {
        return ResponseEntity.ok(userService.createUser(request));
    } catch (Exception ex) {
        return ResponseEntity.status(500).body(ex.getMessage());
    }
}
```

Vấn đề:

- Controller catch `Exception` chung.
- Response lỗi là string tự do.
- Có nguy cơ leak internal message.
- Không có `request_id`.
- Không phân biệt validation/business/unexpected.

## 9. ANTI-PATTERNS CẦN TRÁNH

Không được:

- Trả raw stack trace cho client.
- Trả lỗi lúc thì string, lúc thì object.
- Dùng HTTP 200 cho lỗi nghiệp vụ.
- Hard-code error code string rải rác.
- Catch `Exception` trong controller để format lỗi.
- Log expected validation error ở `ERROR`.
- Expose SQL/constraint raw cho client.
- Expose raw database constraint name/error detail ra client.
- Dựa vào `GlobalExceptionHandler` để xử lý toàn bộ `401/403` của Spring Security filter-chain.
- Tạo `ErrorDetail.reason` song song với `ErrorDetail.code`.
- Sinh `request_id` riêng trong từng handler thay vì dùng `RequestIdFilter`.
- Đưa token/password/import raw row vào message.
- Tạo error code mới cho mọi field nếu `details` đã đủ.
- Đổi error code đã publish mà không migration contract.
- Trả message khác nhau làm lộ resource thuộc tenant khác.

## 10. TESTING CONVENTIONS

### 10.1. Global handler test

Test các mapping chính:

- `MarException` -> status theo `ErrorCode`.
- `MethodArgumentNotValidException` -> `VALIDATION_ERROR`.
- `DataIntegrityViolationException` -> `ConstraintErrorMapper` nếu constraint đã biết.
- Unexpected `Exception` -> `INTERNAL_ERROR` và generic message.
- Không assert `AuthenticationException`/`AccessDeniedException` filter-chain ở `GlobalExceptionHandler`; phần này thuộc security handler test.

### 10.2. Error envelope test

Mọi API error test phải assert:

- `$.error.code`
- `$.error.message`
- `$.meta.request_id`
- `$.error.details` nếu validation/import error.
- Không có `$.error.details[*].reason`.
- Response header `X-Request-Id` khớp với `$.meta.request_id`.

### 10.3. Validation detail test

Ví dụ:

```java
mockMvc.perform(post("/api/v1/tenants")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"tenant_name\":\"\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.error.details[0].field").value("tenant_name"));
```

### 10.4. Security error test

Test:

- Missing token -> `401` + `UNAUTHENTICATED`.
- Missing permission -> `403` + `PERMISSION_DENIED`.
- `ApiAuthenticationEntryPoint` và `ApiAccessDeniedHandler` trả cùng envelope/factory với global handler.
- Security error có `meta.request_id` và response header `X-Request-Id`.
- Cross-tenant direct-id -> `404` + not found code.

### 10.5. No sensitive leak test

Với auth/import endpoint:

- Response không chứa raw token/password.
- Error message không chứa raw row full content.
- Unexpected error không chứa class name/stack trace.
- DB constraint violation không leak SQL/table/constraint detail raw ra client.

### 10.6. Constraint mapper test

Test bắt buộc cho Sprint 1:

- `ux_branches__tenant_code_active` -> `DUPLICATE_ACTIVE_BRANCH`.
- `ux_users__tenant_email` -> `DUPLICATE_USER_EMAIL`.
- `ck_courses__tuition_non_negative` -> `NEGATIVE_TUITION`.
- Unknown constraint -> generic safe error, không leak raw database message.

### 10.7. Error contract snapshot test

Với các endpoint Sprint 1 quan trọng, nên có snapshot/contract test cho:

- `400 VALIDATION_ERROR`.
- `401 UNAUTHENTICATED`.
- `403 PERMISSION_DENIED`.
- `404 RESOURCE_NOT_FOUND` cho cross-tenant direct-id.
- `409 DUPLICATE_ACTIVE_BRANCH` hoặc `DUPLICATE_USER_EMAIL`.

## 11. CODE REVIEW CHECKLIST

- [ ] Exception có `ErrorCode`.
- [ ] Error code dùng UPPER_SNAKE_CASE.
- [ ] HTTP status đúng convention.
- [ ] Response lỗi theo envelope chuẩn.
- [ ] `meta.request_id` có trong error.
- [ ] `meta.request_id` lấy từ `RequestIdFilter`/MDC/request context.
- [ ] Validation lỗi field có `details`.
- [ ] `ErrorDetail` chỉ có `field`, `code`, `message`; không có `reason`.
- [ ] Controller không catch exception để format lỗi thường.
- [ ] `GlobalExceptionHandler` xử lý mapping tập trung.
- [ ] `GlobalExceptionHandler` không bị dùng để thay thế `ApiAuthenticationEntryPoint`/`ApiAccessDeniedHandler`.
- [ ] `ApiAuthenticationEntryPoint` và `ApiAccessDeniedHandler` dùng cùng envelope/factory.
- [ ] `ConstraintErrorMapper` có mapping/test cho constraint đã biết.
- [ ] Message không leak PII/secret/SQL/stack trace.
- [ ] Security error không reveal cross-tenant resource.
- [ ] Error code mới được thêm vào catalog và test.
- [ ] API test assert error envelope.
- [ ] i18n/message key không phá stable error code.

## 12. TÀI LIỆU LIÊN QUAN

- `03-rest-api-convention.md` - API envelope/status code.
- `05-security-auth-authz-convention.md` - Security error `401/403`.
- `07-logging-observability-convention.md` - Log level/request id.
- `08-audit-convention.md` - Audit lỗi/security event quan trọng.
- `09-testing-quality-convention.md` - Test gate cho error response.
- OASIS reference: `exception_convention.md`, `message_convention.md`, `api_security_convention.md`.

## 13. LỊCH SỬ CẬP NHẬT

| Phiên bản | Ngày | Người cập nhật | Nội dung |
|---|---|---|---|
| MAR-CONV-1.1 | 30/06/2026 | Tech Lead / Solution Architect | Bổ sung `ErrorDetail` không dùng `reason`, chốt owner `401/403` giữa security filter-chain và global handler, thêm `ConstraintErrorMapper`, request-id ownership, rule `TENANT_INACTIVE`/`NEGATIVE_TUITION` và test gate tương ứng. |
| MAR-CONV-1.0 | 30/06/2026 | Tech Lead / Solution Architect | Chuẩn hóa exception/error/i18n convention theo pattern OASIS, áp dụng JSON error envelope cho MAR REST API. |
