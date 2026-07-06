package vn.mar.assignment.api;

import java.util.UUID;

public record AssignmentResultSnapshot(
        UUID opportunityId,
        String outcome,
        UUID assignedOwnerId,
        UUID assignmentRuleId,
        String assignmentStrategy,
        boolean fallbackApplied,
        UUID unassignedItemId,
        String unassignedReasonCode
) {
}
