package vn.mar.permission.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import vn.mar.audit.model.AuditActions;
import vn.mar.audit.service.AuditRecordCommand;
import vn.mar.audit.service.AuditService;
import vn.mar.authz.model.PermissionAccessLevel;
import vn.mar.authz.model.PermissionCodes;
import vn.mar.authz.model.PermissionProfile;
import vn.mar.authz.model.PermissionScope;
import vn.mar.authz.repository.PermissionProfileRepository;
import vn.mar.common.error.ErrorCode;
import vn.mar.common.exception.BusinessException;
import vn.mar.common.time.TimeProvider;
import vn.mar.permission.dto.request.PermissionMatrixChangeRequest;
import vn.mar.permission.dto.request.UpdatePermissionMatrixRequest;
import vn.mar.permission.dto.response.PermissionMatrixResponse;
import vn.mar.permission.event.PermissionMatrixUpdatedEvent;
import vn.mar.permission.mapper.PermissionMatrixMapper;
import vn.mar.role.model.RoleStatus;
import vn.mar.role.repository.RoleRepository;
import vn.mar.security.context.CurrentUser;
import vn.mar.security.context.CurrentUserContext;

@ExtendWith(MockitoExtension.class)
class PermissionMatrixServiceTest {

    private static final UUID ACTOR_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID TENANT_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final UUID PROFILE_ID = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
    private static final Instant NOW = Instant.parse("2026-07-01T08:00:00Z");

    @Mock
    private PermissionProfileRepository permissionProfileRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private CurrentUserContext currentUserContext;

    @Mock
    private AuditService auditService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private PermissionMatrixService permissionMatrixService;

    @BeforeEach
    void setUp() {
        TimeProvider timeProvider = () -> NOW;
        permissionMatrixService = new PermissionMatrixService(
                permissionProfileRepository,
                roleRepository,
                new PermissionMatrixMapper(),
                timeProvider,
                currentUserContext,
                auditService,
                eventPublisher
        );
        when(currentUserContext.currentUser()).thenReturn(new CurrentUser(
                ACTOR_ID,
                TENANT_ID,
                "ADMIN",
                Set.of(PermissionCodes.PERMISSION_MANAGE),
                "req_permission_unit_001"
        ));
    }

    @Test
    void getMatrix_whenProfileMissing_shouldReturnFullRoleMatrixWithNoneDefaults() {
        when(permissionProfileRepository.findByTenantId(TENANT_ID)).thenReturn(List.of(adminPermissionManage()));
        when(permissionProfileRepository.findActiveFunctionCodes())
                .thenReturn(List.of(PermissionCodes.IMPORT_MANAGE, PermissionCodes.PERMISSION_MANAGE));

        PermissionMatrixResponse response = permissionMatrixService.getMatrix();

        assertThat(response.tenantId()).isEqualTo(TENANT_ID);
        assertThat(response.permissionCodes()).containsExactly(PermissionCodes.IMPORT_MANAGE, PermissionCodes.PERMISSION_MANAGE);
        assertThat(response.roles()).hasSize(7);
        assertThat(response.roles())
                .filteredOn(role -> role.role().equals("ADMIN"))
                .singleElement()
                .satisfies(role -> assertThat(role.permissions())
                        .filteredOn(permission -> permission.functionCode().equals(PermissionCodes.PERMISSION_MANAGE))
                        .singleElement()
                        .satisfies(permission -> assertThat(permission.accessLevel()).isEqualTo(PermissionAccessLevel.MANAGE.name())));
        assertThat(response.roles())
                .filteredOn(role -> role.role().equals("ADVISOR"))
                .singleElement()
                .satisfies(role -> assertThat(role.permissions())
                        .filteredOn(permission -> permission.functionCode().equals(PermissionCodes.IMPORT_MANAGE))
                        .singleElement()
                        .satisfies(permission -> assertThat(permission.accessLevel()).isEqualTo(PermissionAccessLevel.NONE.name())));
    }

