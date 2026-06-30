# LOGGING & OBSERVABILITY CONVENTION - QUY TẮC LOGGING MAR

**Ngày tạo:** 30/06/2026  
**Phiên bản:** MAR-CONV-1.1  
**Tác giả:** Tech Lead / Solution Architect  
**Trạng thái:** Locked for Sprint 1 technical kickoff  
**Stack:** SLF4J, Logback, Spring Boot Actuator, Micrometer, MDC  
**Tham chiếu:**
- `D:\Documents-for-Expert-Design-Database\MAR\architecture\logging_convention.md` - Pattern logging OASIS
- `D:\Documents-for-Expert-Design-Database\MAR\architecture\monitoring_convention.md` - Pattern monitoring/metrics OASIS
- `D:\Documents-for-Expert-Design-Database\MAR\architecture\api_security_convention.md` - Pattern security logging
- `D:\Documents-for-Expert-Design-Database\MAR\our architecture\06-exception-error-i18n-convention.md` - Error/request_id convention

## 1. TỔNG QUAN & MỤC ĐÍCH

Tài liệu này chuẩn hóa application logging và observability tối thiểu cho MAR.

Mục đích:

- Trace được lỗi API bằng `request_id`.
- Debug được theo tenant/user mà không lộ dữ liệu nhạy cảm.
- Tách rõ application log, audit log và metrics.
- Có health/metrics tối thiểu cho QA/staging và sẵn sàng production.
- Tránh spam log, duplicate stack trace và log body chứa PII.

Nguyên tắc ghi nhớ:

> **"Log để điều tra, metrics để quan sát, audit để chịu trách nhiệm."**

## 2. PHẠM VI ÁP DỤNG

Áp dụng cho:

- Mọi backend class dùng SLF4J/Logback.
- Request logging filter và MDC.
- Error logging trong global exception handler.
- Security logging.
- Import/campaign/catalog business event log.
- Spring Boot Actuator, Micrometer metrics.
- Log masking và test log behavior.

Không áp dụng cho:

- Frontend browser logging.
- Business audit table chi tiết, trừ ranh giới với application log.
- Production dashboard cụ thể nếu deployment stack chưa chốt.
- SIEM/centralized log vendor final.

## 3. NGUYÊN TẮC CHUNG

1. **Use SLF4J, not System.out:** mọi class dùng logger chuẩn.
2. **Every request has request_id:** generate nếu client không gửi.
3. **MDC must be cleared:** set context trong filter và clear bằng `finally`.
4. **English log message:** log message trong code dùng tiếng Anh.
5. **No sensitive data:** không log password, token, secret, raw import row, full PII.
6. **Log once per exception:** unexpected error log ở global handler, không log stack trace lặp tầng.
7. **No global body logging:** không bật log full request/response body toàn hệ thống.
8. **Separate audit:** hành động cần accountability ghi audit DB, không chỉ application log.
9. **Measure key flows:** HTTP, DB pool, JVM, auth, import phải có metrics tối thiểu.
10. **Protect actuator:** chỉ health public; metrics/prometheus/info cần bảo vệ theo environment.

## 4. QUY TẮC ĐẶT TÊN

### 4.1. Logger naming

| Logger | Convention | Dùng cho |
|---|---|---|
| Class logger | `@Slf4j` theo FQN class | Application log thường |
| Security logger | `SECURITY` | Auth/authz/security event runtime |
| Performance logger | `PERF` | Slow API/query/job |
| Job logger | `JOB` | Scheduler/async job |
| Audit logger | Không dùng thay audit DB | Chỉ mirror nếu ops cần |

Ưu tiên:

```java
@Slf4j
public class BranchService {
}
```

Named logger chỉ dùng khi có lý do tách kênh log:

```java
private static final Logger SECURITY_LOG = LoggerFactory.getLogger("SECURITY");
```

### 4.2. MDC key naming

MDC key dùng lower camelCase:

| Key | Ý nghĩa |
|---|---|
| `requestId` | ID duy nhất của request |
| `traceId` | Distributed trace id nếu có |
| `tenantId` | Tenant hiện tại |
| `actorId` | User/actor hiện tại |
| `roleCode` | Role code hiện tại |
| `clientIp` | IP client đã normalize |
| `httpMethod` | HTTP method |
| `path` | Request path |
| `jobName` | Tên job nếu không phải HTTP request |

Không đưa email, phone, token, full name hoặc raw business data vào MDC.

### 4.3. Header naming

Incoming request id:

```text
X-Request-Id
```

Response header:

```text
X-Request-Id
```

Nếu client gửi invalid/blank request id, backend generate id mới.

