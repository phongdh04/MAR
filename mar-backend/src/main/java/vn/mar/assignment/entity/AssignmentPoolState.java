package vn.mar.assignment.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "assignment_pool_states")
public class AssignmentPoolState {

    @Id
    @Column(name = "assignment_pool_state_id", nullable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "assignment_rule_id")
    private UUID assignmentRuleId;

    @Column(name = "last_assigned_advisor_id")
    private UUID lastAssignedAdvisorId;

    @Column(name = "last_assigned_at")
    private Instant lastAssignedAt;

    @Column(name = "version_number", nullable = false)
    private int versionNumber;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "updated_by")
    private UUID updatedBy;

    protected AssignmentPoolState() {
    }

    private AssignmentPoolState(
            UUID id,
            UUID tenantId,
            UUID assignmentRuleId,
            UUID lastAssignedAdvisorId,
            Instant lastAssignedAt,
            int versionNumber,
            Instant createdAt,
            UUID createdBy,
            Instant updatedAt,
            UUID updatedBy) {
        this.id = id;
        this.tenantId = tenantId;
        this.assignmentRuleId = assignmentRuleId;
        this.lastAssignedAdvisorId = lastAssignedAdvisorId;
        this.lastAssignedAt = lastAssignedAt;
        this.versionNumber = versionNumber;
        this.createdAt = createdAt;
        this.createdBy = createdBy;
        this.updatedAt = updatedAt;
        this.updatedBy = updatedBy;
    }

    public static AssignmentPoolState create(
            UUID id,
            UUID tenantId,
            UUID assignmentRuleId,
            UUID actorId,
            Instant now) {
        return new AssignmentPoolState(id, tenantId, assignmentRuleId, null, null, 0, now, actorId, now, actorId);
    }

    public void markAssigned(UUID advisorId, UUID actorId, Instant now) {
        this.lastAssignedAdvisorId = advisorId;
        this.lastAssignedAt = now;
        this.versionNumber += 1;
        this.updatedBy = actorId;
        this.updatedAt = now;
    }

    public UUID id() {
        return id;
    }

    public UUID tenantId() {
        return tenantId;
    }

    public UUID assignmentRuleId() {
        return assignmentRuleId;
    }

    public UUID lastAssignedAdvisorId() {
        return lastAssignedAdvisorId;
    }

    public Instant lastAssignedAt() {
        return lastAssignedAt;
    }
}
