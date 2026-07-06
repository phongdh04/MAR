package vn.mar.sla.api;

import java.time.Instant;

public record DueTimeCalculationResult(
        Instant dueAt,
        SlaPolicySnapshot policy,
        String workingHoursSource,
        boolean afterHoursApplied
) {
}
