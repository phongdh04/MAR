package vn.mar.authz.service;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import vn.mar.common.cache.CacheEvictionService;
import vn.mar.permission.event.PermissionMatrixUpdatedEvent;

@Component
public class PermissionProfileCacheInvalidationListener {

    private final CacheEvictionService cacheEvictionService;

    public PermissionProfileCacheInvalidationListener(CacheEvictionService cacheEvictionService) {
        this.cacheEvictionService = cacheEvictionService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPermissionMatrixUpdated(PermissionMatrixUpdatedEvent event) {
        cacheEvictionService.evictPermissionProfile(event.tenantId(), event.roleCode());
    }
}
