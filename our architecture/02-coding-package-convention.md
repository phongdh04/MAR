# CODING & PACKAGE CONVENTION - QUY TẮC CODE VÀ PACKAGE MAR

**Ngày tạo:** 30/06/2026  
**Phiên bản:** MAR-CONV-1.0  
**Tác giả:** Tech Lead / Solution Architect  
**Trạng thái:** Locked for Sprint 1 technical kickoff  
**Stack:** Java 21, Spring Boot 3.5.x, PostgreSQL 17, Spring Data JPA/Hibernate  
**Tham chiếu:**
- `D:\Documents-for-Expert-Design-Database\MAR\architecture\coding_convention.md` - Pattern OASIS về code convention
- `D:\Documents-for-Expert-Design-Database\MAR\architecture\coding_convention_principles.md` - Nguyên tắc code convention
- `D:\Documents-for-Expert-Design-Database\MAR\architecture\15_common_code_vs_principle.md` - Ranh giới common code
- `D:\Documents-for-Expert-Design-Database\MAR\our architecture\01-architecture-baseline.md` - MAR architecture baseline

## 1. TỔNG QUAN & MỤC ĐÍCH

Tài liệu này là chuẩn bắt buộc cho cách viết code, đặt tên class, tổ chức package và phân tầng trong backend MAR.

Mục đích:

- Giữ toàn bộ code MAR nhất quán dưới root package `vn.mar`.
- Ngăn việc đưa business logic vào controller, repository hoặc package `common`.
- Làm checklist review cho Sprint 1 foundation và các sprint sau.
- Tách rõ MAR domain khỏi domain tham chiếu; chỉ học cách tổ chức, không sao chép package/business cũ.

Nguyên tắc ghi nhớ:

> **"Tên package thể hiện domain, tên class thể hiện trách nhiệm, tên method thể hiện hành động."**

## 2. PHẠM VI ÁP DỤNG

Áp dụng cho:

- Backend Java source code.
- Entity, repository, service, controller, DTO, mapper, validator, config, exception.
- Unit test, integration test, test fixture, test data builder.
- Các module Sprint 1: `tenant`, `branch`, `user`, `role`, `permission`, `catalog`, `leadimport`, `audit`.

Không áp dụng cho:

- Frontend framework và UI component.
- Document BA/PM ngoài thư mục source code.
- Infrastructure-as-code production chi tiết.

## 3. NGUYÊN TẮC CHUNG

1. **Readability before cleverness:** ưu tiên code dễ đọc hơn code ngắn nhưng khó hiểu.
2. **Domain-first package:** package cấp module phải bám nghiệp vụ MAR, không bám màn hình hoặc database table.
3. **One responsibility per class:** một class không vừa validate, vừa query, vừa map response.
4. **Service owns business rule:** controller điều phối request; service quyết định nghiệp vụ.
5. **Repository owns persistence only:** repository không chứa quyết định nghiệp vụ, không check permission.
6. **Common must be earned:** chỉ đưa vào `common` khi thật sự cross-cutting hoặc đã dùng lại ở nhiều module.
7. **Tenant isolation is explicit:** mọi use case có dữ liệu theo tenant phải thể hiện rõ tenant boundary.
8. **No accidental framework leak:** response API không trả thẳng JPA entity hoặc framework exception.
9. **Comment explains why:** code comment bằng tiếng Anh, giải thích lý do, không lặp lại điều code đã nói.
10. **Do not mix architectural layers:** controller không gọi repository; repository không gọi service.

## 4. QUY TẮC ĐẶT TÊN

### 4.1. Naming tổng quát

| Loại | Convention | Ví dụ MAR |
|---|---|---|
| Root package | lowercase | `vn.mar` |
| Package | lowercase, không `_`, không `-` | `vn.mar.leadimport` |
| Class | PascalCase | `TenantService` |
| Interface | PascalCase, không prefix `I` | `TenantLookupService` |
| Method | camelCase, bắt đầu bằng động từ | `createTenant`, `findActiveBranches` |
| Variable | camelCase | `tenantRepository` |
| Constant | UPPER_SNAKE_CASE | `DEFAULT_PAGE_SIZE` |
| Enum value | UPPER_SNAKE_CASE | `ACTIVE`, `PERMISSION_DENIED` |
| File Java | Trùng tên public class | `TenantController.java` |
| DB table | snake_case, số nhiều | `tenants`, `import_batches` |
| DB column | snake_case | `tenant_id`, `created_at` |

