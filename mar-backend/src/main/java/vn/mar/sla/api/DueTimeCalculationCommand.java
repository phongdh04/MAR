package vn.mar.sla.api;

import java.time.Instant;
import java.util.UUID;
import vn.mar.lead.model.LeadTemperature;

public record DueTimeCalculationCommand(
        UUID tenantId,
        UUID branchId,
        LeadTemperature leadTemperature,
        Instant occurredAt
) {
}
