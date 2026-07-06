package vn.mar.assignment.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import vn.mar.assignment.model.UnassignedAssignmentItemStatus;
import vn.mar.assignment.model.UnassignedReasonCode;

@Entity
@Table(name = "unassigned_assignment_items")
public class UnassignedAssignmentItem {

    @Id
    @Column(name = "unassigned_item_id", nullable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "opportunity_id", nullable = false)
    private UUID opportunityId;

    @Column(name = "source_lead_id", nullable = false)
    private UUID sourceLeadId;

    @Column(name = "assignment_rule_id")
    private UUID assignmentRuleId;

    @Enumerated(EnumType.STRING)
    @Column(name = "reason_code", nullable = false)
    private UnassignedReasonCode reasonCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private UnassignedAssignmentItemStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Column(name = "resolved_by")
    private UUID resolvedBy;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "updated_by")
    private UUID updatedBy;

    protected UnassignedAssignmentItem() {
    }

    private UnassignedAssignmentItem(
            UUID id,
            UUID tenantId,
            UUID opportunityId,
            UUID sourceLeadId,
            UUID assignmentRuleId,
            UnassignedReasonCode reasonCode,
            UnassignedAssignmentItemStatus status,
            Instant createdAt,
            UUID createdBy,
            Instant resolvedAt,
            UUID resolvedBy,
            Instant updatedAt,
            UUID updatedBy) {
        this.id = id;
        this.tenantId = tenantId;
        this.opportunityId = opportunityId;
        this.sourceLeadId = sourceLeadId;
        this.assignmentRuleId = assignmentRuleId;
        this.reasonCode = reasonCode;
        this.status = status;
        this.createdAt = createdAt;
        this.createdBy = createdBy;
        this.resolvedAt = resolvedAt;
        this.resolvedBy = resolvedBy;
        this.updatedAt = updatedAt;
        this.updatedBy = updatedBy;
    }

    public static UnassignedAssignmentItem create(
            UUID id,
            UUID tenantId,
            UUID opportunityId,
            UUID sourceLeadId,
            UUID assignmentRuleId,
            UnassignedReasonCode reasonCode,
            UUID actorId,
            Instant now) {
        return new UnassignedAssignmentItem(
                id,
                tenantId,
                opportunityId,
                sourceLeadId,
                assignmentRuleId,
                reasonCode,
                UnassignedAssignmentItemStatus.OPEN,
                now,
                actorId,
                null,
                null,
                now,
                actorId
        );
    }

    public void resolve(UUID actorId, Instant now) {
        this.status = UnassignedAssignmentItemStatus.RESOLVED;
        this.resolvedAt = now;
        this.resolvedBy = actorId;
        this.updatedAt = now;
        this.updatedBy = actorId;
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

    public UUID assignmentRuleId() {
        return assignmentRuleId;
    }

    public UnassignedReasonCode reasonCode() {
        return reasonCode;
    }

    public UnassignedAssignmentItemStatus status() {
        return status;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public UUID createdBy() {
        return createdBy;
    }

    public Instant resolvedAt() {
        return resolvedAt;
    }

    public UUID resolvedBy() {
        return resolvedBy;
    }

    public Instant updatedAt() {
        return updatedAt;
    }
}
