package vn.mar.integration.dto.response;

import java.time.Instant;
import java.util.UUID;

public record IntegrationEventResponse(
        UUID eventId,
        UUID tenantId,
        String sourceType,
        String externalId,
        String idempotencyKey,
        String payloadHash,
        String status,
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
