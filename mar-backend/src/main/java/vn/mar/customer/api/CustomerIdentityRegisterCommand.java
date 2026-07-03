package vn.mar.customer.api;

import java.util.UUID;
import vn.mar.customer.model.CustomerIdentityType;

public record CustomerIdentityRegisterCommand(
        UUID tenantId,
        UUID customerId,
        CustomerIdentityType identityType,
        String rawValue,
        boolean primaryIdentity
) {
}
