package vn.mar.opportunity.api;

import java.util.UUID;

public record AdmissionOpportunitySearchCommand(
        UUID ownerId,
        String stage,
        UUID languageId,
        UUID programId,
        Integer page,
        Integer size
) {
}
