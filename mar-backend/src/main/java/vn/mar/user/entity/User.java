package vn.mar.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import vn.mar.user.model.UserStatus;

@Entity
@Table(name = "users")
public class User {

    @Id
    @Column(name = "user_id", nullable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "email", nullable = false)
    private String email;

    @Column(name = "password_hash")
    private String passwordHash;

    @Column(name = "role_code", nullable = false)
    private String roleCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private UserStatus status;

    protected User() {
    }

    private User(
            UUID id,
            UUID tenantId,
            String email,
            String passwordHash,
            String roleCode,
            UserStatus status) {
        this.id = id;
        this.tenantId = tenantId;
        this.email = email;
        this.passwordHash = passwordHash;
        this.roleCode = roleCode;
        this.status = status;
    }

    public static User restore(
            UUID id,
            UUID tenantId,
            String email,
            String passwordHash,
            String roleCode,
            UserStatus status) {
        return new User(id, tenantId, email, passwordHash, roleCode, status);
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

    public String passwordHash() {
        return passwordHash;
    }

    public String roleCode() {
        return roleCode;
    }

    public UserStatus status() {
        return status;
    }

    public boolean isActive() {
        return status == UserStatus.ACTIVE;
    }
}
