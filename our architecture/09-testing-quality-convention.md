# TESTING & QUALITY CONVENTION - QUY TẮC TEST MAR

**Ngày tạo:** 30/06/2026  
**Phiên bản:** MAR-CONV-1.1  
**Tác giả:** Tech Lead / Solution Architect  
**Trạng thái:** Locked for Sprint 1 technical kickoff  
**Stack:** JUnit 5, AssertJ, Mockito, Spring Boot Test, MockMvc, Testcontainers PostgreSQL, Flyway  
**Tham chiếu:**
- `D:\Documents-for-Expert-Design-Database\MAR\architecture\coding_convention.md` - Pattern testing OASIS
- `D:\Documents-for-Expert-Design-Database\MAR\architecture\database_convention.md` - Pattern repository/migration test
- `D:\Documents-for-Expert-Design-Database\MAR\architecture\api_security_convention.md` - Pattern security/API test
- `D:\Documents-for-Expert-Design-Database\MAR\our architecture\03-rest-api-convention.md` - API contract MAR

## 1. TỔNG QUAN & MỤC ĐÍCH

Tài liệu này chuẩn hóa test và quality gate cho backend MAR.

Mục đích:

- Chứng minh Sprint 1 foundation không phá tenant isolation, permission, migration và error contract.
- Giúp team viết test nhất quán, dễ review, ít flaky.
- Bảo vệ các flow P0/P1 trước khi kickoff dev/sprint acceptance.
- Tránh test giả an toàn như disable security hoặc dùng DB khác behavior production.

Nguyên tắc ghi nhớ:

> **"Test không chỉ để tăng coverage; test để khóa rủi ro quan trọng."**

## 2. PHẠM VI ÁP DỤNG

Áp dụng cho:

- Unit test.
- Controller/API contract test.
- Security test.
- Service/business rule test.
- Repository/integration test.
- Flyway migration test.
- Audit test.
- Logging/observability test.
- Import foundation test.
- CI quality gate.

Không áp dụng cho:

- Manual UAT script chi tiết của BA/QA.
- Frontend E2E test.
- Performance/load test quy mô production nếu chưa vào scope.
- Penetration test chuyên sâu.

## 3. NGUYÊN TẮC CHUNG

1. **Risk-based coverage:** ưu tiên tenant isolation, permission, validation, migration, audit, import.
2. **Fast feedback first:** unit test nhanh chạy ở `mvn test`.
3. **Real DB for DB behavior:** PostgreSQL-specific behavior test bằng PostgreSQL container.
4. **Security is part of API test:** không disable security rồi coi là controller đã đủ test.
5. **Contract stable:** API test assert envelope/status/error code quan trọng.
6. **No manual DB dependency:** test không phụ thuộc database setup tay.
7. **Deterministic fixtures:** test data rõ, không dùng data production.
8. **Readable test names:** tên test nói rõ condition và expected result.
9. **One reason to fail:** mỗi test nên kiểm một behavior chính.
10. **No flaky time/random:** time/random phải control bằng fixture/provider.
11. **Observability is tested:** request id, MDC, actuator security và masking phải có test.
12. **No sensitive logging/audit:** test/review phải bắt được raw password/token/import row nếu bị log/audit.

## 4. QUY TẮC ĐẶT TÊN

### 4.1. Test class naming

| Loại test | Convention | Ví dụ |
|---|---|---|
| Unit test | `<ClassUnderTest>Test` | `TenantServiceTest` |
| Controller test | `<Controller>Test` | `UserControllerTest` |
| Repository integration | `<Repository>IT` | `BranchRepositoryIT` |
| Migration integration | `FlywayMigrationIT` | `FlywayMigrationIT` |
| Security integration | `<Feature>SecurityIT` | `UserSecurityIT` |
| End-to-end slice | `<Flow>IT` | `PermissionMatrixIT` |

### 4.2. Test method naming

Pattern:

```text
methodName_whenCondition_shouldExpectedResult
```

Ví dụ:

