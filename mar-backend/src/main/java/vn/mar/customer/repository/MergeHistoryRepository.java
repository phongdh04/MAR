package vn.mar.customer.repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.mar.customer.entity.MergeHistory;

public interface MergeHistoryRepository extends JpaRepository<MergeHistory, UUID> {

    Optional<MergeHistory> findByIdAndTenantId(UUID id, UUID tenantId);

    Optional<MergeHistory> findByIdAndTenantIdAndTargetCustomerId(UUID id, UUID tenantId, UUID targetCustomerId);
}
