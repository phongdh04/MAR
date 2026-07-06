package vn.mar.sla.api;

import java.time.Instant;
import java.util.UUID;
import vn.mar.lead.model.LeadTemperature;

public record OpenFirstResponseSlaTaskCommand(
        UUID tenantId,
        UUID opportunityId,
        UUID sourceLeadId,
        UUID ownerId,
        UUID branchId,
        LeadTemperature leadTemperature,
        Instant assignedAt,
        UUID actorId,
        String actorRoleCode
) {
}
