package vn.mar.opportunity.api;

import java.util.UUID;

public record OpportunityActivitySearchCommand(
        UUID opportunityId,
        Integer page,
        Integer size
) {
}
