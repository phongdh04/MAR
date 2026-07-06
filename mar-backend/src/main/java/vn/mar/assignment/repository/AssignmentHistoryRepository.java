package vn.mar.assignment.repository;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.mar.assignment.entity.AssignmentHistory;

public interface AssignmentHistoryRepository extends JpaRepository<AssignmentHistory, UUID> {

    List<AssignmentHistory> findByTenantIdAndOpportunityIdOrderByIdAsc(UUID tenantId, UUID opportunityId);
}
