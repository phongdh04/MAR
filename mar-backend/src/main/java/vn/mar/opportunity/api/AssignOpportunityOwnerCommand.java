package vn.mar.opportunity.api;

import java.util.UUID;

public record AssignOpportunityOwnerCommand(
        UUID tenantId,
        UUID opportunityId,
        UUID ownerId,
        UUID assignedBy,
        UUID assignmentRuleId,
        String assignmentSource,
        String assignmentStrategy,
        String reason
) {
}
