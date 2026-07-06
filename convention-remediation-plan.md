# Kế Hoạch Sửa Convention MAR

Ngày tạo: 2026-07-06
Phạm vi: `mar-backend`
Nguồn tổng hợp: 2 trao đổi gần nhất về duplication/convention và service quá lớn.

## 1. Kết Luận Ngắn

Code đã tiến gần hơn tới convention sau khi thêm:

- `PermissionGuard`: gom kiểm tra permission dùng chung.
- `BranchScopeGuard`: gom rule Sales Lead chỉ được thao tác trong branch được assign.

Tuy nhiên chưa thể kết luận là "tuân thủ tuyệt đối". Vẫn còn các nhóm việc cần sửa:

- Permission check còn lặp ở customer/audit/integration/SLA/assignment engine.
- Enum parsing và normalize text còn lặp ở nhiều service.
- Tenant creation chưa đúng tính chất platform/bootstrap-level.
- Một số service đang quá lớn, cần tách theo use case.
- Boundary giữa module còn leak repository/entity trực tiếp ở một số service lớn.

## 2. Những Việc Đã Làm

### 2.1 Permission Guard

Đã thêm `vn.mar.authz.service.PermissionGuard`.

Đã áp dụng vào:

- `AssignmentRuleService`
- `AdmissionOpportunityService`

Đã có test:

- `PermissionGuardTest`

### 2.2 Branch Scope Guard

Đã thêm `vn.mar.authz.service.BranchScopeGuard`.

Đã áp dụng vào:

- `AssignmentRuleService`
- `DefaultAssignmentEngineService`
- `SlaSettingsService`
- `SlaTaskService`

Đã có test:

- `BranchScopeGuardTest`

### 2.3 Verify Hiện Tại

Đã chạy:

- `cmd /c mvn "-Dtest=BranchScopeGuardTest,PermissionGuardTest" test`
- `cmd /c mvn test`
- `git diff --check`

Kết quả: pass.

Lưu ý: `mvnw.cmd` vẫn lỗi wrapper script, nên hiện đang verify bằng Maven cài ngoài PATH.

## 3. Việc Cần Sửa Tiếp

### P0 - Cleanup Permission Check Còn Sót

Mục tiêu: tất cả service-level permission check nên dùng `PermissionGuard`, trừ các lớp authz framework hoặc chính `PermissionGuard`.

Đang còn lặp:

- `DuplicateCaseService`
- `CustomerMergeService`
- `AuditQueryService`
- `IntegrationEventQueryApplicationService`
- `SlaSettingsService`
- `SlaTaskService`
- `DefaultAssignmentEngineService`

Hướng xử lý:

- Inject `PermissionGuard`.
- Thay `actor.hasPermission(...)` và private `assertHasPermission(...)` bằng:
  - `permissionGuard.requirePermission(...)`
  - `permissionGuard.requireAnyPermission(...)`
- Giữ nguyên `detailCode`, message, status/error behavior.
- Chạy `mvn test`.

Rủi ro: thấp.

### P1 - Tạo Common Parser/Normalizer

Mục tiêu: giảm lặp `trim().toUpperCase(Locale.ROOT)`, `Enum.valueOf(...)`, và `normalizeOptional(...)`.

Đề xuất tạo:

- `vn.mar.common.text.TextNormalizer`
  - `optionalTrim(String value)`
  - `requiredTrim(String value, String field, String code, String message)`
- `vn.mar.common.validation.EnumParser`
  - `optionalEnum(Class<E> enumType, String value, String field, String code, String message)`
  - `requiredEnum(Class<E> enumType, String value, String field, String code, String message)`

Nơi cần áp dụng dần:

- `BranchService`
- `CatalogService`
- `AssignmentRuleService`
- `AdmissionOpportunityService`
- `SlaTaskService`
- `SlaSettingsService`
- `DuplicateCaseService`
- `LeadImportQueryService`
- `PermissionMatrixService`
- `TenantService`
- `UserService`

Rủi ro: thấp đến trung bình. Cần làm từng service để giữ nguyên message lỗi hiện tại.

### P2 - Sửa Tenant Creation Theo Platform/Bootstrap Convention

Hiện tại:

- `POST /api/v1/tenants` đang dùng `tenant.manage`.

Convention:

- Tenant creation nên là platform/bootstrap-level.
- Nếu có platform admin, nên dùng permission dạng `platform.tenant.manage`.

Đề xuất:

1. Kiểm tra model permission hiện tại có hỗ trợ platform scope chưa.
2. Thêm constant `PLATFORM_TENANT_MANAGE = "platform.tenant.manage"` nếu cần.
3. Thêm migration seed permission mới.
4. Đổi `TenantController#createTenant` sang permission mới.
5. Cập nhật test tenant/security.
6. Giữ `tenant.manage` cho update/get tenant-scoped.

Rủi ro: trung bình, vì có thể đổi security contract.

### P3 - Giảm Service Quá Lớn Theo Use Case

Không nên tách cơ học theo số dòng. Nên tách từng cụm private method có nghiệp vụ rõ ràng, giữ public API hiện tại.

#### AdmissionOpportunityService

Hiện trạng: service lớn nhất, nhiều responsibility.

Đề xuất tách:

- `OpportunityCatalogResolver`
  - Resolve language/program/course/branch/owner.
