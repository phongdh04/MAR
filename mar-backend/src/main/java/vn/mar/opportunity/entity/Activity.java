package vn.mar.opportunity.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import vn.mar.opportunity.model.ActivityActorType;
import vn.mar.opportunity.model.ActivityResult;
import vn.mar.opportunity.model.ActivitySource;
import vn.mar.opportunity.model.ActivityType;

@Entity
@Table(name = "activities")
public class Activity {

    @Id
    @Column(name = "activity_id", nullable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(name = "opportunity_id", nullable = false)
    private UUID opportunityId;

    @Column(name = "actor_id")
    private UUID actorId;

    @Enumerated(EnumType.STRING)
    @Column(name = "actor_type", nullable = false)
    private ActivityActorType actorType;

    @Enumerated(EnumType.STRING)
    @Column(name = "activity_type", nullable = false)
    private ActivityType activityType;

    @Enumerated(EnumType.STRING)
    @Column(name = "activity_result")
    private ActivityResult activityResult;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "note")
    private String note;

    @Column(name = "next_action_at")
    private Instant nextActionAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false)
    private ActivitySource source;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected Activity() {
    }

    private Activity(
            UUID id,
            UUID tenantId,
            UUID customerId,
            UUID opportunityId,
            UUID actorId,
            ActivityActorType actorType,
            ActivityType activityType,
            ActivityResult activityResult,
            Instant occurredAt,
            String note,
            Instant nextActionAt,
            ActivitySource source,
            Instant createdAt) {
        this.id = id;
        this.tenantId = tenantId;
        this.customerId = customerId;
        this.opportunityId = opportunityId;
        this.actorId = actorId;
        this.actorType = actorType;
        this.activityType = activityType;
        this.activityResult = activityResult;
        this.occurredAt = occurredAt;
        this.note = note;
        this.nextActionAt = nextActionAt;
        this.source = source;
        this.createdAt = createdAt;
    }

    public static Activity create(
            UUID id,
            UUID tenantId,
            UUID customerId,
            UUID opportunityId,
            UUID actorId,
            ActivityActorType actorType,
            ActivityType activityType,
            ActivityResult activityResult,
            Instant occurredAt,
            String note,
            Instant nextActionAt,
            ActivitySource source,
            Instant createdAt) {
        return new Activity(
                id,
                tenantId,
                customerId,
                opportunityId,
                actorId,
                actorType,
                activityType,
                activityResult,
                occurredAt,
                note,
                nextActionAt,
                source,
                createdAt
        );
    }

    public boolean contactSuccess() {
        return activityResult != null && activityResult.isContactSuccess();
    }

    public boolean firstResponseCandidate() {
        return activityType.isOutbound()
                && activityResult != null
                && activityResult.isFirstResponseCandidate();
    }

    public UUID id() {
        return id;
    }

    public UUID tenantId() {
        return tenantId;
    }

    public UUID customerId() {
        return customerId;
    }

    public UUID opportunityId() {
        return opportunityId;
    }

    public UUID actorId() {
        return actorId;
    }

    public ActivityActorType actorType() {
        return actorType;
    }

    public ActivityType activityType() {
        return activityType;
    }

    public ActivityResult activityResult() {
        return activityResult;
    }

    public Instant occurredAt() {
        return occurredAt;
    }

    public String note() {
        return note;
    }

    public Instant nextActionAt() {
        return nextActionAt;
    }

    public ActivitySource source() {
        return source;
    }

    public Instant createdAt() {
        return createdAt;
    }
}
