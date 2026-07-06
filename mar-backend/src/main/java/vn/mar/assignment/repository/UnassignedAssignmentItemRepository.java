package vn.mar.assignment.repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.mar.assignment.entity.UnassignedAssignmentItem;
import vn.mar.assignment.model.UnassignedAssignmentItemStatus;

public interface UnassignedAssignmentItemRepository extends JpaRepository<UnassignedAssignmentItem, UUID> {

    Optional<UnassignedAssignmentItem> findByTenantIdAndOpportunityIdAndStatus(
            UUID tenantId,
            UUID opportunityId,
            UnassignedAssignmentItemStatus status);

    @Query("""
            select item
            from UnassignedAssignmentItem item
            join AdmissionOpportunity opportunity
              on opportunity.tenantId = item.tenantId
             and opportunity.id = item.opportunityId
            where item.tenantId = :tenantId
              and (:status is null or item.status = :status)
              and (:branchId is null or opportunity.branchId = :branchId)
            """)
    Page<UnassignedAssignmentItem> search(
            @Param("tenantId") UUID tenantId,
            @Param("status") UnassignedAssignmentItemStatus status,
            @Param("branchId") UUID branchId,
            Pageable pageable);
}
