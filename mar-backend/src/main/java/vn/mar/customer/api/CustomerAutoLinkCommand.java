package vn.mar.customer.api;

import java.util.UUID;

public record CustomerAutoLinkCommand(
        UUID tenantId,
        String fullName,
        String phone,
        String email,
        String zaloId,
        boolean zaloVerified
) {
}
