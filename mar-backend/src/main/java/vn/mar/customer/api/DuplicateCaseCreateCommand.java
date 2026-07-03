package vn.mar.customer.api;

import java.util.UUID;
import vn.mar.customer.model.DuplicateConfidence;

public record DuplicateCaseCreateCommand(
        UUID tenantId,
        UUID sourceCustomerId,
        UUID matchedCustomerId,
        DuplicateConfidence confidence,
        String reviewReason,
        UUID actorId,
        String actorRole
) {
}
