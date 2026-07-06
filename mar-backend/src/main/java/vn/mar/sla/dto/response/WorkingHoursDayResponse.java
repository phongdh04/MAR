package vn.mar.sla.dto.response;

import java.time.LocalTime;

public record WorkingHoursDayResponse(
        String weekday,
        boolean workingDay,
        LocalTime startTime,
        LocalTime endTime,
        String source
) {
}
