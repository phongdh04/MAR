package vn.mar.lead.repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.mar.lead.entity.Lead;

public interface LeadRepository extends JpaRepository<Lead, UUID> {

    Optional<Lead> findByIdAndTenantId(UUID id, UUID tenantId);
}