### 4.2. Naming class theo layer

| Layer | Pattern | Ví dụ |
|---|---|---|
| Entity | `<DomainNoun>` số ít | `Tenant`, `Branch`, `ImportBatch` |
| Repository | `<Entity>Repository` | `TenantRepository` |
| Service CRUD | `<Entity>Service` | `BranchService` |
| Service use case | `<UseCase>Service` | `ImportLeadFileService` |
| Controller | `<Resource>Controller` | `TenantController` |
| Request DTO | `<Action><Entity>Request` | `CreateTenantRequest` |
| Response DTO | `<Entity>Response` | `TenantResponse` |
| Summary DTO | `<Entity>SummaryResponse` | `CourseSummaryResponse` |
| Detail DTO | `<Entity>DetailResponse` | `ImportBatchDetailResponse` |
| Mapper | `<Entity>Mapper` | `TenantMapper` |
| Validator | `<Entity>Validator` hoặc `<UseCase>Validator` | `ImportMappingValidator` |
| Exception | `<Meaning>Exception` | `DuplicateTenantCodeException` |
| Config | `<Topic>Config` | `SecurityConfig` |

### 4.3. Naming method

| Mục đích | Prefix nên dùng | Ví dụ |
|---|---|---|
| Tạo mới | `create` | `createTenant` |
| Cập nhật | `update` | `updateBranch` |
| Xóa mềm | `deactivate`, `archive`, `markAsDeleted` | `deactivateUser` |
| Tra cứu một record | `get`, `find` | `getTenantDetail`, `findByCode` |
| Tra cứu danh sách | `search`, `findAll` | `searchCourses` |
| Kiểm tra tồn tại | `exists` | `existsByTenantCode` |
| Đếm | `count` | `countActiveUsers` |
| Convert | `toResponse`, `toEntity`, `from` | `toTenantResponse` |
| Validate | `validate` | `validateBranchBelongsToTenant` |

Quy tắc phân biệt `get`, `find`, `search`, `exists`:

| Prefix | Ý nghĩa | Return/behavior |
|---|---|---|
| `getXxx(...)` | Resource bắt buộc tồn tại | Không thấy thì throw `ResourceNotFoundException` |
| `findXxx(...)` | Resource có thể không tồn tại | Trả `Optional<T>` hoặc nullable nếu có convention rõ |
| `searchXxx(...)` | Tra cứu danh sách có filter/pagination | Trả `PageResponse<T>` hoặc `Page<T>` ở layer repository |
| `existsXxx(...)` | Kiểm tra tồn tại | Trả `boolean` |

Ví dụ:

```java
BranchDetailResponse getBranch(UUID branchId);
Optional<Branch> findByIdAndTenantId(UUID branchId, UUID tenantId);
PageResponse<CourseSummaryResponse> searchCourses(CourseSearchRequest request);
boolean existsByTenantIdAndCode(UUID tenantId, String code);
```

### 4.4. Boolean naming

Nên dùng tên khẳng định:

- `active`
- `enabled`
- `deleted`
- `locked`
- `verified`

Tránh tên phủ định gây double-negative:

- `notActive`
- `notDeleted`
- `nonLocked`

## 5. CẤU TRÚC FILE & PACKAGE

### 5.1. Root package bắt buộc

```text
vn.mar
```

Main class:

```text
vn.mar.MarApplication
```

Không dùng root package thử nghiệm, tên cá nhân, package OASIS, hoặc package theo khách hàng chưa chốt.

### 5.2. Package layout chuẩn

```text
vn.mar
├── MarApplication.java
├── common
│   ├── config
│   ├── dto
│   ├── error
│   ├── exception
│   ├── logging
│   ├── mapper
│   ├── pagination
│   ├── search
│   ├── tenant
│   ├── time
│   └── util
├── security
│   ├── config
│   ├── context
│   ├── filter
│   └── jwt
├── auth
├── authz
├── audit
├── tenant
├── branch
├── user
├── role
├── permission
├── catalog
├── leadimport
├── notification
└── reporting
```

Rules:

