package vn.mar.sla.mapper;

import org.springframework.stereotype.Component;
import vn.mar.sla.api.SlaOverdueScanSnapshot;
import vn.mar.sla.api.SlaTaskSnapshot;
import vn.mar.sla.dto.response.SlaOverdueScanResponse;
import vn.mar.sla.dto.response.SlaTaskResponse;
import vn.mar.sla.entity.SlaTask;

@Component
public class SlaTaskMapper {

    public SlaTaskSnapshot toSnapshot(SlaTask task) {
        return new SlaTaskSnapshot(
                task.id(),
                task.tenantId(),
                task.opportunityId(),
                task.sourceLeadId(),
                task.ownerId(),
                task.branchId(),
                task.slaPolicyId(),
                task.taskType().name(),
                task.status().name(),
                task.leadTemperature().name(),
                task.dueAt(),
                task.completedAt(),
                task.completedActivityId(),
                task.slaHit(),
                task.overdueMarkedAt(),
                task.overdueLevel().name(),
                task.escalatedTo(),
                task.salesLeadEscalatedAt(),
                task.createdAt(),
                task.createdBy(),
                task.updatedAt(),
                task.updatedBy()
        );
    }

    public SlaTaskResponse toResponse(SlaTaskSnapshot snapshot) {
        return new SlaTaskResponse(
                snapshot.slaTaskId(),
                snapshot.tenantId(),
                snapshot.opportunityId(),
                snapshot.sourceLeadId(),
                snapshot.ownerId(),
                snapshot.branchId(),
                snapshot.slaPolicyId(),
                snapshot.taskType(),
                snapshot.status(),
                snapshot.leadTemperature(),
                snapshot.dueAt(),
                snapshot.completedAt(),
                snapshot.completedActivityId(),
                snapshot.slaHit(),
                snapshot.overdueMarkedAt(),
                snapshot.overdueLevel(),
                snapshot.escalatedTo(),
                snapshot.salesLeadEscalatedAt(),
                snapshot.createdAt(),
                snapshot.createdBy(),
                snapshot.updatedAt(),
                snapshot.updatedBy()
        );
    }

    public SlaOverdueScanResponse toResponse(SlaOverdueScanSnapshot snapshot) {
        return new SlaOverdueScanResponse(
                snapshot.scannedAt(),
                snapshot.markedOverdueCount(),
                snapshot.escalatedSalesLeadCount()
        );
    }
}
