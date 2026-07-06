package vn.mar.opportunity.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import vn.mar.opportunity.model.TouchpointType;

@Entity
@Table(name = "touchpoints")
public class Touchpoint {

    @Id
    @Column(name = "touchpoint_id", nullable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(name = "lead_id", nullable = false)
    private UUID leadId;

    @Column(name = "opportunity_id")
    private UUID opportunityId;

    @Column(name = "source")
    private String source;

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

    @Column(name = "touch_time", nullable = false)
    private Instant touchTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "touch_type", nullable = false)
    private TouchpointType touchType;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected Touchpoint() {
    }

    private Touchpoint(
            UUID id,
            UUID tenantId,
            UUID customerId,
            UUID leadId,
            UUID opportunityId,
            String source,
            String campaign,
            String adset,
            String ad,
            String utmSource,
            String utmMedium,
            String utmCampaign,
            Instant touchTime,
            TouchpointType touchType,
            Instant createdAt) {
        this.id = id;
        this.tenantId = tenantId;
        this.customerId = customerId;
        this.leadId = leadId;
        this.opportunityId = opportunityId;
        this.source = source;
        this.campaign = campaign;
        this.adset = adset;
        this.ad = ad;
        this.utmSource = utmSource;
        this.utmMedium = utmMedium;
        this.utmCampaign = utmCampaign;
        this.touchTime = touchTime;
        this.touchType = touchType;
        this.createdAt = createdAt;
    }

    public static Touchpoint create(
            UUID id,
            UUID tenantId,
            UUID customerId,
            UUID leadId,
            UUID opportunityId,
            String source,
            String campaign,
            String adset,
            String ad,
            String utmSource,
            String utmMedium,
            String utmCampaign,
            Instant touchTime,
            TouchpointType touchType,
            Instant now) {
        return new Touchpoint(
                id,
                tenantId,
                customerId,
                leadId,
                opportunityId,
                source,
                campaign,
                adset,
                ad,
                utmSource,
                utmMedium,
                utmCampaign,
                touchTime,
                touchType,
                now
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

    public UUID leadId() {
        return leadId;
    }

    public UUID opportunityId() {
        return opportunityId;
    }

    public TouchpointType touchType() {
        return touchType;
    }

    public Instant touchTime() {
        return touchTime;
    }
}
