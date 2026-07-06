package vn.mar.sla.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.mar.sla.entity.SlaPolicy;
import vn.mar.sla.model.SlaPolicyStatus;
import vn.mar.sla.model.SlaPolicyType;

public interface SlaPolicyRepository extends JpaRepository<SlaPolicy, UUID> {

    List<SlaPolicy> findByTenantIdAndBranchIdAndStatus(UUID tenantId, UUID branchId, SlaPolicyStatus status);

    List<SlaPolicy> findByTenantIdAndBranchIdIsNullAndStatus(UUID tenantId, SlaPolicyStatus status);

    Optional<SlaPolicy> findByTenantIdAndBranchIdAndPolicyTypeAndStatus(
            UUID tenantId,
            UUID branchId,
            SlaPolicyType policyType,
            SlaPolicyStatus status);

    Optional<SlaPolicy> findByTenantIdAndBranchIdIsNullAndPolicyTypeAndStatus(
            UUID tenantId,
            SlaPolicyType policyType,
            SlaPolicyStatus status);
}
