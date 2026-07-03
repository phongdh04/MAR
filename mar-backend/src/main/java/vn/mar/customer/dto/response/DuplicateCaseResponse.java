package vn.mar.customer.dto.response;

import java.time.Instant;
import java.util.UUID;

public record DuplicateCaseResponse(
        UUID duplicateCaseId,
        UUID tenantId,
        UUID sourceCustomerId,
        UUID candidateCustomerId,
        String matchType,
        String confidence,
        String status,
        String reviewReason,
        String resolutionAction,
        UUID resolvedBy,
        Instant resolvedAt,
        String resolutionReason,
        Instant createdAt,
        Instant updatedAt
) {
}
