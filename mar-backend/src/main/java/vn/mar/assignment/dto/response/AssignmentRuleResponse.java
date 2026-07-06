package vn.mar.assignment.dto.response;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AssignmentRuleResponse(
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
