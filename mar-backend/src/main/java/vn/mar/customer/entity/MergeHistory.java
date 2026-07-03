package vn.mar.customer.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "merge_history")
public class MergeHistory {

    @Id
    @Column(name = "merge_id", nullable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "source_customer_id", nullable = false)
    private UUID sourceCustomerId;

    @Column(name = "target_customer_id", nullable = false)
    private UUID targetCustomerId;

    @Column(name = "duplicate_case_id")
    private UUID duplicateCaseId;

    @Column(name = "merged_by", nullable = false)
    private UUID mergedBy;

    @Column(name = "merged_at", nullable = false)
    private Instant mergedAt;

    @Column(name = "reason", nullable = false)
    private String reason;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "merge_snapshot", columnDefinition = "jsonb")
    private Map<String, Object> mergeSnapshot;

    @Column(name = "can_unmerge", nullable = false)
    private boolean canUnmerge;

    @Column(name = "unmerged_by")
    private UUID unmergedBy;

    @Column(name = "unmerged_at")
    private Instant unmergedAt;

    protected MergeHistory() {
    }

    private MergeHistory(
            UUID id,
            UUID tenantId,
            UUID sourceCustomerId,
            UUID targetCustomerId,
            UUID duplicateCaseId,
            UUID mergedBy,
            Instant mergedAt,
            String reason,
            Map<String, Object> mergeSnapshot,
            boolean canUnmerge,
            UUID unmergedBy,
            Instant unmergedAt) {
        this.id = id;
        this.tenantId = tenantId;
        this.sourceCustomerId = sourceCustomerId;
        this.targetCustomerId = targetCustomerId;
        this.duplicateCaseId = duplicateCaseId;
        this.mergedBy = mergedBy;
        this.mergedAt = mergedAt;
        this.reason = reason;
        this.mergeSnapshot = mergeSnapshot == null ? null : Map.copyOf(mergeSnapshot);
        this.canUnmerge = canUnmerge;
        this.unmergedBy = unmergedBy;
        this.unmergedAt = unmergedAt;
    }

    public static MergeHistory create(
            UUID id,
            UUID tenantId,
            UUID sourceCustomerId,
            UUID targetCustomerId,
            UUID duplicateCaseId,
            UUID mergedBy,
            Instant mergedAt,
            String reason,
            Map<String, Object> mergeSnapshot,
            boolean canUnmerge) {
        return new MergeHistory(
                id,
                tenantId,
                sourceCustomerId,
                targetCustomerId,
                duplicateCaseId,
                mergedBy,
                mergedAt,
                reason,
                mergeSnapshot,
                canUnmerge,
                null,
                null
        );
    }

    public static MergeHistory restore(
            UUID id,
            UUID tenantId,
            UUID sourceCustomerId,
            UUID targetCustomerId,
            UUID duplicateCaseId,
            UUID mergedBy,
            Instant mergedAt,
            String reason,
            Map<String, Object> mergeSnapshot,
            boolean canUnmerge,
            UUID unmergedBy,
            Instant unmergedAt) {
        return new MergeHistory(
                id,
                tenantId,
                sourceCustomerId,
                targetCustomerId,
                duplicateCaseId,
                mergedBy,
                mergedAt,
                reason,
                mergeSnapshot,
                canUnmerge,
                unmergedBy,
                unmergedAt
        );
    }

    public void markUnmerged(UUID actorId, Instant now) {
        this.unmergedBy = actorId;
        this.unmergedAt = now;
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

    public UUID targetCustomerId() {
        return targetCustomerId;
    }

    public UUID duplicateCaseId() {
        return duplicateCaseId;
    }

    public UUID mergedBy() {
        return mergedBy;
    }

    public Instant mergedAt() {
        return mergedAt;
    }

    public String reason() {
        return reason;
    }

    public Map<String, Object> mergeSnapshot() {
        return mergeSnapshot;
    }

    public boolean canUnmerge() {
        return canUnmerge;
    }

    public UUID unmergedBy() {
        return unmergedBy;
    }

    public Instant unmergedAt() {
        return unmergedAt;
    }
}
