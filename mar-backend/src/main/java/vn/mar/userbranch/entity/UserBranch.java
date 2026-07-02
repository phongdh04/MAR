package vn.mar.userbranch.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import vn.mar.userbranch.model.UserBranchStatus;

@Entity
@Table(name = "user_branches")
public class UserBranch {

    @Id
    @Column(name = "user_branch_id", nullable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "branch_id", nullable = false)
    private UUID branchId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private UserBranchStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "updated_by")
    private UUID updatedBy;

    protected UserBranch() {
    }

    private UserBranch(
            UUID id,
            UUID tenantId,
            UUID userId,
            UUID branchId,
            UserBranchStatus status,
            Instant createdAt,
            UUID createdBy,
            Instant updatedAt,
            UUID updatedBy) {
        this.id = id;
        this.tenantId = tenantId;
        this.userId = userId;
        this.branchId = branchId;
        this.status = status;
        this.createdAt = createdAt;
        this.createdBy = createdBy;
        this.updatedAt = updatedAt;
        this.updatedBy = updatedBy;
    }

    public static UserBranch create(
            UUID id,
            UUID tenantId,
            UUID userId,
            UUID branchId,
            UUID actorId,
            Instant now) {
        return new UserBranch(id, tenantId, userId, branchId, UserBranchStatus.ACTIVE, now, actorId, now, actorId);
    }

    public static UserBranch restore(
            UUID id,
            UUID tenantId,
            UUID userId,
            UUID branchId,
            UserBranchStatus status,
            Instant createdAt,
            UUID createdBy,
            Instant updatedAt,
            UUID updatedBy) {
        return new UserBranch(id, tenantId, userId, branchId, status, createdAt, createdBy, updatedAt, updatedBy);
    }

    public void inactivate(UUID actorId, Instant now) {
        this.status = UserBranchStatus.INACTIVE;
        this.updatedBy = actorId;
        this.updatedAt = now;
    }

    public UUID id() {
        return id;
    }

    public UUID tenantId() {
        return tenantId;
    }

    public UUID userId() {
        return userId;
    }

    public UUID branchId() {
        return branchId;
    }

    public UserBranchStatus status() {
        return status;
    }
}
