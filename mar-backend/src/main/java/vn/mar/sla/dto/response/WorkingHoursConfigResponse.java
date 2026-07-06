package vn.mar.sla.dto.response;

import java.util.List;
import java.util.UUID;

public record WorkingHoursConfigResponse(
        UUID tenantId,
        UUID branchId,
        String timezone,
        String source,
        boolean defaultApplied,
        List<WorkingHoursDayResponse> days
) {
}
