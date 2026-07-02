package vn.mar.permission.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.mar.authz.model.PermissionAccessLevel;
import vn.mar.authz.model.PermissionCodes;
import vn.mar.authz.model.PermissionProfile;
import vn.mar.authz.model.PermissionScope;
import vn.mar.authz.repository.PermissionProfileRepository;

@ExtendWith(MockitoExtension.class)
class PermissionMatrixInitializationServiceTest {

    private static final UUID ACTOR_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID TENANT_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final Instant NOW = Instant.parse("2026-07-01T08:00:00Z");

    @Mock
    private PermissionProfileRepository permissionProfileRepository;

    @Test
    void initializeDefaults_whenTenantHasNoMatrix_shouldInsertBaselineForAllRoles() {
        PermissionMatrixInitializationService service = new PermissionMatrixInitializationService(permissionProfileRepository);
        when(permissionProfileRepository.existsByTenantId(TENANT_ID)).thenReturn(false);
        when(permissionProfileRepository.findActiveFunctionCodes()).thenReturn(List.of(
                PermissionCodes.PERMISSION_MANAGE,
                PermissionCodes.IMPORT_MANAGE,
                PermissionCodes.DATA_EXPORT,
                PermissionCodes.PAYMENT_WRITE,
                PermissionCodes.LEAD_VIEW
        ));

        service.initializeDefaults(TENANT_ID, ACTOR_ID, NOW);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Collection<PermissionProfile>> captor = ArgumentCaptor.forClass(Collection.class);
        verify(permissionProfileRepository).insertAll(captor.capture());
        Collection<PermissionProfile> profiles = captor.getValue();
        assertThat(profiles).hasSize(35);
        assertThat(profiles).anySatisfy(profile -> {
            assertThat(profile.roleCode()).isEqualTo("ADMIN");
            assertThat(profile.functionCode()).isEqualTo(PermissionCodes.PERMISSION_MANAGE);
            assertThat(profile.accessLevel()).isEqualTo(PermissionAccessLevel.MANAGE);
            assertThat(profile.scope()).isEqualTo(PermissionScope.TENANT);
        });
        assertThat(profiles).anySatisfy(profile -> {
            assertThat(profile.roleCode()).isEqualTo("ADVISOR");
            assertThat(profile.functionCode()).isEqualTo(PermissionCodes.DATA_EXPORT);
            assertThat(profile.accessLevel()).isEqualTo(PermissionAccessLevel.NONE);
            assertThat(profile.scope()).isEqualTo(PermissionScope.NONE);
        });
        assertThat(profiles).anySatisfy(profile -> {
            assertThat(profile.roleCode()).isEqualTo("ADVISOR");
            assertThat(profile.functionCode()).isEqualTo(PermissionCodes.LEAD_VIEW);
            assertThat(profile.accessLevel()).isEqualTo(PermissionAccessLevel.VIEW);
            assertThat(profile.scope()).isEqualTo(PermissionScope.OWN);
        });
        assertThat(profiles).anySatisfy(profile -> {
            assertThat(profile.roleCode()).isEqualTo("MARKETING");
            assertThat(profile.functionCode()).isEqualTo(PermissionCodes.PAYMENT_WRITE);
            assertThat(profile.accessLevel()).isEqualTo(PermissionAccessLevel.NONE);
        });
    }

    @Test
    void initializeDefaults_whenTenantAlreadyHasMatrix_shouldSkipInsert() {
        PermissionMatrixInitializationService service = new PermissionMatrixInitializationService(permissionProfileRepository);
        when(permissionProfileRepository.existsByTenantId(TENANT_ID)).thenReturn(true);

        service.initializeDefaults(TENANT_ID, ACTOR_ID, NOW);

        verify(permissionProfileRepository, never()).findActiveFunctionCodes();
        verify(permissionProfileRepository, never()).insertAll(org.mockito.ArgumentMatchers.anyCollection());
    }
}
