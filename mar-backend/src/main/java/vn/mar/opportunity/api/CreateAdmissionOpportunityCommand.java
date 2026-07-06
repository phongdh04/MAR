package vn.mar.opportunity.api;

import java.time.Instant;
import java.util.UUID;
import vn.mar.lead.model.LeadTemperature;
import vn.mar.opportunity.model.TouchpointType;

public record CreateAdmissionOpportunityCommand(
        UUID tenantId,
        UUID customerId,
        UUID sourceLeadId,
        UUID languageId,
        UUID programId,
        UUID courseId,
        UUID branchId,
        UUID ownerId,
        LeadTemperature leadTemperature,
        TouchpointType touchpointType,
        Instant touchTime
) {
}
