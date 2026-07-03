package vn.mar.customer.dto.response;

import java.time.Instant;
import java.util.UUID;

public record MergeHistoryResponse(
        UUID mergeId,
        UUID tenantId,
        UUID sourceCustomerId,
        UUID targetCustomerId,
        UUID duplicateCaseId,
        UUID mergedBy,
        Instant mergedAt,
        String reason,
        boolean canUnmerge,
        UUID unmergedBy,
        Instant unmergedAt
) {
}
