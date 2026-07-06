package vn.mar.audit.mapper;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;
import vn.mar.audit.api.AuditEventSnapshot;
import vn.mar.audit.dto.response.AuditEventResponse;
import vn.mar.audit.entity.AuditEvent;

@Component
public class AuditEventMapper {

    public AuditEventSnapshot toSnapshot(AuditEvent event) {
        return new AuditEventSnapshot(
                event.id(),
                event.tenantId(),
                event.actorId(),
                event.actorType(),
                event.actorRole(),
                event.action(),
                event.resourceType(),
                event.resourceId(),
                event.resourceKey(),
                immutableCopy(event.beforeData()),
                immutableCopy(event.afterData()),
                immutableCopy(event.metadata()),
                event.reason(),
                event.requestId(),
                event.createdAt()
        );
    }

    public AuditEventResponse toResponse(AuditEventSnapshot snapshot) {
        return new AuditEventResponse(
                snapshot.auditEventId(),
                snapshot.tenantId(),
                snapshot.actorId(),
                snapshot.actorType(),
                snapshot.actorRole(),
                snapshot.action(),
                snapshot.resourceType(),
                snapshot.resourceId(),
                snapshot.resourceKey(),
                snapshot.beforeData(),
                snapshot.afterData(),
                snapshot.metadata(),
                snapshot.reason(),
                snapshot.requestId(),
                snapshot.createdAt()
        );
    }

    private Map<String, Object> immutableCopy(Map<String, Object> source) {
        if (source == null) {
            return null;
        }
        return Collections.unmodifiableMap(new LinkedHashMap<>(source));
    }
}
