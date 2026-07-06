package vn.mar.sla.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import vn.mar.lead.model.LeadTemperature;
import vn.mar.sla.model.SlaOverdueLevel;
import vn.mar.sla.model.SlaTaskStatus;
import vn.mar.sla.model.SlaTaskType;

@Entity
@Table(name = "sla_tasks")
public class SlaTask {

    @Id
    @Column(name = "sla_task_id", nullable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "opportunity_id", nullable = false)
    private UUID opportunityId;

    @Column(name = "source_lead_id", nullable = false)
    private UUID sourceLeadId;

    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    @Column(name = "branch_id")
    private UUID branchId;

    @Column(name = "sla_policy_id")
    private UUID slaPolicyId;

    @Enumerated(EnumType.STRING)
    @Column(name = "task_type", nullable = false)
    private SlaTaskType taskType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private SlaTaskStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "lead_temperature", nullable = false)
    private LeadTemperature leadTemperature;

    @Column(name = "due_at", nullable = false)
    private Instant dueAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "completed_activity_id")
    private UUID completedActivityId;

    @Column(name = "overdue_marked_at")
    private Instant overdueMarkedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "overdue_level", nullable = false)
    private SlaOverdueLevel overdueLevel;

    @Column(name = "escalated_to")
    private UUID escalatedTo;

    @Column(name = "sales_lead_escalated_at")
    private Instant salesLeadEscalatedAt;

    @Column(name = "cancellation_reason")
    private String cancellationReason;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "updated_by")
    private UUID updatedBy;

    protected SlaTask() {
    }

    private SlaTask(
            UUID id,
            UUID tenantId,
            UUID opportunityId,
            UUID sourceLeadId,
            UUID ownerId,
            UUID branchId,
            UUID slaPolicyId,
            SlaTaskType taskType,
            SlaTaskStatus status,
            LeadTemperature leadTemperature,
            Instant dueAt,
            Instant completedAt,
            UUID completedActivityId,
            Instant overdueMarkedAt,
            SlaOverdueLevel overdueLevel,
            UUID escalatedTo,
            Instant salesLeadEscalatedAt,
            String cancellationReason,
            Instant createdAt,
            UUID createdBy,
            Instant updatedAt,
            UUID updatedBy) {
        this.id = id;
        this.tenantId = tenantId;
        this.opportunityId = opportunityId;
        this.sourceLeadId = sourceLeadId;
        this.ownerId = ownerId;
        this.branchId = branchId;
        this.slaPolicyId = slaPolicyId;
        this.taskType = taskType;
        this.status = status;
        this.leadTemperature = leadTemperature;
        this.dueAt = dueAt;
        this.completedAt = completedAt;
        this.completedActivityId = completedActivityId;
        this.overdueMarkedAt = overdueMarkedAt;
        this.overdueLevel = overdueLevel;
        this.escalatedTo = escalatedTo;
        this.salesLeadEscalatedAt = salesLeadEscalatedAt;
        this.cancellationReason = cancellationReason;
        this.createdAt = createdAt;
        this.createdBy = createdBy;
        this.updatedAt = updatedAt;
        this.updatedBy = updatedBy;
    }

    public static SlaTask openFirstResponse(
            UUID id,
            UUID tenantId,
            UUID opportunityId,
            UUID sourceLeadId,
            UUID ownerId,
            UUID branchId,
            UUID slaPolicyId,
            LeadTemperature leadTemperature,
            Instant dueAt,
            UUID actorId,
            Instant now) {
        return new SlaTask(
                id,
                tenantId,
                opportunityId,
                sourceLeadId,
                ownerId,
                branchId,
                slaPolicyId,
                SlaTaskType.FIRST_RESPONSE,
                SlaTaskStatus.OPEN,
                leadTemperature,
                dueAt,
                null,
                null,
                null,
                SlaOverdueLevel.NONE,
                null,
                null,
                null,
                now,
                actorId,
                now,
                actorId
        );
    }

    public void refreshAssignment(
            UUID ownerId,
            UUID branchId,
            UUID slaPolicyId,
            LeadTemperature leadTemperature,
            Instant dueAt,
            UUID actorId,
            Instant now) {
        this.ownerId = ownerId;
        this.branchId = branchId;
        this.slaPolicyId = slaPolicyId;
        this.leadTemperature = leadTemperature;
        this.dueAt = dueAt;
        this.status = SlaTaskStatus.OPEN;
        this.overdueMarkedAt = null;
        this.overdueLevel = SlaOverdueLevel.NONE;
        this.escalatedTo = null;
        this.salesLeadEscalatedAt = null;
        this.updatedBy = actorId;
        this.updatedAt = now;
    }

    public void complete(UUID activityId, Instant occurredAt, UUID actorId, Instant now) {
        if (status == SlaTaskStatus.COMPLETED) {
            return;
        }
        this.status = SlaTaskStatus.COMPLETED;
        this.completedActivityId = activityId;
        this.completedAt = occurredAt;
        this.updatedBy = actorId;
        this.updatedAt = now;
    }

    public void markOverdue(UUID actorId, Instant now) {
        if (status != SlaTaskStatus.OPEN) {
            return;
        }
        this.status = SlaTaskStatus.OVERDUE;
        this.overdueMarkedAt = now;
        this.overdueLevel = SlaOverdueLevel.ADVISOR;
        this.escalatedTo = ownerId;
        this.updatedBy = actorId;
        this.updatedAt = now;
    }

    public void escalateToSalesLead(UUID actorId, Instant now) {
        if (status != SlaTaskStatus.OVERDUE || overdueLevel != SlaOverdueLevel.ADVISOR) {
            return;
        }
        this.overdueLevel = SlaOverdueLevel.SALES_LEAD;
        this.escalatedTo = null;
        this.salesLeadEscalatedAt = now;
        this.updatedBy = actorId;
        this.updatedAt = now;
    }

    public Boolean slaHit() {
        if (completedAt == null) {
            return null;
        }
        return !completedAt.isAfter(dueAt);
    }

    public UUID id() {
        return id;
    }

    public UUID tenantId() {
        return tenantId;
    }

    public UUID opportunityId() {
        return opportunityId;
    }

    public UUID sourceLeadId() {
        return sourceLeadId;
    }

    public UUID ownerId() {
        return ownerId;
    }

    public UUID branchId() {
        return branchId;
    }

    public UUID slaPolicyId() {
        return slaPolicyId;
    }

    public SlaTaskType taskType() {
        return taskType;
    }

    public SlaTaskStatus status() {
        return status;
    }

    public LeadTemperature leadTemperature() {
        return leadTemperature;
    }

    public Instant dueAt() {
        return dueAt;
    }

    public Instant completedAt() {
        return completedAt;
    }

    public UUID completedActivityId() {
        return completedActivityId;
    }

    public Instant overdueMarkedAt() {
        return overdueMarkedAt;
    }

    public SlaOverdueLevel overdueLevel() {
        return overdueLevel;
    }

    public UUID escalatedTo() {
        return escalatedTo;
    }

    public Instant salesLeadEscalatedAt() {
        return salesLeadEscalatedAt;
    }

    public String cancellationReason() {
        return cancellationReason;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public UUID createdBy() {
        return createdBy;
    }

    public Instant updatedAt() {
        return updatedAt;
    }

    public UUID updatedBy() {
        return updatedBy;
    }
}