### 4.4. Metric naming

Custom metric dùng prefix:

```text
mar.<domain>.<event>
```

Ví dụ:

```text
mar.auth.login.success
mar.auth.login.failure
mar.import.batch.created
mar.import.row.failed
mar.catalog.course.created
```

Rules:

- Lowercase dot-separated.
- Không đưa tenant/user id vào metric name.
- Dùng tag có cardinality thấp: `status`, `type`, `reason`.
- Tránh tag có cardinality cao: email, phone, request id, file name.

Metric tag registry Sprint 1:

| Tag | Allowed values |
|---|---|
| `status` | `SUCCESS`, `FAILURE` |
| `type` | `CSV`, `GOOGLE_SHEET`, `WEBSITE_FORM`, `META_LEAD_ADS`, `MANUAL` |
| `reason` | `VALIDATION_ERROR`, `DUPLICATE`, `PERMISSION_DENIED`, `SYSTEM_ERROR`, `AUDIT_WRITE_FAILED` |
| `action` | Audit/security action enum đã chốt, ví dụ `PERMISSION_MATRIX_UPDATED`, `USER_STATUS_CHANGED` |

Không để dev tự truyền raw string tự do vào tag `reason`.

## 5. CẤU TRÚC FILE & PACKAGE

### 5.1. Logging package

```text
vn.mar.common.logging
├── LogContext.java
├── LogMasker.java
├── RequestContext.java
├── RequestIdFilter.java
├── MdcFilter.java
├── RequestLoggingFilter.java
├── PerformanceLogger.java
└── SensitiveFields.java
```

### 5.2. Observability package

```text
vn.mar.common.observability
├── ActuatorConfig.java
├── BusinessMetrics.java
├── AuditMetrics.java
├── AuthMetrics.java
├── ImportMetrics.java
└── DatabaseHealthIndicator.java
```

### 5.3. Config files

```text
src/main/resources/logback-spring.xml
src/main/resources/application.yml
src/main/resources/application-dev.yml
src/main/resources/application-prod.yml
```

### 5.4. Separation of concerns

| Concern | Nơi xử lý | Lưu ở đâu |
|---|---|---|
| Application log | SLF4J/Logback | Console/file/central log |
| Audit event | Audit service/table | PostgreSQL `audit_events` |
| Metrics | Micrometer | Actuator/Prometheus |
| Health | Actuator health indicator | Actuator endpoint |
| Error response | GlobalExceptionHandler | API response |

## 6. CÁC PATTERN BẮT BUỘC

### 6.1. RequestId/MDC filter pattern

```java
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestIdFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String requestId = resolveOrGenerateRequestId(request);

        try {
            RequestContext.setRequestId(requestId);
            MDC.put("requestId", requestId);
            MDC.put("httpMethod", request.getMethod());
            MDC.put("path", request.getRequestURI());
            MDC.put("clientIp", resolveClientIp(request));
            response.setHeader("X-Request-Id", requestId);

            filterChain.doFilter(request, response);
        } finally {
            RequestContext.clear();
            MDC.clear();
        }
    }
}
```

Rules:

- `RequestIdFilter` là source duy nhất tạo/propagate request id.
- MDC clear bằng `finally`.
- Request id response phải khớp error response `meta.request_id`.
- `ApiMeta.current()`, `LogContext.requestId()` và `AuditRecordCommand.requestId` phải đọc cùng request id từ `RequestContext`/MDC.
- Authenticated context có thể set sau khi security filter parse token.

### 6.1.1. Filter ordering pattern

Thứ tự filter tối thiểu:

```text
RequestIdFilter / MdcFilter
-> Spring Security / JwtAuthenticationFilter
-> TenantActorMdcEnrichmentFilter
-> Controller/service
-> RequestLoggingFilter final log
```

Rules:

- `RequestIdFilter` phải chạy trước security để lỗi `401/403` vẫn có `X-Request-Id` và `meta.request_id`.
- Tenant/actor/role chỉ enrich sau khi token đã được parse.
- `RequestLoggingFilter` log ở `finally` để có status và duration cuối cùng.

### 6.2. Request logging filter pattern

```java
@Component
@Slf4j
public class RequestLoggingFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain) throws ServletException, IOException {
        long startNanos = System.nanoTime();

        try {
            chain.doFilter(request, response);
        } finally {
            long durationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);
            log.info("http request completed method={} path={} status={} durationMs={}",
                    request.getMethod(),
                    request.getRequestURI(),
                    response.getStatus(),
                    durationMs);
        }
    }
}
```

Không log body mặc định.

Rules:

