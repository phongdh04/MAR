package vn.mar.integration.api;

import java.time.Instant;
import java.util.UUID;

public record IntegrationEventSearchCommand(
        String sourceType,
        String status,
        String externalId,
        String idempotencyKey,
        String payloadHash,
        String errorCode,
        UUID createdLeadId,
        UUID createdCustomerId,
        UUID createdOpportunityId,
        Instant from,
        Instant to,
        Integer page,
        Integer size
) {
}
