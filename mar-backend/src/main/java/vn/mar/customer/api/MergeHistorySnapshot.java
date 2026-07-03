package vn.mar.customer.api;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record MergeHistorySnapshot(
        UUID mergeId,
        UUID tenantId,
        UUID sourceCustomerId,
        UUID targetCustomerId,
        UUID duplicateCaseId,
        UUID mergedBy,
        Instant mergedAt,
        String reason,
        Map<String, Object> mergeSnapshot,
        boolean canUnmerge,
        UUID unmergedBy,
        Instant unmergedAt
) {
}