- Sprint 1 log toàn bộ HTTP request ở `INFO` sau khi hoàn tất request.
- Slow API threshold mặc định: `1000ms`; request vượt ngưỡng log thêm qua `PERF` hoặc structured field `slow=true`.
- Sau pilot/production high-volume, có thể sampling request `2xx` nhưng phải giữ full log cho `4xx`, `5xx` và slow request.

### 6.3. Log level pattern

| Level | Dùng khi | Ví dụ |
|---|---|---|
| `DEBUG` | Chi tiết kỹ thuật khi debug local | Query builder condition |
| `INFO` | Event nghiệp vụ hoặc lifecycle quan trọng | Import batch created |
| `WARN` | Expected nhưng đáng chú ý | Permission denied, validation spike |
| `ERROR` | Unexpected failure cần điều tra | DB unavailable, unhandled exception |

Expected business error không log stack trace ở `ERROR`.

### 6.4. Sensitive data masking pattern

Không log:

- Password.
- JWT/access token/refresh token.
- API key/secret.
- OTP/reset token/invite token.
- Full phone/email nếu không cần.
- Raw import row có PII.
- Payment/bank data.

Mask examples:

```text
phone=090****123
email=a***@domain.com
token=[REDACTED]
```

Utility:

```java
public final class LogMasker {

    private LogMasker() {
    }

    public static String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return "[REDACTED]";
        }
        String[] parts = email.split("@", 2);
        return parts[0].charAt(0) + "***@" + parts[1];
    }
}
```

### 6.5. Business log pattern

Good:

```java
log.info("tenant created tenantId={} actorId={}", tenantId, actorId);
log.info("permission matrix updated tenantId={} roleCode={} actorId={}", tenantId, roleCode, actorId);
log.info("import batch created batchId={} rowCount={}", batchId, rowCount);
```

Bad:

```java
log.info("create done");
log.info("request body={}", request);
```

### 6.6. Exception logging pattern

Expected:

```java
log.warn("business rule violation code={} requestId={}", errorCode, requestId);
```

Unexpected:

```java
log.error("unexpected error requestId={}", requestId, exception);
```

Rules:

- Global handler log unexpected exception once.
- Service không catch/log/rethrow cùng exception nếu không thêm context thật sự.
- Không dùng `printStackTrace`.

### 6.7. Actuator pattern

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  endpoint:
    health:
      show-details: when_authorized
  metrics:
    tags:
      application: mar
```

Access:

- `/actuator/health` có thể public cho load balancer.
- `/actuator/info`, `/actuator/metrics`, `/actuator/prometheus` phải được bảo vệ theo environment.
- Không expose `env`, `configprops`, `heapdump` ra public.

Profile policy:

| Environment | Public | Protected |
|---|---|---|
| Local/dev | `health`, có thể mở `info` nếu không lộ config | `metrics`, `prometheus` tùy dev profile |
| QA/staging | `health` | `info`, `metrics`, `prometheus` |
| Production | `health` hoặc health group tối thiểu cho load balancer | `info`, `metrics`, `prometheus`; không public internet |

Không bật public endpoint mới nếu chưa có decision Ops/Security.

### 6.8. Custom metrics pattern

```java
@Component
@RequiredArgsConstructor
public class ImportMetrics {

    private final MeterRegistry meterRegistry;

    public void recordBatchCreated(String importType) {
        Counter.builder("mar.import.batch.created")
                .tag("type", importType)
                .register(meterRegistry)
                .increment();
    }

    public void recordRowFailed(String reason) {
        Counter.builder("mar.import.row.failed")
                .tag("reason", reason)
                .register(meterRegistry)
                .increment();
    }
}
```

Tag `reason` phải là enum/low-cardinality, không dùng raw message.

Audit failure metrics:

```text
mar.audit.write.success
mar.audit.write.failed
```

Allowed tags:

- `action`
- `status`
- `reason`

Không đưa `tenantId`, `actorId`, `requestId`, email, phone hoặc file name vào metric tag.

### 6.9. Health indicator pattern

Custom health chỉ thêm khi Actuator built-in chưa đủ:

```java
@Component
public class ImportStorageHealthIndicator implements HealthIndicator {

