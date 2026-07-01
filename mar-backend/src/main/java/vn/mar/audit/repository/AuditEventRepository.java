package vn.mar.audit.repository;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.mar.audit.entity.AuditEvent;

public interface AuditEventRepository extends JpaRepository<AuditEvent, UUID> {
}
