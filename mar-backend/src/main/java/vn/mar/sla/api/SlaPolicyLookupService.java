package vn.mar.sla.api;

public interface SlaPolicyLookupService {

    DueTimeCalculationResult calculateFirstResponseDueAt(DueTimeCalculationCommand command);
}
