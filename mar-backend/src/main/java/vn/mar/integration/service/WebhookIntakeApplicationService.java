package vn.mar.integration.service;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import vn.mar.common.error.ErrorCode;
import vn.mar.common.error.ErrorDetail;
import vn.mar.common.exception.BusinessException;
import vn.mar.common.exception.ValidationException;
import vn.mar.common.time.TimeProvider;
import vn.mar.integration.api.WebhookIntakeCommand;
import vn.mar.integration.api.WebhookIntakeService;
import vn.mar.integration.api.WebhookIntakeSnapshot;
import vn.mar.integration.entity.IntegrationEvent;
import vn.mar.integration.mapper.IntegrationEventMapper;
import vn.mar.integration.model.IntegrationEventStatus;
import vn.mar.integration.repository.IntegrationEventRepository;
import vn.mar.lead.model.LeadSourceType;
import vn.mar.tenant.entity.Tenant;
import vn.mar.tenant.model.TenantStatus;
import vn.mar.tenant.repository.TenantRepository;

@Service
public class WebhookIntakeApplicationService implements WebhookIntakeService {

    private static final int TENANT_KEY_MAX_LENGTH = 50;
    private static final int EXTERNAL_ID_MAX_LENGTH = 255;
    private static final int IDEMPOTENCY_KEY_MAX_LENGTH = 255;

    private final TenantRepository tenantRepository;
    private final IntegrationEventRepository integrationEventRepository;
    private final WebhookPayloadSecurityService webhookPayloadSecurityService;
    private final IntegrationEventMapper integrationEventMapper;
    private final TimeProvider timeProvider;

    public WebhookIntakeApplicationService(
            TenantRepository tenantRepository,
            IntegrationEventRepository integrationEventRepository,
            WebhookPayloadSecurityService webhookPayloadSecurityService,
            IntegrationEventMapper integrationEventMapper,
            TimeProvider timeProvider) {
        this.tenantRepository = tenantRepository;
        this.integrationEventRepository = integrationEventRepository;
        this.webhookPayloadSecurityService = webhookPayloadSecurityService;
        this.integrationEventMapper = integrationEventMapper;
        this.timeProvider = timeProvider;
    }

    @Override
    @Transactional
    public WebhookIntakeSnapshot receiveWebsiteLead(WebhookIntakeCommand command) {
        validateCommand(command, LeadSourceType.WEBSITE_FORM);
        JsonNode payload = requirePayload(command.payload());
        String tenantKey = normalizeTenantKey(command.tenantKey());
        assertSignatureValid(payload, command.signature());

        Tenant tenant = findActiveTenantByKey(tenantKey);
        String externalId = normalizeOptional(payload.path("external_id").asText(null), "external_id", EXTERNAL_ID_MAX_LENGTH);
        String payloadHash = webhookPayloadSecurityService.payloadHash(payload);
        String idempotencyKey = resolveIdempotencyKey(command.idempotencyKey(), externalId, payloadHash);
        Instant now = timeProvider.now();

        IntegrationEvent event = findExistingEvent(tenant.id(), command.sourceType(), externalId, idempotencyKey, payloadHash)
                .map(existing -> IntegrationEvent.duplicate(
                        UUID.randomUUID(),
                        tenant.id(),
                        command.sourceType(),
                        externalId,
                        idempotencyKey,
                        payloadHash,
                        now
                ))
                .orElseGet(() -> IntegrationEvent.received(
                        UUID.randomUUID(),
                        tenant.id(),
                        command.sourceType(),
                        externalId,
                        idempotencyKey,
                        payloadHash,
                        now
                ));

        return integrationEventMapper.toSnapshot(integrationEventRepository.save(event));
    }

    private void validateCommand(WebhookIntakeCommand command, LeadSourceType expectedSourceType) {
        if (command == null) {
            throw ValidationException.of("command", "REQUIRED", "Webhook command is required");
        }
        if (command.sourceType() != expectedSourceType) {
            throw ValidationException.of("source_type", "INVALID_SOURCE_TYPE", "Webhook source type is invalid");
        }
    }

    private JsonNode requirePayload(JsonNode payload) {
        if (payload == null || payload.isNull()) {
            throw ValidationException.of("payload", "REQUIRED", "Webhook payload is required");
        }
        if (!payload.isObject()) {
            throw ValidationException.of("payload", "INVALID_JSON_OBJECT", "Webhook payload must be a JSON object");
        }
        return payload;
    }

