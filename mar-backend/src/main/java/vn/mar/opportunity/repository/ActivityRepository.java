package vn.mar.opportunity.repository;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.mar.opportunity.entity.Activity;

public interface ActivityRepository extends JpaRepository<Activity, UUID> {

    Page<Activity> findByTenantIdAndOpportunityIdOrderByOccurredAtDescIdDesc(
            UUID tenantId,
            UUID opportunityId,
            Pageable pageable);
}
