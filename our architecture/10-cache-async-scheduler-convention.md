# CACHE, ASYNC & SCHEDULER CONVENTION - QUY TẮC CACHE/JOB MAR

**Ngày tạo:** 30/06/2026  
**Phiên bản:** MAR-CONV-1.1  
**Tác giả:** Tech Lead / Solution Architect  
**Trạng thái:** Locked for Sprint 1 technical kickoff  
**Stack:** Spring Cache, Caffeine optional, Spring ApplicationEvent, Spring `@Async`, Spring `@Scheduled`, ShedLock optional  
**Tham chiếu:**
- `D:\Documents-for-Expert-Design-Database\MAR\architecture\cache_convention.md` - Pattern cache OASIS
- `D:\Documents-for-Expert-Design-Database\MAR\architecture\scheduler_convention.md` - Pattern scheduler OASIS
- `D:\Documents-for-Expert-Design-Database\MAR\architecture\notification_convention.md` - Pattern async notification
- `D:\Documents-for-Expert-Design-Database\MAR\architecture\workflow_convention.md` - Pattern workflow/status transition

## 1. TỔNG QUAN & MỤC ĐÍCH

Tài liệu này chuẩn hóa cách dùng cache, async event và scheduler trong MAR.

Mục đích:

- Tránh đưa Redis/Kafka/job framework vào quá sớm khi Sprint 1 chưa cần.
- Vẫn có convention đầy đủ nếu phải cache permission/catalog hoặc chạy cleanup job.
- Đảm bảo cache có tenant key, TTL và invalidation.
- Đảm bảo async/job không phá transaction correctness.
- Đảm bảo scheduler idempotent, có lock khi multi-instance và có observability.

Nguyên tắc ghi nhớ:

> **"Cache để giảm tải, async để tách side effect, scheduler để tự động hóa; cả ba không được che lỗi nghiệp vụ."**

## 2. PHẠM VI ÁP DỤNG

Áp dụng cho:

- Spring Cache và local cache.
- Permission/catalog/system config cache.
- Spring ApplicationEvent.
- `@Async` executor.
- Scheduled job nếu phát sinh.
- Cleanup token/import draft/reminder/report snapshot.
- Job metrics/logging.
- Test cache invalidation, async handler, scheduler idempotency.

Không áp dụng cho:

- Distributed event streaming enterprise chưa có consumer.
- Workflow engine phức tạp ngoài scope Sprint 1.
- External queue/Kafka nếu chưa có quyết định kiến trúc.
- Frontend cache/browser cache.

## 3. NGUYÊN TẮC CHUNG

1. **Start local:** Sprint 1 ưu tiên local cache nếu thật sự cần.
2. **Tenant in key:** dữ liệu tenant-scoped phải có `tenant_id` trong cache key.
3. **TTL required:** cache nào cũng phải có TTL hoặc invalidation rõ.
4. **Permission cache must evict:** đổi permission matrix phải evict cache liên quan.
5. **Cache only stable reads:** không cache dữ liệu thay đổi liên tục.
6. **Async is side effect:** không đẩy core business correctness vào async nếu chưa có retry/recovery.
7. **Event payload uses IDs:** không publish object lớn/raw PII.
8. **Scheduler must be idempotent:** chạy lại không gây duplicate.
9. **Lock when multi-instance:** scheduler production multi-instance phải có distributed lock.
10. **Observe background work:** async/job cần log, metric và error handling.

## 4. QUY TẮC ĐẶT TÊN

### 4.1. Cache name

Cache name dùng lowercase dot-separated:

```text
permission.profile
catalog.languages
catalog.programs
catalog.courses
system.config
message.templates
```

Constants:

```java
public final class CacheNames {
    public static final String PERMISSION_PROFILE = "permission.profile";
    public static final String CATALOG_LANGUAGES = "catalog.languages";
}
```

### 4.2. Cache key

Key text pattern:

```text
tenant:{tenant_id}:permission:{role_code}
tenant:{tenant_id}:catalog:languages
tenant:{tenant_id}:catalog:programs:{language_id}
tenant:{tenant_id}:system-config:{config_key}
```

Rules:

