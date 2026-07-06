package vn.mar.assignment.dto.response;

import java.util.UUID;

public record AssignmentResultResponse(
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
