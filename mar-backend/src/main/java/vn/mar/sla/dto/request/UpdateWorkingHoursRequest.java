package vn.mar.sla.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

public record UpdateWorkingHoursRequest(
        UUID branchId,

        @NotBlank
        @Size(max = 100)
        String timezone,

        @NotEmpty
        @Size(min = 7, max = 7)
        List<@Valid WorkingHoursDayRequest> days
) {
}