- `OpportunityStageTransitionService`
  - Validate transition, lost reason, reopen rule, duration stage.
- `OpportunityActivityService`
  - Create/search activity.
- `OpportunityAuditRecorder`
  - Audit opportunity/activity/stage/touchpoint.
- `OpportunityVisibilityGuard`
  - Advisor own-scope và visibility rule.

Thứ tự an toàn:

1. Tách `OpportunityAuditRecorder` trước.
2. Tách `OpportunityStageTransitionService`.
3. Tách `OpportunityActivityService`.
4. Tách resolver/lookup sau khi có api boundary tốt hơn.

#### SlaSettingsService

Đề xuất tách:

- `WorkingHoursService`
- `SlaPolicyService`
- `DueTimeCalculator`

Thứ tự an toàn:

1. Tách `DueTimeCalculator`.
2. Tách working-hours update/get.
3. Tách SLA policy update/get.

#### CatalogService

Đề xuất tách theo aggregate:

- `LanguageService`
- `ProgramService`
- `CourseService`
- Giữ `CatalogService` làm facade nếu controller đang phụ thuộc vào nó.

Đây là ứng viên nên làm trước vì ít cross-module và pattern lặp rõ.

#### LeadImportPreviewService

Đề xuất tách pipeline:

- `LeadImportFileValidator`
- `LeadImportRowParser`
- `LeadImportRowNormalizer`
- `LeadImportDuplicateDetector`
- `LeadImportPreviewPersister`

Nên làm sau vì import có nhiều edge case.

### P4 - Giảm Boundary Leak Bằng API Boundary

Hiện trạng: một số service import repository/entity trực tiếp từ module khác, ví dụ opportunity dùng branch/catalog/lead/customer/user repository.

Đề xuất tạo api boundary dần dần:

- `branch.api.BranchLookupService`
- `catalog.api.CatalogLookupService`
- `user.api.UserLookupService`
- `lead.api.LeadLookupService`
- `customer.api.CustomerLookupService`

Nguyên tắc:

- Module khác chỉ gọi qua `api`, không gọi repository trực tiếp.
- Làm từng module, không refactor lớn một lần.
- Giữ snapshot/command object gọn, không expose entity JPA.

Rủi ro: trung bình đến cao, nên làm sau P0/P1/P3 nhỏ.

## 4. Đề Xuất Thứ Tự Thực Hiện

1. P0 - PermissionGuard cleanup còn lại.
2. P1 - TextNormalizer + EnumParser, áp dụng từ các service dễ trước.
3. P3a - Tách `CatalogService` theo aggregate.
4. P3b - Tách `SlaSettingsService` theo calculator/working-hours/policy.
5. P2 - Tenant platform permission, sau khi confirm model platform.
6. P3c - Tách `AdmissionOpportunityService`.
7. P4 - API boundary refactor từng module.

## 5. Ý Kiến Các Agent

### David - System Architect

Đồng ý không nên refactor lớn ngay. Ưu tiên các thay đổi hạn chế rủi ro trước:

- Permission và branch scope guard là đúng hướng vì tăng consistency và security.
- Tenant creation là điểm kiến trúc cần sửa, nhưng phải kiểm tra platform permission model trước.
- API boundary là cần thiết cho độ bền 2-3 năm, nhưng nên làm theo từng module.

Đề xuất của David:

1. Không đổi DB/API contract nếu chưa có test bao phủ.
2. Mỗi boundary service phải trả DTO/snapshot, không expose JPA entity.
3. Tenant creation phải tách rõ platform/bootstrap và tenant-scoped permission.

### Bob - Tech Lead

Đồng ý ưu tiên convention có giá trị thực tế:

- Common chỉ tạo khi có reuse thật sự. `PermissionGuard` và `BranchScopeGuard` đạt tiêu chí.
- Enum/text normalizer nên làm vì duplication đã xuất hiện ở nhiều service.
- Service quá lớn cần tách theo use case, không tách theo "cho ngắn".

Đề xuất của Bob:

1. Mỗi PR/refactor chỉ nên có một mục tiêu rõ.
2. Phải chạy `mvn test` sau mỗi bước.
3. Chưa nên làm API boundary lớn nếu P0/P1 chưa xong.

### Kevin - Senior Backend Developer

Đồng ý cách làm incremental:

- Trước khi tách service lớn, cần characterization test cho behavior hiện tại.
- Khi move logic, giữ message lỗi và audit payload không đổi.
- Service gốc có thể làm orchestrator tạm thời để controller/API không bị ảnh hưởng.

Đề xuất của Kevin:

1. Làm P0 trước vì nhanh, ít rủi ro.
2. Với `AdmissionOpportunityService`, tách audit/stage/activity theo từng lần, không làm tất cả cùng lúc.
3. Mỗi component mới cần unit test riêng nếu có rule nghiệp vụ.

## 6. Acceptance Criteria Cho Đợt Cleanup Tiếp Theo

- Không còn private permission helper trùng lặp trong service domain.
- Permission check service-level dùng `PermissionGuard`.
- Sales Lead branch-scope dùng `BranchScopeGuard`.
- Common parser/normalizer có test riêng.
- Các service tách mới không expose JPA entity qua module boundary.
- `mvn test` pass.
- `git diff --check` pass.
