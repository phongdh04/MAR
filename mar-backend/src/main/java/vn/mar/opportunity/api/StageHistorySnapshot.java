package vn.mar.opportunity.api;

import java.time.Instant;
import java.util.UUID;

public record StageHistorySnapshot(
        UUID stageHistoryId,
        String fromStage,
        String toStage,
        UUID changedBy,
        String changedByType,
        Instant changedAt,
        String reason,
        Long durationInPreviousStageSeconds
) {
}
