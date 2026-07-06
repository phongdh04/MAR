package vn.mar.assignment.repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.mar.assignment.entity.AssignmentRuleAdvisor;
import vn.mar.assignment.model.AssignmentRuleAdvisorStatus;

public interface AssignmentRuleAdvisorRepository extends JpaRepository<AssignmentRuleAdvisor, UUID> {

    List<AssignmentRuleAdvisor> findByTenantIdAndAssignmentRuleIdAndStatus(
            UUID tenantId,
            UUID assignmentRuleId,
            AssignmentRuleAdvisorStatus status);

    List<AssignmentRuleAdvisor> findByTenantIdAndAssignmentRuleId(UUID tenantId, UUID assignmentRuleId);

    List<AssignmentRuleAdvisor> findByTenantIdAndAssignmentRuleIdInAndStatus(
            UUID tenantId,
            Collection<UUID> assignmentRuleIds,
            AssignmentRuleAdvisorStatus status);

    List<AssignmentRuleAdvisor> findByTenantIdAndAssignmentRuleIdAndAdvisorIdInAndStatus(
            UUID tenantId,
            UUID assignmentRuleId,
            Collection<UUID> advisorIds,
            AssignmentRuleAdvisorStatus status);
}
