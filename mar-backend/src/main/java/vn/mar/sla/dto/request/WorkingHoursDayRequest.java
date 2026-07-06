package vn.mar.sla.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalTime;

public record WorkingHoursDayRequest(
        @NotBlank
        @Size(max = 16)
        String weekday,

        @NotNull
        Boolean workingDay,

        LocalTime startTime,

        LocalTime endTime
) {
}