- Tenant-scoped data phải có tenant.
- Role/permission cache phải có role hoặc permission profile version.
- Không dùng raw object `toString()` làm key.

### 4.3. Event naming

Event class dùng past-tense:

```text
TenantCreatedEvent
PermissionMatrixUpdatedEvent
ImportBatchCreatedEvent
UserStatusChangedEvent
```

Event handler:

```text
PermissionCacheInvalidationHandler
ImportBatchEventHandler
```

### 4.4. Scheduler job naming

Job class:

```text
<Action><Resource>Job
```

Ví dụ:

```text
ExpirePasswordResetTokenJob
CleanupDraftImportBatchJob
SendEnrollmentReminderJob
RebuildDailyReportSnapshotJob
```

Lock name:

```text
expire-password-reset-token
cleanup-draft-import-batch
send-enrollment-reminder
```

### 4.5. Metric naming

```text
mar.cache.eviction
mar.async.event.failure
mar.scheduler.job.duration
mar.scheduler.job.success
mar.scheduler.job.failure
mar.scheduler.job.records
```

Metric tags phải low-cardinality: `cache`, `event`, `job`, `status`, `reason`.

## 5. CẤU TRÚC FILE & PACKAGE

### 5.1. Cache package

```text
vn.mar.common.cache
├── CacheConfig.java
├── CacheNames.java
├── CacheKeyBuilder.java
└── CacheEvictionService.java
```

Domain cache logic ở module:

```text
vn.mar.authz.service.PermissionProfileCacheService
vn.mar.catalog.service.CatalogLookupService
```

### 5.2. Async package

```text
vn.mar.common.async
├── AsyncConfig.java
├── AsyncExceptionHandler.java
└── ContextAwareTaskDecorator.java
```

Event package trong module:

```text
vn.mar.permission.event.PermissionMatrixUpdatedEvent
vn.mar.leadimport.event.ImportBatchCreatedEvent
```

### 5.3. Scheduler package

Nếu scheduler được bật:

```text
vn.mar.common.scheduler
├── SchedulerConfig.java
├── ShedLockConfig.java
├── JobMetrics.java
├── JobResult.java
└── BaseScheduledJob.java

vn.mar.<module>.scheduler
└── CleanupDraftImportBatchJob.java
```

### 5.4. Scheduler migration

Nếu dùng ShedLock JDBC:

```text
VYYYYMMDD_NN__create_shedlock_table.sql
```

PostgreSQL table:

```sql
create table shedlock (
    name varchar(64) not null,
    lock_until timestamptz not null,
    locked_at timestamptz not null,
    locked_by varchar(255) not null,
    constraint pk_shedlock primary key (name)
);
```

## 6. CÁC PATTERN BẮT BUỘC

### 6.1. Default decision pattern

| Capability | Sprint 1 default | Khi nào nâng cấp |
|---|---|---|
| Local cache | Caffeine/Spring Cache allowed | Read-heavy, low consistency risk |
| Distributed cache | Deferred | Multi-instance cache consistency/session/rate limit yêu cầu |
| Async event | Spring ApplicationEvent sync by default | Side effect local, in-process; async only when explicitly configured |
| Message broker | Deferred | Có external consumer và durability requirement |
| Scheduler | Deferred | Có cleanup/reminder/retry/report job thật |
| Workflow engine | Deferred | State transition phức tạp vượt enum/service rule |

### 6.2. Cache config pattern

```java
@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    CacheManager cacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager(
                CacheNames.PERMISSION_PROFILE,
                CacheNames.CATALOG_LANGUAGES,
                CacheNames.CATALOG_PROGRAMS
        );
        manager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(10_000)
                .expireAfterWrite(Duration.ofMinutes(15)));
        return manager;
    }
}
```

Rules:

- Cache name phải nằm trong `CacheNames`.
- TTL phải explicit.
- Không tạo cache động không kiểm soát.

### 6.3. Cache key builder pattern

