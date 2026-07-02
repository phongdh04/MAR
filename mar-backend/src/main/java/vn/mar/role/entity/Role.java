package vn.mar.role.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import vn.mar.role.model.RoleStatus;

@Entity
@Table(name = "roles")
public class Role {

    @Id
    @Column(name = "role_id", nullable = false)
    private UUID id;

    @Column(name = "role_code", nullable = false)
    private String roleCode;

    @Column(name = "role_name", nullable = false)
    private String roleName;

    @Column(name = "description")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private RoleStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "updated_by")
    private UUID updatedBy;

    protected Role() {
    }

    public UUID id() {
        return id;
    }

    public String roleCode() {
        return roleCode;
    }

    public String roleName() {
        return roleName;
    }

    public String description() {
        return description;
    }

    public RoleStatus status() {
        return status;
    }
}
