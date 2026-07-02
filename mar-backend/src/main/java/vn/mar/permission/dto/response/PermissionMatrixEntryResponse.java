package vn.mar.permission.dto.response;

import java.time.Instant;
import java.util.UUID;

public record PermissionMatrixEntryResponse(
        UUID permissionProfileId,
        String functionCode,
        String accessLevel,
        String scope,
        String status,
        Instant updatedAt
) {
}
