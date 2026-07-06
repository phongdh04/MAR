package vn.mar.lead.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import vn.mar.lead.model.ConsentStatus;
import vn.mar.lead.model.Contactability;
import vn.mar.lead.model.LeadSourceType;
import vn.mar.lead.model.LeadStatus;
import vn.mar.lead.model.LeadTemperature;

@Entity
@Table(name = "leads")
public class Lead {

    @Id
    @Column(name = "lead_id", nullable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "external_id")
    private String externalId;

    @Column(name = "full_name")
    private String fullName;

    @Column(name = "phone_raw")
    private String phoneRaw;

    @Column(name = "phone_normalized")
    private String phoneNormalized;

    @Column(name = "email")
    private String email;

    @Column(name = "zalo_id")
    private String zaloId;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false)
    private LeadSourceType sourceType;

    @Column(name = "source")
    private String source;

    @Column(name = "source_created_at")
    private Instant sourceCreatedAt;

    @Column(name = "language_id")
    private UUID languageId;

    @Column(name = "program_id")
    private UUID programId;

    @Column(name = "branch_id")
    private UUID branchId;

    @Column(name = "campaign")
    private String campaign;

    @Column(name = "adset")
    private String adset;

    @Column(name = "ad")
    private String ad;

    @Column(name = "utm_source")
    private String utmSource;

    @Column(name = "utm_medium")
    private String utmMedium;

    @Column(name = "utm_campaign")
    private String utmCampaign;

    @Enumerated(EnumType.STRING)
    @Column(name = "consent_consultation")
    private ConsentStatus consentConsultation;

    @Enumerated(EnumType.STRING)
    @Column(name = "consent_marketing")
    private ConsentStatus consentMarketing;

    @Enumerated(EnumType.STRING)
    @Column(name = "contactability", nullable = false)
    private Contactability contactability;

    @Enumerated(EnumType.STRING)
    @Column(name = "lead_temperature")
    private LeadTemperature leadTemperature;

    @Column(name = "temperature_reason")
    private String temperatureReason;

    @Column(name = "import_batch_id")
    private UUID importBatchId;

    @Column(name = "integration_event_id")
    private UUID integrationEventId;

    @Enumerated(EnumType.STRING)
    @Column(name = "lead_status", nullable = false)
    private LeadStatus leadStatus;

    @Column(name = "customer_id")
    private UUID customerId;

    @Column(name = "opportunity_id")
    private UUID opportunityId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Lead() {
    }

    private Lead(
            UUID id,
            UUID tenantId,
            String externalId,
            String fullName,
            String phoneRaw,
            String phoneNormalized,
            String email,
            String zaloId,
            LeadSourceType sourceType,
            String source,
            Instant sourceCreatedAt,
            UUID languageId,
            UUID programId,
            UUID branchId,
            String campaign,
            String adset,
            String ad,
            String utmSource,
            String utmMedium,
            String utmCampaign,
            ConsentStatus consentConsultation,
            ConsentStatus consentMarketing,
            Contactability contactability,
            LeadTemperature leadTemperature,
            String temperatureReason,
            UUID importBatchId,
            UUID integrationEventId,
            LeadStatus leadStatus,
            UUID customerId,
            UUID opportunityId,
            Instant createdAt,
            Instant updatedAt) {
        this.id = id;
        this.tenantId = tenantId;
        this.externalId = externalId;
        this.fullName = fullName;
        this.phoneRaw = phoneRaw;
        this.phoneNormalized = phoneNormalized;
        this.email = email;
        this.zaloId = zaloId;
        this.sourceType = sourceType;
        this.source = source;
        this.sourceCreatedAt = sourceCreatedAt;
        this.languageId = languageId;
        this.programId = programId;
        this.branchId = branchId;
        this.campaign = campaign;
        this.adset = adset;
        this.ad = ad;
        this.utmSource = utmSource;
        this.utmMedium = utmMedium;
        this.utmCampaign = utmCampaign;
        this.consentConsultation = consentConsultation;
        this.consentMarketing = consentMarketing;
        this.contactability = contactability;
        this.leadTemperature = leadTemperature;
        this.temperatureReason = temperatureReason;
        this.importBatchId = importBatchId;
        this.integrationEventId = integrationEventId;
        this.leadStatus = leadStatus;
        this.customerId = customerId;
        this.opportunityId = opportunityId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Lead create(
            UUID id,
            UUID tenantId,
            String fullName,
            String phoneNormalized,
            String email,
            String zaloId,
            LeadSourceType sourceType,
            UUID languageId,
            UUID programId,
            UUID branchId,
            LeadTemperature leadTemperature,
            UUID customerId,
            Instant now) {
        return new Lead(
                id,
                tenantId,
                null,
                fullName,
                phoneNormalized,
                phoneNormalized,
                email,
                zaloId,
                sourceType,
                null,
                now,
                languageId,
                programId,
                branchId,
                null,
                null,
                null,
                null,
                null,
                null,
                ConsentStatus.UNKNOWN,
                ConsentStatus.UNKNOWN,
                Contactability.MEDIUM,
                leadTemperature,
                null,
                null,
                null,
                LeadStatus.VALID,
                customerId,
                null,
                now,
                now
        );
    }

    public void linkCustomerAndOpportunity(UUID customerId, UUID opportunityId, Instant now) {
        this.customerId = customerId;
        this.opportunityId = opportunityId;
        this.leadStatus = LeadStatus.LINKED;
        this.updatedAt = now;
    }

    public UUID id() {
        return id;
    }

    public UUID tenantId() {
        return tenantId;
    }

    public String source() {
        return source;
    }

    public Instant sourceCreatedAt() {
        return sourceCreatedAt;
    }

    public UUID languageId() {
        return languageId;
    }

    public UUID programId() {
        return programId;
    }

    public UUID branchId() {
        return branchId;
    }

    public String campaign() {
        return campaign;
    }

    public String adset() {
        return adset;
    }

    public String ad() {
        return ad;
    }

    public String utmSource() {
        return utmSource;
    }

    public String utmMedium() {
        return utmMedium;
    }

    public String utmCampaign() {
        return utmCampaign;
    }

    public LeadTemperature leadTemperature() {
        return leadTemperature;
    }

    public LeadSourceType sourceType() {
        return sourceType;
    }

    public UUID customerId() {
        return customerId;
    }

    public UUID opportunityId() {
        return opportunityId;
    }
}
