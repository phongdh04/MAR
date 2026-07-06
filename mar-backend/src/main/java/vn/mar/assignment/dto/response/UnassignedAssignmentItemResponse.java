package vn.mar.assignment.dto.response;

import java.time.Instant;
import java.util.UUID;

public record UnassignedAssignmentItemResponse(
        UUID unassignedItemId,
        UUID tenantId,
        UUID opportunityId,
        UUID sourceLeadId,
        UUID assignmentRuleId,
        String reasonCode,
        String status,
        Instant createdAt,
        UUID createdBy,
        Instant resolvedAt,
        UUID resolvedBy,
        Instant updatedAt
) {
}
