package vn.mar.audit.api;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record AuditEventSnapshot(
        UUID auditEventId,
        UUID tenantId,
        UUID actorId,
        String actorType,
        String actorRole,
        String action,
        String resourceType,
        UUID resourceId,
        String resourceKey,
        Map<String, Object> beforeData,
        Map<String, Object> afterData,
        Map<String, Object> metadata,
        String reason,
        String requestId,
        Instant createdAt
) {
}