- `role` là module thật nếu Sprint 1 có `/api/v1/roles` hoặc role lookup service; nếu role chỉ là enum/seed, package này là reserved và không thêm business code khi chưa có ticket.
- `notification` và `reporting` là reserved packages trong Sprint 1.
- `common.tenant` chỉ chứa tenant context/helper dùng chung.
- `common.time` chứa `TimeProvider`/`Clock` abstraction để test audit/import/SLA/timezone.
- `common.mapper` chỉ chứa mapper utility thật sự cross-cutting; không đặt `TenantMapper`, `BranchMapper`, `CourseMapper` vào `common.mapper`.

### 5.3. Layout bên trong domain module

```text
vn.mar.<module>
├── api
├── controller
├── dto
│   ├── request
│   └── response
├── entity
├── repository
├── service
├── mapper
├── validator
├── event
└── config
```

Ví dụ:

```text
vn.mar.tenant.controller.TenantController
vn.mar.tenant.dto.request.CreateTenantRequest
vn.mar.tenant.dto.response.TenantDetailResponse
vn.mar.tenant.entity.Tenant
vn.mar.tenant.repository.TenantRepository
vn.mar.tenant.service.TenantService
vn.mar.catalog.api.CatalogLookupService
vn.mar.tenant.mapper.TenantMapper
vn.mar.tenant.validator.TenantValidator
```

`api` package là public module boundary cho module khác gọi vào. Service/repository nội bộ không được gọi trực tiếp từ module khác nếu chưa expose qua `api`.

Ví dụ:

```text
vn.mar.catalog.api.CatalogLookupService
vn.mar.branch.api.BranchLookupService
vn.mar.user.api.UserLookupService
```

Nếu sau này cần ports theo kiến trúc hexagonal, dùng package `port` có decision riêng; Sprint 1 mặc định dùng `api` cho public lookup/service contract trong modular monolith.

### 5.4. Quy tắc một file / một top-level public type

- Mỗi file `.java` chỉ chứa một top-level `public class`, `public interface`, `public enum` hoặc `public record`.
- Tên file phải trùng public type.
- Nested class chỉ dùng khi phạm vi thật sự nội bộ và không cần reuse.
- Không tạo file lớn chứa nhiều DTO nhỏ nếu DTO đó là API contract riêng.

### 5.5. Thứ tự khai báo trong class

1. Constant.
2. Field dependency.
3. Field state.
4. Constructor.
5. Public method.
6. Package-private/protected method.
7. Private helper.
8. Static factory/helper.

Với Lombok constructor injection, ưu tiên:

```java
@Service
@RequiredArgsConstructor
public class TenantService {

    private final TenantRepository tenantRepository;
    private final TenantMapper tenantMapper;
}
```

## 6. CÁC PATTERN BẮT BUỘC

### 6.1. Controller pattern

Controller chỉ làm nhiệm vụ HTTP boundary:

- Nhận request DTO.
- Validate bằng Jakarta Bean Validation.
- Lấy current user/tenant từ security context nếu cần.
- Gọi service.
- Trả response DTO hoặc `ApiResponse<T>`.
- Không chứa business rule, không mở transaction, không gọi repository.

```java
@RestController
@RequestMapping("/api/v1/tenants")
@RequiredArgsConstructor
public class TenantController {

    private final TenantService tenantService;

    @PostMapping
    public ApiResponse<TenantDetailResponse> createTenant(
            @Valid @RequestBody CreateTenantRequest request) {
        return ApiResponse.success(tenantService.createTenant(request));
    }
}
```

### 6.2. Service pattern

Service là nơi giữ business rule và transaction boundary:

- Method ghi dữ liệu dùng `@Transactional`.
- Method đọc dữ liệu dùng `@Transactional(readOnly = true)`.
- Enforce tenant isolation.
- Enforce permission qua `authz` khi endpoint cần bảo vệ.
- Ghi audit event khi thay đổi dữ liệu quan trọng.
- Không trả JPA entity ra controller.

```java
@Service
@RequiredArgsConstructor
public class TenantService {

    private final TenantRepository tenantRepository;
    private final TenantMapper tenantMapper;

    @Transactional
    public TenantDetailResponse createTenant(CreateTenantRequest request) {
        if (tenantRepository.existsByCode(request.code())) {
            throw new BusinessException("TENANT_CODE_DUPLICATED");
        }

        Tenant tenant = Tenant.create(request.code(), request.name());
        Tenant savedTenant = tenantRepository.save(tenant);
        return tenantMapper.toDetailResponse(savedTenant);
    }
}
```

### 6.3. Repository pattern

Repository chỉ chứa persistence query:

