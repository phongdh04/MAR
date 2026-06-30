# DATABASE & FLYWAY CONVENTION - QUY TẮC DATABASE MAR

**Ngày tạo:** 30/06/2026  
**Phiên bản:** MAR-CONV-1.1  
**Tác giả:** Tech Lead / Solution Architect  
**Trạng thái:** Locked for Sprint 1 technical kickoff  
**Stack:** PostgreSQL 17, Flyway, Spring Data JPA/Hibernate, Java 21  
**Tham chiếu:**
- `D:\Documents-for-Expert-Design-Database\MAR\architecture\database_convention.md` - Pattern OASIS về DB/Flyway/JPA
- `D:\Documents-for-Expert-Design-Database\MAR\architecture\audit_convention.md` - Pattern audit fields/log
- `D:\Documents-for-Expert-Design-Database\MAR\architecture\search_convention.md` - Pattern search/index/pagination
- `D:\Documents-for-Expert-Design-Database\MAR\our architecture\01-architecture-baseline.md` - MAR architecture baseline

## 1. TỔNG QUAN & MỤC ĐÍCH

Tài liệu này chuẩn hóa database convention cho MAR, bao gồm schema strategy, naming, data type, Flyway migration, JPA mapping, index, seed data và test database.

Mục đích:

- Đảm bảo mọi thay đổi schema đều đi qua Flyway.
- Giữ PostgreSQL là source of truth cho schema.
- Giảm rủi ro sai tenant isolation trong Sprint 1.
- Chuẩn hóa cách viết entity/repository/migration để team backend làm việc nhất quán.
- Ngăn việc kéo nhầm assumption DB từ dự án tham chiếu sang MAR.

Nguyên tắc ghi nhớ:

> **"Không có migration thì không có schema change."**

## 2. PHẠM VI ÁP DỤNG

Áp dụng cho:

- PostgreSQL database của MAR.
- Flyway migration trong `src/main/resources/db/migration`.
- JPA entity, repository query, database index, constraint.
- Seed data hệ thống bắt buộc.
- Testcontainers/integration test cho repository và migration.

Không áp dụng cho:

- Data warehouse/reporting riêng ngoài MVP.
- BI dashboard schema tương lai.
- External CRM/database integration chưa chốt.
- Manual DBA operation production chưa có runbook.

Baseline:

```text
Database: PostgreSQL 17
Migration: Flyway
ORM: Spring Data JPA / Hibernate
Schema strategy MVP: public schema + tenant_id column
```

## 3. NGUYÊN TẮC CHUNG

1. **Flyway first:** mọi schema change phải có file migration mới.
2. **Never edit applied migration:** migration đã chạy ở environment chia sẻ thì không sửa; fix forward bằng migration mới.
3. **Hibernate validates only:** không dùng Hibernate để tự tạo/sửa schema.
4. **Tenant-scoped by default:** business table thuộc tenant phải có `tenant_id`.
5. **Database constraint protects invariant:** rule quan trọng phải có constraint/index nếu có thể.
6. **Application enum, DB varchar:** enum lưu bằng `varchar`, không ordinal.
7. **UTC timestamp:** lưu timestamp dạng `timestamptz`; API trả ISO-8601.
8. **Index follows query:** index phải bám use case search/list/detail thực tế.
9. **JSONB with reason:** dùng JSONB cho dữ liệu linh hoạt, không dùng để tránh modeling.
10. **Fresh DB must boot:** environment mới phải migrate từ zero lên latest không cần thao tác tay.

## 4. QUY TẮC ĐẶT TÊN

### 4.1. Database object naming

