package vn.mar.customer.api;

import java.util.UUID;

public record DuplicateCaseResolveCommand(
        UUID duplicateCaseId,
        String action,
        UUID targetCustomerId,
        String reason
) {
}
