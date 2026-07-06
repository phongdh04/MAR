package vn.mar.opportunity.api;

import java.util.UUID;

public record ChangeOpportunityStageCommand(
        UUID opportunityId,
        String toStage,
        String lostReason,
        String lostNote,
        String reason
) {
}
