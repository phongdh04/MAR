package vn.mar.tenant.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import vn.mar.tenant.model.TenantStatus;

@Entity
@Table(name = "tenants")
public class Tenant {

    @Id
    @Column(name = "tenant_id", nullable = false)
    private UUID id;

    @Column(name = "tenant_code", nullable = false)
    private String tenantCode;

    @Column(name = "tenant_name", nullable = false)
    private String tenantName;

    @Column(name = "timezone", nullable = false)
    private String timezone;

    @Column(name = "default_currency", nullable = false)
    private String defaultCurrency;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private TenantStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "updated_by")
    private UUID updatedBy;

    protected Tenant() {
    }

    private Tenant(
            UUID id,
            String tenantCode,
            String tenantName,
            String timezone,
            String defaultCurrency,
            TenantStatus status,
            Instant createdAt,
            UUID createdBy,
            Instant updatedAt,
            UUID updatedBy) {
        this.id = id;
        this.tenantCode = tenantCode;
        this.tenantName = tenantName;
        this.timezone = timezone;
        this.defaultCurrency = defaultCurrency;
        this.status = status;
        this.createdAt = createdAt;
        this.createdBy = createdBy;
        this.updatedAt = updatedAt;
        this.updatedBy = updatedBy;
    }

    public static Tenant create(
            UUID id,
            String tenantCode,
            String tenantName,
            String timezone,
            String defaultCurrency,
            TenantStatus status,
            UUID actorId,
            Instant now) {
        return new Tenant(id, tenantCode, tenantName, timezone, defaultCurrency, status, now, actorId, now, actorId);
    }

    public static Tenant restore(
            UUID id,
            String tenantCode,
            String tenantName,
            String timezone,
            String defaultCurrency,
            TenantStatus status,
            Instant createdAt,
            UUID createdBy,
            Instant updatedAt,
            UUID updatedBy) {
        return new Tenant(id, tenantCode, tenantName, timezone, defaultCurrency, status, createdAt, createdBy, updatedAt, updatedBy);
    }

    public void update(
            String tenantName,
            String timezone,
            String defaultCurrency,
            TenantStatus status,
            UUID actorId,
            Instant now) {
        this.tenantName = tenantName;
        this.timezone = timezone;
        this.defaultCurrency = defaultCurrency;
        this.status = status;
        this.updatedBy = actorId;
        this.updatedAt = now;
    }

    public UUID id() {
        return id;
    }

    public String tenantCode() {
        return tenantCode;
    }

    public String tenantName() {
        return tenantName;
    }

    public String timezone() {
        return timezone;
    }

    public String defaultCurrency() {
        return defaultCurrency;
    }

    public TenantStatus status() {
        return status;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }
}