    private String normalizeTenantKey(String tenantKey) {
        if (!StringUtils.hasText(tenantKey)) {
            throw webhookAuthError(
                    ErrorCode.WEBHOOK_TENANT_KEY_INVALID,
                    "Webhook tenant key is required",
                    "tenant_key",
                    "REQUIRED"
            );
        }
        String normalized = tenantKey.trim().toUpperCase(Locale.ROOT);
        if (normalized.length() > TENANT_KEY_MAX_LENGTH) {
            throw webhookAuthError(
                    ErrorCode.WEBHOOK_TENANT_KEY_INVALID,
                    "Webhook tenant key is invalid",
                    "tenant_key",
                    "INVALID_SIZE"
            );
        }
        return normalized;
    }

    private void assertSignatureValid(JsonNode payload, String signature) {
        if (!webhookPayloadSecurityService.verifySignature(payload, signature)) {
            throw webhookAuthError(
                    ErrorCode.WEBHOOK_SIGNATURE_INVALID,
                    ErrorCode.WEBHOOK_SIGNATURE_INVALID.defaultMessage(),
                    "webhook_signature",
                    StringUtils.hasText(signature) ? "INVALID_SIGNATURE" : "REQUIRED"
            );
        }
    }

    private Tenant findActiveTenantByKey(String tenantKey) {
        Tenant tenant = tenantRepository.findByTenantCodeIgnoreCase(tenantKey)
                .orElseThrow(() -> webhookAuthError(
                        ErrorCode.WEBHOOK_TENANT_KEY_INVALID,
                        ErrorCode.WEBHOOK_TENANT_KEY_INVALID.defaultMessage(),
                        "tenant_key",
                        "INVALID_TENANT_KEY"
                ));
        if (tenant.status() != TenantStatus.ACTIVE) {
            throw new BusinessException(
                    ErrorCode.TENANT_INACTIVE,
                    ErrorCode.TENANT_INACTIVE.defaultMessage(),
                    List.of(ErrorDetail.of("tenant_key", "TENANT_INACTIVE", "Tenant is inactive"))
            );
        }
        return tenant;
    }

    private String resolveIdempotencyKey(String requestedIdempotencyKey, String externalId, String payloadHash) {
        String normalized = normalizeOptional(requestedIdempotencyKey, "idempotency_key", IDEMPOTENCY_KEY_MAX_LENGTH);
        if (normalized != null) {
            return normalized;
        }
        if (externalId != null) {
            return externalId;
        }
        return payloadHash;
    }

    private Optional<IntegrationEvent> findExistingEvent(
            UUID tenantId,
            LeadSourceType sourceType,
            String externalId,
            String idempotencyKey,
            String payloadHash) {
        Optional<IntegrationEvent> byExternalId = externalId == null
                ? Optional.empty()
                : integrationEventRepository.findFirstByTenantIdAndSourceTypeAndExternalIdAndStatusNotOrderByReceivedAtAsc(
                        tenantId,
                        sourceType,
                        externalId,
                        IntegrationEventStatus.DUPLICATE
                );
        if (byExternalId.isPresent()) {
            return byExternalId;
        }

        Optional<IntegrationEvent> byIdempotencyKey = idempotencyKey == null
                ? Optional.empty()
                : integrationEventRepository.findFirstByTenantIdAndSourceTypeAndIdempotencyKeyAndStatusNotOrderByReceivedAtAsc(
                        tenantId,
                        sourceType,
                        idempotencyKey,
                        IntegrationEventStatus.DUPLICATE
                );
        if (byIdempotencyKey.isPresent()) {
            return byIdempotencyKey;
        }

        return integrationEventRepository.findFirstByTenantIdAndSourceTypeAndPayloadHashAndStatusNotOrderByReceivedAtAsc(
                tenantId,
                sourceType,
                payloadHash,
                IntegrationEventStatus.DUPLICATE
        );
    }

    private String normalizeOptional(String value, String field, int maxLength) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String normalized = value.trim();
        if (normalized.length() > maxLength) {
            throw ValidationException.of(field, "INVALID_SIZE", "Value is too long");
        }
        return normalized;
    }

    private BusinessException webhookAuthError(ErrorCode errorCode, String message, String field, String detailCode) {
        return new BusinessException(errorCode, message, List.of(ErrorDetail.of(field, detailCode, message)));
    }

}
