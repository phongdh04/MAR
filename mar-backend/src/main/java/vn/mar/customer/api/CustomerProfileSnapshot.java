package vn.mar.customer.api;

import java.util.UUID;

public record CustomerProfileSnapshot(
        UUID customerId,
        UUID tenantId,
        String fullName,
        String primaryPhone,
        String primaryEmail,
        String zaloId
) {
}
