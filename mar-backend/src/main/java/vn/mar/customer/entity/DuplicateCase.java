package vn.mar.customer.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import vn.mar.customer.model.DuplicateCaseStatus;
import vn.mar.customer.model.DuplicateConfidence;
import vn.mar.customer.model.DuplicateMatchType;
import vn.mar.customer.model.DuplicateResolutionAction;

@Entity
@Table(name = "duplicate_cases")
public class DuplicateCase {

    @Id
    @Column(name = "duplicate_case_id", nullable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "source_customer_id", nullable = false)
    private UUID sourceCustomerId;

    @Column(name = "matched_customer_id", nullable = false)
    private UUID matchedCustomerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "match_type", nullable = false)
    private DuplicateMatchType matchType;

    @Enumerated(EnumType.STRING)
    @Column(name = "confidence", nullable = false)
    private DuplicateConfidence confidence;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private DuplicateCaseStatus status;

    @Column(name = "review_reason", nullable = false)
    private String reviewReason;

    @Enumerated(EnumType.STRING)
    @Column(name = "resolution_action")
    private DuplicateResolutionAction resolutionAction;

    @Column(name = "resolved_by")
    private UUID resolvedBy;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Column(name = "resolution_reason")
    private String resolutionReason;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected DuplicateCase() {
    }

    private DuplicateCase(
            UUID id,
            UUID tenantId,
            UUID sourceCustomerId,
            UUID matchedCustomerId,
            DuplicateMatchType matchType,
            DuplicateConfidence confidence,
            DuplicateCaseStatus status,
            String reviewReason,
            DuplicateResolutionAction resolutionAction,
            UUID resolvedBy,
            Instant resolvedAt,
            String resolutionReason,
            Instant createdAt,
            Instant updatedAt) {
        this.id = id;
        this.tenantId = tenantId;
        this.sourceCustomerId = sourceCustomerId;
        this.matchedCustomerId = matchedCustomerId;
        this.matchType = matchType;
        this.confidence = confidence;
        this.status = status;
        this.reviewReason = reviewReason;
        this.resolutionAction = resolutionAction;
        this.resolvedBy = resolvedBy;
        this.resolvedAt = resolvedAt;
        this.resolutionReason = resolutionReason;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static DuplicateCase create(
            UUID id,
            UUID tenantId,
            UUID sourceCustomerId,
            UUID matchedCustomerId,
            DuplicateMatchType matchType,
            DuplicateConfidence confidence,
            String reviewReason,
            Instant now) {
        return new DuplicateCase(
                id,
                tenantId,
                sourceCustomerId,
                matchedCustomerId,
                matchType,
                confidence,
                DuplicateCaseStatus.NEEDS_REVIEW,
                reviewReason,
                null,
                null,
                null,
                null,
                now,
                now
        );
    }

    public static DuplicateCase restore(
            UUID id,
            UUID tenantId,
            UUID sourceCustomerId,
            UUID matchedCustomerId,
            DuplicateMatchType matchType,
            DuplicateConfidence confidence,
            DuplicateCaseStatus status,
            String reviewReason,
            DuplicateResolutionAction resolutionAction,
            UUID resolvedBy,
            Instant resolvedAt,
            String resolutionReason,
            Instant createdAt,
            Instant updatedAt) {
        return new DuplicateCase(
                id,
                tenantId,
                sourceCustomerId,
                matchedCustomerId,
                matchType,
                confidence,
                status,
                reviewReason,
                resolutionAction,
                resolvedBy,
                resolvedAt,
                resolutionReason,
                createdAt,
                updatedAt
        );
    }

    public void resolve(DuplicateResolutionAction action, String reason, UUID actorId, Instant now) {
        this.resolutionAction = action;
        this.status = switch (action) {
            case MERGE -> DuplicateCaseStatus.MERGED;
            case LINK -> DuplicateCaseStatus.LINKED;
            case IGNORE -> DuplicateCaseStatus.IGNORED;
        };
        this.resolvedBy = actorId;
        this.resolvedAt = now;
        this.resolutionReason = reason == null ? null : reason.trim();
        this.updatedAt = now;
    }

    public void markUnmerged(Instant now) {
        this.status = DuplicateCaseStatus.UNMERGED;
        this.updatedAt = now;
    }

    public UUID id() {
        return id;
    }

    public UUID tenantId() {
        return tenantId;
    }

    public UUID sourceCustomerId() {
        return sourceCustomerId;
    }

    public UUID matchedCustomerId() {
        return matchedCustomerId;
    }

    public DuplicateMatchType matchType() {
        return matchType;
    }

    public DuplicateConfidence confidence() {
        return confidence;
    }

    public DuplicateCaseStatus status() {
        return status;
    }

    public String reviewReason() {
        return reviewReason;
    }

    public DuplicateResolutionAction resolutionAction() {
        return resolutionAction;
    }

    public UUID resolvedBy() {
        return resolvedBy;
    }

    public Instant resolvedAt() {
        return resolvedAt;
    }

    public String resolutionReason() {
        return resolutionReason;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }
}