    @Override
    public Health health() {
        boolean available = checkStorage();
        return available
                ? Health.up().withDetail("component", "import-storage").build()
                : Health.down().withDetail("component", "import-storage").build();
    }
}
```

Không log PII hoặc secret trong health detail.

### 6.10. Log format pattern

Local/dev:

- Console pattern readable cho dev.
- Có đủ `level`, `logger`, `message`, `requestId`.

QA/staging/production:

- Ưu tiên JSON log format nếu central logging hỗ trợ.
- Nếu chưa chọn vendor, log vẫn phải structured đủ để parser tách field.

Field tối thiểu:

```text
timestamp
level
logger
message
requestId
tenantId
actorId
roleCode
httpMethod
path
status
durationMs
exception_class
```

`exception_class` chỉ có khi lỗi; không log raw stack trace vào field message của expected business error.

## 7. QUY TẮC RIÊNG CỦA MAR OBSERVABILITY

### 7.1. MDC tối thiểu

Mỗi HTTP request phải có:

```text
requestId
httpMethod
path
clientIp
```

Sau authentication, nếu có:

```text
tenantId
actorId
roleCode
```

### 7.1.1. Request id propagation rule

Luồng request id:

```text
X-Request-Id
-> RequestIdFilter
-> RequestContext + MDC
-> ApiMeta.current()
-> LogContext.requestId()
-> AuditRecordCommand.requestId
```

Rules:

- Không có nguồn request id thứ hai trong handler/service.
- `meta.request_id`, MDC `requestId`, audit `request_id` và response header `X-Request-Id` phải giống nhau trong cùng request.
- Nếu request id incoming invalid/blank, backend generate id mới và trả lại ở response header.

### 7.2. Metrics tối thiểu Sprint 1

| Metric | Type | Ghi chú |
|---|---|---|
| `http.server.requests` | timer | Spring Boot built-in |
| `jdbc.connections.active` | gauge | Hikari/Micrometer |
| `jvm.memory.used` | gauge | Built-in |
| `mar.auth.login.success` | counter | Nếu MAR owns auth |
| `mar.auth.login.failure` | counter | Nếu MAR owns auth |
| `mar.import.batch.created` | counter | Khi import foundation có API |
| `mar.import.row.failed` | counter | Khi row validation có API |
| `mar.audit.write.success` | counter | Audit write thành công |
| `mar.audit.write.failed` | counter | Audit write lỗi |

### 7.3. Alert candidates

Không nhất thiết implement hết Sprint 1, nhưng metrics/log phải hỗ trợ:

- 5xx rate cao.
- P95 latency vượt ngưỡng.
- DB connection pool gần cạn.
- Login failure spike.
- Import row failure spike.
- Flyway migration failed.
- Audit write failed.

### 7.4. SQL logging

Dev:

- Có thể bật SQL log ngắn hạn để debug.

Staging/production:

- Không bật SQL/bind parameter verbose mặc định.
- Không log query chứa PII.
- Slow query nên log qua performance logger với metadata, không log raw parameter nhạy cảm.

### 7.5. Audit vs application log

Application log có thể xoay vòng/xóa theo policy vận hành.

Audit event là record accountability:

- Ai làm?
- Làm gì?
- Khi nào?
- Tenant nào?
- Resource nào?
- Trước/sau nếu cần?

Không thay audit DB bằng application log.

### 7.6. Sampling and slow request rule

Sprint 1:

- Log all HTTP request completion ở `INFO`.
- Log slow request khi `durationMs >= 1000`.
- Luôn log đầy đủ `4xx`, `5xx`, security denied và audit write failed.

Sau pilot:

- Có thể sampling `2xx` high-volume nếu Ops/Tech Lead chốt.
- Không sampling error, slow request hoặc security/audit failure.

### 7.7. Security log vs audit rule

- Application security log ở `WARN` cho permission denied/security denied.
- Audit chỉ ghi với endpoint nhạy cảm hoặc cross-tenant access theo `08-audit-convention.md`.
- Không log/audit duplicate vô nghĩa cho mọi request denied nếu gây noise; action nào cần accountability thì ghi audit DB.

## 8. VÍ DỤ CODE MẪU

### 8.1. Good example - service log đủ context

```java
@Transactional
public ImportBatchResponse createImportBatch(CreateImportBatchRequest request) {
    ImportBatch batch = importBatchRepository.save(ImportBatch.create(...));
    log.info("import batch created batchId={} tenantId={} rowCount={}",
            batch.getId(),
            batch.getTenantId(),
            batch.getRowCount());
    importMetrics.recordBatchCreated(batch.getImportType().name());
    return importBatchMapper.toResponse(batch);
}
```

### 8.2. Good example - security denied log

```java
SECURITY_LOG.warn("permission denied actorId={} tenantId={} permission={}",
        actorId,
        tenantId,
        permissionCode);
