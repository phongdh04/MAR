package vn.mar.opportunity.dto.request;

import java.util.UUID;

public record UpdateOpportunityRequest(
        UUID languageId,
        UUID programId,
        UUID courseId,
        UUID branchId,
        String qualificationStatus,
        String note
) {
}
