package vn.mar.catalog.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import vn.mar.catalog.model.CatalogStatus;

@Entity
@Table(name = "languages")
public class Language {

    @Id
    @Column(name = "language_id", nullable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "language_code", nullable = false)
    private String languageCode;

    @Column(name = "language_name", nullable = false)
    private String languageName;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private CatalogStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "updated_by")
    private UUID updatedBy;

    protected Language() {
    }

    private Language(
            UUID id,
            UUID tenantId,
            String languageCode,
            String languageName,
            CatalogStatus status,
            Instant createdAt,
            UUID createdBy,
            Instant updatedAt,
            UUID updatedBy) {
        this.id = id;
        this.tenantId = tenantId;
        this.languageCode = languageCode;
        this.languageName = languageName;
        this.status = status;
        this.createdAt = createdAt;
        this.createdBy = createdBy;
        this.updatedAt = updatedAt;
        this.updatedBy = updatedBy;
    }

    public static Language create(
            UUID id,
            UUID tenantId,
            String languageCode,
            String languageName,
            CatalogStatus status,
            UUID actorId,
            Instant now) {
        return new Language(id, tenantId, languageCode, languageName, status, now, actorId, now, actorId);
    }

    public static Language restore(
            UUID id,
            UUID tenantId,
            String languageCode,
            String languageName,
            CatalogStatus status,
            Instant createdAt,
            UUID createdBy,
            Instant updatedAt,
            UUID updatedBy) {
        return new Language(id, tenantId, languageCode, languageName, status, createdAt, createdBy, updatedAt, updatedBy);
    }

    public void update(
            String languageCode,
            String languageName,
            CatalogStatus status,
            UUID actorId,
            Instant now) {
        this.languageCode = languageCode;
        this.languageName = languageName;
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

    public String languageCode() {
        return languageCode;
    }

    public String languageName() {
        return languageName;
    }

    public CatalogStatus status() {
        return status;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }
}