| Object | Convention | Ví dụ |
|---|---|---|
| Schema | lowercase snake_case | `public`, `mar_reporting` |
| Table | plural snake_case | `tenants`, `import_rows` |
| Column | snake_case | `tenant_id`, `created_at` |
| Primary key column | `<entity>_id` | `tenant_id`, `course_id` |
| Foreign key column | `<referenced_entity>_id` | `branch_id`, `program_id` |
| Primary key constraint | `pk_<table>` | `pk_tenants` |
| Foreign key constraint | `fk_<from_table>__<to_table>` | `fk_branches__tenants` |
| Unique constraint/index | `ux_<table>__<columns>` | `ux_branches__tenant_code` |
| Normal index | `idx_<table>__<columns>` | `idx_users__tenant_status` |
| Check constraint | `ck_<table>__<rule>` | `ck_courses__tuition_non_negative` |

### 4.2. Flyway file naming

```text
VYYYYMMDD_NN__lower_snake_case_description.sql
```

Ví dụ:

```text
V20260630_01__create_tenant_foundation.sql
V20260630_02__create_user_permission_profile.sql
V20260630_03__create_catalog_tables.sql
V20260630_04__create_import_foundation.sql
V20260630_05__create_audit_events.sql
```

Rules:

- `YYYYMMDD` là ngày tạo migration.
- `NN` là sequence hai chữ số trong ngày.
- Description dùng lowercase snake_case.
- Một file nên đại diện cho một logical change.
- Không đổi tên file migration đã apply.

### 4.3. Entity/table naming

| Entity | Table | PK |
|---|---|---|
| `Tenant` | `tenants` | `tenant_id` |
| `Branch` | `branches` | `branch_id` |
| `User` | `users` | `user_id` |
| `Role` | `roles` | `role_id` |
| `Permission` | `permissions` | `permission_id` |
| `PermissionProfile` | `permission_profiles` | `permission_profile_id` |
| `Language` | `languages` | `language_id` |
| `Program` | `programs` | `program_id` |
| `Course` | `courses` | `course_id` |
| `ImportBatch` | `import_batches` | `import_batch_id` |
| `ImportRow` | `import_rows` | `import_row_id` |
| `AuditEvent` | `audit_events` | `audit_event_id` |

### 4.4. Audit naming decision

Chốt naming Sprint 1:

| Layer | Tên chuẩn |
|---|---|
| BA/ticket term | `AuditLog` |
| DB table | `audit_events` |
| Java entity | `AuditEvent` |
| Service | `AuditService` hoặc `AuditEventService` |

Rules:

- `AuditLog` trong BA/ticket cũ được hiểu là `audit_events` trong DB/code.
- Không tạo đồng thời `audit_logs` và `audit_events`.
- `audit_events` là append-only event log; không expose update/delete API.

## 5. CẤU TRÚC FILE & PACKAGE

### 5.1. Migration directory

```text
src/main/resources/db/migration
├── V20260630_01__create_tenant_foundation.sql
├── V20260630_02__create_user_permission_profile.sql
├── V20260630_03__create_catalog_tables.sql
├── V20260630_04__create_import_foundation.sql
└── V20260630_05__create_audit_events.sql
```

Nếu cần seed bắt buộc:

```text
src/main/resources/db/migration
└── V20260630_06__seed_system_permissions.sql
```

Demo/test seed không đặt chung production migration.

### 5.2. Entity package

```text
vn.mar.common.entity
├── AuditableEntity.java
├── SoftDeletableEntity.java
└── TenantScopedEntity.java

vn.mar.<module>.entity
├── Tenant.java
├── Branch.java
└── ...
```

### 5.3. Repository package

```text
vn.mar.<module>.repository
```

Ví dụ:

```text
vn.mar.tenant.repository.TenantRepository
vn.mar.branch.repository.BranchRepository
vn.mar.leadimport.repository.ImportBatchRepository
```

### 5.4. Configuration

```text
vn.mar.common.config.JpaAuditingConfig
vn.mar.common.time.CurrentTimeProvider
vn.mar.common.tenant.TenantContext
```

### 5.5. Application configuration

```yaml
spring:
  datasource:
    driver-class-name: org.postgresql.Driver
  jpa:
    hibernate:
      ddl-auto: validate
    open-in-view: false
    properties:
      hibernate:
        format_sql: false
  flyway:
    enabled: true
    locations: classpath:db/migration
    validate-on-migrate: true
    baseline-on-migrate: false
```

