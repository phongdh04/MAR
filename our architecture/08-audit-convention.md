# AUDIT CONVENTION - QUY TẮC AUDIT MAR

**Ngày tạo:** 30/06/2026  
**Phiên bản:** MAR-CONV-1.1  
**Tác giả:** Tech Lead / Solution Architect  
**Trạng thái:** Locked for Sprint 1 technical kickoff  
**Stack:** Spring Boot, Spring Data JPA, PostgreSQL JSONB, Application Events optional  
**Tham chiếu:**
- `D:\Documents-for-Expert-Design-Database\MAR\architecture\audit_convention.md` - Pattern audit OASIS
- `D:\Documents-for-Expert-Design-Database\MAR\architecture\logging_convention.md` - Ranh giới logging/audit
- `D:\Documents-for-Expert-Design-Database\MAR\architecture\api_security_convention.md` - Security audit event
- `D:\Documents-for-Expert-Design-Database\MAR\our architecture\07-logging-observability-convention.md` - Logging/observability MAR

## 1. TỔNG QUAN & MỤC ĐÍCH

Tài liệu này chuẩn hóa audit trail cho MAR, tập trung vào các thay đổi cấu hình, quyền, user, tenant, catalog và import có ảnh hưởng đến Lead-to-Enrollment workflow.

Mục đích:

- Trả lời được ai đã làm gì, khi nào, ở tenant nào, trên resource nào.
- Tách audit event khỏi application log.
- Cung cấp bằng chứng cho QA/UAT khi kiểm tra permission và setup nhạy cảm.
- Chuẩn bị nền cho audit query/export sau Sprint 1.
- Tránh ghi audit payload chứa password/token/PII không cần thiết.

Nguyên tắc ghi nhớ:

> **"Audit không phải log debug; audit là lịch sử trách nhiệm."**

## 2. PHẠM VI ÁP DỤNG

Áp dụng cho:

- Audit event database table.
- Audit service và audit command.
- Security event audit.
- Permission matrix/user/role/branch assignment changes.
- Tenant/branch/catalog/import sensitive changes.
- Audit query API nếu được đưa vào scope.
- Test audit cho các flow P0/P1.

Không áp dụng cho:

- Application debug log.
- Frontend clickstream/page view analytics.
- BI/event tracking marketing.
- Legal retention/export policy cuối cùng nếu chưa được PO/legal chốt.

## 3. NGUYÊN TẮC CHUNG

1. **Insert-only:** audit event chỉ insert, không update/delete qua app.
2. **Capture 5W:** who, what, when, where, why nếu có.
3. **Tenant-aware:** event tenant-scoped phải có `tenant_id`.
4. **Actor snapshot:** lưu role/context tại thời điểm hành động.
5. **Resource identifiable:** action phải có resource type và resource id/key nếu có.
6. **Payload minimal:** before/after chỉ chứa field cần thiết.
7. **No secrets:** không audit password, token, secret, OTP, raw import full row.
8. **Business transaction clarity:** flow nhạy cảm nên ghi audit cùng transaction với business change.
9. **Query restricted:** audit chỉ cho role/permission được duyệt.
10. **Application log is not audit:** log có thể hỗ trợ debug, không thay thế audit DB.

## 4. QUY TẮC ĐẶT TÊN

### 4.1. Audit action naming

Audit action dùng UPPER_SNAKE_CASE, dạng:

```text
<RESOURCE>_<ACTION>
```

Ví dụ:

```text
TENANT_CREATED
TENANT_STATUS_CHANGED
BRANCH_CREATED
BRANCH_STATUS_CHANGED
USER_CREATED
USER_STATUS_CHANGED
USER_BRANCH_ASSIGNED
ROLE_CREATED
ROLE_PERMISSION_UPDATED
PERMISSION_MATRIX_UPDATED
LANGUAGE_CREATED
PROGRAM_UPDATED
COURSE_STATUS_CHANGED
IMPORT_BATCH_CREATED
IMPORT_BATCH_VALIDATED
IMPORT_BATCH_CONFIRMED
DATA_EXPORTED
IMPORT_BATCH_FIXTURE_CREATED
AUDIT_EVENT_VIEWED
LOGIN_SUCCESS
LOGIN_FAILED
PERMISSION_DENIED
CROSS_TENANT_ACCESS_DENIED
```

Sprint 1 permission action:

