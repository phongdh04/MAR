package vn.mar.audit.repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import vn.mar.audit.entity.AuditEvent;

public interface AuditEventRepository extends JpaRepository<AuditEvent, UUID>, JpaSpecificationExecutor<AuditEvent> {

    Optional<AuditEvent> findByIdAndTenantId(UUID id, UUID tenantId);

    default Page<AuditEvent> search(
            UUID tenantId,
            String resourceType,
            UUID resourceId,
            String resourceKey,
            UUID actorId,
            String actorType,
            String action,
            String requestId,
            Instant fromCreatedAt,
            Instant toCreatedAt,
            Pageable pageable) {
        return findAll(AuditEventSpecifications.search(
                tenantId,
                resourceType,
                resourceId,
                resourceKey,
                actorId,
                actorType,
                action,
                requestId,
                fromCreatedAt,
                toCreatedAt
        ), pageable);
    }
}