Không dùng `ddl-auto: update`, `create`, `create-drop`.

## 6. CÁC PATTERN BẮT BUỘC

### 6.1. Base audit columns

Mọi business table cần audit cơ bản:

```sql
created_at timestamptz not null default now(),
created_by uuid,
updated_at timestamptz not null default now(),
updated_by uuid
```

Rules:

- `created_at` không update sau insert.
- `updated_at` update khi record thay đổi.
- `created_by`/`updated_by` lấy từ current user nếu có.
- System job dùng system user/id convention riêng.
- Sprint 1 không bắt buộc FK cứng từ `created_by`/`updated_by` sang `users` vì actor có thể là platform admin, seed runner, system job hoặc integration actor.
- Traceability đầy đủ nằm ở `audit_events` với actor context riêng.

### 6.2. Tenant-scoped table pattern

```sql
tenant_id uuid not null,
constraint fk_<table>__tenants foreign key (tenant_id) references tenants (tenant_id)
```

Mọi tenant-scoped table phải có index:

```sql
create index idx_<table>__tenant_id on <table> (tenant_id);
```

List API thường cần composite index:

```sql
create index idx_users__tenant_status_role
on users (tenant_id, status, role_id);
```

### 6.3. UUID primary key pattern

Sprint 1 default: application-generated UUID.

```java
@Id
@Column(name = "tenant_id", nullable = false, updatable = false)
private UUID id;
```

Entity factory:

```java
public static Tenant create(String name, String timezone) {
    Tenant tenant = new Tenant();
    tenant.id = UUID.randomUUID();
    tenant.name = name;
    tenant.timezone = timezone;
    tenant.status = TenantStatus.ACTIVE;
    return tenant;
}
```

Chỉ dùng DB-generated UUID nếu migration cài extension và SA chốt rõ.

### 6.4. PostgreSQL data type pattern

| Data | PostgreSQL type | Java type | Ghi chú |
|---|---|---|---|
| ID | `uuid` | `UUID` | PK/FK |
| Code | `varchar(50)` | `String` | Có unique/index nếu lookup |
| Name | `varchar(255)` | `String` | Tùy domain có thể giảm |
| Description | `text` | `String` | Text dài |
| Money | `numeric(18,2)` | `BigDecimal` | Không dùng double |
| Count/quantity | `integer` / `bigint` | `Integer` / `Long` | Tùy range |
| Boolean | `boolean` | `Boolean` / `boolean` | Không dùng `0/1` |
| Timestamp | `timestamptz` | `Instant` hoặc `OffsetDateTime` | Ưu tiên `Instant` service/API |
| Enum | `varchar(50)` | Java enum | `EnumType.STRING` |
| Flexible payload | `jsonb` | `JsonNode` / mapped class | Có lý do rõ |

Money source of truth:

- Tất cả money columns dùng `numeric(18,2)` trừ khi có decision rõ.
- Nếu ERD/ticket cũ còn `numeric(14,2)`, cập nhật về `numeric(18,2)` hoặc ghi lý do lệch.
- Java luôn dùng `BigDecimal`, không dùng `double`/`float`.

### 6.5. Enum mapping pattern

```java
@Enumerated(EnumType.STRING)
@Column(name = "status", nullable = false, length = 50)
private TenantStatus status;
```

Migration:

```sql
status varchar(50) not null,
constraint ck_tenants__status check (status in ('ACTIVE', 'INACTIVE'))
```

Không dùng ordinal vì đổi thứ tự enum sẽ làm hỏng dữ liệu.

Khi thêm enum value mới:

- Tạo Flyway migration để update check constraint trước khi application dùng enum mới.
- Không deploy app sử dụng enum mới trước migration.
- Không đổi tên enum key đã publish nếu chưa có migration/compatibility plan.

### 6.6. Soft delete pattern

Dùng soft delete khi cần giữ record khỏi list mặc định nhưng vẫn cần audit/history:

