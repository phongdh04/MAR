package vn.mar.assignment.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import vn.mar.assignment.model.AssignmentSource;
import vn.mar.assignment.model.AssignmentStrategy;

@Entity
@Table(name = "assignment_histories")
public class AssignmentHistory {

    @Id
    @Column(name = "assignment_history_id", nullable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "opportunity_id", nullable = false)
    private UUID opportunityId;

    @Column(name = "source_lead_id", nullable = false)
    private UUID sourceLeadId;

    @Column(name = "assignment_rule_id")
    private UUID assignmentRuleId;

    @Column(name = "from_owner_id")
    private UUID fromOwnerId;

    @Column(name = "to_owner_id", nullable = false)
    private UUID toOwnerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "assignment_source", nullable = false)
    private AssignmentSource assignmentSource;

    @Enumerated(EnumType.STRING)
    @Column(name = "assignment_strategy", nullable = false)
    private AssignmentStrategy assignmentStrategy;

    @Column(name = "assigned_at", nullable = false)
    private Instant assignedAt;

    @Column(name = "assigned_by")
    private UUID assignedBy;

    @Column(name = "reason")
    private String reason;

    protected AssignmentHistory() {
    }

    private AssignmentHistory(
            UUID id,
            UUID tenantId,
            UUID opportunityId,
            UUID sourceLeadId,
            UUID assignmentRuleId,
            UUID fromOwnerId,
            UUID toOwnerId,
            AssignmentSource assignmentSource,
            AssignmentStrategy assignmentStrategy,
            Instant assignedAt,
            UUID assignedBy,
            String reason) {
        this.id = id;
        this.tenantId = tenantId;
        this.opportunityId = opportunityId;
        this.sourceLeadId = sourceLeadId;
        this.assignmentRuleId = assignmentRuleId;
        this.fromOwnerId = fromOwnerId;
        this.toOwnerId = toOwnerId;
        this.assignmentSource = assignmentSource;
        this.assignmentStrategy = assignmentStrategy;
        this.assignedAt = assignedAt;
        this.assignedBy = assignedBy;
        this.reason = reason;
    }

    public static AssignmentHistory create(
            UUID id,
            UUID tenantId,
            UUID opportunityId,
            UUID sourceLeadId,
            UUID assignmentRuleId,
            UUID fromOwnerId,
            UUID toOwnerId,
            AssignmentSource assignmentSource,
            AssignmentStrategy assignmentStrategy,
            Instant assignedAt,
            UUID assignedBy,
            String reason) {
        return new AssignmentHistory(
                id,
                tenantId,
                opportunityId,
                sourceLeadId,
                assignmentRuleId,
                fromOwnerId,
                toOwnerId,
                assignmentSource,
                assignmentStrategy,
                assignedAt,
                assignedBy,
                reason
        );
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

    public UUID toOwnerId() {
        return toOwnerId;
    }
}