- `PERMISSION_MATRIX_UPDATED` là action chính cho permission model dùng `permission_profiles`.
- `ROLE_PERMISSION_UPDATED` chỉ reserved nếu sau này có model `role_permissions` riêng được approve.
- Không ghi cả hai action cho cùng một thao tác permission matrix.

### 4.2. Resource type naming

Resource type dùng UPPER_SNAKE_CASE:

```text
TENANT
BRANCH
USER
ROLE
PERMISSION
LANGUAGE
PROGRAM
COURSE
IMPORT_BATCH
IMPORT_ROW
AUDIT_EVENT
AUTH_SESSION
```

### 4.3. Class naming

| Loại | Convention | Ví dụ |
|---|---|---|
| Entity | `AuditEvent` | `AuditEvent` |
| Repository | `AuditEventRepository` | `AuditEventRepository` |
| Write service | `AuditService` | `AuditService` |
| Query service | `AuditQueryService` | `AuditQueryService` |
| Command | `AuditRecordCommand` | `AuditRecordCommand` |
| Action enum | `AuditAction` | `AuditAction` |
| Resource enum | `AuditResourceType` | `AuditResourceType` |
| Annotation optional | `Auditable` | `@Auditable` |
| Event optional | `AuditApplicationEvent` | `AuditApplicationEvent` |

### 4.4. Audit naming decision

| Layer | Naming |
|---|---|
| Business/BA term | `AuditLog` |
| DB table | `audit_events` |
| Java entity | `AuditEvent` |
| Service | `AuditService` |
| API resource | `/api/v1/audit-events` |

Rules:

- `AuditLog` trong BA/ticket cũ được hiểu là audit event append-only.
- Không tạo đồng thời `audit_logs` và `audit_events`.
- Nếu tài liệu cũ còn nói `audit_logs`, implementation Sprint 1 vẫn dùng `audit_events` theo convention này.

### 4.5. Field naming

DB columns dùng snake_case:

```text
audit_event_id
tenant_id
actor_id
actor_type
actor_role
action
resource_type
resource_id
before_data
after_data
request_id
created_at
```

Java fields dùng camelCase:

```java
private UUID auditEventId;
private UUID tenantId;
private UUID actorId;
private ActorType actorType;
private String actorRole;
```

## 5. CẤU TRÚC FILE & PACKAGE

### 5.1. Audit module package

```text
vn.mar.audit
├── controller
│   └── AuditEventController.java
├── dto
│   ├── request
│   │   └── AuditEventSearchRequest.java
│   └── response
│       └── AuditEventResponse.java
├── entity
│   └── AuditEvent.java
├── repository
│   └── AuditEventRepository.java
├── service
│   ├── AuditService.java
│   └── AuditQueryService.java
├── model
│   ├── AuditAction.java
│   ├── ActorType.java
│   ├── AuditResourceType.java
│   └── AuditRecordCommand.java
└── mapper
    └── AuditEventMapper.java
```

### 5.2. Common audit helper

Chỉ tạo nếu dùng ở nhiều module:

```text
vn.mar.common.audit
├── Auditable.java
├── AuditApplicationEvent.java
└── AuditPayloadSanitizer.java
```

Không đưa business audit rule riêng của `user`, `catalog`, `leadimport` vào `common`.

### 5.3. Migration file

```text
src/main/resources/db/migration
└── V20260630_05__create_audit_events.sql
```

### 5.4. API package

Audit query API nếu được mở:

```text
GET /api/v1/audit-events
GET /api/v1/audit-events/{audit_event_id}
```

Audit API mặc định read-only; không có update/delete.

## 6. CÁC PATTERN BẮT BUỘC

### 6.1. Audit table pattern

```sql
create table audit_events (
    audit_event_id uuid not null,
    tenant_id uuid,
    actor_id uuid,
    actor_type varchar(32) not null default 'USER',
    actor_role varchar(50),
    action varchar(100) not null,
    resource_type varchar(100) not null,
    resource_id uuid,
    resource_key varchar(255),
    before_data jsonb,
    after_data jsonb,
    metadata jsonb,
    reason text,
    request_id varchar(100),
    client_ip varchar(100),
    user_agent varchar(500),
    created_at timestamptz not null default now(),
    constraint pk_audit_events primary key (audit_event_id)
);

create index idx_audit_events__tenant_resource_created
on audit_events (tenant_id, resource_type, resource_id, created_at desc);

create index idx_audit_events__actor_created
on audit_events (actor_id, created_at desc);

create index idx_audit_events__actor_type_created
on audit_events (actor_type, created_at desc);

create index idx_audit_events__action_created
on audit_events (action, created_at desc);

create index idx_audit_events__request_id
on audit_events (request_id);
```

