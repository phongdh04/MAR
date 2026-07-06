package vn.mar.sla.api;

import java.util.UUID;

public record SlaTaskSearchCommand(
        UUID ownerId,
        UUID branchId,
        String status,
        String taskType,
        Integer page,
        Integer size
) {
}