```java
public final class CacheKeyBuilder {

    private CacheKeyBuilder() {
    }

    public static String permissionProfile(UUID tenantId, String roleCode) {
        return "tenant:%s:permission:%s".formatted(tenantId, roleCode);
    }

    public static String permissionProfile(UUID tenantId, String roleCode, long permissionVersion) {
        return "tenant:%s:permission:%s:v:%d".formatted(tenantId, roleCode, permissionVersion);
    }

    public static String catalogPrograms(UUID tenantId, UUID languageId) {
        return "tenant:%s:catalog:programs:%s".formatted(tenantId, languageId);
    }
}
```

Không build key bằng string rải rác ở service.

### 6.4. Permission cache pattern

```java
@Cacheable(
        cacheNames = CacheNames.PERMISSION_PROFILE,
        key = "T(vn.mar.common.cache.CacheKeyBuilder).permissionProfile(#tenantId, #roleCode)"
)
public PermissionProfile loadPermissionProfile(UUID tenantId, String roleCode) {
    return permissionRepository.loadProfile(tenantId, roleCode);
}
```

Evict khi permission matrix đổi:

```java
@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
public void onPermissionMatrixUpdated(PermissionMatrixUpdatedEvent event) {
    cacheEvictionService.evictPermissionProfile(event.tenantId(), event.roleCode());
}
```

Sprint 1 default:

- `tenant_id + role_code + TTL + explicit evict` là đủ nếu pilot chạy single-instance hoặc consistency risk thấp.
- Sau Sprint 1, thêm `permission_version` vào key nếu permission cache consistency trở thành vấn đề.
- Nếu dùng version key, version phải tăng trong cùng transaction với permission matrix change.

### 6.5. Cache invalidation pattern

Invalidation bắt buộc khi:

- Permission matrix updated.
- Role permission changed.
- Catalog item status changed nếu cache catalog lookup.
- System config changed.

Nếu không define được invalidation, không cache.

### 6.6. Application event pattern

```java
public record PermissionMatrixUpdatedEvent(
        UUID tenantId,
        String roleCode,
        UUID actorId,
        String requestId
) {
}
```

Publish sau khi business state thay đổi:

```java
applicationEventPublisher.publishEvent(
        new PermissionMatrixUpdatedEvent(tenantId, roleCode, actorId, requestId)
);
```

Rules:

- `ApplicationEvent` của Spring mặc định chạy synchronous nếu không cấu hình async executor hoặc không dùng `@Async`.
- Không được giả định event handler là background job.
- Event payload chứa IDs/metadata nhỏ.
- Không chứa raw file, raw row, password, token.
- Event handler phải log lỗi.
- Event phụ thuộc database state đã commit phải dùng `@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)`.
- Audit trong sensitive business transaction không chuyển sang async best-effort nếu chưa có durable retry/recovery.

### 6.6.1. Transaction event timing pattern

```java
@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
public void onPermissionMatrixUpdated(PermissionMatrixUpdatedEvent event) {
    cacheEvictionService.evictPermissionProfile(event.tenantId(), event.roleCode());
}
```

| Event type | Timing |
|---|---|
| Cache eviction sau permission/catalog change | `AFTER_COMMIT` |
| Notification/reminder side effect | `AFTER_COMMIT` |
| Post-import lightweight metrics/log | Trong flow chính hoặc `AFTER_COMMIT` tùy correctness |
| Audit sensitive setup write | Cùng transaction, không async best-effort |
| Core business state transition | Không phụ thuộc async event nếu chưa có durable recovery |

Nếu transaction rollback, handler `AFTER_COMMIT` không chạy; đây là behavior mong muốn để cache/event không lệch state.

### 6.7. Async executor pattern

```java
@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {

    @Bean(name = "applicationTaskExecutor")
    ThreadPoolTaskExecutor applicationTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("mar-async-");
        executor.setTaskDecorator(new ContextAwareTaskDecorator());
        executor.initialize();
        return executor;
    }
}
```

Rules:

- Không dùng async executor không giới hạn.
- Propagate request/job context nếu cần log.
- Có handler cho exception.

Context propagation allowed:

- `requestId`
- `tenantId`
- `actorId`
- `roleCode`
- `jobName`

Không propagate:

- Raw request body.
- Token/password/secret.
- Full PII.
- Mutable JPA entity/session hoặc lazy-loaded entity graph.

### 6.8. Scheduler pattern

Khi có scheduler:

