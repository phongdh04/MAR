package vn.mar.assignment.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import vn.mar.assignment.model.AssignmentRuleStatus;
import vn.mar.assignment.model.AssignmentStrategy;

@Entity
@Table(name = "assignment_rules")
public class AssignmentRule {

    @Id
    @Column(name = "assignment_rule_id", nullable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "rule_name", nullable = false)
    private String ruleName;

    @Column(name = "priority", nullable = false)
    private int priority;

    @Column(name = "language_id")
    private UUID languageId;

    @Column(name = "program_id")
    private UUID programId;

    @Column(name = "branch_id")
    private UUID branchId;

    @Enumerated(EnumType.STRING)
    @Column(name = "assignment_strategy", nullable = false)
    private AssignmentStrategy assignmentStrategy;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private AssignmentRuleStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "updated_by")
    private UUID updatedBy;

    protected AssignmentRule() {
    }

    private AssignmentRule(
            UUID id,
            UUID tenantId,
            String ruleName,
            int priority,
            UUID languageId,
            UUID programId,
            UUID branchId,
            AssignmentStrategy assignmentStrategy,
            AssignmentRuleStatus status,
            Instant createdAt,
            UUID createdBy,
            Instant updatedAt,
            UUID updatedBy) {
        this.id = id;
        this.tenantId = tenantId;
        this.ruleName = ruleName;
        this.priority = priority;
        this.languageId = languageId;
        this.programId = programId;
        this.branchId = branchId;
        this.assignmentStrategy = assignmentStrategy;
        this.status = status;
        this.createdAt = createdAt;
        this.createdBy = createdBy;
        this.updatedAt = updatedAt;
        this.updatedBy = updatedBy;
    }

    public static AssignmentRule create(
            UUID id,
            UUID tenantId,
            String ruleName,
            int priority,
            UUID languageId,
            UUID programId,
            UUID branchId,
            AssignmentStrategy assignmentStrategy,
            AssignmentRuleStatus status,
            UUID actorId,
            Instant now) {
        return new AssignmentRule(
                id,
                tenantId,
                ruleName,
                priority,
                languageId,
                programId,
                branchId,
                assignmentStrategy,
                status,
                now,
                actorId,
                now,
                actorId
        );
    }

    public void update(
            String ruleName,
            int priority,
            UUID languageId,
            UUID programId,
            UUID branchId,
            AssignmentStrategy assignmentStrategy,
            AssignmentRuleStatus status,
            UUID actorId,
            Instant now) {
        this.ruleName = ruleName;
        this.priority = priority;
        this.languageId = languageId;
        this.programId = programId;
        this.branchId = branchId;
        this.assignmentStrategy = assignmentStrategy;
        this.status = status;
        this.updatedBy = actorId;
        this.updatedAt = now;
    }

    public boolean matches(UUID requestedLanguageId, UUID requestedProgramId, UUID requestedBranchId) {
        return matchesNullable(languageId, requestedLanguageId)
                && matchesNullable(programId, requestedProgramId)
                && matchesNullable(branchId, requestedBranchId);
    }

    private boolean matchesNullable(UUID ruleValue, UUID requestedValue) {
        return ruleValue == null || ruleValue.equals(requestedValue);
    }

    public UUID id() {
        return id;
    }

    public UUID tenantId() {
        return tenantId;
    }

    public String ruleName() {
        return ruleName;
    }

    public int priority() {
        return priority;
    }

    public UUID languageId() {
        return languageId;
    }

    public UUID programId() {
        return programId;
    }

    public UUID branchId() {
        return branchId;
    }

    public AssignmentStrategy assignmentStrategy() {
        return assignmentStrategy;
    }

    public AssignmentRuleStatus status() {
        return status;
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
