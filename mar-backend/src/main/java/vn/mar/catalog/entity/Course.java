package vn.mar.catalog.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import vn.mar.catalog.model.CatalogStatus;

@Entity
@Table(name = "courses")
public class Course {

    @Id
    @Column(name = "course_id", nullable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "program_id", nullable = false)
    private UUID programId;

    @Column(name = "course_code", nullable = false)
    private String courseCode;

    @Column(name = "course_name", nullable = false)
    private String courseName;

    @Column(name = "level")
    private String level;

    @Column(name = "tuition_amount", nullable = false)
    private BigDecimal tuitionAmount;

    @Column(name = "currency", nullable = false)
    private String currency;

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

    protected Course() {
    }

    private Course(
            UUID id,
            UUID tenantId,
            UUID programId,
            String courseCode,
            String courseName,
            String level,
            BigDecimal tuitionAmount,
            String currency,
            CatalogStatus status,
            Instant createdAt,
            UUID createdBy,
            Instant updatedAt,
            UUID updatedBy) {
        this.id = id;
        this.tenantId = tenantId;
        this.programId = programId;
        this.courseCode = courseCode;
        this.courseName = courseName;
        this.level = level;
        this.tuitionAmount = tuitionAmount;
        this.currency = currency;
        this.status = status;
        this.createdAt = createdAt;
        this.createdBy = createdBy;
        this.updatedAt = updatedAt;
        this.updatedBy = updatedBy;
    }

    public static Course create(
            UUID id,
            UUID tenantId,
            UUID programId,
            String courseCode,
            String courseName,
            String level,
            BigDecimal tuitionAmount,
            String currency,
            CatalogStatus status,
            UUID actorId,
            Instant now) {
        return new Course(id, tenantId, programId, courseCode, courseName, level, tuitionAmount, currency, status, now, actorId, now, actorId);
    }

    public static Course restore(
            UUID id,
            UUID tenantId,
            UUID programId,
            String courseCode,
            String courseName,
            String level,
            BigDecimal tuitionAmount,
            String currency,
            CatalogStatus status,
            Instant createdAt,
            UUID createdBy,
            Instant updatedAt,
            UUID updatedBy) {
        return new Course(id, tenantId, programId, courseCode, courseName, level, tuitionAmount, currency, status, createdAt, createdBy, updatedAt, updatedBy);
    }

    public void update(
            UUID programId,
            String courseCode,
            String courseName,
            String level,
            BigDecimal tuitionAmount,
            String currency,
            CatalogStatus status,
            UUID actorId,
            Instant now) {
        this.programId = programId;
        this.courseCode = courseCode;
        this.courseName = courseName;
        this.level = level;
        this.tuitionAmount = tuitionAmount;
        this.currency = currency;
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

    public UUID programId() {
        return programId;
    }

    public String courseCode() {
        return courseCode;
    }

    public String courseName() {
        return courseName;
    }

    public String level() {
        return level;
    }

    public BigDecimal tuitionAmount() {
        return tuitionAmount;
    }

    public String currency() {
        return currency;
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
