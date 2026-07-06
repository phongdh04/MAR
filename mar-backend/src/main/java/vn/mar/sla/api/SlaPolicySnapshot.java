package vn.mar.sla.api;

import java.util.UUID;

public record SlaPolicySnapshot(
        UUID slaPolicyId,
        UUID tenantId,
        UUID branchId,
        String policyType,
        int responseDueMinutes,
        String timezone,
        String source
) {
}
