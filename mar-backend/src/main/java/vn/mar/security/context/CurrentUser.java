package vn.mar.security.context;

import java.util.Set;
import java.util.UUID;

public record CurrentUser(
        UUID actorId,
        UUID tenantId,
        String roleCode,
        Set<String> permissionCodes,
        String requestId
) {

    public CurrentUser {
        permissionCodes = permissionCodes == null ? Set.of() : Set.copyOf(permissionCodes);
    }

    public boolean hasPermission(String permissionCode) {
        return permissionCode != null
                && !permissionCode.isBlank()
                && permissionCodes.contains(permissionCode);
    }
}