```

### 8.3. Bad example - PII/body logging

```java
log.info("login request email={} password={}", request.email(), request.password());
log.info("import row={}", rawRow);
log.info("authorization={}", authorizationHeader);
```

Vấn đề:

- Log password/token.
- Raw import row có thể chứa PII.
- Log có thể tồn tại lâu hơn dữ liệu nghiệp vụ.

## 9. ANTI-PATTERNS CẦN TRÁNH

Không được:

- Dùng `System.out.println`.
- Log full request/response body toàn cục.
- Log token/password/secret/raw import row.
- Đưa PII vào MDC.
- Log cùng exception stack trace ở nhiều tầng.
- Swallow exception mà không trả error/log phù hợp.
- Bật DEBUG security/web/SQL ở production mặc định.
- Expose actuator `env`, `configprops`, `heapdump` public.
- Tạo metric name chứa tenant/user/request id.
- Dùng tag metric có cardinality cao như `requestId`, email, file name.
- Để `ApiMeta.current()` và `LogContext.requestId()` đọc từ hai nguồn khác nhau.
- Để security `401/403` chạy trước `RequestIdFilter`.
- Dùng application log thay audit log.

## 10. TESTING CONVENTIONS

### 10.1. MDC filter test

Test:

- Generate `requestId` nếu thiếu header.
- Preserve valid `X-Request-Id` nếu client gửi.
- Response có `X-Request-Id`.
- MDC clear sau request.
- `ApiMeta.current().requestId()` và `LogContext.requestId()` cùng giá trị.

### 10.2. Error request_id test

API error test phải assert:

- `$.meta.request_id` tồn tại.
- `X-Request-Id` response header khớp hoặc trace được.
- Security error `401/403` cũng có `meta.request_id`.

### 10.3. Log masking test

Unit test cho `LogMasker`:

- Email masking.
- Phone masking.
- Token redact.
- Null/invalid input không throw.

### 10.4. Actuator security test

Test:

- `/actuator/health` accessible theo config environment.
- `/actuator/metrics` không public nếu policy yêu cầu protected.
- `/actuator/prometheus` không public ở QA/staging/production nếu chưa có gateway bảo vệ.
- Không expose endpoint nguy hiểm.

### 10.5. Metrics test

Với custom metrics:

- Counter increment khi event xảy ra.
- Tag low-cardinality đúng.
- Không tạo metric mới theo dynamic id.
- `mar.audit.write.failed` increment khi audit write lỗi.
- Metric tag `reason` nằm trong enum registry.

## 11. CODE REVIEW CHECKLIST

- [ ] Class dùng SLF4J/Logback, không dùng `System.out`.
- [ ] Log message bằng tiếng Anh.
- [ ] Request có `requestId`.
- [ ] Error response có `request_id`.
- [ ] `ApiMeta.current()` và `LogContext.requestId()` dùng cùng request id.
- [ ] `RequestIdFilter` chạy trước security/JWT filter.
- [ ] MDC có tenant/actor khi authenticated.
- [ ] MDC được clear.
- [ ] Log format production đủ field structured/JSON.
- [ ] Không log password/token/secret/raw import row.
- [ ] Không log full body mặc định.
- [ ] Slow request threshold/sampling policy không làm mất log error/security.
- [ ] Expected exception không spam stack trace.
- [ ] Unexpected exception log một lần ở handler.
- [ ] Actuator endpoint được bảo vệ đúng.
- [ ] Custom metric dùng prefix `mar.`.
- [ ] Metric tag không high-cardinality.
- [ ] Metric tag `reason` dùng enum registry.
- [ ] Audit failure có metric `mar.audit.write.failed`.
- [ ] Audit event không bị thay bằng application log.

## 12. TÀI LIỆU LIÊN QUAN

- `03-rest-api-convention.md` - `X-Request-Id` và API envelope.
- `05-security-auth-authz-convention.md` - Security logging và sensitive data.
- `06-exception-error-i18n-convention.md` - Error logging và request_id.
- `08-audit-convention.md` - Audit event DB.
- `09-testing-quality-convention.md` - Test logging/observability.
- OASIS reference: `logging_convention.md`, `monitoring_convention.md`, `api_security_convention.md`.

## 13. LỊCH SỬ CẬP NHẬT

| Phiên bản | Ngày | Người cập nhật | Nội dung |
|---|---|---|---|
| MAR-CONV-1.1 | 30/06/2026 | Tech Lead / Solution Architect | Chốt `RequestIdFilter` là source duy nhất cho request id, bổ sung filter ordering, log format theo environment, sampling/slow request policy, metric tag registry, audit write metrics và observability tests. |
| MAR-CONV-1.0 | 30/06/2026 | Tech Lead / Solution Architect | Chuẩn hóa logging/observability convention theo pattern OASIS, map sang REST API tenant-aware của MAR. |
