package vn.mar.branch.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import vn.mar.branch.model.BranchStatus;

@Entity
@Table(name = "branches")
public class Branch {

    @Id
    @Column(name = "branch_id", nullable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "branch_code", nullable = false)
    private String branchCode;

    @Column(name = "branch_name", nullable = false)
    private String branchName;

    @Column(name = "city")
    private String city;

    @Column(name = "phone_number")
    private String phoneNumber;

    @Column(name = "address")
    private String address;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private BranchStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "updated_by")
    private UUID updatedBy;

    protected Branch() {
    }

    private Branch(
            UUID id,
            UUID tenantId,
            String branchCode,
            String branchName,
            String city,
            String phoneNumber,
            String address,
            BranchStatus status,
            Instant createdAt,
            UUID createdBy,
            Instant updatedAt,
            UUID updatedBy) {
        this.id = id;
        this.tenantId = tenantId;
        this.branchCode = branchCode;
        this.branchName = branchName;
        this.city = city;
        this.phoneNumber = phoneNumber;
        this.address = address;
        this.status = status;
        this.createdAt = createdAt;
        this.createdBy = createdBy;
        this.updatedAt = updatedAt;
        this.updatedBy = updatedBy;
    }

    public static Branch create(
            UUID id,
            UUID tenantId,
            String branchCode,
            String branchName,
            String city,
            String phoneNumber,
            String address,
            BranchStatus status,
            UUID actorId,
            Instant now) {
        return new Branch(id, tenantId, branchCode, branchName, city, phoneNumber, address, status, now, actorId, now, actorId);
    }

    public static Branch restore(
            UUID id,
            UUID tenantId,
            String branchCode,
            String branchName,
            String city,
            String phoneNumber,
            String address,
            BranchStatus status,
            Instant createdAt,
            UUID createdBy,
            Instant updatedAt,
            UUID updatedBy) {
        return new Branch(id, tenantId, branchCode, branchName, city, phoneNumber, address, status, createdAt, createdBy, updatedAt, updatedBy);
    }

    public void update(
            String branchName,
            String city,
            String phoneNumber,
            String address,
            BranchStatus status,
            UUID actorId,
            Instant now) {
        this.branchName = branchName;
        this.city = city;
        this.phoneNumber = phoneNumber;
        this.address = address;
        this.status = status;
        this.updatedBy = actorId;
        this.updatedAt = now;
    }

    public UUID id() {
        return id;
    }

    public UUID tenantId() {
        return tenantId;
    }

    public String branchCode() {
        return branchCode;
    }

    public String branchName() {
        return branchName;
    }

    public String city() {
        return city;
    }

    public String phoneNumber() {
        return phoneNumber;
    }

    public String address() {
        return address;
    }

    public BranchStatus status() {
        return status;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }
}
