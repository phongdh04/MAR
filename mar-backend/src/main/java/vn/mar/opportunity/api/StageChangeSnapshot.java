package vn.mar.opportunity.api;

import java.time.Instant;
import java.util.UUID;

public record StageChangeSnapshot(
        UUID opportunityId,
        String fromStage,
        String toStage,
        UUID stageHistoryId,
        Instant changedAt
) {
}
