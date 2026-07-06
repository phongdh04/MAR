package vn.mar.assignment.api;

import java.time.Instant;
import java.util.UUID;

public record UnassignedAssignmentItemSnapshot(
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
