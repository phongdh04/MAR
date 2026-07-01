package vn.mar.authz.service;

import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class PermissionProfileResolver {

    private final PermissionProfileCacheService permissionProfileCacheService;

    public PermissionProfileResolver(PermissionProfileCacheService permissionProfileCacheService) {
        this.permissionProfileCacheService = permissionProfileCacheService;
    }

    public Set<String> resolvePermissionCodes(UUID tenantId, String roleCode) {
        if (tenantId == null || !StringUtils.hasText(roleCode)) {
            return Set.of();
        }
        return Set.copyOf(permissionProfileCacheService.loadPermissionCodes(tenantId, roleCode.trim()));
    }

    public boolean hasPermission(UUID tenantId, String roleCode, String permissionCode) {
        return StringUtils.hasText(permissionCode)
                && resolvePermissionCodes(tenantId, roleCode).contains(permissionCode);
    }
}