```java
@Component
@RequiredArgsConstructor
@Slf4j
public class CleanupDraftImportBatchJob {

    private final ImportBatchCleanupService cleanupService;

    @Scheduled(cron = "${mar.scheduler.cleanup-draft-import-batch.cron}")
    @SchedulerLock(
            name = "cleanup-draft-import-batch",
            lockAtMostFor = "PT30M",
            lockAtLeastFor = "PT30S"
    )
    public void run() {
        long startNanos = System.nanoTime();
        log.info("job started jobName=cleanup-draft-import-batch");
        try {
            int processed = cleanupService.cleanupExpiredDrafts(500);
            log.info("job completed jobName=cleanup-draft-import-batch processed={}", processed);
        } catch (Exception ex) {
            log.error("job failed jobName=cleanup-draft-import-batch", ex);
        } finally {
            long durationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);
            log.info("job finished jobName=cleanup-draft-import-batch durationMs={}", durationMs);
        }
    }
}
```

### 6.9. Idempotency pattern

Job phải chạy lại an toàn:

```java
@Transactional
public int cleanupExpiredDrafts(int batchSize) {
    List<ImportBatch> batches = importBatchRepository.findExpiredDrafts(batchSize);
    batches.forEach(ImportBatch::markExpired);
    return batches.size();
}
```

Chạy lần hai không tạo duplicate side effect.

### 6.10. Retry pattern

Retry chỉ dùng cho transient error:

- Temporary DB connection issue.
- External service timeout.
- Network glitch.

Không retry:

- Validation error.
- Permission denied.
- Business rule violation.
- Bad data import row.

Rules:

- Không thêm Spring Retry/Resilience4j chỉ để “có retry”.
- Retry library là dependency mới, cần approval theo `12-dev-workflow-release-convention.md`.
- Retry phải có max attempts, backoff, metric và log reason low-cardinality.
- Không retry operation không idempotent nếu chưa có idempotency key hoặc recovery plan.

## 7. QUY TẮC RIÊNG CỦA MAR CACHE/ASYNC/SCHEDULER

### 7.1. Allowed cache candidates

Được cân nhắc cache:

- Permission profile theo `tenant_id + role_code`.
- Catalog lookup read-heavy: languages/programs/courses.
- System config ít thay đổi.
- Message templates nếu backend dùng.

Không cache:

- Raw import rows.
- Password/token/reset token.
- User-specific sensitive data nếu invalidation chưa rõ.
- Permission matrix forever.
- Data tenant-scoped thiếu tenant key.

### 7.2. TTL guidance

| Data | TTL gợi ý | Invalidation |
|---|---:|---|
| Permission profile | 5-15 phút | Evict khi permission/role đổi |
| Catalog lookup | 15-60 phút | Evict khi catalog đổi |
| System config | 5-60 phút | Evict khi config đổi |
| Static public lookup | 1-24 giờ | Deploy/reload |

### 7.3. Redis decision

Redis chưa là baseline Sprint 1.

Chỉ thêm Redis khi:

- Multi-instance cần cache consistency mạnh hơn local cache.
- Rate limit cần distributed counter.
- Session/token blacklist cần shared state.
- Ops có khả năng vận hành Redis.

Không thêm Redis chỉ vì “kiến trúc lớn hơn”.

### 7.3.1. Local cache multi-instance rule

Local cache là instance-local:

- Invalidation chỉ guaranteed trong instance hiện tại.
- Nếu MAR chạy multi-instance, permission/catalog cache ở instance khác có thể còn stale đến khi TTL hết.
- Production multi-instance dùng local cache phải có TTL ngắn và risk acceptance rõ.
- Nếu permission consistency cần mạnh, mở decision Redis/distributed invalidation hoặc permission version strategy.
- Không claim local cache eviction là cross-instance invalidation.

### 7.4. Kafka/message broker decision

Kafka chưa là baseline Sprint 1.

Chỉ thêm broker khi:

- Có consumer ngoài process.
- Event cần durable delivery sau restart.
- Hệ thống tách thành nhiều service.
- Có monitoring/retry/DLQ rõ.

Spring ApplicationEvent đủ cho side effect local trong monolith.

