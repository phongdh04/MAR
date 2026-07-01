package vn.mar.tenant.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import vn.mar.audit.model.AuditActions;
import vn.mar.audit.service.AuditRecordCommand;
import vn.mar.audit.service.AuditService;
import vn.mar.common.error.ErrorCode;
import vn.mar.common.exception.BusinessException;
import vn.mar.common.exception.ValidationException;
import vn.mar.common.time.TimeProvider;
import vn.mar.security.context.CurrentUser;
import vn.mar.security.context.CurrentUserContext;
import vn.mar.tenant.dto.request.CreateTenantRequest;
import vn.mar.tenant.dto.request.UpdateTenantRequest;
import vn.mar.tenant.dto.response.TenantDetailResponse;
import vn.mar.tenant.entity.Tenant;
import vn.mar.tenant.mapper.TenantMapper;
import vn.mar.tenant.model.TenantStatus;
import vn.mar.tenant.repository.TenantRepository;

@ExtendWith(MockitoExtension.class)
class TenantServiceTest {

    private static final UUID ACTOR_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID ACTOR_TENANT_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final UUID TENANT_ID = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
    private static final Instant NOW = Instant.parse("2026-07-01T08:00:00Z");

    @Mock
    private TenantRepository tenantRepository;

    @Mock
    private CurrentUserContext currentUserContext;

    @Mock
    private AuditService auditService;

    private TenantService tenantService;

    @BeforeEach
    void setUp() {
        TimeProvider timeProvider = () -> NOW;
        tenantService = new TenantService(
                tenantRepository,
                new TenantMapper(),
                timeProvider,
                currentUserContext,
                auditService
        );
        when(currentUserContext.currentUser()).thenReturn(new CurrentUser(
                ACTOR_ID,
                ACTOR_TENANT_ID,
                "ADMIN",
                Set.of("tenant.manage"),
                "req_unit_001"
        ));
    }

    @Test
    void createTenant_whenTimezoneAndCurrencyMissing_shouldDefaultAndAuditCreated() {
        when(tenantRepository.save(any(Tenant.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TenantDetailResponse response = tenantService.createTenant(new CreateTenantRequest(
                null,
                "ABC Language Center",
                null,
                null,
                null
        ));

        assertThat(response.tenantName()).isEqualTo("ABC Language Center");
        assertThat(response.timezone()).isEqualTo("Asia/Ho_Chi_Minh");
        assertThat(response.defaultCurrency()).isEqualTo("VND");
        assertThat(response.status()).isEqualTo("ACTIVE");
        assertThat(response.tenantCode()).startsWith("ABC_LANGUAGE_CENTER-");

        ArgumentCaptor<AuditRecordCommand> auditCaptor = ArgumentCaptor.forClass(AuditRecordCommand.class);
        verify(auditService).record(auditCaptor.capture());
        assertThat(auditCaptor.getValue().action()).isEqualTo(AuditActions.TENANT_CREATED);
        assertThat(auditCaptor.getValue().actorId()).isEqualTo(ACTOR_ID);
    }

    @Test
    void createTenant_whenCodeDuplicate_shouldRejectWithConflict() {
        when(tenantRepository.existsByTenantCodeIgnoreCase("ABC")).thenReturn(true);

        assertThatThrownBy(() -> tenantService.createTenant(new CreateTenantRequest(
                "abc",
                "ABC Language Center",
                "Asia/Ho_Chi_Minh",
                "VND",
                "ACTIVE"
        )))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.DUPLICATE_TENANT_CODE);
    }

    @Test
    void updateTenant_whenStatusChangedToInactive_shouldAuditStatusChanged() {
        Tenant tenant = activeTenant();
        when(tenantRepository.findById(TENANT_ID)).thenReturn(Optional.of(tenant));
        when(tenantRepository.save(any(Tenant.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TenantDetailResponse response = tenantService.updateTenant(TENANT_ID, new UpdateTenantRequest(
                null,
                null,
                null,
                "INACTIVE",
                "Pilot ended"
        ));

        assertThat(response.status()).isEqualTo("INACTIVE");
        ArgumentCaptor<AuditRecordCommand> auditCaptor = ArgumentCaptor.forClass(AuditRecordCommand.class);
        verify(auditService).record(auditCaptor.capture());
        assertThat(auditCaptor.getValue().action()).isEqualTo(AuditActions.TENANT_STATUS_CHANGED);
        assertThat(auditCaptor.getValue().reason()).isEqualTo("Pilot ended");
    }

    @Test
    void updateTenant_whenTimezoneInvalid_shouldRejectWithValidationError() {
        when(tenantRepository.findById(TENANT_ID)).thenReturn(Optional.of(activeTenant()));

        assertThatThrownBy(() -> tenantService.updateTenant(TENANT_ID, new UpdateTenantRequest(
                null,
                "Mars/Nope",
                null,
                null,
                null
        )))
                .isInstanceOf(ValidationException.class);
    }

    private Tenant activeTenant() {
        return Tenant.restore(
                TENANT_ID,
                "ABC",
                "ABC Language Center",
                "Asia/Ho_Chi_Minh",
                "VND",
                TenantStatus.ACTIVE,
                NOW,
                ACTOR_ID,
                NOW,
                ACTOR_ID
        );
    }
}
