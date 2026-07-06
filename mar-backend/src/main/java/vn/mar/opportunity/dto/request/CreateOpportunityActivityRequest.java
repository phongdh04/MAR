package vn.mar.opportunity.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public record CreateOpportunityActivityRequest(
        @NotBlank
        String activityType,

        String activityResult,

        @NotNull
        Instant occurredAt,

        @Size(max = 4000)
        String note,

        Instant nextActionAt,

        @NotBlank
        String source
) {
}
