package vn.mar.permission.dto.response;

import java.util.List;
import java.util.UUID;

public record PermissionMatrixResponse(
        UUID tenantId,
        List<String> permissionCodes,
        List<PermissionRoleMatrixResponse> roles
) {
}
