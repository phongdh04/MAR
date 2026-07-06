package vn.mar.opportunity.dto.response;

import java.time.Instant;
import java.util.UUID;

public record OpportunityActivityResponse(
        UUID activityId,
        UUID opportunityId,
        UUID customerId,
        UUID actorId,
        String actorType,
        String activityType,
        String activityResult,
        Instant occurredAt,
        String note,
        Instant nextActionAt,
        String source,
        boolean firstResponseCandidate,
        boolean contactSuccess,
        Instant createdAt
) {
}
