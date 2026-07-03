package vn.mar.customer.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.mar.customer.entity.CustomerProfile;

public interface CustomerProfileRepository extends JpaRepository<CustomerProfile, UUID> {

    Optional<CustomerProfile> findByIdAndTenantId(UUID id, UUID tenantId);

    List<CustomerProfile> findByTenantIdAndPrimaryPhone(UUID tenantId, String primaryPhone);

    List<CustomerProfile> findByTenantIdAndZaloId(UUID tenantId, String zaloId);
}
