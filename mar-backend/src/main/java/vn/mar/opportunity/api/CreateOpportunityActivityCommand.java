package vn.mar.opportunity.api;

import java.time.Instant;
import java.util.UUID;

public record CreateOpportunityActivityCommand(
        UUID opportunityId,
        String activityType,
        String activityResult,
        Instant occurredAt,
        String note,
        Instant nextActionAt,
        String source
) {
}
