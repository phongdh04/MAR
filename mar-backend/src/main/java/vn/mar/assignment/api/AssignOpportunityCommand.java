package vn.mar.assignment.api;

import java.util.UUID;

public record AssignOpportunityCommand(
        UUID tenantId,
        UUID opportunityId,
        UUID requestedBy,
        String reason
) {
}
