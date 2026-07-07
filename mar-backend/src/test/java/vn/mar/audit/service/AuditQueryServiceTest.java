package vn.mar.audit.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.mar.audit.api.AuditEventSearchCommand;
import vn.mar.audit.api.AuditEventSnapshot;
import vn.mar.audit.entity.AuditEvent;
import vn.mar.audit.mapper.AuditEventMapper;
import vn.mar.audit.model.AuditActions;
import vn.mar.audit.model.AuditResourceTypes;
import vn.mar.audit.repository.AuditEventRepository;
import vn.mar.authz.model.PermissionCodes;
import vn.mar.authz.service.PermissionGuard;
import vn.mar.common.error.ErrorCode;
import vn.mar.common.exception.BusinessException;
import vn.mar.common.exception.ValidationException;
import vn.mar.common.pagination.PageResponse;
import vn.mar.security.context.CurrentUser;
import vn.mar.security.context.CurrentUserContext;

@ExtendWith(MockitoExtension.class)
class AuditQueryServiceTest {

    private static final UUID ACTOR_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID TENANT_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final UUID RESOURCE_ID = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
    private static final Instant NOW = Instant.parse("2026-07-06T01:00:00Z");
    private static final Instant FROM = Instant.parse("2026-07-06T00:00:00Z");
    private static final Instant TO = Instant.parse("2026-07-06T02:00:00Z");

    @Mock
    private AuditEventRepository auditEventRepository;

    @Mock
    private CurrentUserContext currentUserContext;

    private AuditQueryService auditQueryService;

    @BeforeEach
    void setUp() {
        auditQueryService = new AuditQueryService(
                auditEventRepository,
                currentUserContext,
                new AuditEventMapper(),
                new PermissionGuard()
        );
    }

    @Test
    void searchEvents_whenActorHasAuditView_shouldQueryCurrentTenantAndReturnPayload() {
        allowAuditView();
        AuditEvent event = auditEvent();
        when(auditEventRepository.search(
                eq(TENANT_ID),
                eq(AuditResourceTypes.USER),
                eq(RESOURCE_ID),
                eq("user@example.com"),
                eq(ACTOR_ID),
                eq("USER"),
                eq(AuditActions.USER_STATUS_CHANGED),
                eq("req_audit_001"),
                eq(FROM),
                eq(TO),
                any(Pageable.class)
        )).thenReturn(new PageImpl<>(List.of(event), PageRequest.of(0, 20), 1));

        PageResponse<AuditEventSnapshot> response = auditQueryService.searchEvents(new AuditEventSearchCommand(
                "user",
                RESOURCE_ID,
                " user@example.com ",
                ACTOR_ID,
                "user",
                "user_status_changed",
                " req_audit_001 ",
                FROM,
                TO,
                0,
                20
        ));

        assertThat(response.totalElements()).isEqualTo(1);
        assertThat(response.items()).hasSize(1);
        AuditEventSnapshot snapshot = response.items().getFirst();
        assertThat(snapshot.tenantId()).isEqualTo(TENANT_ID);
        assertThat(snapshot.actorId()).isEqualTo(ACTOR_ID);
        assertThat(snapshot.action()).isEqualTo(AuditActions.USER_STATUS_CHANGED);
        assertThat(snapshot.resourceType()).isEqualTo(AuditResourceTypes.USER);
        assertThat(snapshot.beforeData()).containsEntry("status", "ACTIVE");
        assertThat(snapshot.afterData()).containsEntry("status", "INACTIVE");
        assertThat(snapshot.reason()).isEqualTo("Deactivate user");
    }

    @Test
    void getEvent_whenEventBelongsToCurrentTenant_shouldReturnDetail() {
        allowAuditView();
        AuditEvent event = auditEvent();
        when(auditEventRepository.findByIdAndTenantId(event.id(), TENANT_ID)).thenReturn(Optional.of(event));

        AuditEventSnapshot snapshot = auditQueryService.getEvent(event.id());

        assertThat(snapshot.auditEventId()).isEqualTo(event.id());
        assertThat(snapshot.requestId()).isEqualTo("req_audit_001");
    }

    @Test
    void getEvent_whenEventNotInCurrentTenant_shouldReturnNotFound() {
        allowAuditView();
        UUID auditEventId = UUID.randomUUID();
        when(auditEventRepository.findByIdAndTenantId(auditEventId, TENANT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> auditQueryService.getEvent(auditEventId))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
    }

    @Test
    void searchEvents_whenPermissionMissing_shouldRejectWithoutRepositoryCall() {
        when(currentUserContext.currentUser()).thenReturn(new CurrentUser(
                ACTOR_ID,
                TENANT_ID,
                "ADVISOR",
                Set.of(PermissionCodes.USER_VIEW),
                "req_audit_002"
        ));

        assertThatThrownBy(() -> auditQueryService.searchEvents(emptySearch()))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.PERMISSION_DENIED);
        verify(auditEventRepository, never()).search(
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any()
        );
    }

    @Test
    void searchEvents_whenFromIsAfterTo_shouldReject() {
        allowAuditView();

        assertThatThrownBy(() -> auditQueryService.searchEvents(new AuditEventSearchCommand(
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                TO,
                FROM,
                0,
                20
        ))).isInstanceOf(ValidationException.class);
    }

    private void allowAuditView() {
        when(currentUserContext.currentUser()).thenReturn(new CurrentUser(
                ACTOR_ID,
                TENANT_ID,
                "ADMIN",
                Set.of(PermissionCodes.AUDIT_VIEW),
                "req_audit_001"
        ));
    }

    private AuditEventSearchCommand emptySearch() {
        return new AuditEventSearchCommand(null, null, null, null, null, null, null, null, null, 0, 20);
    }

    private AuditEvent auditEvent() {
        return AuditEvent.create(new AuditRecordCommand(
                TENANT_ID,
                ACTOR_ID,
                "USER",
                "ADMIN",
                AuditActions.USER_STATUS_CHANGED,
                AuditResourceTypes.USER,
                RESOURCE_ID,
                "user@example.com",
                Map.of("status", "ACTIVE"),
                Map.of("status", "INACTIVE"),
                Map.of("source", "unit-test"),
                "Deactivate user",
                "req_audit_001"
        ), NOW);
    }
}
