package vn.mar.assignment.dto.request;

import java.util.List;
import java.util.UUID;

public record UpdateAssignmentRuleRequest(
        String ruleName,
        Integer priority,
        UUID languageId,
        UUID programId,
        UUID branchId,
        String assignmentStrategy,
        String status,
        List<UUID> advisorIds,
        String reason
) {
}
