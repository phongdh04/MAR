package vn.mar.opportunity.api;

import java.time.Instant;
import java.util.UUID;

public record OpportunityAssignmentSnapshot(
        UUID opportunityId,
        UUID tenantId,
        UUID sourceLeadId,
        UUID languageId,
        UUID programId,
        UUID branchId,
        UUID ownerId,
        String currentStage,
        String leadTemperature,
        Instant createdAt,
        Instant updatedAt
) {
}
