package vn.mar.opportunity.api;

import java.time.Instant;
import java.util.UUID;

public record OpportunityActivitySnapshot(
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