```sql
deleted_at timestamptz,
deleted_by uuid
```

Với configuration/master data đơn giản, ưu tiên `status`:

```sql
status varchar(50) not null
```

Không dùng cả soft delete và status nếu chưa có lý do nghiệp vụ rõ.

### 6.7. JSONB pattern

Dùng `jsonb` cho:

- `mapping_config` của import template.
- `raw_row` khi import file có cấu trúc linh hoạt.
- `normalized_row` nếu cần lưu dữ liệu đã chuẩn hóa.
- `error_details` nếu lỗi import có cấu trúc nested.

Không dùng `jsonb` cho:

- Tenant.
- Branch.
- User.
- Role/permission.
- Catalog ổn định như language/program/course.

### 6.8. Repository query pattern

```java
public interface BranchRepository extends JpaRepository<Branch, UUID> {

    Optional<Branch> findByIdAndTenantId(UUID branchId, UUID tenantId);

    boolean existsByTenantIdAndCodeIgnoreCase(UUID tenantId, String code);

    Page<Branch> findByTenantIdAndStatus(UUID tenantId, BranchStatus status, Pageable pageable);
}
```

Rules:

- Detail query tenant-scoped phải có `tenantId`.
- Query list phải phân trang.
- Custom query phải parameterized.
- Native query chỉ dùng khi JPQL không đủ rõ hoặc cần PostgreSQL feature.

### 6.9. Flyway transaction pattern

PostgreSQL hỗ trợ transactional DDL cho phần lớn operation. Tuy nhiên cần cẩn trọng với:

- `CREATE INDEX CONCURRENTLY`
- `ALTER TYPE ...`
- Operation lớn lock bảng

Sprint 1 không dùng `CREATE INDEX CONCURRENTLY` trong migration mặc định. Nếu cần, tách migration và document rollback/operation note.

### 6.10. Seed data pattern

System seed được phép trong Flyway nếu app không chạy được nếu thiếu nó:

- Permission key mặc định.
- Platform role mặc định nếu bắt buộc.
- Static catalog system-level đã chốt.

Sprint 1 seed strategy:

- `roles` seed nếu có role lookup `/api/v1/roles`.
- `permissions` seed nếu dùng global `function_code` catalog.
- `permission_profiles` seed cho tenant/system baseline chỉ khi app không thể khởi động/QA không thể verify nếu thiếu.
- Demo/test tenant, demo user, demo lead và import fixture phải tách khỏi production migration.

Không đưa demo data vào production migration:

- Demo tenant.
- Demo user.
- Demo lead.
- Demo course.

Demo/test seed để ở test resource hoặc local script riêng.

## 7. QUY TẮC RIÊNG CỦA MAR DATABASE

### 7.1. Schema strategy Sprint 1

Chốt:

```text
schema: public
tenant isolation: tenant_id column
```

Không dùng database-per-tenant hoặc schema-per-tenant trong MVP.

Lý do:

- Phù hợp tốc độ delivery Sprint 1.
- QA dễ tạo data cross-module.
- Permission và tenant filter dễ test.
- Đủ cho pilot scale.
- Có thể tiến hóa sau nếu enterprise tenant scale yêu cầu.

### 7.2. Migration order Sprint 1

Thứ tự đề xuất:

1. `tenants`
2. `branches`
3. `users`
4. `roles`
5. `permissions`
6. `permission_profiles`
7. `audit_events`
8. `user_branches`
9. `languages`
10. `programs`
11. `courses`
12. `import_batches`
13. `import_rows`

Lý do:

- Tenant là boundary cha.
- User/role/permission profile cần trước protected API.
- Audit table phải tồn tại trước khi application cho phép permission matrix changes.
- Catalog cần trước import mapping.
- Import foundation phụ thuộc tenant và catalog.
- `user_branches` có thể sau audit table vì branch assignment là sensitive change cần audit khi được thao tác qua app.

