package vn.mar.customer.api;

import java.time.Instant;
import java.util.UUID;
import vn.mar.customer.model.DuplicateCaseStatus;
import vn.mar.customer.model.DuplicateConfidence;
import vn.mar.customer.model.DuplicateMatchType;
import vn.mar.customer.model.DuplicateResolutionAction;

public record DuplicateCaseSnapshot(
        UUID duplicateCaseId,
        UUID tenantId,
        UUID sourceCustomerId,
        UUID matchedCustomerId,
        DuplicateMatchType matchType,
        DuplicateConfidence confidence,
        DuplicateCaseStatus status,
        String reviewReason,
        DuplicateResolutionAction resolutionAction,
        UUID resolvedBy,
        Instant resolvedAt,
        String resolutionReason,
        Instant createdAt,
        Instant updatedAt
) {
}