```java
createTenant_whenNameBlank_shouldReturnValidationError()
getBranch_whenCrossTenant_shouldReturnNotFound()
updatePermissionMatrix_whenMissingPermission_shouldReturnForbidden()
createImportBatch_whenFileInvalid_shouldReturnValidationError()
```

### 4.3. Fixture naming

Fixture dùng tên có nghĩa:

```text
tenantA
tenantB
adminA
advisorA
adminB
branchA1
languageEnglish
programIelts
courseIeltsFoundation
```

Không dùng `test1`, `abc`, `foo`.

### 4.4. Tag naming

JUnit tag nếu cần:

```text
unit
integration
security
migration
observability
contract
slow
```

CI có thể chạy `unit`/`contract` thường xuyên và `integration`/`observability` ở `mvn verify` hoặc profile riêng.

## 5. CẤU TRÚC FILE & PACKAGE

### 5.1. Test source layout

```text
src/test/java
└── vn/mar
    ├── tenant
    ├── branch
    ├── user
    ├── authz
    ├── catalog
    ├── leadimport
    ├── audit
    └── testsupport
```

Test package mirror source package.

### 5.2. Test support package

```text
vn.mar.testsupport
├── fixture
│   ├── TenantFixtures.java
│   ├── UserFixtures.java
│   └── CatalogFixtures.java
├── security
│   ├── WithMockMarUser.java
│   └── MarSecurityTestSupport.java
├── db
│   ├── PostgresTestContainer.java
│   └── FlywayTestSupport.java
└── json
    └── JsonTestSupport.java
```

Rules:

- Test support không chứa business production code.
- Fixture phải rõ tenant/user/permission.
- Không phụ thuộc production seed.

### 5.3. Maven test phases

Đề xuất:

```text
mvn test     -> unit + slice tests nhanh
mvn verify   -> integration tests, migration tests, contract tests
```

Nếu dùng Failsafe:

```text
*Test.java -> Surefire
*IT.java   -> Failsafe
```

CI pipeline baseline:

| Gate | Khi chạy | Commands tối thiểu |
|---|---|---|
| PR fast gate | Mỗi PR | `mvn test` + format/checkstyle nếu dùng |
| Merge/full gate | Trước merge hoặc trước Sprint acceptance | `mvn verify -Pintegration` |
| Nightly regression | Hằng ngày nếu pipeline đủ | Full integration/security/migration/observability |

Ticket chạm DB migration, security, tenant isolation, audit hoặc import phải qua full integration gate trước khi acceptance.

### 5.4. Test resources

```text
src/test/resources
├── application-test.yml
├── fixtures
└── import-samples
```

Import sample file phải nhỏ, rõ case, không chứa dữ liệu thật.

## 6. CÁC PATTERN BẮT BUỘC

### 6.1. Test pyramid pattern

| Layer | Mục tiêu | Công cụ |
|---|---|---|
| Unit | Business rule/helper/mapper | JUnit 5, AssertJ, Mockito |
| Slice | Controller validation/API envelope | `@WebMvcTest`, MockMvc |
| Repository | Query/mapping/index behavior | `@DataJpaTest`, Testcontainers PostgreSQL |
| Integration | Security + DB + service flow | `@SpringBootTest` |
| Migration | Fresh DB migrate/validate | Flyway + PostgreSQL container |

Không dùng full `@SpringBootTest` cho mọi test nếu slice/unit test đủ.

### 6.2. Unit service test pattern

```java
@ExtendWith(MockitoExtension.class)
class BranchServiceTest {

    @Mock
    private BranchRepository branchRepository;

    @Mock
    private CurrentUserContext currentUserContext;

    @InjectMocks
    private BranchService branchService;

    @Test
    void createBranch_whenCodeDuplicated_shouldThrowConflictException() {
        UUID tenantId = UUID.randomUUID();
        when(currentUserContext.currentTenantId()).thenReturn(tenantId);
        when(branchRepository.existsByTenantIdAndCodeIgnoreCase(tenantId, "HN01"))
                .thenReturn(true);

        assertThatThrownBy(() -> branchService.createBranch(new CreateBranchRequest("HN01", "Ha Noi")))
                .isInstanceOf(ConflictException.class);
    }
}
```

### 6.3. Controller/API test pattern