    @Test
    void updateMatrix_whenSalesLeadImportEnabled_shouldPersistAuditAndEvictRoleCache() {
        PermissionProfile beforeProfile = profile(
                "SALES_LEAD",
                PermissionCodes.IMPORT_MANAGE,
                PermissionAccessLevel.VIEW,
                PermissionScope.BRANCH
        );
        PermissionProfile afterProfile = beforeProfile.update(PermissionAccessLevel.CREATE, PermissionScope.BRANCH, ACTOR_ID, NOW);
        when(roleRepository.existsByRoleCodeAndStatus("SALES_LEAD", RoleStatus.ACTIVE)).thenReturn(true);
        when(permissionProfileRepository.existsActiveFunctionCode(PermissionCodes.IMPORT_MANAGE)).thenReturn(true);
        when(permissionProfileRepository.findByTenantId(TENANT_ID))
                .thenReturn(List.of(beforeProfile))
                .thenReturn(List.of(afterProfile));
        when(permissionProfileRepository.findByTenantIdAndRoleCodeAndFunctionCode(
                TENANT_ID,
                "SALES_LEAD",
                PermissionCodes.IMPORT_MANAGE
        )).thenReturn(Optional.of(beforeProfile));
        when(permissionProfileRepository.findActiveFunctionCodes()).thenReturn(List.of(PermissionCodes.IMPORT_MANAGE));

        PermissionMatrixResponse response = permissionMatrixService.updateMatrix(new UpdatePermissionMatrixRequest(
                List.of(new PermissionMatrixChangeRequest(
                        "SALES_LEAD",
                        PermissionCodes.IMPORT_MANAGE,
                        "CREATE",
                        "BRANCH"
                )),
                "Pilot import for branch"
        ));

        assertThat(response.roles())
                .filteredOn(role -> role.role().equals("SALES_LEAD"))
                .singleElement()
                .satisfies(role -> assertThat(role.permissions())
                        .singleElement()
                        .satisfies(permission -> assertThat(permission.accessLevel()).isEqualTo(PermissionAccessLevel.CREATE.name())));

        ArgumentCaptor<PermissionProfile> profileCaptor = ArgumentCaptor.forClass(PermissionProfile.class);
        verify(permissionProfileRepository).update(profileCaptor.capture());
        assertThat(profileCaptor.getValue().accessLevel()).isEqualTo(PermissionAccessLevel.CREATE);

        ArgumentCaptor<AuditRecordCommand> auditCaptor = ArgumentCaptor.forClass(AuditRecordCommand.class);
        verify(auditService).record(auditCaptor.capture());
        assertThat(auditCaptor.getValue().action()).isEqualTo(AuditActions.PERMISSION_MATRIX_UPDATED);
        assertThat(auditCaptor.getValue().reason()).isEqualTo("Pilot import for branch");

        ArgumentCaptor<PermissionMatrixUpdatedEvent> eventCaptor = ArgumentCaptor.forClass(PermissionMatrixUpdatedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().tenantId()).isEqualTo(TENANT_ID);
        assertThat(eventCaptor.getValue().roleCode()).isEqualTo("SALES_LEAD");
    }

    @Test
    void updateMatrix_whenAdvisorExportEnabled_shouldRejectGuardrail() {
        when(roleRepository.existsByRoleCodeAndStatus("ADVISOR", RoleStatus.ACTIVE)).thenReturn(true);
        when(permissionProfileRepository.existsActiveFunctionCode(PermissionCodes.DATA_EXPORT)).thenReturn(true);
        when(permissionProfileRepository.findByTenantId(TENANT_ID)).thenReturn(List.of());

        assertThatThrownBy(() -> permissionMatrixService.updateMatrix(new UpdatePermissionMatrixRequest(
                List.of(new PermissionMatrixChangeRequest(
                        "ADVISOR",
                        PermissionCodes.DATA_EXPORT,
                        "MANAGE",
                        "TENANT"
                )),
                "Try export"
        )))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_PERMISSION_GUARDRAIL);

        verify(permissionProfileRepository, never()).insert(any(PermissionProfile.class));
        verify(permissionProfileRepository, never()).update(any(PermissionProfile.class));
        verify(auditService, never()).record(any(AuditRecordCommand.class));
    }

    @Test
    void updateMatrix_whenMarketingPaymentWriteEnabled_shouldRejectGuardrail() {
        when(roleRepository.existsByRoleCodeAndStatus("MARKETING", RoleStatus.ACTIVE)).thenReturn(true);
        when(permissionProfileRepository.existsActiveFunctionCode(PermissionCodes.PAYMENT_WRITE)).thenReturn(true);
        when(permissionProfileRepository.findByTenantId(TENANT_ID)).thenReturn(List.of());

        assertThatThrownBy(() -> permissionMatrixService.updateMatrix(new UpdatePermissionMatrixRequest(
                List.of(new PermissionMatrixChangeRequest(
                        "MARKETING",
                        PermissionCodes.PAYMENT_WRITE,
                        "UPDATE",
                        "TENANT"
                )),
                "Try payment"
        )))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_PERMISSION_GUARDRAIL);

        verify(auditService, never()).record(any(AuditRecordCommand.class));
    }

    @Test
    void updateMatrix_whenTeamScopeEnabled_shouldRejectReservedScope() {
        when(roleRepository.existsByRoleCodeAndStatus("SALES_LEAD", RoleStatus.ACTIVE)).thenReturn(true);
        when(permissionProfileRepository.existsActiveFunctionCode(PermissionCodes.IMPORT_MANAGE)).thenReturn(true);
        when(permissionProfileRepository.findByTenantId(TENANT_ID)).thenReturn(List.of());

        assertThatThrownBy(() -> permissionMatrixService.updateMatrix(new UpdatePermissionMatrixRequest(
                List.of(new PermissionMatrixChangeRequest(
                        "SALES_LEAD",
                        PermissionCodes.IMPORT_MANAGE,
                        "CREATE",
                        "TEAM"
                )),
                "Try team"
        )))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_PERMISSION_GUARDRAIL);
    }

    private PermissionProfile adminPermissionManage() {
        return profile("ADMIN", PermissionCodes.PERMISSION_MANAGE, PermissionAccessLevel.MANAGE, PermissionScope.TENANT);
    }

    private PermissionProfile profile(
            String roleCode,
            String functionCode,
            PermissionAccessLevel accessLevel,
            PermissionScope scope) {
        return PermissionProfile.create(
                PROFILE_ID,
                TENANT_ID,
                roleCode,
                functionCode,
                accessLevel,
                scope,
                ACTOR_ID,
                NOW
        );
    }
}
