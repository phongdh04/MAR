package vn.mar.assignment.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import vn.mar.assignment.model.AssignmentRuleAdvisorStatus;

@Entity
@Table(name = "assignment_rule_advisors")
public class AssignmentRuleAdvisor {

    @Id
    @Column(name = "assignment_rule_advisor_id", nullable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "assignment_rule_id", nullable = false)
    private UUID assignmentRuleId;

    @Column(name = "advisor_id", nullable = false)
    private UUID advisorId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private AssignmentRuleAdvisorStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "updated_by")
    private UUID updatedBy;

    protected AssignmentRuleAdvisor() {
    }

    private AssignmentRuleAdvisor(
            UUID id,
            UUID tenantId,
            UUID assignmentRuleId,
            UUID advisorId,
            AssignmentRuleAdvisorStatus status,
            Instant createdAt,
            UUID createdBy,
            Instant updatedAt,
            UUID updatedBy) {
        this.id = id;
        this.tenantId = tenantId;
        this.assignmentRuleId = assignmentRuleId;
        this.advisorId = advisorId;
        this.status = status;
        this.createdAt = createdAt;
        this.createdBy = createdBy;
        this.updatedAt = updatedAt;
        this.updatedBy = updatedBy;
    }

    public static AssignmentRuleAdvisor create(
            UUID id,
            UUID tenantId,
            UUID assignmentRuleId,
            UUID advisorId,
            UUID actorId,
            Instant now) {
        return new AssignmentRuleAdvisor(
                id,
                tenantId,
                assignmentRuleId,
                advisorId,
                AssignmentRuleAdvisorStatus.ACTIVE,
                now,
                actorId,
                now,
                actorId
        );
    }

    public void inactivate(UUID actorId, Instant now) {
        this.status = AssignmentRuleAdvisorStatus.INACTIVE;
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

    public UUID advisorId() {
        return advisorId;
    }

    public AssignmentRuleAdvisorStatus status() {
        return status;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }
}
