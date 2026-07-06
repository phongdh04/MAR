package vn.mar.sla.dto.response;

import java.util.UUID;

public record SlaPolicyItemResponse(
        UUID slaPolicyId,
        String policyType,
        int responseDueMinutes,
        String source
) {
}
