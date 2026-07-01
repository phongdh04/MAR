package vn.mar.permission.event;

import java.util.UUID;

public record PermissionMatrixUpdatedEvent(
        UUID tenantId,
        String roleCode,
        UUID actorId,
        String requestId
) {
}