Rules:

- Event cần durable delivery sau restart không dùng plain ApplicationEvent.
- Async handler lỗi chỉ log/metric không đủ cho business-critical side effect; phải có retry/recovery rõ.
- Event publish từ transaction nên theo rule `AFTER_COMMIT` ở section 6.6.1.

### 7.5. Scheduler candidates

Scheduler chỉ thêm khi có use case rõ:

- Expire activation/reset token.
- Cleanup draft import batch.
- Send reminder.
- Rebuild report snapshot.
- Retry failed integration.

Nếu chưa có job thật trong Sprint 1, không bật scheduler chỉ để “đủ kiến trúc”.

Cleanup draft import batch chỉ bật khi có ticket cleanup thật và test idempotency/lock; nếu không, import status `EXPIRED` chỉ là reserved lifecycle.

### 7.6. Workflow rule

Sprint 1 dùng enum/status field cho:

- Tenant status.
- Branch status.
- User status.
- Import batch status.
- Import row status.

Workflow engine/state machine chỉ cân nhắc khi:

- Admission pipeline có nhiều transition/guard/action.
- SLA escalation phức tạp.
- Payment/enrollment lifecycle có nhiều external event.

## 8. VÍ DỤ CODE MẪU

### 8.1. Good example - permission cache with eviction

```java
@Service
@RequiredArgsConstructor
public class PermissionProfileService {

    private final PermissionRepository permissionRepository;

    @Cacheable(
            cacheNames = CacheNames.PERMISSION_PROFILE,
            key = "T(vn.mar.common.cache.CacheKeyBuilder).permissionProfile(#tenantId, #roleCode)"
    )
    public PermissionProfile load(UUID tenantId, String roleCode) {
        return permissionRepository.loadProfile(tenantId, roleCode);
    }
}
```

```java
@Component
@RequiredArgsConstructor
public class PermissionCacheInvalidationHandler {

    private final CacheEvictionService cacheEvictionService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPermissionMatrixUpdated(PermissionMatrixUpdatedEvent event) {
        cacheEvictionService.evictPermissionProfile(event.tenantId(), event.roleCode());
    }
}
```

### 8.2. Good example - async side effect

```java
@Async("applicationTaskExecutor")
@EventListener
public void onImportBatchCreated(ImportBatchCreatedEvent event) {
    try {
        importMetrics.recordBatchCreated(event.importType());
        log.info("import batch event handled batchId={}", event.batchId());
    } catch (Exception ex) {
        log.error("failed to handle import batch event batchId={}", event.batchId(), ex);
    }
}
```

### 8.3. Bad example - unsafe cache key

```java
@Cacheable(cacheNames = "permission", key = "#roleCode")
public PermissionProfile load(String roleCode) {
    return permissionRepository.loadProfile(roleCode);
}
```

Vấn đề:

- Thiếu tenant id.
- Có thể trả permission tenant khác.
- Không có TTL/invalidation rõ.

### 8.4. Bad example - non-idempotent scheduler

```java
@Scheduled(cron = "0 0 8 * * *")
public void sendReminder() {
    reminderRepository.findDue().forEach(emailService::send);
}
```

Vấn đề:

- Không lock multi-instance.
- Không đánh dấu đã gửi.
- Chạy lại có thể gửi duplicate.
- Không batch size/log/metric.

## 9. ANTI-PATTERNS CẦN TRÁNH

Không được:

- Cache tenant-scoped data mà thiếu tenant key.
- Cache permission không TTL/invalidation.
- Cache raw import row hoặc token/password.
- Thêm Redis khi chưa có distributed need.
- Thêm Kafka cho event local-only.
- Publish event chứa object lớn/raw PII.
- Giả định `ApplicationEvent` tự chạy async khi chưa cấu hình async.
- Handle cache eviction trước commit rồi transaction rollback.
- Claim local cache invalidation hoạt động cross-instance.
- Propagate token/raw body/JPA entity sang async thread.
- Dùng async để che business failure.
- Scheduler không idempotent.
- Scheduler multi-instance không lock.
- Job xử lý unbounded rows.
- Retry permanent business error.
- Tạo workflow engine khi enum/status service rule đủ.