Nếu seed permission không audit trong migration, vẫn phải đảm bảo `audit_events` tồn tại trước khi API permission matrix update được mở.

### 7.3. Permission matrix schema Sprint 1

Sprint 1 source of truth cho permission matrix:

```text
permission_profiles(
  permission_profile_id uuid,
  tenant_id uuid not null,
  role_code varchar(50) not null,
  function_code varchar(100) not null,
  access_level varchar(50) not null,
  scope varchar(50),
  status varchar(50) not null
)
```

Chốt:

- `permission_profiles` là tenant-level role/function/access/scope matrix.
- `roles` là seed/lookup table nếu có `/api/v1/roles`.
- `permissions` có thể là global catalog của `function_code`.
- Không implement full `role_permissions` trong Sprint 1 nếu Tech Lead không approve rõ.
- Nếu sau này chuyển sang RBAC chuẩn, phải có migration/compatibility plan cho `permission_profiles`.

Index/unique gợi ý:

```sql
create unique index ux_permission_profiles__tenant_role_function_scope
on permission_profiles (tenant_id, role_code, function_code, coalesce(scope, 'GLOBAL'));
```

### 7.4. Unique rule theo tenant

Code/name nghiệp vụ thường unique trong tenant, không unique toàn hệ thống.

Ví dụ:

```sql
create unique index ux_branches__tenant_code
on branches (tenant_id, lower(branch_code));
```

Nếu chỉ unique khi active:

```sql
create unique index ux_branches__tenant_code_active
on branches (tenant_id, lower(branch_code))
where status = 'ACTIVE';
```

### 7.5. Timezone rule

- DB lưu `timestamptz`.
- Service xử lý bằng `Instant` hoặc `OffsetDateTime`.
- API trả ISO-8601.
- Tenant timezone dùng cho hiển thị/lọc ngày nghiệp vụ.
- Không lưu local time mơ hồ cho audit/security event.

### 7.6. Import table rule

`import_batches` lưu trạng thái batch.

`import_rows` lưu row-level result.

Các field tối thiểu:

```sql
import_batch_id uuid not null
row_number integer not null
row_status varchar(50) not null
raw_row jsonb
normalized_row jsonb
error_details jsonb
```

Index bắt buộc:

```sql
create index idx_import_rows__tenant_batch_status
on import_rows (tenant_id, import_batch_id, row_status);
```

### 7.7. Constraint-to-error-code rule

Constraint names must be stable because `06-exception-error-i18n-convention.md` maps DB constraint violations to API error codes.

Known mapping examples:

| Constraint/index | Error code |
|---|---|
| `ux_branches__tenant_code_active` | `DUPLICATE_ACTIVE_BRANCH` |
| `ux_users__tenant_email` | `DUPLICATE_USER_EMAIL` |
| `ck_courses__tuition_non_negative` | `NEGATIVE_TUITION` |
| `ux_permission_profiles__tenant_role_function_scope` | `INVALID_PERMISSION_GUARDRAIL` or `DUPLICATE_RESOURCE` depending use case |

Rules:

- New unique/check constraint for API-visible business rule should be added to `ConstraintErrorMapper`.
- Unknown constraint should not leak raw DB detail to client.
- Constraint names are part of API error mapping contract; do not rename casually.

## 8. VÍ DỤ CODE MẪU

### 8.1. Good example - migration tenant foundation

```sql
create table tenants (
    tenant_id uuid not null,
    tenant_name varchar(255) not null,
    timezone varchar(100) not null default 'Asia/Ho_Chi_Minh',
    default_currency varchar(10) not null default 'VND',
    status varchar(50) not null,
    created_at timestamptz not null default now(),
    created_by uuid,
    updated_at timestamptz not null default now(),
    updated_by uuid,
    constraint pk_tenants primary key (tenant_id),
    constraint ck_tenants__status check (status in ('ACTIVE', 'INACTIVE')),
    constraint ck_tenants__name_not_blank check (length(trim(tenant_name)) > 0)
);

create index idx_tenants__status on tenants (status);
```