- Tên method query phải đọc được.
- Query có tham số phải dùng parameter binding.
- Không dùng string concatenation để tạo SQL/JPQL từ input người dùng.
- Với list API, phải có phân trang.
- Với dữ liệu theo tenant, query phải có tenant boundary hoặc được service kiểm soát rõ.

```java
public interface BranchRepository extends JpaRepository<Branch, UUID> {

    boolean existsByTenantIdAndCode(UUID tenantId, String code);

    Optional<Branch> findByIdAndTenantId(UUID id, UUID tenantId);

    Page<Branch> findByTenantIdAndStatus(UUID tenantId, BranchStatus status, Pageable pageable);
}
```

### 6.4. DTO pattern

Request/response DTO là API contract, không dùng entity làm request/response.

```java
public record CreateBranchRequest(
        @NotBlank
        @Size(max = 50)
        String code,

        @NotBlank
        @Size(max = 255)
        String name
) {
}
```

```java
public record BranchSummaryResponse(
        UUID id,
        String code,
        String name,
        BranchStatus status
) {
}
```

### 6.5. Mapper pattern

Mapper chỉ chuyển đổi object:

- Không gọi repository.
- Không check permission.
- Không tự mở transaction.
- Không format dữ liệu nhạy cảm nếu rule đó thuộc service.

MapStruct được phép dùng nếu team chốt dependency; nếu chưa chốt, dùng mapper thủ công rõ ràng.

```java
@Component
public class BranchMapper {

    public BranchSummaryResponse toSummaryResponse(Branch branch) {
        return new BranchSummaryResponse(
                branch.getId(),
                branch.getCode(),
                branch.getName(),
                branch.getStatus()
        );
    }
}
```

### 6.6. Validator pattern

Validator dùng cho validation nghiệp vụ lặp lại hoặc validation cần nhiều nguồn dữ liệu.

```java
@Component
@RequiredArgsConstructor
public class BranchValidator {

    private final BranchRepository branchRepository;

    public void ensureCodeIsUnique(UUID tenantId, String code) {
        if (branchRepository.existsByTenantIdAndCode(tenantId, code)) {
            throw new BusinessException("BRANCH_CODE_DUPLICATED");
        }
    }
}
```

### 6.7. Entity pattern

Entity chứa persistence state và invariant nhỏ:

- Không inject service/repository.
- Không gọi API ngoài.
- Không chứa workflow dài.
- Field nhạy cảm không đưa vào `toString`.
- Entity name là danh từ số ít.
- `AuditableEntity`, `TenantScopedEntity` và base entity liên quan được định nghĩa tại `04-database-flyway-convention.md`.
- Không tự tạo base entity mới trong từng module.

```java
@Entity
@Table(name = "branches")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Branch extends AuditableEntity {

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "code", nullable = false, length = 50)
    private String code;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private BranchStatus status;

    public static Branch create(UUID tenantId, String code, String name) {
        Branch branch = new Branch();
        branch.tenantId = tenantId;
        branch.code = code;
        branch.name = name;
        branch.status = BranchStatus.ACTIVE;
        return branch;
    }
}
```

### 6.8. Transaction pattern

```java
@Transactional
public UserDetailResponse createUser(CreateUserRequest request) {
    // write use case
}
```

```java
@Transactional(readOnly = true)
public PageResponse<UserSummaryResponse> searchUsers(UserSearchRequest request) {
    // read use case
}
```

Không đặt `@Transactional` ở controller.

### 6.9. Lombok pattern

Được dùng:

- `@Getter`
- `@Setter` cho DTO mutable khi thật sự cần
- `@Builder` cho DTO/command/test fixture
- `@RequiredArgsConstructor`
- `@NoArgsConstructor(access = AccessLevel.PROTECTED)` cho JPA entity

Tránh:

- `@Data` trên JPA entity.
- `@ToString` trên entity có thông tin nhạy cảm.
- `@EqualsAndHashCode` dựa trên mutable field.

## 7. QUY TẮC RIÊNG CỦA MAR

### 7.1. Common package rule

Chỉ đưa vào `common` nếu thỏa một trong các điều kiện:

- Dùng lại ở ít nhất hai module hiện tại.
- Là cross-cutting concern: error, pagination, logging, tenant context, time provider, search abstraction.
- Không chứa business rule riêng của một module.

Không đưa vào `common`:

- `TenantService`
- `TenantMapper`
- `BranchMapper`
- `CourseMapper`
- `PermissionMatrixService`
- `LeadImportMappingService`
- `CourseCatalogWorkflow`
- Validation riêng của một màn hình hoặc một use case.

