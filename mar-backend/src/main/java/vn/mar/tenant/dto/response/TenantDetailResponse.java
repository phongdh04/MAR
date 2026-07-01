package vn.mar.tenant.dto.response;

import java.time.Instant;
import java.util.UUID;

public record TenantDetailResponse(
        UUID tenantId,
        String tenantCode,
        String tenantName,
        String timezone,
        String defaultCurrency,
        String status,
        Instant createdAt,
        Instant updatedAt
) {
}
