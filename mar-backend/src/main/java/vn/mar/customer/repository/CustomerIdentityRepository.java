package vn.mar.customer.repository;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.mar.customer.entity.CustomerIdentity;
import vn.mar.customer.model.CustomerIdentityType;

public interface CustomerIdentityRepository extends JpaRepository<CustomerIdentity, UUID> {

    List<CustomerIdentity> findByTenantIdAndCustomerId(UUID tenantId, UUID customerId);

    List<CustomerIdentity> findByTenantIdAndIdentityTypeAndNormalizedValue(
            UUID tenantId,
            CustomerIdentityType identityType,
            String normalizedValue
    );

    boolean existsByTenantIdAndCustomerIdAndIdentityTypeAndNormalizedValue(
            UUID tenantId,
            UUID customerId,
            CustomerIdentityType identityType,
            String normalizedValue
    );

    boolean existsByTenantIdAndCustomerIdAndIdentityTypeAndPrimaryIdentityTrue(
            UUID tenantId,
            UUID customerId,
            CustomerIdentityType identityType
    );
}
