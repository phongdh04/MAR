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
@Table(name = "programs")
public class Program {

    @Id
    @Column(name = "program_id", nullable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "language_id", nullable = false)
    private UUID languageId;

    @Column(name = "program_code", nullable = false)
    private String programCode;

    @Column(name = "program_name", nullable = false)
    private String programName;

    @Column(name = "exam_track")
    private String examTrack;

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

    protected Program() {
    }

    private Program(
            UUID id,
            UUID tenantId,
            UUID languageId,
            String programCode,
            String programName,
            String examTrack,
            CatalogStatus status,
            Instant createdAt,
            UUID createdBy,
            Instant updatedAt,
            UUID updatedBy) {
        this.id = id;
        this.tenantId = tenantId;
        this.languageId = languageId;
        this.programCode = programCode;
        this.programName = programName;
        this.examTrack = examTrack;
        this.status = status;
        this.createdAt = createdAt;
        this.createdBy = createdBy;
        this.updatedAt = updatedAt;
        this.updatedBy = updatedBy;
    }

    public static Program create(
            UUID id,
            UUID tenantId,
            UUID languageId,
            String programCode,
            String programName,
            String examTrack,
            CatalogStatus status,
            UUID actorId,
            Instant now) {
        return new Program(id, tenantId, languageId, programCode, programName, examTrack, status, now, actorId, now, actorId);
    }

    public static Program restore(
            UUID id,
            UUID tenantId,
            UUID languageId,
            String programCode,
            String programName,
            String examTrack,
            CatalogStatus status,
            Instant createdAt,
            UUID createdBy,
            Instant updatedAt,
            UUID updatedBy) {
        return new Program(id, tenantId, languageId, programCode, programName, examTrack, status, createdAt, createdBy, updatedAt, updatedBy);
    }

    public void update(
            UUID languageId,
            String programCode,
            String programName,
            String examTrack,
            CatalogStatus status,
            UUID actorId,
            Instant now) {
        this.languageId = languageId;
        this.programCode = programCode;
        this.programName = programName;
        this.examTrack = examTrack;
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

    public UUID languageId() {
        return languageId;
    }

    public String programCode() {
        return programCode;
    }

    public String programName() {
        return programName;
    }

    public String examTrack() {
        return examTrack;
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
