package vn.mar.opportunity.repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.mar.opportunity.entity.StageHistory;

public interface StageHistoryRepository extends JpaRepository<StageHistory, UUID> {

    Optional<StageHistory> findFirstByTenantIdAndOpportunityIdOrderByChangedAtDesc(UUID tenantId, UUID opportunityId);
}
