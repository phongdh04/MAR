package vn.mar.audit.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import vn.mar.audit.service.AuditRecordCommand;

@Entity
@Table(name = "audit_events")
public class AuditEvent {

    @Id
    @Column(name = "audit_event_id", nullable = false)
    private UUID id;

    @Column(name = "tenant_id")
    private UUID tenantId;

    @Column(name = "actor_id")
    private UUID actorId;

    @Column(name = "actor_type", nullable = false)
    private String actorType;

    @Column(name = "actor_role")
    private String actorRole;

    @Column(name = "action", nullable = false)
    private String action;

    @Column(name = "resource_type", nullable = false)
    private String resourceType;

    @Column(name = "resource_id")
    private UUID resourceId;

    @Column(name = "resource_key")
    private String resourceKey;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "before_data", columnDefinition = "jsonb")
    private Map<String, Object> beforeData;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "after_data", columnDefinition = "jsonb")
    private Map<String, Object> afterData;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "jsonb")
    private Map<String, Object> metadata;

    @Column(name = "reason")
    private String reason;

    @Column(name = "request_id")
    private String requestId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected AuditEvent() {
    }

    private AuditEvent(AuditRecordCommand command, Instant createdAt) {
        this.id = UUID.randomUUID();
        this.tenantId = command.tenantId();
        this.actorId = command.actorId();
        this.actorType = command.actorType() == null ? "USER" : command.actorType();
        this.actorRole = command.actorRole();
        this.action = command.action();
        this.resourceType = command.resourceType();
        this.resourceId = command.resourceId();
        this.resourceKey = command.resourceKey();
        this.beforeData = command.beforeData();
        this.afterData = command.afterData();
        this.metadata = command.metadata();
        this.reason = command.reason();
        this.requestId = command.requestId();
        this.createdAt = createdAt;
    }

    public static AuditEvent create(AuditRecordCommand command, Instant createdAt) {
        return new AuditEvent(command, createdAt);
    }

    public String action() {
        return action;
    }

    public UUID tenantId() {
        return tenantId;
    }

    public UUID actorId() {
        return actorId;
    }

    public UUID resourceId() {
        return resourceId;
    }

    public String reason() {
        return reason;
    }
}
