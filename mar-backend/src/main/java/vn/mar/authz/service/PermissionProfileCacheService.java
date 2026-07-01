package vn.mar.authz.service;

import java.util.Set;
import java.util.UUID;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import vn.mar.authz.repository.PermissionProfileRepository;
import vn.mar.common.cache.CacheKeyBuilder;
import vn.mar.common.cache.CacheNames;

@Service
public class PermissionProfileCacheService {

    private final PermissionProfileRepository permissionProfileRepository;

    public PermissionProfileCacheService(PermissionProfileRepository permissionProfileRepository) {
        this.permissionProfileRepository = permissionProfileRepository;
    }

    @Cacheable(
            cacheNames = CacheNames.PERMISSION_PROFILE,
            key = "T(vn.mar.common.cache.CacheKeyBuilder).permissionProfile(#tenantId, #roleCode)",
            sync = true
    )
    public Set<String> loadPermissionCodes(UUID tenantId, String roleCode) {
        return permissionProfileRepository.findActivePermissionCodes(
                tenantId,
                CacheKeyBuilder.normalizeRoleCode(roleCode)
        );
    }
}