```java
@WebMvcTest(BranchController.class)
class BranchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BranchService branchService;

    @Test
    void createBranch_whenNameBlank_shouldReturnValidationError() throws Exception {
        mockMvc.perform(post("/api/v1/branches")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "code": "HN01",
                                  "name": ""
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.meta.request_id").exists());
    }
}
```

Controller test phải assert status và response envelope.

### 6.4. Security test pattern

Test auth fixture phải giống production context shape:

```text
tenant_id
actor_id
role_code
permissions
```

Ví dụ:

```java
@Test
void updateUser_whenMissingPermission_shouldReturnForbidden() throws Exception {
    mockMvc.perform(patch("/api/v1/users/{user_id}", userId)
                    .with(marUser()
                            .tenantId(tenantA)
                            .actorId(advisorA)
                            .roleCode("ADVISOR")
                            .permissions("user.view"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"status\":\"INACTIVE\"}"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.error.code").value("PERMISSION_DENIED"));
}
```

### 6.5. Repository test pattern

Repository test dùng migrated PostgreSQL schema:

```java
@DataJpaTest
@Testcontainers
class BranchRepositoryIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17");

    @Autowired
    private BranchRepository branchRepository;

    @Test
    void findByIdAndTenantId_whenBranchBelongsToOtherTenant_shouldReturnEmpty() {
        Optional<Branch> result = branchRepository.findByIdAndTenantId(branchA1Id, tenantBId);

        assertThat(result).isEmpty();
    }
}
```

Không dùng H2 cho JSONB, partial index, PostgreSQL SQL syntax hoặc Flyway behavior.

### 6.6. Migration test pattern

```java
@Testcontainers
class FlywayMigrationIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17");

    @Test
    void migrate_whenFreshDatabase_shouldSucceed() {
        Flyway flyway = Flyway.configure()
                .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                .locations("classpath:db/migration")
                .load();

        MigrateResult result = flyway.migrate();

        assertThat(result.success).isTrue();
    }
}
```

### 6.7. Audit test pattern

```java
@Test
void updatePermissionMatrix_whenSuccess_shouldCreateAuditEvent() {
    permissionMatrixService.updateMatrix(request);

    List<AuditEvent> events = auditEventRepository.findByAction(AuditAction.PERMISSION_MATRIX_UPDATED);
    assertThat(events).hasSize(1);
    assertThat(events.get(0).getTenantId()).isEqualTo(tenantA);
    assertThat(events.get(0).getActorId()).isEqualTo(adminA);
}
```

### 6.8. Import foundation test pattern

Test:

- Create import batch.
- Store mapping config JSONB.
- Store row error JSONB.
- Query errors by `tenant_id + batch_id`.
- Block cross-tenant import history.

### 6.9. Time/random pattern

Production code nên phụ thuộc `Clock` hoặc `CurrentTimeProvider` cho logic thời gian:

```java
@Bean
Clock clock() {
    return Clock.systemUTC();
}
```

Test dùng fixed clock:

```java
Clock fixedClock = Clock.fixed(Instant.parse("2026-06-30T00:00:00Z"), ZoneOffset.UTC);
```

Không dùng `Thread.sleep` trong test nếu có thể tránh.

### 6.10. Observability test pattern

Required tests:

- Request không có `X-Request-Id` thì backend generate request id.
- Request có `X-Request-Id` hợp lệ thì backend preserve.
- Response luôn có `X-Request-Id`.
- Error response có `meta.request_id`.
- MDC clear sau request.
- `RequestLoggingFilter` không log full body.
- Actuator metrics/prometheus protected theo profile.

Các test này đồng bộ trực tiếp với `07-logging-observability-convention.md`.

### 6.11. Contract/golden response test pattern

Các error nền phải có contract test/golden response để FE/QA không bị vỡ field:

```text
VALIDATION_ERROR
PERMISSION_DENIED
RESOURCE_NOT_FOUND
DUPLICATE_ACTIVE_BRANCH
INVALID_PARENT_STATUS
IMPORT_BATCH_NOT_FOUND
```

Rules:

