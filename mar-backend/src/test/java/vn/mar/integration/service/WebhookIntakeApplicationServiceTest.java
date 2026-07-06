package vn.mar.integration.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.mar.common.error.ErrorCode;
import vn.mar.common.exception.MarException;
import vn.mar.common.time.TimeProvider;
import vn.mar.integration.api.WebhookIntakeCommand;
import vn.mar.integration.api.WebhookIntakeSnapshot;
import vn.mar.integration.config.WebhookProperties;
import vn.mar.integration.entity.IntegrationEvent;
import vn.mar.integration.mapper.IntegrationEventMapper;
import vn.mar.integration.model.IntegrationEventStatus;
import vn.mar.integration.repository.IntegrationEventRepository;
import vn.mar.lead.model.LeadSourceType;
import vn.mar.tenant.entity.Tenant;
import vn.mar.tenant.model.TenantStatus;
import vn.mar.tenant.repository.TenantRepository;

@ExtendWith(MockitoExtension.class)
class WebhookIntakeApplicationServiceTest {

    private static final UUID TENANT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final Instant NOW = Instant.parse("2026-07-06T01:00:00Z");

    @Mock
    private TenantRepository tenantRepository;

    @Mock
    private IntegrationEventRepository integrationEventRepository;

    private ObjectMapper objectMapper;
    private WebhookPayloadSecurityService webhookPayloadSecurityService;
    private WebhookIntakeApplicationService service;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        webhookPayloadSecurityService = new WebhookPayloadSecurityService(
                objectMapper,
                new WebhookProperties("test-secret")
        );
        TimeProvider timeProvider = () -> NOW;
        service = new WebhookIntakeApplicationService(
                tenantRepository,
                integrationEventRepository,
                webhookPayloadSecurityService,
                new IntegrationEventMapper(),
                timeProvider
        );
    }

    @Test
    void receiveWebsiteLead_whenPayloadIsNew_shouldStoreReceivedEventWithPayloadHash() throws Exception {
        JsonNode payload = objectMapper.readTree("""
                {
                  "external_id": "webform_001",
                  "full_name": "Nguyen Minh A",
                  "phone": "0912345678"
                }
                """);
        when(tenantRepository.findByTenantCodeIgnoreCase("MAR")).thenReturn(Optional.of(activeTenant()));
        when(integrationEventRepository
                .findFirstByTenantIdAndSourceTypeAndExternalIdAndStatusNotOrderByReceivedAtAsc(any(), any(), any(), any()))
                .thenReturn(Optional.empty());
        when(integrationEventRepository
                .findFirstByTenantIdAndSourceTypeAndIdempotencyKeyAndStatusNotOrderByReceivedAtAsc(any(), any(), any(), any()))
                .thenReturn(Optional.empty());
        when(integrationEventRepository
                .findFirstByTenantIdAndSourceTypeAndPayloadHashAndStatusNotOrderByReceivedAtAsc(any(), any(), any(), any()))
                .thenReturn(Optional.empty());
        when(integrationEventRepository.save(any(IntegrationEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));

        WebhookIntakeSnapshot snapshot = service.receiveWebsiteLead(command(payload, "MAR", null));

        assertThat(snapshot.tenantId()).isEqualTo(TENANT_ID);
        assertThat(snapshot.sourceType()).isEqualTo(LeadSourceType.WEBSITE_FORM);
        assertThat(snapshot.externalId()).isEqualTo("webform_001");
        assertThat(snapshot.idempotencyKey()).isEqualTo("webform_001");
        assertThat(snapshot.payloadHash()).hasSize(64);
        assertThat(snapshot.status()).isEqualTo(IntegrationEventStatus.RECEIVED);
        assertThat(snapshot.duplicate()).isFalse();
    }

    @Test
    void receiveWebsiteLead_whenExternalIdAlreadyExists_shouldStoreDuplicateEvent() throws Exception {
        JsonNode payload = objectMapper.readTree("""
                {
                  "external_id": "webform_002",
                  "full_name": "Nguyen Minh B",
                  "phone": "0912345679"
                }
                """);
        IntegrationEvent existing = IntegrationEvent.received(
                UUID.randomUUID(),
                TENANT_ID,
                LeadSourceType.WEBSITE_FORM,
                "webform_002",
                "webform_002",
                webhookPayloadSecurityService.payloadHash(payload),
                NOW
        );
        when(tenantRepository.findByTenantCodeIgnoreCase("MAR")).thenReturn(Optional.of(activeTenant()));
        when(integrationEventRepository
                .findFirstByTenantIdAndSourceTypeAndExternalIdAndStatusNotOrderByReceivedAtAsc(any(), any(), any(), any()))
                .thenReturn(Optional.of(existing));
        when(integrationEventRepository.save(any(IntegrationEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));

        WebhookIntakeSnapshot snapshot = service.receiveWebsiteLead(command(payload, "MAR", null));

        assertThat(snapshot.status()).isEqualTo(IntegrationEventStatus.DUPLICATE);
        assertThat(snapshot.errorCode()).isEqualTo("WEBHOOK_DUPLICATE_IGNORED");
        assertThat(snapshot.duplicate()).isTrue();
    }

    @Test
    void receiveWebsiteLead_whenTenantKeyInvalid_shouldRejectBeforeSavingEvent() throws Exception {
        JsonNode payload = objectMapper.readTree("""
                {
                  "external_id": "webform_003"
                }
                """);
        when(tenantRepository.findByTenantCodeIgnoreCase("UNKNOWN")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.receiveWebsiteLead(command(payload, "UNKNOWN", null)))
                .isInstanceOf(MarException.class)
                .extracting(exception -> ((MarException) exception).getErrorCode())
                .isEqualTo(ErrorCode.WEBHOOK_TENANT_KEY_INVALID);

        verify(integrationEventRepository, never()).save(any());
    }

    @Test
    void receiveWebsiteLead_whenSignatureInvalid_shouldRejectBeforeTenantLookup() throws Exception {
        JsonNode payload = objectMapper.readTree("""
                {
                  "external_id": "webform_004"
                }
                """);

        assertThatThrownBy(() -> service.receiveWebsiteLead(new WebhookIntakeCommand(
                        LeadSourceType.WEBSITE_FORM,
                        "MAR",
                        "sha256=invalid",
                        null,
                        payload
                )))
                .isInstanceOf(MarException.class)
                .extracting(exception -> ((MarException) exception).getErrorCode())
                .isEqualTo(ErrorCode.WEBHOOK_SIGNATURE_INVALID);

        verifyNoInteractions(tenantRepository);
        verify(integrationEventRepository, never()).save(any());
    }

    private WebhookIntakeCommand command(JsonNode payload, String tenantKey, String idempotencyKey) {
        return new WebhookIntakeCommand(
                LeadSourceType.WEBSITE_FORM,
                tenantKey,
                webhookPayloadSecurityService.signature(payload),
                idempotencyKey,
                payload
        );
    }

    private Tenant activeTenant() {
        return Tenant.restore(
                TENANT_ID,
                "MAR",
                "MAR Tenant",
                "Asia/Ho_Chi_Minh",
                "VND",
                TenantStatus.ACTIVE,
                NOW,
                null,
                NOW,
                null
        );
    }
}