### 7.2. Module dependency rule

Một module có thể phụ thuộc vào:

- `common`
- `security`
- `authz` để check quyền
- Module khác qua interface công khai trong package `api` khi thật sự cần

Ví dụ hợp lệ:

- `leadimport` dùng `vn.mar.catalog.api.CatalogLookupService` để validate mã chương trình.
- `user` dùng `vn.mar.branch.api.BranchLookupService` để validate user thuộc branch.
- `audit` nhận event từ các module khác.

Ví dụ không hợp lệ:

- `catalog` gọi ngược vào `leadimport`.
- `tenant` phụ thuộc vào `reporting`.
- Controller của module này gọi thẳng repository của module khác.
- Module này gọi thẳng `service` nội bộ của module khác khi chưa có contract trong `api`.

### 7.3. Tenant boundary rule

Mọi service xử lý dữ liệu tenant-scoped phải có một trong các cơ chế:

- Nhận `tenantId` từ security context.
- Nhận `tenantId` từ request đã được authorize.
- Gọi `TenantContext.currentTenantId()`.
- Query repository bằng `tenantId`.

Không lấy `tenantId` từ request body cho endpoint của user tenant bình thường nếu tenant đã có trong token/context.

### 7.4. Language rule

- Code comment, SQL comment, config comment và log message dùng tiếng Anh.
- Markdown tài liệu nội bộ dùng tiếng Việt có dấu.
- API error code dùng tiếng Anh, UPPER_SNAKE_CASE.
- User-facing message có thể i18n sau; Sprint 1 ưu tiên error code ổn định.

## 8. VÍ DỤ CODE MẪU

### 8.1. Good example - controller mỏng, service rõ trách nhiệm

```java
@RestController
@RequestMapping("/api/v1/catalog/courses")
@RequiredArgsConstructor
public class CourseController {

    private final CourseService courseService;

    @GetMapping
    public ApiResponse<PageResponse<CourseSummaryResponse>> searchCourses(
            @Valid CourseSearchRequest request) {
        return ApiResponse.success(courseService.searchCourses(request));
    }
}
```

```java
@Service
@RequiredArgsConstructor
public class CourseService {

    private final CourseRepository courseRepository;
    private final CourseMapper courseMapper;

    @Transactional(readOnly = true)
    public PageResponse<CourseSummaryResponse> searchCourses(CourseSearchRequest request) {
        Page<Course> page = courseRepository.search(
                request.keyword(),
                request.status(),
                request.toPageable()
        );
        return PageResponse.from(page.map(courseMapper::toSummaryResponse));
    }
}
```

### 8.2. Bad example - controller chứa business logic và trả entity

```java
@RestController
public class BadTenantController {

    private final TenantRepository tenantRepository;

    @PostMapping("/api/v1/tenants")
    public Tenant create(@RequestBody Map<String, Object> body) {
        if (tenantRepository.existsByCode((String) body.get("code"))) {
            throw new RuntimeException("Duplicated");
        }
        Tenant tenant = new Tenant();
        return tenantRepository.save(tenant);
    }
}
```

Vấn đề:

- Request dùng `Map` nên mất API contract.
- Controller gọi repository trực tiếp.
- Business rule nằm trong controller.
- Trả JPA entity ra API.
- Exception không chuẩn hóa.
- Không có validation rõ ràng.

### 8.3. Good example - common code có lý do

```java
public record PageResponse<T>(
        List<T> items,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }
}
```

`PageResponse` được đặt trong `common.pagination` vì dùng chung cho nhiều module và không chứa business rule.

## 9. ANTI-PATTERNS CẦN TRÁNH

Không được:

- Tạo package `utils` làm nơi chứa mọi thứ.
- Tạo global package `dto` chứa toàn bộ DTO của mọi module.
- Viết controller gọi repository trực tiếp.
- Đưa business service vào `common`.
- Dùng `Map<String, Object>` cho request/response đã biết schema.
- Trả JPA entity ra API.
- Đặt tên module theo màn hình thay vì domain.
- Tự thêm dependency vào `pom.xml` khi chưa review.
- Đặt `@Transactional` ở controller.
- Dùng `@Data` trên entity.
- Log token, password, OTP, file content, hoặc dữ liệu tuyển sinh nhạy cảm.
- Viết comment tiếng Việt trong `.java`, `.sql`, `.yml`, `.properties`.
- Tạo package backend phục vụ server-rendered UI.

