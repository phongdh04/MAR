package vn.mar.sla.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import vn.mar.sla.model.SlaPolicyStatus;
import vn.mar.sla.model.SlaPolicyType;

@Entity
@Table(name = "sla_policies")
public class SlaPolicy {

    @Id
    @Column(name = "sla_policy_id", nullable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "branch_id")
    private UUID branchId;

    @Enumerated(EnumType.STRING)
    @Column(name = "policy_type", nullable = false)
    private SlaPolicyType policyType;

    @Column(name = "response_due_minutes", nullable = false)
    private int responseDueMinutes;

    @Column(name = "timezone", nullable = false)
    private String timezone;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private SlaPolicyStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "updated_by")
    private UUID updatedBy;

    protected SlaPolicy() {
    }

    private SlaPolicy(
            UUID id,
            UUID tenantId,
            UUID branchId,
            SlaPolicyType policyType,
            int responseDueMinutes,
            String timezone,
            SlaPolicyStatus status,
            Instant createdAt,
            UUID createdBy,
            Instant updatedAt,
            UUID updatedBy) {
        this.id = id;
        this.tenantId = tenantId;
        this.branchId = branchId;
        this.policyType = policyType;
        this.responseDueMinutes = responseDueMinutes;
        this.timezone = timezone;
        this.status = status;
        this.createdAt = createdAt;
        this.createdBy = createdBy;
        this.updatedAt = updatedAt;
        this.updatedBy = updatedBy;
    }

    public static SlaPolicy create(
            UUID id,
            UUID tenantId,
            UUID branchId,
            SlaPolicyType policyType,
            int responseDueMinutes,
            String timezone,
            UUID actorId,
            Instant now) {
        return new SlaPolicy(
                id,
                tenantId,
                branchId,
                policyType,
                responseDueMinutes,
                timezone,
                SlaPolicyStatus.ACTIVE,
                now,
                actorId,
                now,
                actorId
        );
    }

    public static SlaPolicy restore(
            UUID id,
            UUID tenantId,
            UUID branchId,
            SlaPolicyType policyType,
            int responseDueMinutes,
            String timezone,
            SlaPolicyStatus status,
            Instant createdAt,
            UUID createdBy,
            Instant updatedAt,
            UUID updatedBy) {
        return new SlaPolicy(
                id,
                tenantId,
                branchId,
                policyType,
                responseDueMinutes,
                timezone,
                status,
                createdAt,
                createdBy,
                updatedAt,
                updatedBy
        );
    }

    public void update(int responseDueMinutes, String timezone, UUID actorId, Instant now) {
        this.responseDueMinutes = responseDueMinutes;
        this.timezone = timezone;
        this.updatedBy = actorId;
        this.updatedAt = now;
    }

    public UUID id() {
        return id;
    }

    public UUID tenantId() {
        return tenantId;
    }

    public UUID branchId() {
        return branchId;
    }

    public SlaPolicyType policyType() {
        return policyType;
    }

    public int responseDueMinutes() {
        return responseDueMinutes;
    }

    public String timezone() {
        return timezone;
    }

    public SlaPolicyStatus status() {
        return status;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }
}
