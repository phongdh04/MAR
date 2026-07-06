package vn.mar.integration.api;

import java.time.Instant;
import java.util.UUID;
import vn.mar.integration.model.IntegrationEventStatus;
import vn.mar.lead.model.LeadSourceType;

public record IntegrationEventSnapshot(
        UUID eventId,
        UUID tenantId,
        LeadSourceType sourceType,
        String externalId,
        String idempotencyKey,
        String payloadHash,
        IntegrationEventStatus status,
        String errorCode,
        String errorMessage,
        String rawPayloadUri,
        UUID createdLeadId,
        UUID createdCustomerId,
        UUID createdOpportunityId,
        Instant receivedAt,
        Instant processedAt
) {
}
