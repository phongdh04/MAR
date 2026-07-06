package vn.mar.opportunity.api;

import java.time.Instant;
import java.util.UUID;

public record AdmissionOpportunitySnapshot(
        UUID opportunityId,
        UUID tenantId,
        UUID customerId,
        UUID sourceLeadId,
        UUID languageId,
        UUID programId,
        UUID courseId,
        UUID branchId,
        UUID ownerId,
        String currentStage,
        String qualificationStatus,
        String leadTemperature,
        UUID firstTouchId,
        UUID lastTouchId,
        Instant createdAt,
        Instant updatedAt
) {
}
