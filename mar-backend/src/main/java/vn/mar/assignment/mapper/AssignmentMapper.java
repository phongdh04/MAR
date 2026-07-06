package vn.mar.assignment.mapper;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;
import vn.mar.assignment.api.AssignmentResultSnapshot;
import vn.mar.assignment.api.AssignmentRuleSnapshot;
import vn.mar.assignment.api.UnassignedAssignmentItemSnapshot;
import vn.mar.assignment.dto.response.AssignmentResultResponse;
import vn.mar.assignment.dto.response.AssignmentRuleResponse;
import vn.mar.assignment.dto.response.UnassignedAssignmentItemResponse;
import vn.mar.assignment.entity.AssignmentRule;
import vn.mar.assignment.entity.UnassignedAssignmentItem;

@Component
public class AssignmentMapper {

    public AssignmentRuleSnapshot toSnapshot(AssignmentRule rule, List<UUID> advisorIds) {
        return new AssignmentRuleSnapshot(
                rule.id(),
                rule.tenantId(),
                rule.ruleName(),
                rule.priority(),
                rule.languageId(),
                rule.programId(),
                rule.branchId(),
                rule.assignmentStrategy().name(),
                rule.status().name(),
                advisorIds == null ? List.of() : List.copyOf(advisorIds),
                rule.createdAt(),
                rule.updatedAt()
        );
    }

    public AssignmentRuleResponse toResponse(AssignmentRuleSnapshot snapshot) {
        return new AssignmentRuleResponse(
                snapshot.assignmentRuleId(),
                snapshot.tenantId(),
                snapshot.ruleName(),
                snapshot.priority(),
                snapshot.languageId(),
                snapshot.programId(),
                snapshot.branchId(),
                snapshot.assignmentStrategy(),
                snapshot.status(),
                snapshot.advisorIds(),
                snapshot.createdAt(),
                snapshot.updatedAt()
        );
    }

    public AssignmentResultResponse toResponse(AssignmentResultSnapshot snapshot) {
        return new AssignmentResultResponse(
                snapshot.opportunityId(),
                snapshot.outcome(),
                snapshot.assignedOwnerId(),
                snapshot.assignmentRuleId(),
                snapshot.assignmentStrategy(),
                snapshot.fallbackApplied(),
                snapshot.unassignedItemId(),
                snapshot.unassignedReasonCode()
        );
    }

    public UnassignedAssignmentItemSnapshot toSnapshot(UnassignedAssignmentItem item) {
        return new UnassignedAssignmentItemSnapshot(
                item.id(),
                item.tenantId(),
                item.opportunityId(),
                item.sourceLeadId(),
                item.assignmentRuleId(),
                item.reasonCode().name(),
                item.status().name(),
                item.createdAt(),
                item.createdBy(),
                item.resolvedAt(),
                item.resolvedBy(),
                item.updatedAt()
        );
    }

    public UnassignedAssignmentItemResponse toResponse(UnassignedAssignmentItemSnapshot snapshot) {
        return new UnassignedAssignmentItemResponse(
                snapshot.unassignedItemId(),
                snapshot.tenantId(),
                snapshot.opportunityId(),
                snapshot.sourceLeadId(),
                snapshot.assignmentRuleId(),
                snapshot.reasonCode(),
                snapshot.status(),
                snapshot.createdAt(),
                snapshot.createdBy(),
                snapshot.resolvedAt(),
                snapshot.resolvedBy(),
                snapshot.updatedAt()
        );
    }

    public Map<String, Object> toAuditData(AssignmentRule rule, List<UUID> advisorIds) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("assignment_rule_id", rule.id().toString());
        data.put("tenant_id", rule.tenantId().toString());
        data.put("rule_name", rule.ruleName());
        data.put("priority", rule.priority());
        data.put("language_id", rule.languageId() == null ? null : rule.languageId().toString());
        data.put("program_id", rule.programId() == null ? null : rule.programId().toString());
        data.put("branch_id", rule.branchId() == null ? null : rule.branchId().toString());
        data.put("assignment_strategy", rule.assignmentStrategy().name());
        data.put("status", rule.status().name());
        data.put("advisor_ids", advisorIds == null ? List.of() : advisorIds.stream().map(UUID::toString).toList());
        return data;
    }
}
