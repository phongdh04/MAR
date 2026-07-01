package vn.mar.authz.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import vn.mar.authz.repository.PermissionProfileRepository;
import vn.mar.common.cache.CacheConfig;
import vn.mar.common.cache.CacheEvictionService;

@SpringJUnitConfig(classes = {
        CacheConfig.class,
        CacheEvictionService.class,
        PermissionProfileCacheService.class,
        PermissionProfileResolver.class
})
class PermissionProfileResolverTest {

    private static final UUID TENANT_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");

    @Autowired
    private PermissionProfileResolver permissionProfileResolver;

    @Autowired
    private CacheEvictionService cacheEvictionService;

    @MockitoBean
    private PermissionProfileRepository permissionProfileRepository;

    @BeforeEach
    void clearPermissionCache() {
        cacheEvictionService.clearPermissionProfiles();
    }

    @Test
    void resolvePermissionCodes_whenCacheHit_shouldReadRepositoryOnlyOnce() {
        when(permissionProfileRepository.findActivePermissionCodes(TENANT_ID, "ADMIN"))
                .thenReturn(Set.of("branch.view"));

        Set<String> firstResolve = permissionProfileResolver.resolvePermissionCodes(TENANT_ID, " admin ");
        Set<String> secondResolve = permissionProfileResolver.resolvePermissionCodes(TENANT_ID, "ADMIN");

        assertThat(firstResolve).containsExactly("branch.view");
        assertThat(secondResolve).containsExactly("branch.view");
        verify(permissionProfileRepository, times(1)).findActivePermissionCodes(TENANT_ID, "ADMIN");
    }

    @Test
    void resolvePermissionCodes_whenCacheEvicted_shouldReflectUpdatedPermissionProfile() {
        when(permissionProfileRepository.findActivePermissionCodes(TENANT_ID, "ADMIN"))
                .thenReturn(Set.of("branch.view"))
                .thenReturn(Set.of("branch.manage"));

        assertThat(permissionProfileResolver.resolvePermissionCodes(TENANT_ID, "ADMIN"))
                .containsExactly("branch.view");

        cacheEvictionService.evictPermissionProfile(TENANT_ID, "ADMIN");

        assertThat(permissionProfileResolver.resolvePermissionCodes(TENANT_ID, "ADMIN"))
                .containsExactly("branch.manage");
        verify(permissionProfileRepository, times(2)).findActivePermissionCodes(TENANT_ID, "ADMIN");
    }

    @Test
    void resolvePermissionCodes_whenContextMissing_shouldFailClosedWithoutDbRead() {
        assertThat(permissionProfileResolver.resolvePermissionCodes(null, "ADMIN")).isEmpty();
        assertThat(permissionProfileResolver.resolvePermissionCodes(TENANT_ID, " ")).isEmpty();

        verifyNoInteractions(permissionProfileRepository);
    }

    @Test
    void hasPermission_whenPermissionExists_shouldReturnTrue() {
        when(permissionProfileRepository.findActivePermissionCodes(TENANT_ID, "ADMIN"))
                .thenReturn(Set.of("tenant.view", "tenant.manage"));

        assertThat(permissionProfileResolver.hasPermission(TENANT_ID, "ADMIN", "tenant.manage")).isTrue();
        assertThat(permissionProfileResolver.hasPermission(TENANT_ID, "ADMIN", "audit.view")).isFalse();
    }
}
