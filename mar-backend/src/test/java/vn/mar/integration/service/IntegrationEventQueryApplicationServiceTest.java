package vn.mar.integration.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import vn.mar.authz.model.PermissionCodes;
import vn.mar.authz.service.PermissionGuard;
import vn.mar.common.error.ErrorCode;
import vn.mar.common.exception.BusinessException;
import vn.mar.common.exception.ValidationException;
import vn.mar.common.pagination.PageResponse;
import vn.mar.integration.api.IntegrationEventSearchCommand;
import vn.mar.integration.api.IntegrationEventSnapshot;
import vn.mar.integration.entity.IntegrationEvent;
import vn.mar.integration.mapper.IntegrationEventMapper;
import vn.mar.integration.model.IntegrationEventStatus;
import vn.mar.integration.repository.IntegrationEventRepository;
import vn.mar.lead.model.LeadSourceType;
import vn.mar.security.context.CurrentUser;
import vn.mar.security.context.CurrentUserContext;

@ExtendWith(MockitoExtension.class)
class IntegrationEventQueryApplicationServiceTest {

    private static final UUID ACTOR_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID TENANT_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final UUID EVENT_ID = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
    private static final UUID LEAD_ID = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");
    private static final UUID CUSTOMER_ID = UUID.fromString("eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee");
    private static final UUID OPPORTUNITY_ID = UUID.fromString("ffffffff-ffff-ffff-ffff-ffffffffffff");
    private static final String PAYLOAD_HASH =
            "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";
    private static final Instant NOW = Instant.parse("2026-07-06T01:00:00Z");
    private static final Instant FROM = Instant.parse("2026-07-06T00:00:00Z");
    private static final Instant TO = Instant.parse("2026-07-06T02:00:00Z");

    @Mock
    private IntegrationEventRepository integrationEventRepository;

    @Mock
    private CurrentUserContext currentUserContext;

    private IntegrationEventQueryApplicationService service;

    @BeforeEach
    void setUp() {
        service = new IntegrationEventQueryApplicationService(
                integrationEventRepository,
                currentUserContext,
                new IntegrationEventMapper(),
                new PermissionGuard()
        );
    }

    @Test
    void searchEvents_whenActorCanViewIntegrationLogs_shouldQueryCurrentTenantAndReturnPayload() {
        allowIntegrationLogView();
        IntegrationEvent event = duplicateEvent();
        when(integrationEventRepository.search(
                eq(TENANT_ID),
                eq(LeadSourceType.WEBSITE_FORM),
                eq(IntegrationEventStatus.DUPLICATE),
                eq("webform_001"),
                eq("webform_001"),
                eq(PAYLOAD_HASH),
                eq("WEBHOOK_DUPLICATE_IGNORED"),
                eq(LEAD_ID),
                eq(CUSTOMER_ID),
                eq(OPPORTUNITY_ID),
                eq(FROM),
                eq(TO),
                any(Pageable.class)
        )).thenReturn(new PageImpl<>(List.of(event), PageRequest.of(1, 10), 11));

        PageResponse<IntegrationEventSnapshot> response = service.searchEvents(new IntegrationEventSearchCommand(
                " website_form ",
                " duplicate ",
                " webform_001 ",
                " webform_001 ",
                PAYLOAD_HASH,
                " webhook_duplicate_ignored ",
                LEAD_ID,
                CUSTOMER_ID,
                OPPORTUNITY_ID,
                FROM,
                TO,
                1,
                10
        ));

        assertThat(response.totalElements()).isEqualTo(11);
        assertThat(response.items()).hasSize(1);
        IntegrationEventSnapshot snapshot = response.items().getFirst();
        assertThat(snapshot.eventId()).isEqualTo(EVENT_ID);
        assertThat(snapshot.tenantId()).isEqualTo(TENANT_ID);
        assertThat(snapshot.sourceType()).isEqualTo(LeadSourceType.WEBSITE_FORM);
        assertThat(snapshot.status()).isEqualTo(IntegrationEventStatus.DUPLICATE);
        assertThat(snapshot.errorCode()).isEqualTo("WEBHOOK_DUPLICATE_IGNORED");

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(integrationEventRepository).search(
                eq(TENANT_ID),
                eq(LeadSourceType.WEBSITE_FORM),
                eq(IntegrationEventStatus.DUPLICATE),
                eq("webform_001"),
                eq("webform_001"),
                eq(PAYLOAD_HASH),
                eq("WEBHOOK_DUPLICATE_IGNORED"),
                eq(LEAD_ID),
                eq(CUSTOMER_ID),
                eq(OPPORTUNITY_ID),
                eq(FROM),
                eq(TO),
                pageableCaptor.capture()
        );
        Pageable pageable = pageableCaptor.getValue();
        assertThat(pageable.getPageNumber()).isEqualTo(1);
        assertThat(pageable.getPageSize()).isEqualTo(10);
        assertThat(pageable.getSort().getOrderFor("receivedAt").getDirection().isDescending()).isTrue();
    }

