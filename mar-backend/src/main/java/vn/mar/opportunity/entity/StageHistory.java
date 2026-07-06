package vn.mar.opportunity.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import vn.mar.opportunity.model.OpportunityStage;

@Entity
@Table(name = "stage_history")
public class StageHistory {

    @Id
    @Column(name = "stage_history_id", nullable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "opportunity_id", nullable = false)
    private UUID opportunityId;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_stage")
    private OpportunityStage fromStage;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_stage", nullable = false)
    private OpportunityStage toStage;

    @Column(name = "changed_by")
    private UUID changedBy;

    @Column(name = "changed_by_type", nullable = false)
    private String changedByType;

    @Column(name = "changed_at", nullable = false)
    private Instant changedAt;

    @Column(name = "reason")
    private String reason;

    @Column(name = "duration_in_previous_stage_seconds")
    private Long durationInPreviousStageSeconds;

    protected StageHistory() {
    }

    private StageHistory(
            UUID id,
            UUID tenantId,
            UUID opportunityId,
            OpportunityStage fromStage,
            OpportunityStage toStage,
            UUID changedBy,
            String changedByType,
            Instant changedAt,
            String reason,
            Long durationInPreviousStageSeconds) {
        this.id = id;
        this.tenantId = tenantId;
        this.opportunityId = opportunityId;
        this.fromStage = fromStage;
        this.toStage = toStage;
        this.changedBy = changedBy;
        this.changedByType = changedByType;
        this.changedAt = changedAt;
        this.reason = reason;
        this.durationInPreviousStageSeconds = durationInPreviousStageSeconds;
    }

    public static StageHistory create(
            UUID id,
            UUID tenantId,
            UUID opportunityId,
            OpportunityStage fromStage,
            OpportunityStage toStage,
            UUID changedBy,
            String changedByType,
            Instant changedAt,
            String reason,
            Long durationInPreviousStageSeconds) {
        return new StageHistory(
                id,
                tenantId,
                opportunityId,
                fromStage,
                toStage,
                changedBy,
                changedByType,
                changedAt,
                reason,
                durationInPreviousStageSeconds
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

    public OpportunityStage fromStage() {
        return fromStage;
    }

    public OpportunityStage toStage() {
        return toStage;
    }

    public UUID changedBy() {
        return changedBy;
    }

    public String changedByType() {
        return changedByType;
    }

    public Instant changedAt() {
        return changedAt;
    }

    public String reason() {
        return reason;
    }

    public Long durationInPreviousStageSeconds() {
        return durationInPreviousStageSeconds;
    }
}
