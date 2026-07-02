package vn.mar.authz.model;

import java.time.Instant;
import java.util.UUID;

public record PermissionProfile(
        UUID id,
        UUID tenantId,
        String roleCode,
        String functionCode,
        PermissionAccessLevel accessLevel,
        PermissionScope scope,
        PermissionProfileStatus status,
        Instant createdAt,
        UUID createdBy,
        Instant updatedAt,
        UUID updatedBy
) {

    public static PermissionProfile create(
            UUID id,
            UUID tenantId,
            String roleCode,
            String functionCode,
            PermissionAccessLevel accessLevel,
            PermissionScope scope,
            UUID actorId,
            Instant now) {
        return new PermissionProfile(
                id,
                tenantId,
                roleCode,
                functionCode,
                accessLevel,
                scope,
                PermissionProfileStatus.ACTIVE,
                now,
                actorId,
                now,
                actorId
        );
    }

    public PermissionProfile update(
            PermissionAccessLevel accessLevel,
            PermissionScope scope,
            UUID actorId,
            Instant now) {
        return new PermissionProfile(
                id,
                tenantId,
                roleCode,
                functionCode,
                accessLevel,
                scope,
                PermissionProfileStatus.ACTIVE,
                createdAt,
                createdBy,
                now,
                actorId
        );
    }
}
