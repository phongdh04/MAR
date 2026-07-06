package vn.mar.sla.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SlaPolicyItemRequest(
        @NotBlank
        @Size(max = 32)
        String policyType,

        @NotNull
        @Min(0)
        @Max(10080)
        Integer responseDueMinutes
) {
}