## 10. TESTING CONVENTIONS

### 10.1. Cấu trúc test package

Test package phải mirror source package:

```text
src/main/java/vn/mar/tenant/service/TenantService.java
src/test/java/vn/mar/tenant/service/TenantServiceTest.java
```

### 10.2. Naming test

Dùng pattern:

```text
methodName_condition_expectedResult
```

Ví dụ:

```java
@Test
void createTenant_whenCodeDuplicated_shouldThrowBusinessException() {
}
```

Quy tắc đặt tên class test:

```text
TenantServiceTest          // unit/service test
TenantControllerTest       // controller slice/API test
TenantRepositoryIT         // integration test with Testcontainers PostgreSQL
FlywayMigrationIT          // migration integration test
PermissionMatrixIT         // integration flow test when needed
```

### 10.3. Test theo layer

Controller test:

- Verify request validation.
- Verify HTTP status.
- Verify response envelope.
- Mock service.

Service test:

- Verify business rule.
- Verify tenant isolation.
- Verify permission path nếu service gọi authz.
- Mock repository/mapper/event publisher khi là unit test.

Repository test:

- Verify query.
- Verify mapping entity-column.
- Dùng test database phù hợp với PostgreSQL behavior cho integration test.

Mapper test:

- Verify field mapping quan trọng.
- Verify không leak sensitive field.

### 10.4. Coverage target

| Layer | Target |
|---|---|
| Service business rule | >= 80% line/branch cho use case quan trọng |
| Controller validation | Có test cho request lỗi chính |
| Repository custom query | Có integration test |
| Mapper có logic | Có unit test |
| Common utility | >= 90% nếu dùng rộng |

## 11. CODE REVIEW CHECKLIST

Reviewer phải kiểm tra:

- [ ] Package nằm dưới `vn.mar`.
- [ ] Module/package bám domain MAR.
- [ ] Module-to-module call đi qua package `api`, không gọi repository/service nội bộ tùy tiện.
- [ ] Class có một trách nhiệm rõ ràng.
- [ ] Controller không chứa business rule.
- [ ] Controller không gọi repository.
- [ ] Service giữ transaction boundary.
- [ ] Repository chỉ chứa persistence query.
- [ ] Request/response dùng DTO/record rõ ràng.
- [ ] Không trả entity ra API.
- [ ] Tenant isolation được enforce ở service/query.
- [ ] Permission được check ở endpoint/use case được bảo vệ.
- [ ] Không đưa business module vào `common`.
- [ ] Không đưa mapper nghiệp vụ vào `common.mapper`.
- [ ] Không tạo base entity riêng khi đã có convention DB.
- [ ] Không có dependency vòng giữa các module.
- [ ] Không dùng `@Data` trên entity.
- [ ] Không log dữ liệu nhạy cảm.
- [ ] Comment trong code/config/SQL dùng tiếng Anh.
- [ ] Test đúng layer responsibility.
- [ ] PR không sửa lan sang ngoài phạm vi task.

## 12. TÀI LIỆU LIÊN QUAN

- `01-architecture-baseline.md` - Nền kiến trúc MAR.
- `03-rest-api-convention.md` - Quy tắc REST API.
- `04-database-flyway-convention.md` - Quy tắc DB/Flyway/entity.
- `05-security-auth-authz-convention.md` - Quy tắc security/auth/authz.
- `06-exception-error-i18n-convention.md` - Quy tắc exception/error code.
- `07-logging-observability-convention.md` - Quy tắc logging.
- `09-testing-quality-convention.md` - Quy tắc test/quality gate.
- OASIS reference: `coding_convention.md`, `coding_convention_principles.md`, `15_common_code_vs_principle.md`.

## 13. LỊCH SỬ CẬP NHẬT

| Phiên bản | Ngày | Người cập nhật | Nội dung |
|---|---|---|---|
| MAR-CONV-1.0 | 30/06/2026 | Tech Lead / Solution Architect | Đồng bộ role/common package với architecture baseline, thêm get/find/search rule, public module `api` boundary, common.mapper guardrail và integration test naming. |
| MAR-CONV-1.0 | 30/06/2026 | Tech Lead / Solution Architect | Chuẩn hóa đầy đủ coding/package convention theo pattern OASIS và map sang MAR Sprint 1. |
