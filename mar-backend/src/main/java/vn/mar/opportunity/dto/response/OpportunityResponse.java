package vn.mar.opportunity.dto.response;

import java.time.Instant;
import java.util.UUID;

public record OpportunityResponse(
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
