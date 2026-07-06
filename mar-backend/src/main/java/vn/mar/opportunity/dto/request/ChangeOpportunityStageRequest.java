package vn.mar.opportunity.dto.request;

import jakarta.validation.constraints.NotBlank;

public record ChangeOpportunityStageRequest(
        @NotBlank
        String toStage,
        String lostReason,
        String lostNote,
        String reason
) {
}
