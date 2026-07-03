package vn.mar.customer.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import vn.mar.customer.model.PreferredContactChannel;

@Entity
@Table(name = "customer_profiles")
public class CustomerProfile {

    @Id
    @Column(name = "customer_id", nullable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "full_name")
    private String fullName;

    @Column(name = "primary_phone")
    private String primaryPhone;

    @Column(name = "primary_email")
    private String primaryEmail;

    @Column(name = "zalo_id")
    private String zaloId;

    @Column(name = "guardian_name")
    private String guardianName;

    @Column(name = "guardian_phone")
    private String guardianPhone;

    @Enumerated(EnumType.STRING)
    @Column(name = "preferred_channel")
    private PreferredContactChannel preferredChannel;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected CustomerProfile() {
    }

    private CustomerProfile(
            UUID id,
            UUID tenantId,
            String fullName,
            String primaryPhone,
            String primaryEmail,
            String zaloId,
            String guardianName,
            String guardianPhone,
            PreferredContactChannel preferredChannel,
            Instant createdAt,
            Instant updatedAt) {
        this.id = id;
        this.tenantId = tenantId;
        this.fullName = fullName;
        this.primaryPhone = primaryPhone;
        this.primaryEmail = primaryEmail;
        this.zaloId = zaloId;
        this.guardianName = guardianName;
        this.guardianPhone = guardianPhone;
        this.preferredChannel = preferredChannel;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static CustomerProfile create(
            UUID id,
            UUID tenantId,
            String fullName,
            String primaryPhone,
            String primaryEmail,
            String zaloId,
            Instant now) {
        return new CustomerProfile(
                id,
                tenantId,
                fullName,
                primaryPhone,
                primaryEmail,
                zaloId,
                null,
                null,
                null,
                now,
                now
        );
    }

    public static CustomerProfile restore(
            UUID id,
            UUID tenantId,
            String fullName,
            String primaryPhone,
            String primaryEmail,
            String zaloId,
            String guardianName,
            String guardianPhone,
            PreferredContactChannel preferredChannel,
            Instant createdAt,
            Instant updatedAt) {
        return new CustomerProfile(
                id,
                tenantId,
                fullName,
                primaryPhone,
                primaryEmail,
                zaloId,
                guardianName,
                guardianPhone,
                preferredChannel,
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

    public String fullName() {
        return fullName;
    }

    public String primaryPhone() {
        return primaryPhone;
    }

    public String primaryEmail() {
        return primaryEmail;
    }

    public String zaloId() {
        return zaloId;
    }

    public String guardianName() {
        return guardianName;
    }

    public String guardianPhone() {
        return guardianPhone;
    }

    public PreferredContactChannel preferredChannel() {
        return preferredChannel;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }
}
