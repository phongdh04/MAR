# Kế Hoạch Sửa Convention MAR

Ngày tạo: 2026-07-06
Ngày cập nhật: 2026-07-07
Phạm vi: `mar-backend`
Nguồn tổng hợp: 2 trao đổi gần nhất về duplication/convention và service quá lớn.
Trạng thái review agent: Approved with revisions.

## 1. Kết Luận Ngắn

Code đã tiến gần hơn tới convention sau khi thêm và áp dụng rộng hơn:

- `PermissionGuard`: gom kiểm tra permission dùng chung.
- `BranchScopeGuard`: gom rule Sales Lead chỉ được thao tác trong branch được assign.
- `EnumParser`: gom parser enum dùng chung và tận dụng `SearchText`.

Sau remediation batch ngày 2026-07-07, các mục P0/P1/P2a đã được xử lý trong phạm vi đã chốt. Những nhóm việc còn lại không nên trộn vào cùng batch vì làm thay đổi security contract hoặc là refactor kiến trúc lớn:

- P2b tenant creation theo platform/bootstrap-level: đã có ADR, nhưng chưa implement vì còn thiếu bootstrap/platform actor seed evidence.
- P3 service split: một số service vẫn lớn, cần tách theo use case trong batch riêng.
- P4 API/module boundary: cần giảm leak repository/entity trực tiếp ở một số service lớn, nhưng phải có characterization/API smoke test trước.

Nguyên tắc sau review agent:

- Làm nhỏ từng bước, mỗi thay đổi chỉ có một mục tiêu kỹ thuật rõ ràng.
- Không tạo `common` mới nếu helper hiện có đã xử lý được cùng trách nhiệm.
- Không đổi security/API/DB contract nếu chưa có decision và test bao phủ.
- Mọi bước cleanup phải chạy `mvn test` và `git diff --check`.

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

Cập nhật 2026-07-07:

- Sau khi pull `f018e72`/`fc4b379`, `SlaTaskServiceTest` bị lệch constructor do `SlaTaskService` đã bỏ `UserBranchRepository`.
- Đã sửa test để truyền đúng constructor mới và vẫn dùng `BranchScopeGuard`.
- `mvn test` pass: 274 tests, 0 failures, 0 errors.

### 2.4 Remediation Batch 2026-07-07

Đã thực hiện:

- P0: thay service-level permission check còn sót bằng `PermissionGuard` ở assignment engine, audit query, integration event query, SLA settings/task, duplicate case và customer merge.
- P1: thêm `common.validation.EnumParser`, dùng lại `common.search.SearchText` cho optional text normalization; không tạo `TextNormalizer` mới.
- P2a: tạo ADR `docs BA/20-r1a-platform-tenant-permission-adr.md`.
- Test/evidence: `mvn test` pass 288 tests, 0 failures, 0 errors; `git diff --check` pass.

Chưa làm trong batch này:

- P2b platform tenant permission implementation.
  - Lý do: review agent phát hiện chưa có bootstrap/platform actor seed evidence. Nếu đổi controller ngay, migrated system có thể không còn actor thật nào tạo tenant được.
- P3/P4 service split và API boundary refactor sâu.
- Lý do: đây là refactor kiến trúc lớn, phải có characterization/API smoke test theo flow chính trước khi move logic để tránh đổi behavior ngầm.

Lưu ý: `mvnw.cmd` vẫn lỗi wrapper script, nên hiện đang verify bằng Maven cài ngoài PATH.

## 3. Trạng Thái Remediation Và Việc Cần Sửa Tiếp

### P0 - Cleanup Permission Check Còn Sót

Trạng thái: đã xử lý trong remediation batch 2026-07-07.

Mục tiêu đã chốt: tất cả service-level permission check dùng `PermissionGuard`, trừ các lớp authz framework hoặc chính `PermissionGuard`.

Đã cleanup ở:

- `DuplicateCaseService`
- `CustomerMergeService`
- `AuditQueryService`
- `IntegrationEventQueryApplicationService`
- `SlaSettingsService`
- `SlaTaskService`
- `DefaultAssignmentEngineService`

Tiêu chí đối chiếu sau batch:

- Không còn `actor.hasPermission(...)` trực tiếp trong service domain thuộc phạm vi P0.
- Không còn private `assertHasPermission(...)` trùng lặp trong service domain thuộc phạm vi P0.
- Service-level permission dùng:
  - `permissionGuard.requirePermission(...)`
  - `permissionGuard.requireAnyPermission(...)`
