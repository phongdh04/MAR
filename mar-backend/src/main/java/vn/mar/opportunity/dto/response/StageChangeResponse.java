package vn.mar.opportunity.dto.response;

import java.time.Instant;
import java.util.UUID;

public record StageChangeResponse(
        UUID opportunityId,
        String fromStage,
        String toStage,
        UUID stageHistoryId,
        Instant changedAt
) {
}