Rules:

- Table append-only by application convention.
- Không tạo endpoint update/delete.
- Nếu production DB user policy cho phép, revoke UPDATE/DELETE với app user.

### 6.2. Audit entity pattern

```java
@Entity
@Table(name = "audit_events")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AuditEvent {

    @Id
    @Column(name = "audit_event_id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "tenant_id")
    private UUID tenantId;

    @Column(name = "actor_id")
    private UUID actorId;

    @Enumerated(EnumType.STRING)
    @Column(name = "actor_type", nullable = false, length = 32)
    private ActorType actorType;

    @Column(name = "actor_role", length = 50)
    private String actorRole;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, length = 100)
    private AuditAction action;

    @Enumerated(EnumType.STRING)
    @Column(name = "resource_type", nullable = false, length = 100)
    private AuditResourceType resourceType;

    @Column(name = "resource_id")
    private UUID resourceId;

    @Column(name = "resource_key", length = 255)
    private String resourceKey;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "before_data", columnDefinition = "jsonb")
    private JsonNode beforeData;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "after_data", columnDefinition = "jsonb")
    private JsonNode afterData;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
```

Entity không expose setter public cho fields sau khi tạo.

### 6.3. Audit command pattern

```java
public record AuditRecordCommand(
        UUID tenantId,
        UUID actorId,
        ActorType actorType,
        String actorRole,
        AuditAction action,
        AuditResourceType resourceType,
        UUID resourceId,
        String resourceKey,
        JsonNode beforeData,
        JsonNode afterData,
        JsonNode metadata,
        String reason,
        String requestId,
        String clientIp,
        String userAgent
) {
}
```

### 6.4. Audit service pattern

```java
public interface AuditService {

    void record(AuditRecordCommand command);
}
```

```java
@Service
@RequiredArgsConstructor
public class DefaultAuditService implements AuditService {

    private final AuditEventRepository auditEventRepository;
    private final AuditPayloadSanitizer sanitizer;

    @Override
    public void record(AuditRecordCommand command) {
        AuditRecordCommand safeCommand = sanitizer.sanitize(command);
        AuditEvent event = AuditEvent.from(safeCommand);
        auditEventRepository.save(event);
    }
}
```

Rules:

- Audit payload phải sanitize trước khi save.
- Sensitive flow nên ghi audit cùng transaction.
- Nếu audit failure xảy ra ở sensitive setup write, phải rollback business transaction.
- Security event audit failure không được làm grant access; log `ERROR` và tăng metric `mar.audit.write.failed`.
- Audit success/failure nên tăng `mar.audit.write.success` hoặc `mar.audit.write.failed`.

Actor type:

| Actor type | Dùng khi |
|---|---|
| `USER` | Tenant user bình thường |
| `PLATFORM_ADMIN` | Platform admin thao tác cross-tenant/platform-level |
| `SYSTEM` | System job/scheduler/bootstrap |
| `INTEGRATION` | Webhook/integration/API client |

### 6.5. Explicit audit pattern

Sprint 1 ưu tiên explicit audit trong service để dễ đọc và dễ test:

```java
@Transactional
public UserDetailResponse updateUserStatus(UUID userId, UpdateUserStatusRequest request) {
    User user = userRepository.findByIdAndTenantId(userId, currentUserContext.currentTenantId())
            .orElseThrow(() -> new ResourceNotFoundException("USER_NOT_FOUND"));

    UserStatus beforeStatus = user.getStatus();
    user.changeStatus(request.status());

    auditService.record(AuditRecordCommand.builder()
            .tenantId(user.getTenantId())
            .actorId(currentUserContext.currentActorId())
            .actorType(ActorType.USER)
            .actorRole(currentUserContext.currentUser().roleCode())
            .action(AuditAction.USER_STATUS_CHANGED)
            .resourceType(AuditResourceType.USER)
            .resourceId(user.getId())
            .beforeData(json.object("status", beforeStatus))
            .afterData(json.object("status", user.getStatus()))
            .reason(request.reason())
            .requestId(LogContext.requestId())
            .build());

    return userMapper.toDetailResponse(user);
}
```

### 6.6. Annotation audit pattern optional

Sau Sprint 1 có thể dùng annotation nếu nhiều use case lặp:

```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Auditable {

    AuditAction action();

    AuditResourceType resourceType();

    String resourceIdParam() default "";

    boolean captureBeforeAfter() default false;
}
```

