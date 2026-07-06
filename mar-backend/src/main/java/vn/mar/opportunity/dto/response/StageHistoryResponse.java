package vn.mar.opportunity.dto.response;

import java.time.Instant;
import java.util.UUID;

public record StageHistoryResponse(
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
