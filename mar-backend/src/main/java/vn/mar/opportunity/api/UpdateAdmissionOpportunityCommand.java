package vn.mar.opportunity.api;

import java.util.UUID;

public record UpdateAdmissionOpportunityCommand(
        UUID opportunityId,
        UUID languageId,
        UUID programId,
        UUID courseId,
        UUID branchId,
        String qualificationStatus,
        String note
) {
}