### 8.2. Good example - tenant-scoped branch migration

```sql
create table branches (
    branch_id uuid not null,
    tenant_id uuid not null,
    branch_code varchar(50) not null,
    branch_name varchar(255) not null,
    status varchar(50) not null,
    created_at timestamptz not null default now(),
    created_by uuid,
    updated_at timestamptz not null default now(),
    updated_by uuid,
    constraint pk_branches primary key (branch_id),
    constraint fk_branches__tenants foreign key (tenant_id) references tenants (tenant_id),
    constraint ck_branches__status check (status in ('ACTIVE', 'INACTIVE')),
    constraint ck_branches__code_not_blank check (length(trim(branch_code)) > 0)
);

create unique index ux_branches__tenant_code
on branches (tenant_id, lower(branch_code));

create index idx_branches__tenant_status
on branches (tenant_id, status);
```

### 8.3. Good example - JPA entity

```java
@Entity
@Table(
        name = "branches",
        indexes = {
                @Index(name = "idx_branches__tenant_status", columnList = "tenant_id,status")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Branch extends AuditableEntity {

    @Id
    @Column(name = "branch_id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "branch_code", nullable = false, length = 50)
    private String code;

    @Column(name = "branch_name", nullable = false, length = 255)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private BranchStatus status;
}
```

### 8.4. Bad example - migration nhiều rủi ro

```sql
create table Branch (
    id serial primary key,
    tenantId varchar(50),
    name varchar(255),
    status int,
    data text
);
```

Vấn đề:

- Table PascalCase/số ít.
- ID không theo UUID convention.
- `tenantId` sai snake_case và sai type.
- Không có FK tenant.
- Không có audit columns.
- Enum lưu bằng integer.
- JSON-like payload lưu text không rõ schema.
- Không có index tenant/list.

## 9. ANTI-PATTERNS CẦN TRÁNH

Không được:

- Sửa migration đã apply ở shared environment.
- Dùng Hibernate `ddl-auto=update/create/create-drop`.
- Tạo table tenant-scoped nhưng thiếu `tenant_id`.
- Tạo query detail chỉ bằng id cho resource tenant-scoped.
- Lưu enum bằng ordinal.
- Dùng `double` cho tiền học phí/chi phí.
- Tạo index theo cảm tính không bám query.
- Dùng JSONB cho dữ liệu đã ổn định chỉ vì nhanh.
- Đưa demo data vào production migration.
- Tạo FK/constraint không đặt tên.
- Dùng timestamp không timezone cho audit/security event.
- Tạo schema-per-tenant trong MVP khi chưa có quyết định mới.
- Tạo cả `audit_logs` và `audit_events` trong cùng hệ thống.
- Implement `role_permissions` trong Sprint 1 khi chưa có approval, trong khi `permission_profiles` mới là source of truth.
- Tạo FK cứng cho `created_by`/`updated_by` làm system/platform actor không ghi được audit columns.
- Thêm enum value trong code nhưng quên migration update check constraint.
- Dùng money precision khác `numeric(18,2)` mà không có decision.
- Đổi tên constraint đã được map sang ErrorCode.

## 10. TESTING CONVENTIONS

### 10.1. Migration test

Mỗi PR có migration phải chứng minh:

- Fresh database migrate từ empty lên latest thành công.
- `flyway validate` pass.
- App boot với `ddl-auto=validate`.
- Repository critical query chạy được trên PostgreSQL thật.

### 10.2. Testcontainers PostgreSQL

Ưu tiên Testcontainers cho integration test:

```java
@Testcontainers
@DataJpaTest
class BranchRepositoryTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17");
}
```

Không dùng H2 để test behavior phụ thuộc PostgreSQL như JSONB, `timestamptz`, partial index.

### 10.3. Repository test cases

Tenant-scoped repository cần test:

