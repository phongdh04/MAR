package vn.mar.tenant.repository;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.mar.tenant.entity.Tenant;

public interface TenantRepository extends JpaRepository<Tenant, UUID> {

    boolean existsByTenantCodeIgnoreCase(String tenantCode);
}