Rules:

- Không dùng annotation nếu before/after khó hiểu hoặc khó test.
- Không để AOP tự capture quá nhiều payload.
- Annotation phải có integration test.

### 6.7. Security audit pattern

Security events ghi từ auth/security handler:

```java
auditService.record(AuditRecordCommand.builder()
        .tenantId(currentTenantId)
        .actorId(actorId)
        .actorType(ActorType.USER)
        .actorRole(roleCode)
        .action(AuditAction.PERMISSION_DENIED)
        .resourceType(AuditResourceType.PERMISSION)
        .resourceKey(permissionCode)
        .metadata(json.object("path", path, "method", method))
        .requestId(requestId)
        .clientIp(clientIp)
        .build());
```

Không ghi raw token/password trong metadata.

### 6.8. Query pattern

Audit query phải phân trang và filter rõ:

```text
GET /api/v1/audit-events?resource_type=USER&actor_id=...&from=...&to=...&page=0&size=20
```

Rules:

- Chỉ role/permission được duyệt mới query.
- Tenant user chỉ xem audit tenant của mình.
- Platform admin endpoint phải có permission riêng.
- Response không trả payload nhạy cảm đã bị sanitize.
- Audit query API là read-only, tenant-scoped và luôn phân trang.
- Nếu audit query cần theo dõi, ghi `AUDIT_EVENT_VIEWED` sau Sprint 1 theo decision riêng.

### 6.9. Retention and immutability pattern

Sprint 1:

- Audit table append-only theo application convention.
- Không expose update/delete API.
- Retention production mặc định phải đồng bộ với BA baseline.

Retention mặc định cho MAR MVP:

- Non-production: retention theo ops cleanup.
- Production/pilot: giữ tối thiểu 24 tháng, trừ khi PO/Legal/Ops approve retention khác bằng decision riêng.
- Nếu contract/legal yêu cầu cao hơn 24 tháng thì theo contract/legal.

Hash chain/tamper detection:

- Không bắt buộc Sprint 1.
- Nếu yêu cầu compliance cao hơn, mở decision mới và bổ sung `event_hash`, `previous_event_hash`.

## 7. QUY TẮC RIÊNG CỦA MAR AUDIT

### 7.1. Must audit Sprint 1

Bắt buộc audit:

- `PERMISSION_MATRIX_UPDATED`
- `USER_CREATED`
- `USER_STATUS_CHANGED`
- `USER_BRANCH_ASSIGNED`
- `TENANT_STATUS_CHANGED`
- `BRANCH_STATUS_CHANGED`
- `COURSE_STATUS_CHANGED` nếu ảnh hưởng import/enrollment downstream.
- `LOGIN_SUCCESS` / `LOGIN_FAILED` nếu MAR owns auth.
- `PERMISSION_DENIED` với endpoint nhạy cảm.
- `CROSS_TENANT_ACCESS_DENIED` nếu phát hiện explicit.
- `DATA_EXPORTED` nếu có export dữ liệu khách hàng/lead/user.

`ROLE_PERMISSION_UPDATED` không thuộc Must audit Sprint 1 nếu hệ thống chưa implement `role_permissions`; dùng `PERMISSION_MATRIX_UPDATED` cho permission matrix dựa trên `permission_profiles`.

### 7.2. Should audit Sprint 1

Nên audit:

- `TENANT_CREATED`
- `BRANCH_CREATED`
- `LANGUAGE_CREATED`
- `PROGRAM_UPDATED`
- `IMPORT_BATCH_CREATED`
- `IMPORT_BATCH_VALIDATED`
- Import summary changed.
- `IMPORT_BATCH_FIXTURE_CREATED` nếu dùng fixture script/import fixture trong Sprint 1.
- `AUDIT_EVENT_VIEWED` nếu audit query được xem là thao tác nhạy cảm cần theo dõi.

### 7.3. Not required Sprint 1

Chưa bắt buộc:

- Full hash chain.
- Legal export workflow.
- Audit UI đầy đủ nếu PO chưa yêu cầu.
- Audit mọi read request.
- Audit từng row import thành công.

### 7.4. Audit failure behavior rule