- Assert `error.code`, `error.message`, `meta.request_id`.
- Với validation/import error, assert `error.details[*].field` và `error.details[*].code`.
- Không snapshot raw dynamic value như UUID/timestamp nếu không normalize.

### 6.12. Test data isolation pattern

Integration test phải isolate dữ liệu:

- Mỗi test dùng tenant/fixture rõ, ví dụ `tenantA`, `tenantB`.
- Không phụ thuộc thứ tự chạy test.
- Nếu DB container share giữa test, dùng transaction rollback hoặc cleanup rõ.
- Không dùng production seed/data.
- Test import sample phải nhỏ, deterministic và không chứa PII thật.

## 7. QUY TẮC RIÊNG CỦA MAR QUALITY

### 7.1. P0 risk areas

P0 phải có test rõ:

- Tenant isolation.
- Permission enforcement.
- Permission matrix guardrail.
- User/role/branch assignment.
- Catalog parent-child status rule.
- Lead import foundation storage/query.
- Audit sensitive changes.
- Audit failure behavior.
- Logging/observability baseline.
- No sensitive logging/audit payload.
- Error envelope.
- Flyway migration.

### 7.2. Required API test matrix

| Endpoint nhóm | Required cases |
|---|---|
| Tenant | happy, validation, forbidden |
| Branch | happy, validation, forbidden, cross-tenant |
| User | happy, validation, duplicate email, forbidden, cross-tenant |
| Permission matrix | happy, guardrail, forbidden, audit |
| Catalog | parent inactive, duplicate code/name, list pagination |
| Import | invalid file/config, row error query, cross-tenant |
| Audit | permission denied, tenant filter, pagination |
| Observability | request_id generate/preserve, MDC clear, no body logging, actuator protected |
| Error contract | validation, forbidden, not found/cross-tenant, duplicate, invalid parent, import batch not found |

### 7.2.1. Tenant isolation minimum test set

Mọi resource tenant-scoped phải có tối thiểu:

- List endpoint không trả dữ liệu tenant khác.
- Detail endpoint tenant khác trả `404`.
- Patch/update endpoint tenant khác không update dữ liệu.
- Search/filter/sort không làm lộ tenant khác.
- Import batch/rows cross-tenant bị chặn.
- Audit query tenant khác bị chặn.

Không chỉ test cross-tenant detail; list/search/update cũng phải được khóa.

### 7.3. Coverage guidance

Không chạy theo coverage tổng thể một cách mù quáng. Target theo rủi ro:

| Area | Target gợi ý |
|---|---:|
| Permission/authz service | >= 85% |
| Tenant isolation service/query | >= 85% |
| Service business rule P0 | >= 80% |
| Error handler/error code mapping | >= 80% |
| Audit service/payload sanitizer | >= 80% |
| Common utility dùng rộng | >= 90% |

### 7.4. Definition of Done backend ticket

Một ticket backend được coi là done khi:

- Code compile.
- Unit/slice tests liên quan pass.
- Migration có nếu DB thay đổi.
- API contract/update docs nếu endpoint thay đổi.
- Permission test có nếu endpoint protected.
- Tenant isolation test có nếu resource tenant-scoped.
- Error envelope test có cho negative path.
- Audit test có nếu sensitive change.
- Observability test có nếu chạm request filter/logging/actuator.
- Contract/golden response cập nhật nếu error/API envelope thay đổi.
- OpenAPI/API contract update nếu endpoint mới hoặc response shape đổi.
- Không có secret/log sensitive data.

### 7.5. Flaky test policy

Flaky test phải xử lý như bug:

- Không ignore lâu dài.
- Xác định do time/random/concurrency/external dependency.
- Fix bằng deterministic fixture, fake clock, container readiness hoặc retry đúng chỗ.

### 7.6. Audit failure behavior test rule

Các flow sau phải có integration/unit test tương ứng khi implement:

- Permission matrix update + audit save fail -> rollback.
- User status change + audit save fail -> rollback.
- Tenant/branch status change + audit save fail -> rollback.
- Permission denied audit fail -> vẫn trả `403/404` đúng policy, không grant access, có metric/log theo `08`.
- Login audit fail -> behavior đúng decision, không leak secret.

