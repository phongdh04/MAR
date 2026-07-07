# ADR-20260707-01 - Platform Tenant Creation Permission

> Phiên bản: `v1.0 - 2026-07-07`.
> Trạng thái: **Accepted for implementation planning**.
> Phạm vi: `mar-backend`, tenant creation API, permission seed, local/test security evidence.

## 1. Bối cảnh

`POST /api/v1/tenants` hiện đang dùng `tenant.manage`. Cách này chưa đúng convention vì tạo tenant là hành động platform/bootstrap-level, còn `tenant.manage` nên là quyền quản trị tenant-scoped cho tenant đã tồn tại.

Các convention liên quan:

- `our architecture/03-rest-api-convention.md`: tenant create/update by platform admin phải dùng platform permission rõ.
- `our architecture/05-security-auth-authz-convention.md`: tenant creation là platform-level hoặc bootstrap-only.
- `convention-remediation-plan.md`: P2 yêu cầu ADR trước khi đổi security contract.

## 2. Hiện trạng kỹ thuật

- `CurrentUser` đang mang `actorId`, `tenantId`, `roleCode`, `permissionCodes`, `requestId`.
- `PermissionProfile` hiện tenant-scoped qua `tenantId`.
- `PermissionCodes` chưa có `platform.tenant.manage`.
- Seed permission hiện có `tenant.view`, `tenant.manage` và các quyền foundation khác.
- API hiện tại:
  - `POST /api/v1/tenants`: đang dùng `tenant.manage`.
  - `GET /api/v1/tenants/{tenant_id}`: dùng `tenant.view` hoặc `tenant.manage`.
  - `PATCH /api/v1/tenants/{tenant_id}`: dùng `tenant.manage`.

## 3. Quyết định

Chốt thêm permission riêng:

```text
platform.tenant.manage
```

Ý nghĩa:

- Dùng cho hành động tạo tenant mới qua `POST /api/v1/tenants`.
- Là quyền platform/bootstrap-level.
- Không thay thế `tenant.manage`.
- `tenant.manage` tiếp tục dùng cho update tenant-scoped và các thao tác quản trị tenant đã tồn tại.

P2b implementation phải:

1. Thêm constant `PermissionCodes.PLATFORM_TENANT_MANAGE = "platform.tenant.manage"`.
2. Seed permission `platform.tenant.manage`.
3. Đổi `TenantController#createTenant` sang `@PreAuthorize("@authz.hasPermission(authentication, 'platform.tenant.manage')")`.
4. Giữ `tenant.manage` cho `PATCH /api/v1/tenants/{tenant_id}`.
5. Giữ `tenant.view` hoặc `tenant.manage` cho `GET /api/v1/tenants/{tenant_id}`.
6. Cập nhật local/test permission fixture để actor tạo tenant có `platform.tenant.manage`.
7. Thêm security/API tests:
   - Có `platform.tenant.manage` tạo tenant thành công.
   - Chỉ có `tenant.manage` gọi create tenant bị `403`.
   - Không làm hỏng update/get tenant-scoped.

## 4. Vì sao chốt như vậy

- Tạo tenant là hành động vượt ra ngoài tenant hiện tại. Nếu dùng `tenant.manage`, tenant admin của một tenant có thể bị hiểu nhầm là có quyền tạo tenant khác.
- Tách `platform.tenant.manage` giúp audit và review permission matrix rõ hơn.
- Giữ `tenant.manage` cho update/get giúp không phá tenant-scoped admin flow hiện tại.
- Cách này phù hợp với model hiện tại vì `CurrentUser.permissionCodes` đã là set string; chưa cần đổi ngay sang bảng platform-scope riêng.

## 5. Vì sao chưa chọn cách khác

| Option | Lý do không chọn lúc này |
|---|---|
| Tiếp tục dùng `tenant.manage` cho create tenant | Không phân biệt platform-level và tenant-scoped action; dễ mở rộng sai quyền. |
| Tạo platform role/table permission scope riêng ngay | Đúng hướng dài hạn nhưng đổi DB/security model lớn hơn P2b; chưa cần để sửa endpoint create tenant. |
| Chỉ cho bootstrap script tạo tenant, không expose API | Có thể dùng cho tenant đầu tiên, nhưng admin/API setup vẫn cần contract rõ nếu product có quản trị tenant. |
| Dùng hard-code role `CEO`/`ADMIN` | Trái convention permission-based authz; khó audit và khó mở rộng. |

## 6. Ràng buộc triển khai

- Không đổi DB permission model sang platform scope trong P2b nếu chưa có ADR riêng.
- Không cấp `platform.tenant.manage` mặc định cho mọi tenant admin production.
- Local/test seed được phép cấp quyền này cho actor bootstrap/admin để giữ evidence tự động.
- Mọi thay đổi phải chạy `mvn test` và `git diff --check`.

## 7. Traceability

- Source remediation: `convention-remediation-plan.md`, P2a/P2b.
- Decision register: `16-techlead-sa-decision-register.md`, P0-17.
- Rationale: `17-techlead-sa-decision-rationale.md`, P0-17.
- Convention: `our architecture/03-rest-api-convention.md`, `our architecture/05-security-auth-authz-convention.md`.