- Controller `@PreAuthorize`, `PermissionAuthorizationService`, `PermissionGuard` và `CurrentUser.hasPermission(...)` không thuộc phạm vi cleanup P0.
- Giữ nguyên `detailCode`, message, status/error behavior.

Rủi ro còn lại: thấp, chủ yếu là regression nếu các service mới sau này tự tạo permission helper thay vì dùng `PermissionGuard`.

### P1 - Chuẩn Hóa Parser/Normalizer Không Trùng Common Hiện Có

Mục tiêu: giảm lặp `trim().toUpperCase(Locale.ROOT)`, `Enum.valueOf(...)`, và `normalizeOptional(...)`.

Trạng thái: đã xử lý trong remediation batch 2026-07-07 cho các service trong scope.

Hiện trạng cần lưu ý:

- Đã có `vn.mar.common.search.SearchText` với:
  - `textOrNull(String value)`
  - `keyword(String value)`
  - `upperOrNull(String value)`
- Không được tạo `TextNormalizer` mới chỉ để lặp lại các hàm trên.

Đã xử lý:

- `vn.mar.common.validation.EnumParser`
  - `optionalEnum(Class<E> enumType, String value, String field, String code, String message)`
  - `requiredEnum(Class<E> enumType, String value, String field, String code, String message)`
- Nếu cần normalize text ngoài search:
  - Ưu tiên mở rộng `SearchText` khi trách nhiệm vẫn là normalize input/search/filter.
  - Chỉ tạo package/class mới sau khi có ít nhất 3 use case không thuộc search/filter và tên class không chồng nghĩa với `SearchText`.
  - Không refactor email/phone/code domain-specific vào common generic.

Đã áp dụng ở:

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
- `IntegrationEventQueryApplicationService`

Tiêu chí đối chiếu sau batch:

- Không còn pattern `Enum.valueOf(...)`/`valueOf(trim().toUpperCase(...))` rải rác trong các service đã nêu.
- Optional text normalization dùng lại `SearchText.textOrNull(...)` hoặc `SearchText.upperOrNull(...)`.
- Common parser có test riêng cho optional, required, blank và invalid enum.

Rủi ro còn lại: thấp đến trung bình khi áp dụng cho service mới; phải giữ nguyên message lỗi hiện tại.

### P2 - Sửa Tenant Creation Theo Platform/Bootstrap Convention

Hiện tại:

- `POST /api/v1/tenants` đang dùng `tenant.manage`.

Convention:

- Tenant creation nên là platform/bootstrap-level.
- Nếu có platform admin, nên dùng permission dạng `platform.tenant.manage`.

Đề xuất:

P2a - Decision/ADR trước khi code:

Trạng thái: đã làm trong `docs BA/20-r1a-platform-tenant-permission-adr.md`.

1. Kiểm tra model permission hiện tại có hỗ trợ platform scope chưa.
2. Chốt rõ khái niệm platform admin, tenant admin và tenant-scoped admin.
3. Chốt tên permission: đề xuất `platform.tenant.manage`.
4. Chốt migration/seed và actor nào được cấp quyền này ở local/test.

P2b - Implement sau khi ADR được chốt:

Trạng thái: chưa làm. Review agent thống nhất chưa được đổi code trước khi có bootstrap/platform actor seed evidence.

1. Thêm constant `PLATFORM_TENANT_MANAGE = "platform.tenant.manage"` nếu cần.
2. Thêm migration seed permission mới.
3. Đổi `TenantController#createTenant` sang permission mới.
4. Cập nhật test tenant/security.
5. Giữ `tenant.manage` cho update/get tenant-scoped.

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

1. Bổ sung characterization test cho các flow chính trước khi move logic.
2. Tách `OpportunityAuditRecorder` trước nếu audit payload đã có test đủ.
3. Tách `OpportunityStageTransitionService`.
4. Tách `OpportunityActivityService`.
5. Tách resolver/lookup sau khi có api boundary tốt hơn.

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

Riêng với `AdmissionOpportunityService`, nên làm P4a trước khi tách sâu resolver:

- Tạo `catalog.api.CatalogLookupService`.
- Tạo `branch.api.BranchLookupService`.
- Tạo `user.api.UserLookupService`.
- Sau đó mới tách `OpportunityCatalogResolver`.

## 4. Đề Xuất Thứ Tự Thực Hiện