### 7.7. No sensitive logging/audit quality gate

Quality gate bắt buộc:

- `LogMasker` tests pass.
- `RequestLoggingFilter` không log body mặc định.
- `AuditPayloadSanitizer` tests pass.
- Không có test nào assert raw password/token/authorization header xuất hiện trong log/audit.
- Import raw row không bị log/audit full payload.

## 8. VÍ DỤ CODE MẪU

### 8.1. Good example - cross-tenant service test

```java
@Test
void getBranch_whenBranchBelongsToAnotherTenant_shouldThrowNotFound() {
    UUID branchId = UUID.randomUUID();
    UUID tenantA = UUID.randomUUID();
    UUID tenantB = UUID.randomUUID();

    when(currentUserContext.currentTenantId()).thenReturn(tenantA);
    when(branchRepository.findByIdAndTenantId(branchId, tenantA)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> branchService.getBranch(branchId))
            .isInstanceOf(ResourceNotFoundException.class);

    verify(branchRepository).findByIdAndTenantId(branchId, tenantA);
    verify(branchRepository, never()).findById(branchId);
}
```

### 8.2. Good example - API error envelope test

```java
mockMvc.perform(get("/api/v1/branches/{branch_id}", branchId)
                .with(marUser().tenantId(tenantA).permissions("branch.view")))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.error.code").value("RESOURCE_NOT_FOUND"))
        .andExpect(jsonPath("$.meta.request_id").exists());
```

### 8.3. Bad example - security disabled

```java
@AutoConfigureMockMvc(addFilters = false)
class UserControllerTest {
}
```

Vấn đề:

- Không test authentication.
- Không test permission.
- Có thể bỏ sót lỗi `401/403`.
- Không chứng minh API thật được bảo vệ.

## 9. ANTI-PATTERNS CẦN TRÁNH

Không được:

- Chỉ test happy path.
- Disable security cho toàn bộ API test và coi là đủ.
- Dùng H2 cho PostgreSQL-specific behavior.
- Phụ thuộc manual DB setup.
- Dùng production seed/data trong test.
- Test quá nhiều implementation detail không ảnh hưởng behavior.
- Dùng `Thread.sleep` thay vì await/fake clock.
- Viết test name không mô tả scenario.
- Assert response string tự do thay vì error code/envelope.
- Bỏ qua cross-tenant test.
- Chỉ test cross-tenant detail mà bỏ list/search/update.
- Bỏ qua migration test khi có thay đổi DB.
- Bỏ qua request_id/MDC/actuator tests khi sửa logging/observability.
- Bỏ qua audit failure behavior khi sửa permission/user/tenant sensitive flow.
- Snapshot contract chứa dynamic UUID/timestamp chưa normalize.
- Dùng chung DB/container mà phụ thuộc test execution order.
- Ignore flaky test lâu dài.

## 10. TESTING CONVENTIONS

### 10.1. Unit tests

Unit test dùng cho:

- Service business rule.
- Validator.
- Mapper có logic.
- Error code mapping.
- Permission helper.
- Payload sanitizer.

### 10.2. Slice tests

Slice test dùng cho:

- Controller validation.
- Error envelope.
- Security filter/handler theo scope.
- Serialization/deserialization.

### 10.3. Integration tests

Integration test dùng cho:

- Repository + PostgreSQL.
- Flyway migration.
- Security + API + service flow.
- Audit write/query.
- Audit failure transaction behavior.
- Import storage/query.
- Observability filter/actuator/security integration nếu cần.

### 10.4. Manual QA handoff

Backend ticket có API mới phải cung cấp:

- Endpoint/method.
- Required permission.
- Test data/fixture.
- Expected status.
- Important negative cases.

### 10.5. CI gates

PR fast gate:

```text
mvn test
```

Gồm:

- Unit tests.
- Slice/controller tests.
- Error contract tests đủ nhanh.
- Format/checkstyle nếu project bật.

Merge/full gate hoặc before Sprint acceptance:

```text
mvn verify -Pintegration
```

Gồm:

- Testcontainers PostgreSQL.
- FlywayMigrationIT.
- SecurityIT.
- Audit/observability integration tests liên quan.

Nightly regression nếu integration quá nặng cho mọi PR:

```text
mvn verify -Pintegration -Pnightly
```

Không đưa ticket P0 vào Sprint acceptance nếu chưa chạy full gate tương ứng với risk area.

### 10.6. Observability test pack

Required cases:

- Request without `X-Request-Id` generates one.
- Request with valid `X-Request-Id` preserves it.
- Response contains `X-Request-Id`.
- Error response has `meta.request_id`.
- MDC is cleared after request.
- Request logging does not log full body.
- Actuator metrics/prometheus protected according to profile.

### 10.7. Audit/error/log contract test pack

Required cases:

- Permission matrix update creates audit event.
- Audit event does not contain password/token/raw import row.
- Audit query enforces tenant isolation.
- `VALIDATION_ERROR` golden response.
- `PERMISSION_DENIED` golden response.
- `RESOURCE_NOT_FOUND`/cross-tenant golden response.
- `DUPLICATE_ACTIVE_BRANCH` golden response.
- `INVALID_PARENT_STATUS` golden response.
- `IMPORT_BATCH_NOT_FOUND` golden response.

### 10.8. Test data cleanup/isolation

Rules:

- Each integration test creates or loads its own deterministic tenant fixture.
- No test depends on another test's inserted data.
- Use transaction rollback or explicit cleanup if a DB container is shared.
- Use fresh migrated schema for migration tests.
- Import fixtures must be synthetic and minimal.

## 11. CODE REVIEW CHECKLIST

- [ ] Test name mô tả scenario và expected result.
- [ ] P0 endpoint có positive và negative tests.
- [ ] Protected endpoint có `401/403` tests.
- [ ] Tenant-scoped resource có cross-tenant test.
- [ ] Tenant isolation minimum set covered for list/detail/update/search where applicable.
- [ ] Error envelope được assert.
- [ ] Contract/golden response updated for stable error envelope.
- [ ] Migration test có nếu DB thay đổi.
- [ ] Repository test dùng PostgreSQL khi có DB-specific behavior.
- [ ] Security context fixture giống production shape.
- [ ] Audit assertion có với sensitive change.
- [ ] Audit failure behavior tested for sensitive configuration write.
- [ ] Observability tests cover request_id/MDC/actuator when touched.
- [ ] No sensitive logging/audit payload tests or checklist passed.
- [ ] Test data isolated; no execution-order dependency.
- [ ] OpenAPI/API contract updated when endpoint/response changes.
- [ ] Import JSONB/query có test nếu chạm import foundation.
- [ ] Test không phụ thuộc data production/manual setup.
- [ ] Không có flaky sleep/random/time uncontrolled.
- [ ] CI command được cập nhật nếu thêm test profile.

## 12. TÀI LIỆU LIÊN QUAN

- `02-coding-package-convention.md` - Test package mirror source.
- `03-rest-api-convention.md` - API contract/error/pagination tests.
- `04-database-flyway-convention.md` - Migration/repository tests.
- `05-security-auth-authz-convention.md` - Security tests.
- `06-exception-error-i18n-convention.md` - Error envelope tests.
- `07-logging-observability-convention.md` - Request id/MDC/actuator/log masking tests.
- `08-audit-convention.md` - Audit tests.
- OASIS reference: `coding_convention.md`, `database_convention.md`, `api_security_convention.md`.

## 13. LỊCH SỬ CẬP NHẬT

| Phiên bản | Ngày | Người cập nhật | Nội dung |
|---|---|---|---|
| MAR-CONV-1.1 | 30/06/2026 | Tech Lead / Solution Architect | Bổ sung CI gate PR/full/nightly, observability test pack, audit failure behavior tests, contract/golden error responses, tenant isolation minimum set, test data isolation và no sensitive logging/audit gate. |
| MAR-CONV-1.0 | 30/06/2026 | Tech Lead / Solution Architect | Chuẩn hóa testing/quality convention theo pattern OASIS, map sang risk-based QA gate của MAR Sprint 1. |
