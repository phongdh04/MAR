package vn.mar.tenant.dto.request;

import jakarta.validation.constraints.Size;

public record UpdateTenantRequest(
        @Size(max = 255) String tenantName,
        @Size(max = 100) String timezone,
        @Size(max = 10) String defaultCurrency,
        @Size(max = 50) String status,
        @Size(max = 500) String reason
) {
}