1. Done - P0 PermissionGuard cleanup còn lại.
2. Done - P1 EnumParser + tận dụng/mở rộng `SearchText`.
3. Done - P2a Tenant platform permission ADR/decision.
4. Next blocker decision - P2b chỉ implement sau khi có bootstrap/platform actor seed evidence.
5. P3a - Tách `CatalogService` theo aggregate, kèm characterization/API smoke test.
6. P3b - Tách `SlaSettingsService` theo calculator/working-hours/policy, kèm unit test rule.
7. P4a - Tạo lookup API boundary tối thiểu cho catalog/branch/user.
8. P3c - Tách `AdmissionOpportunityService` theo audit/stage/activity/resolver.
9. P4b - API boundary refactor tiếp từng module còn lại.

## 5. Ý Kiến Các Agent

### David - System Architect

Đồng ý không nên refactor lớn ngay. Ưu tiên các thay đổi hạn chế rủi ro trước:

- Permission và branch scope guard là đúng hướng vì tăng consistency và security.
- Tenant creation là điểm kiến trúc cần sửa, nhưng phải có ADR/decision trước khi đổi security contract.
- API boundary là cần thiết cho độ bền 2-3 năm, nhưng nên làm theo từng module; với `AdmissionOpportunityService`, lookup boundary nên đi trước resolver extraction.

Đề xuất của David:

1. Không đổi DB/API contract nếu chưa có test bao phủ.
2. Mỗi boundary service phải trả DTO/snapshot, không expose JPA entity.
3. Tenant creation phải tách rõ platform/bootstrap và tenant-scoped permission.

### Bob - Tech Lead

Đồng ý ưu tiên convention có giá trị thực tế:

- Common chỉ tạo khi có reuse thật sự. `PermissionGuard` và `BranchScopeGuard` đạt tiêu chí.
- Enum parser nên làm vì duplication đã xuất hiện ở nhiều service.
- Text normalizer phải tận dụng `SearchText` hiện có, không tạo class mới chồng trách nhiệm.
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

### Sarah - QC Lead

Đồng ý kế hoạch nhưng yêu cầu bổ sung negative tests trước khi kết luận pass:

- Missing permission phải trả đúng 403 và đúng error envelope.
- Null/missing actor trong service-level guard phải bị chặn nhất quán.
- Sales Lead không có branch phải bị chặn với `BRANCH_SCOPE_REQUIRED`.
- Sales Lead truy cập branch ngoài scope phải bị chặn với `OUT_OF_SCOPE`.
- Invalid enum phải giữ nguyên field/code/message hiện tại.
- Blank/trimmed text phải giữ nguyên behavior hiện tại.

Đề xuất của Sarah:

1. Với P0, thêm hoặc cập nhật test cho từng service còn permission check.
2. Với P1, mỗi enum parser adoption phải có invalid enum test.
3. Với P3/P4, cần smoke test/API test cho flow chính trước và sau refactor.

### Mattin - BA Lead

Đồng ý kế hoạch ở mức kỹ thuật, nhưng tài liệu cần rõ tiêu chí đối chiếu:

- Phải ghi rõ controller `@PreAuthorize` và authz framework không nằm trong phạm vi P0 cleanup service-level.
- Phải tách decision khỏi implementation ở P2 để tránh đổi security contract lén.
- Phải có acceptance criteria định lượng cho từng đợt cleanup.

Đề xuất của Mattin:

1. Mỗi task cleanup phải nêu rõ phạm vi file/service.
2. Mỗi task phải có "không đổi behavior" hoặc "có đổi contract" rõ ràng.
3. Nếu đổi contract, phải có decision log/ADR trước.

## 6. Acceptance Criteria Cho Đợt Cleanup Tiếp Theo

- Không còn private permission helper trùng lặp trong service domain.
- Permission check service-level dùng `PermissionGuard`.
- Controller `@PreAuthorize`, `PermissionAuthorizationService`, `PermissionGuard` và `CurrentUser.hasPermission(...)` không thuộc phạm vi cleanup P0.
- Sales Lead branch-scope dùng `BranchScopeGuard`.
- Common enum parser có test riêng.
- Text normalization không tạo helper trùng `SearchText`.
- Các service tách mới không expose JPA entity qua module boundary.
- Negative tests bao phủ missing permission, null actor, out-of-scope branch, invalid enum và blank text ở nơi có thay đổi.
- `mvn test` pass.
- `git diff --check` pass.