    @Test
    void getEvent_whenEventBelongsToCurrentTenant_shouldReturnDetail() {
        allowIntegrationLogView();
        IntegrationEvent event = duplicateEvent();
        when(integrationEventRepository.findByIdAndTenantId(EVENT_ID, TENANT_ID)).thenReturn(Optional.of(event));

        IntegrationEventSnapshot snapshot = service.getEvent(EVENT_ID);

        assertThat(snapshot.eventId()).isEqualTo(EVENT_ID);
        assertThat(snapshot.externalId()).isEqualTo("webform_001");
        assertThat(snapshot.receivedAt()).isEqualTo(NOW);
        assertThat(snapshot.processedAt()).isEqualTo(NOW);
    }

    @Test
    void getEvent_whenEventIsOutsideCurrentTenant_shouldReturnNotFound() {
        allowIntegrationLogView();
        when(integrationEventRepository.findByIdAndTenantId(EVENT_ID, TENANT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getEvent(EVENT_ID))
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
                Set.of(PermissionCodes.LEAD_VIEW),
                "req_integration_log_002"
        ));

        assertThatThrownBy(() -> service.searchEvents(emptySearch()))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.PERMISSION_DENIED);

        verify(integrationEventRepository, never()).search(
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
                any(),
                any(),
                any()
        );
    }

    @Test
    void searchEvents_whenSourceTypeInvalid_shouldRejectBeforeRepositoryCall() {
        allowIntegrationLogView();

        assertThatThrownBy(() -> service.searchEvents(new IntegrationEventSearchCommand(
                "unknown",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                0,
                20
        ))).isInstanceOf(ValidationException.class);

        verifyNoInteractions(integrationEventRepository);
    }

    @Test
    void searchEvents_whenFromIsAfterTo_shouldRejectBeforeRepositoryCall() {
        allowIntegrationLogView();

        assertThatThrownBy(() -> service.searchEvents(new IntegrationEventSearchCommand(
                null,
                null,
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

        verifyNoInteractions(integrationEventRepository);
    }

    @Test
    void searchEvents_whenPageSizeTooLarge_shouldRejectBeforeRepositoryCall() {
        allowIntegrationLogView();

        assertThatThrownBy(() -> service.searchEvents(new IntegrationEventSearchCommand(
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                0,
                101
        ))).isInstanceOf(ValidationException.class);

        verifyNoInteractions(integrationEventRepository);
    }

    private void allowIntegrationLogView() {
        when(currentUserContext.currentUser()).thenReturn(new CurrentUser(
                ACTOR_ID,
                TENANT_ID,
                "MARKETING",
                Set.of(PermissionCodes.INTEGRATION_LOG_VIEW),
                "req_integration_log_001"
        ));
    }

    private IntegrationEventSearchCommand emptySearch() {
        return new IntegrationEventSearchCommand(null, null, null, null, null, null, null, null, null, null, null, 0, 20);
    }

    private IntegrationEvent duplicateEvent() {
        return IntegrationEvent.duplicate(
                EVENT_ID,
                TENANT_ID,
                LeadSourceType.WEBSITE_FORM,
                "webform_001",
                "webform_001",
                PAYLOAD_HASH,
                NOW
        );
    }
}
