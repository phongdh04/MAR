package vn.mar.sla.api;

import java.time.Instant;
import java.util.UUID;

public record SlaTaskSnapshot(
        UUID slaTaskId,
        UUID tenantId,
        UUID opportunityId,
        UUID sourceLeadId,
        UUID ownerId,
        UUID branchId,
        UUID slaPolicyId,
        String taskType,
        String status,
        String leadTemperature,
        Instant dueAt,
        Instant completedAt,
        UUID completedActivityId,
        Boolean slaHit,
        Instant overdueMarkedAt,
        String overdueLevel,
        UUID escalatedTo,
        Instant salesLeadEscalatedAt,
        Instant createdAt,
        UUID createdBy,
        Instant updatedAt,
        UUID updatedBy
) {
}
