package vn.mar.customer.api;

import java.util.UUID;
import vn.mar.customer.model.CustomerIdentityType;

public record CustomerIdentitySnapshot(
        UUID identityId,
        UUID tenantId,
        UUID customerId,
        CustomerIdentityType identityType,
        String rawValue,
        String normalizedValue,
        boolean primaryIdentity
) {
}
