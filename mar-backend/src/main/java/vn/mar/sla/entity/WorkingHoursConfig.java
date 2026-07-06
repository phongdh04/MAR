package vn.mar.sla.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.util.UUID;
import vn.mar.sla.model.WorkingHoursConfigStatus;

@Entity
@Table(name = "working_hours_configs")
public class WorkingHoursConfig {

    @Id
    @Column(name = "working_hours_id", nullable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "branch_id")
    private UUID branchId;

    @Enumerated(EnumType.STRING)
    @Column(name = "weekday", nullable = false)
    private DayOfWeek weekday;

    @Column(name = "start_time")
    private LocalTime startTime;

    @Column(name = "end_time")
    private LocalTime endTime;

    @Column(name = "timezone", nullable = false)
    private String timezone;

    @Column(name = "is_working_day", nullable = false)
    private boolean workingDay;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private WorkingHoursConfigStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "updated_by")
    private UUID updatedBy;

    protected WorkingHoursConfig() {
    }

    private WorkingHoursConfig(
            UUID id,
            UUID tenantId,
            UUID branchId,
            DayOfWeek weekday,
            LocalTime startTime,
            LocalTime endTime,
            String timezone,
            boolean workingDay,
            WorkingHoursConfigStatus status,
            Instant createdAt,
            UUID createdBy,
            Instant updatedAt,
            UUID updatedBy) {
        this.id = id;
        this.tenantId = tenantId;
        this.branchId = branchId;
        this.weekday = weekday;
        this.startTime = startTime;
        this.endTime = endTime;
        this.timezone = timezone;
        this.workingDay = workingDay;
        this.status = status;
        this.createdAt = createdAt;
        this.createdBy = createdBy;
        this.updatedAt = updatedAt;
        this.updatedBy = updatedBy;
    }

    public static WorkingHoursConfig create(
            UUID id,
            UUID tenantId,
            UUID branchId,
            DayOfWeek weekday,
            LocalTime startTime,
            LocalTime endTime,
            String timezone,
            boolean workingDay,
            UUID actorId,
            Instant now) {
        return new WorkingHoursConfig(
                id,
                tenantId,
                branchId,
                weekday,
                startTime,
                endTime,
                timezone,
                workingDay,
                WorkingHoursConfigStatus.ACTIVE,
                now,
                actorId,
                now,
                actorId
        );
    }

    public static WorkingHoursConfig restore(
            UUID id,
            UUID tenantId,
            UUID branchId,
            DayOfWeek weekday,
            LocalTime startTime,
            LocalTime endTime,
            String timezone,
            boolean workingDay,
            WorkingHoursConfigStatus status,
            Instant createdAt,
            UUID createdBy,
            Instant updatedAt,
            UUID updatedBy) {
        return new WorkingHoursConfig(
                id,
                tenantId,
                branchId,
                weekday,
                startTime,
                endTime,
                timezone,
                workingDay,
                status,
                createdAt,
                createdBy,
                updatedAt,
                updatedBy
        );
    }

    public void update(LocalTime startTime, LocalTime endTime, String timezone, boolean workingDay, UUID actorId, Instant now) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.timezone = timezone;
        this.workingDay = workingDay;
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

    public DayOfWeek weekday() {
        return weekday;
    }

    public LocalTime startTime() {
        return startTime;
    }

    public LocalTime endTime() {
        return endTime;
    }

    public String timezone() {
        return timezone;
    }

    public boolean workingDay() {
        return workingDay;
    }

    public WorkingHoursConfigStatus status() {
        return status;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }
}