## 10. TESTING CONVENTIONS

### 10.1. Cache test

Test:

- Cache key có tenant id.
- Permission profile cache hit/miss đúng.
- Permission matrix update evict cache.
- Catalog status change evict cache nếu cache catalog.

### 10.2. Async event test

Test:

- Event được publish sau business change.
- Event phụ thuộc DB state được handle `AFTER_COMMIT`.
- Handler nhận đúng event payload.
- Handler không chứa raw PII.
- Exception trong handler được log/metric, không silently drop.
- ApplicationEvent sync/async behavior được test hoặc document rõ khi dùng `@Async`.

### 10.3. Scheduler idempotency test

Test:

- Chạy job hai lần liên tiếp không duplicate side effect.
- Job dùng batch size.
- Job skip/lock behavior nếu dùng ShedLock.

### 10.4. Scheduler integration test

Nếu dùng ShedLock:

- Migration `shedlock` chạy được.
- Chỉ một execution acquire lock trong multi-instance simulation.
- Lock hết hạn đúng.

### 10.5. Metrics/log test

Test:

- Job success/failure metrics increment.
- Job log start/end/duration.
- Async failure metric/log có event name.
- Retry failure/success metric nếu retry được implement.

### 10.6. Multi-instance cache test/review

Nếu deploy multi-instance hoặc dùng local permission/catalog cache trong environment nhiều instance:

- TTL ngắn được cấu hình.
- Risk local cache stale được ghi trong release note/decision.
- Redis/distributed invalidation hoặc permission version strategy được mở decision nếu cần consistency mạnh.
- Không viết test giả định local cache evict được instance khác.

## 11. CODE REVIEW CHECKLIST

- [ ] Cache có lý do rõ.
- [ ] Cache key có tenant nếu tenant-scoped.
- [ ] TTL/invalidation được định nghĩa.
- [ ] Permission cache evict khi permission matrix đổi.
- [ ] Local cache multi-instance risk đã được xử lý bằng TTL/decision.
- [ ] Permission cache dùng TTL + explicit evict hoặc có version strategy.
- [ ] Event payload chỉ chứa ID/metadata nhỏ.
- [ ] Event phụ thuộc committed DB state dùng `AFTER_COMMIT`.
- [ ] ApplicationEvent sync/async behavior được khai báo rõ.
- [ ] Async handler có error handling.
- [ ] Context propagation không mang token/raw body/JPA entity.
- [ ] Core business correctness không phụ thuộc async best-effort.
- [ ] Retry dependency mới có approval; retry chỉ cho transient/idempotent case.
- [ ] Scheduler job có use case thật.
- [ ] Scheduler job idempotent.
- [ ] Scheduler job có batch size.
- [ ] Multi-instance scheduler có lock.
- [ ] Job có log/metric.
- [ ] Redis/Kafka/dependency mới có quyết định kỹ thuật rõ.

## 12. TÀI LIỆU LIÊN QUAN

- `05-security-auth-authz-convention.md` - Permission cache/auth context.
- `07-logging-observability-convention.md` - Job/async log và metrics.
- `08-audit-convention.md` - Audit event không thay bằng async best-effort.
- `09-testing-quality-convention.md` - Cache/async/scheduler tests.
- `12-dev-workflow-release-convention.md` - Dependency review/release gates.
- OASIS reference: `cache_convention.md`, `scheduler_convention.md`, `notification_convention.md`, `workflow_convention.md`.

## 13. LỊCH SỬ CẬP NHẬT

| Phiên bản | Ngày | Người cập nhật | Nội dung |
|---|---|---|---|
| MAR-CONV-1.1 | 30/06/2026 | Tech Lead / Solution Architect | Bổ sung rule ApplicationEvent sync/async, `AFTER_COMMIT` cho event phụ thuộc transaction, cảnh báo local cache multi-instance, permission version strategy, context propagation an toàn và retry dependency approval. |
| MAR-CONV-1.0 | 30/06/2026 | Tech Lead / Solution Architect | Chuẩn hóa cache/async/scheduler convention theo pattern OASIS, giữ Sprint 1 lightweight nhưng đầy đủ rule khi triển khai. |
