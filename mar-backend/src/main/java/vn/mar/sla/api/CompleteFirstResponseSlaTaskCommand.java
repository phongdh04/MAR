package vn.mar.sla.api;

import java.time.Instant;
import java.util.UUID;

public record CompleteFirstResponseSlaTaskCommand(
        UUID tenantId,
        UUID opportunityId,
        UUID activityId,
        Instant occurredAt,
        UUID actorId,
        String actorRoleCode
) {
}
