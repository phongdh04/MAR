package vn.mar.customer.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import vn.mar.customer.model.CustomerIdentityType;

@Entity
@Table(name = "customer_identities")
public class CustomerIdentity {

    @Id
    @Column(name = "identity_id", nullable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "identity_type", nullable = false)
    private CustomerIdentityType identityType;

    @Column(name = "raw_value", nullable = false)
    private String rawValue;

    @Column(name = "normalized_value", nullable = false)
    private String normalizedValue;

    @Column(name = "primary_identity", nullable = false)
    private boolean primaryIdentity;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected CustomerIdentity() {
    }

    private CustomerIdentity(
            UUID id,
            UUID tenantId,
            UUID customerId,
            CustomerIdentityType identityType,
            String rawValue,
            String normalizedValue,
            boolean primaryIdentity,
            Instant createdAt,
            Instant updatedAt) {
        this.id = id;
        this.tenantId = tenantId;
        this.customerId = customerId;
        this.identityType = identityType;
        this.rawValue = rawValue;
        this.normalizedValue = normalizedValue;
        this.primaryIdentity = primaryIdentity;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static CustomerIdentity create(
            UUID id,
            UUID tenantId,
            UUID customerId,
            CustomerIdentityType identityType,
            String rawValue,
            String normalizedValue,
            boolean primaryIdentity,
            Instant now) {
        return new CustomerIdentity(
                id,
                tenantId,
                customerId,
                identityType,
                rawValue,
                normalizedValue,
                primaryIdentity,
                now,
                now
        );
    }

    public static CustomerIdentity restore(
            UUID id,
            UUID tenantId,
            UUID customerId,
            CustomerIdentityType identityType,
            String rawValue,
            String normalizedValue,
            boolean primaryIdentity,
            Instant createdAt,
            Instant updatedAt) {
        return new CustomerIdentity(
                id,
                tenantId,
                customerId,
                identityType,
                rawValue,
                normalizedValue,
                primaryIdentity,
                createdAt,
                updatedAt
        );
    }

    public UUID id() {
        return id;
    }

    public UUID tenantId() {
        return tenantId;
    }

    public UUID customerId() {
        return customerId;
    }

    public CustomerIdentityType identityType() {
        return identityType;
    }

    public String rawValue() {
        return rawValue;
    }

    public String normalizedValue() {
        return normalizedValue;
    }

    public boolean primaryIdentity() {
        return primaryIdentity;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }
}
