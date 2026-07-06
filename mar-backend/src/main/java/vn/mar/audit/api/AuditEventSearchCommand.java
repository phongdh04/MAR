package vn.mar.audit.api;

import java.time.Instant;
import java.util.UUID;

public record AuditEventSearchCommand(
        String resourceType,
        UUID resourceId,
        String resourceKey,
        UUID actorId,
        String actorType,
        String action,
        String requestId,
        Instant from,
        Instant to,
        Integer page,
        Integer size
) {
}
