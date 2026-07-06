package vn.mar.assignment.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.mar.assignment.entity.AssignmentRule;
import vn.mar.assignment.model.AssignmentRuleStatus;

public interface AssignmentRuleRepository extends JpaRepository<AssignmentRule, UUID> {

    Optional<AssignmentRule> findByIdAndTenantId(UUID id, UUID tenantId);

    List<AssignmentRule> findByTenantIdAndStatusOrderByPriorityAscIdAsc(
            UUID tenantId,
            AssignmentRuleStatus status);

    boolean existsByTenantIdAndPriorityAndStatus(UUID tenantId, int priority, AssignmentRuleStatus status);

    boolean existsByTenantIdAndPriorityAndStatusAndIdNot(
            UUID tenantId,
            int priority,
            AssignmentRuleStatus status,
            UUID id);

    @Query("""
            select rule
            from AssignmentRule rule
            where rule.tenantId = :tenantId
              and (:status is null or rule.status = :status)
              and (:branchId is null or rule.branchId = :branchId)
            """)
    Page<AssignmentRule> search(
            @Param("tenantId") UUID tenantId,
            @Param("status") AssignmentRuleStatus status,
            @Param("branchId") UUID branchId,
            Pageable pageable);
}
