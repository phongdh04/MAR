package vn.mar.common.cache;

import java.util.UUID;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

@Service
public class CacheEvictionService {

    private final CacheManager cacheManager;

    public CacheEvictionService(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    public void evictPermissionProfile(UUID tenantId, String roleCode) {
        Cache cache = cacheManager.getCache(CacheNames.PERMISSION_PROFILE);
        if (cache != null) {
            cache.evictIfPresent(CacheKeyBuilder.permissionProfile(tenantId, roleCode));
        }
    }

    public void clearPermissionProfiles() {
        Cache cache = cacheManager.getCache(CacheNames.PERMISSION_PROFILE);
        if (cache != null) {
            cache.clear();
        }
    }
}
