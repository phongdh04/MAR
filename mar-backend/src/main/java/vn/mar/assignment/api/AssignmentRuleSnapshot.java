package vn.mar.assignment.api;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AssignmentRuleSnapshot(
        UUID assignmentRuleId,
        UUID tenantId,
        String ruleName,
        int priority,
        UUID languageId,
        UUID programId,
        UUID branchId,
        String assignmentStrategy,
        String status,
        List<UUID> advisorIds,
        Instant createdAt,
        Instant updatedAt
) {
}
