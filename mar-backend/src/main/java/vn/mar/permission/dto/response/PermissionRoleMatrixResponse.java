package vn.mar.permission.dto.response;

import java.util.List;

public record PermissionRoleMatrixResponse(
        String role,
        List<PermissionMatrixEntryResponse> permissions
) {
}