| Flow | Nếu audit write fail |
|---|---|
| Permission matrix update | Rollback business transaction |
| User status change/block user | Rollback business transaction |
| Tenant/branch status change | Rollback business transaction |
| Role/permission change nếu có | Rollback business transaction |
| Login success/failed audit | Không block login chỉ vì audit lỗi, nhưng log `ERROR` và tăng `mar.audit.write.failed` |
| Permission denied/cross-tenant denied audit | Không biến request thành grant access; giữ `403/404`, log `ERROR` và tăng `mar.audit.write.failed` |
| Export data | Rollback hoặc block export nếu audit không ghi được |

Rule chính:

- Sensitive configuration write cần accountability mạnh thì audit fail phải rollback.
- Security denied/login event là telemetry/accountability bổ sung; audit fail không được làm lộ quyền hoặc grant access.
- Behavior khác bảng này phải có decision riêng trước khi code.

### 7.5. Before/after payload rule

Payload chỉ chứa field liên quan:

Good:

```json
{
  "status": "ACTIVE",
  "role_code": "ADMIN",
  "branch_ids": ["..."]
}
```

Bad:

```json
{
  "password_hash": "...",
  "access_token": "...",
  "full_import_row": {...}
}
```

### 7.6. Reason rule

`reason` optional mặc định.

Bắt buộc nhập reason khi:

- Deactivate tenant.
- Block/deactivate user.
- Permission downgrade/upgrade lớn.
- Bulk rollback/import reversal.

### 7.7. Request id traceability rule

Audit request id phải đến từ cùng source với logging/error:

```text
RequestIdFilter -> RequestContext/MDC -> LogContext.requestId() -> AuditRecordCommand.requestId
```

Rules:

- Audit `request_id`, application log `requestId`, error `meta.request_id` và response header `X-Request-Id` phải trace được cùng một request.
- Không tự sinh request id mới trong `AuditService`.
- System job không có HTTP request phải tạo correlation id/job id riêng và đưa vào `request_id` hoặc `metadata.correlation_id`.

## 8. VÍ DỤ CODE MẪU

### 8.1. Good example - permission matrix update

```java
@Transactional
public PermissionMatrixResponse updateMatrix(UpdatePermissionMatrixRequest request) {
    PermissionMatrixSnapshot before = permissionMatrixService.snapshot(request.roleCode());

    PermissionMatrix matrix = permissionMatrixService.apply(request);

    auditService.record(AuditRecordCommand.builder()
            .tenantId(currentUserContext.currentTenantId())
            .actorId(currentUserContext.currentActorId())
            .actorType(ActorType.USER)
            .actorRole(currentUserContext.currentUser().roleCode())
            .action(AuditAction.PERMISSION_MATRIX_UPDATED)
            .resourceType(AuditResourceType.PERMISSION)
            .resourceKey(request.roleCode())
            .beforeData(toJson(before))
            .afterData(toJson(matrix.snapshot()))
            .reason(request.reason())
            .requestId(LogContext.requestId())
            .build());

    return permissionMapper.toResponse(matrix);
}
```

### 8.2. Good example - import batch audit

```java
auditService.record(AuditRecordCommand.builder()
        .tenantId(batch.getTenantId())
        .actorId(currentUserContext.currentActorId())
        .actorType(ActorType.USER)
        .actorRole(currentUserContext.currentUser().roleCode())
        .action(AuditAction.IMPORT_BATCH_CREATED)
        .resourceType(AuditResourceType.IMPORT_BATCH)
        .resourceId(batch.getId())
        .afterData(json.object(
                "import_type", batch.getImportType(),
                "row_count", batch.getRowCount(),
                "status", batch.getStatus()))
        .requestId(LogContext.requestId())
        .build());
```

### 8.3. Bad example - audit unsafe payload

```java
auditService.record(AuditRecordCommand.builder()
        .action(AuditAction.USER_CREATED)
        .afterData(objectMapper.valueToTree(createUserRequest))
        .build());
```

Vấn đề:

- Có thể ghi password/token/raw PII từ request.
- Thiếu tenant/actor/resource.
- Payload quá rộng, khó review.

## 9. ANTI-PATTERNS CẦN TRÁNH

Không được:

- Ghi raw password/token/secret vào audit.
- Ghi full request body làm audit payload.
- Chỉ ghi message `"updated"` mà thiếu action/resource/actor.
- Cho user thường update/delete audit event.
- Dùng application log thay audit DB.
- Audit từ frontend.
- Audit mọi request đọc gây noise.
- Ghi audit ngoài transaction cho permission/user status khi chưa rõ failure handling.
- Bỏ audit cho permission matrix update.
- Ghi đồng thời `ROLE_PERMISSION_UPDATED` và `PERMISSION_MATRIX_UPDATED` cho cùng một change Sprint 1.
- Tạo thêm table `audit_logs` trong khi convention đã chốt `audit_events`.
- Sinh request id riêng trong audit service làm lệch log/error/audit trace.
- Bỏ qua metric `mar.audit.write.failed` khi audit write lỗi.
- Query audit không phân trang.

