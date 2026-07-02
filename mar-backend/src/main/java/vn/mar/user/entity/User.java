package vn.mar.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import vn.mar.user.model.UserStatus;

@Entity(name = "MarUser")
@Table(name = "users")
public class User {

    @Id
    @Column(name = "user_id", nullable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "email", nullable = false)
    private String email;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(name = "phone_number")
    private String phoneNumber;

    @Column(name = "password_hash")
    private String passwordHash;

    @Column(name = "role_code", nullable = false)
    private String roleCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private UserStatus status;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "updated_by")
    private UUID updatedBy;

    protected User() {
    }

    private User(
            UUID id,
            UUID tenantId,
            String email,
            String fullName,
            String phoneNumber,
            String passwordHash,
            String roleCode,
            UserStatus status,
            Instant lastLoginAt,
            Instant createdAt,
            UUID createdBy,
            Instant updatedAt,
            UUID updatedBy) {
        this.id = id;
        this.tenantId = tenantId;
        this.email = email;
        this.fullName = fullName;
        this.phoneNumber = phoneNumber;
        this.passwordHash = passwordHash;
        this.roleCode = roleCode;
        this.status = status;
        this.lastLoginAt = lastLoginAt;
        this.createdAt = createdAt;
        this.createdBy = createdBy;
        this.updatedAt = updatedAt;
        this.updatedBy = updatedBy;
    }

    public static User create(
            UUID id,
            UUID tenantId,
            String email,
            String fullName,
            String phoneNumber,
            String roleCode,
            UserStatus status,
            UUID actorId,
            Instant now) {
        return new User(id, tenantId, email, fullName, phoneNumber, null, roleCode, status, null, now, actorId, now, actorId);
    }

    public static User restore(
            UUID id,
            UUID tenantId,
            String email,
            String passwordHash,
            String roleCode,
            UserStatus status) {
        return new User(id, tenantId, email, email, null, passwordHash, roleCode, status, null, null, null, null, null);
    }

    public static User restore(
            UUID id,
            UUID tenantId,
            String email,
            String fullName,
            String phoneNumber,
            String passwordHash,
            String roleCode,
            UserStatus status,
            Instant lastLoginAt,
            Instant createdAt,
            UUID createdBy,
            Instant updatedAt,
            UUID updatedBy) {
        return new User(id, tenantId, email, fullName, phoneNumber, passwordHash, roleCode, status, lastLoginAt, createdAt, createdBy, updatedAt, updatedBy);
    }

    public void update(
            String email,
            String fullName,
            String phoneNumber,
            String roleCode,
            UserStatus status,
            UUID actorId,
            Instant now) {
        this.email = email;
        this.fullName = fullName;
        this.phoneNumber = phoneNumber;
        this.roleCode = roleCode;
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

    public String email() {
        return email;
    }

    public String fullName() {
        return fullName;
    }

    public String phoneNumber() {
        return phoneNumber;
    }

    public String passwordHash() {
        return passwordHash;
    }

    public String roleCode() {
        return roleCode;
    }

    public UserStatus status() {
        return status;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }

    public boolean isActive() {
        return status == UserStatus.ACTIVE;
    }
}
