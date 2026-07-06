package vn.mar.integration.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import vn.mar.integration.model.IntegrationEventStatus;
import vn.mar.lead.model.LeadSourceType;

@Entity
@Table(name = "integration_events")
public class IntegrationEvent {

    @Id
    @Column(name = "integration_event_id", nullable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false)
    private LeadSourceType sourceType;

    @Column(name = "external_id")
    private String externalId;

    @Column(name = "idempotency_key")
    private String idempotencyKey;

    @Column(name = "payload_hash", nullable = false)
    private String payloadHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private IntegrationEventStatus status;

    @Column(name = "error_code")
    private String errorCode;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "raw_payload_uri")
    private String rawPayloadUri;

    @Column(name = "created_lead_id")
    private UUID createdLeadId;

    @Column(name = "created_customer_id")
    private UUID createdCustomerId;

    @Column(name = "created_opportunity_id")
    private UUID createdOpportunityId;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;

    @Column(name = "processed_at")
    private Instant processedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected IntegrationEvent() {
    }

    private IntegrationEvent(
            UUID id,
            UUID tenantId,
            LeadSourceType sourceType,
            String externalId,
            String idempotencyKey,
            String payloadHash,
            IntegrationEventStatus status,
            String errorCode,
            String errorMessage,
            String rawPayloadUri,
            UUID createdLeadId,
            UUID createdCustomerId,
            UUID createdOpportunityId,
            Instant receivedAt,
            Instant processedAt,
            Instant createdAt,
            Instant updatedAt) {
        this.id = id;
        this.tenantId = tenantId;
        this.sourceType = sourceType;
        this.externalId = externalId;
        this.idempotencyKey = idempotencyKey;
        this.payloadHash = payloadHash;
        this.status = status;
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
        this.rawPayloadUri = rawPayloadUri;
        this.createdLeadId = createdLeadId;
        this.createdCustomerId = createdCustomerId;
        this.createdOpportunityId = createdOpportunityId;
        this.receivedAt = receivedAt;
        this.processedAt = processedAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static IntegrationEvent received(
            UUID id,
            UUID tenantId,
            LeadSourceType sourceType,
            String externalId,
            String idempotencyKey,
            String payloadHash,
            Instant now) {
        return new IntegrationEvent(
                id,
                tenantId,
                sourceType,
                externalId,
                idempotencyKey,
                payloadHash,
                IntegrationEventStatus.RECEIVED,
                null,
                null,
                null,
                null,
                null,
                null,
                now,
                null,
                now,
                now
        );
    }

    public static IntegrationEvent duplicate(
            UUID id,
            UUID tenantId,
            LeadSourceType sourceType,
            String externalId,
            String idempotencyKey,
            String payloadHash,
            Instant now) {
        return new IntegrationEvent(
                id,
                tenantId,
                sourceType,
                externalId,
                idempotencyKey,
                payloadHash,
                IntegrationEventStatus.DUPLICATE,
                "WEBHOOK_DUPLICATE_IGNORED",
                "Duplicate webhook was ignored by idempotency store",
                null,
                null,
                null,
                null,
                now,
                now,
                now,
                now
        );
    }

    public UUID id() {
        return id;
    }

    public UUID tenantId() {
        return tenantId;
    }

    public LeadSourceType sourceType() {
        return sourceType;
    }

    public String externalId() {
        return externalId;
    }

    public String idempotencyKey() {
        return idempotencyKey;
    }

    public String payloadHash() {
        return payloadHash;
    }

    public IntegrationEventStatus status() {
        return status;
    }

    public String errorCode() {
        return errorCode;
    }

    public String errorMessage() {
        return errorMessage;
    }

    public String rawPayloadUri() {
        return rawPayloadUri;
    }

    public UUID createdLeadId() {
        return createdLeadId;
    }

    public UUID createdCustomerId() {
        return createdCustomerId;
    }

    public UUID createdOpportunityId() {
        return createdOpportunityId;
    }

    public Instant receivedAt() {
        return receivedAt;
    }

    public Instant processedAt() {
        return processedAt;
    }
}