## 10. TESTING CONVENTIONS

### 10.1. Audit service test

Test:

- `record` insert audit event.
- Payload sanitizer loại bỏ/mask sensitive field.
- Missing required action/resource bị reject.
- `created_at` được set.
- `actor_type` được set đúng theo actor source.
- Audit metric success/failure được tăng đúng nếu đã implement metrics.

### 10.2. Sensitive flow integration test

Flow P0/P1 cần verify audit:

- Update permission matrix tạo audit.
- Change user status tạo audit.
- Assign user branch tạo audit.
- Change tenant/branch status tạo audit.
- Permission denied tạo audit nếu flow đã implement.

### 10.3. Transaction behavior test

Với flow audit cùng transaction:

- Business save thành công + audit save thành công -> commit.
- Permission matrix update + audit save fail -> rollback.
- User status change + audit save fail -> rollback.
- Tenant/branch status change + audit save fail -> rollback.
- Permission denied/login audit fail -> không grant access, log `ERROR`, tăng `mar.audit.write.failed`.

### 10.4. Query permission test

Test:

- Admin/CEO hoặc permission `audit.view` xem được.
- Advisor/Marketing không có quyền bị `403`.
- Tenant user không xem audit tenant khác.

### 10.5. Payload safety test

Test response/audit record không chứa:

- `password`
- `token`
- `secret`
- Raw import full row nếu không cần

### 10.6. Naming/retention traceability test

Review/test checklist:

- Repository/entity dùng `audit_events`/`AuditEvent`, không có `audit_logs`.
- Audit event `request_id` khớp với `X-Request-Id`/MDC trong integration test quan trọng.
- Retention policy production đọc từ config/ops decision, default document là tối thiểu 24 tháng.

## 11. CODE REVIEW CHECKLIST

- [ ] Sensitive setup change có audit.
- [ ] Permission/role/user status change có audit.
- [ ] Audit event có tenant, actor, action, resource.
- [ ] Audit event có `actor_type`.
- [ ] Action/resource type đúng enum.
- [ ] Permission matrix Sprint 1 dùng `PERMISSION_MATRIX_UPDATED`, không ghi trùng `ROLE_PERMISSION_UPDATED`.
- [ ] Before/after payload tối thiểu và an toàn.
- [ ] Không ghi password/token/secret/raw import row.
- [ ] Audit write failure behavior rõ.
- [ ] Sensitive configuration write rollback nếu audit write fail.
- [ ] Security/login audit fail log `ERROR` và tăng `mar.audit.write.failed`.
- [ ] Audit `request_id` cùng nguồn với logging/error.
- [ ] Audit retention production mặc định tối thiểu 24 tháng hoặc có decision override.
- [ ] Audit API read-only.
- [ ] Audit query có permission và pagination.
- [ ] Tenant isolation áp dụng cho audit query.
- [ ] Test verify audit cho flow P0/P1.
- [ ] Application log không bị dùng thay audit DB.

## 12. TÀI LIỆU LIÊN QUAN

- `04-database-flyway-convention.md` - PostgreSQL/JSONB/migration cho audit table.
- `05-security-auth-authz-convention.md` - Security event và permission query audit.
- `06-exception-error-i18n-convention.md` - Error/security event mapping.
- `07-logging-observability-convention.md` - Ranh giới application log vs audit.
- `09-testing-quality-convention.md` - Test audit flow.
- OASIS reference: `audit_convention.md`, `logging_convention.md`, `api_security_convention.md`.

## 13. LỊCH SỬ CẬP NHẬT

| Phiên bản | Ngày | Người cập nhật | Nội dung |
|---|---|---|---|
| MAR-CONV-1.1 | 30/06/2026 | Tech Lead / Solution Architect | Đồng bộ audit retention tối thiểu 24 tháng, chốt naming `AuditLog` BA -> `audit_events` DB, bổ sung `actor_type`, audit failure behavior theo flow, audit metrics và request-id traceability. |
| MAR-CONV-1.0 | 30/06/2026 | Tech Lead / Solution Architect | Chuẩn hóa audit convention theo pattern OASIS, map sang audit event append-only cho MAR Sprint 1. |
