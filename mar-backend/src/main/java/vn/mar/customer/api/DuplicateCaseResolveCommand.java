package vn.mar.customer.api;

import java.util.UUID;
import vn.mar.customer.model.DuplicateResolutionAction;

public record DuplicateCaseResolveCommand(
        UUID tenantId,
        UUID duplicateCaseId,
        DuplicateResolutionAction action,
        String reason,
        UUID actorId,
        String actorRole
) {
}