- Tìm thấy record đúng tenant.
- Không tìm thấy record khác tenant.
- Unique constraint theo tenant hoạt động.
- Pagination/sort query hoạt động.
- Enum mapping đúng string value.
- `permission_profiles` unique theo `tenant_id + role_code + function_code + scope`.
- Audit query dùng `audit_events`, không tạo/truy vấn table naming khác.

### 10.4. Import table test cases

Import repository cần test:

- Insert `raw_row`/`error_details` JSONB.
- Query row error theo `tenant_id + batch_id + row_status`.
- Không đọc được batch khác tenant.
- Batch status transition không làm mất row data.

### 10.5. Performance smoke test

Với list endpoint quan trọng:

- Kiểm tra index được dùng cho query phổ biến.
- Không tạo N+1 query ở service/API.
- Không fetch toàn bộ bảng rồi filter trong Java.

### 10.6. Constraint mapping test

Với constraint được client/QA quan tâm:

- Unique branch code violation map được sang `DUPLICATE_ACTIVE_BRANCH`.
- Unique user email violation map được sang `DUPLICATE_USER_EMAIL`.
- Tuition negative check map được sang `NEGATIVE_TUITION` hoặc validation error theo `06`.
- Unknown DB constraint không leak raw SQL/constraint detail ra client.

## 11. CODE REVIEW CHECKLIST

- [ ] Có migration Flyway mới cho mọi schema change.
- [ ] Tên migration đúng `VYYYYMMDD_NN__description.sql`.
- [ ] Không sửa migration đã apply.
- [ ] Table/column/constraint/index đúng naming convention.
- [ ] Tenant-scoped table có `tenant_id`.
- [ ] Tenant-scoped query có tenant filter.
- [ ] PK/FK dùng `uuid`.
- [ ] Audit columns có mặt ở business table.
- [ ] `created_by`/`updated_by` không tạo FK cứng nếu actor có thể là system/platform.
- [ ] Enum lưu `varchar`, không ordinal.
- [ ] Enum check constraint có migration khi thêm value mới.
- [ ] Money dùng `numeric(18,2)`/`BigDecimal`.
- [ ] JSONB có lý do nghiệp vụ rõ.
- [ ] Unique/index bám use case query.
- [ ] Audit naming dùng `audit_events`/`AuditEvent`; không tạo thêm `audit_logs`.
- [ ] Permission matrix dùng `permission_profiles` làm source of truth Sprint 1.
- [ ] Không dùng `role_permissions` nếu chưa được Tech Lead approve.
- [ ] System/demo/test seed được tách đúng.
- [ ] Constraint business quan trọng có mapping sang ErrorCode ở `06`.
- [ ] Không dùng `ddl-auto=update/create/create-drop`.
- [ ] Fresh migration pass.
- [ ] Repository/migration test dùng PostgreSQL thật khi cần.

## 12. TÀI LIỆU LIÊN QUAN

- `01-architecture-baseline.md` - Nền kiến trúc MAR.
- `02-coding-package-convention.md` - Entity/repository/service package.
- `03-rest-api-convention.md` - API contract và pagination.
- `07-logging-observability-convention.md` - Logging/trace DB issue.
- `08-audit-convention.md` - Audit event và audit columns.
- `09-testing-quality-convention.md` - Test quality gate.
- OASIS reference: `database_convention.md`, `audit_convention.md`, `search_convention.md`.

## 13. LỊCH SỬ CẬP NHẬT

| Phiên bản | Ngày | Người cập nhật | Nội dung |
|---|---|---|---|
| MAR-CONV-1.1 | 30/06/2026 | Tech Lead / Solution Architect | Đồng bộ audit naming `AuditLog` BA -> `audit_events` DB, chốt `permission_profiles` là source of truth Sprint 1, cập nhật migration order, seed strategy, enum/money/created_by rules và constraint-to-error mapping. |
| MAR-CONV-1.0 | 30/06/2026 | Tech Lead / Solution Architect | Chuẩn hóa database/Flyway convention theo pattern OASIS, chuyển sang PostgreSQL 17 và tenant_id column strategy cho MAR. |
