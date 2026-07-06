package vn.mar.opportunity.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import vn.mar.lead.model.LeadTemperature;
import vn.mar.opportunity.model.LostReason;
import vn.mar.opportunity.model.OpportunityStage;
import vn.mar.opportunity.model.QualificationStatus;

@Entity
@Table(name = "admission_opportunities")
public class AdmissionOpportunity {

    @Id
    @Column(name = "opportunity_id", nullable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(name = "source_lead_id", nullable = false)
    private UUID sourceLeadId;

    @Column(name = "language_id")
    private UUID languageId;

    @Column(name = "program_id")
    private UUID programId;

    @Column(name = "course_id")
    private UUID courseId;

    @Column(name = "branch_id")
    private UUID branchId;

    @Column(name = "owner_id")
    private UUID ownerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "current_stage", nullable = false)
    private OpportunityStage currentStage;

    @Enumerated(EnumType.STRING)
    @Column(name = "qualification_status")
    private QualificationStatus qualificationStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "lead_temperature")
    private LeadTemperature leadTemperature;

    @Column(name = "sla_policy_id")
    private UUID slaPolicyId;

    @Enumerated(EnumType.STRING)
    @Column(name = "lost_reason")
    private LostReason lostReason;

    @Column(name = "lost_note")
    private String lostNote;

    @Column(name = "first_touch_id")
    private UUID firstTouchId;

    @Column(name = "last_touch_id")
    private UUID lastTouchId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected AdmissionOpportunity() {
    }

    private AdmissionOpportunity(
            UUID id,
            UUID tenantId,
            UUID customerId,
            UUID sourceLeadId,
            UUID languageId,
            UUID programId,
            UUID courseId,
            UUID branchId,
            UUID ownerId,
            OpportunityStage currentStage,
            QualificationStatus qualificationStatus,
            LeadTemperature leadTemperature,
            UUID slaPolicyId,
            LostReason lostReason,
            String lostNote,
            UUID firstTouchId,
            UUID lastTouchId,
            Instant createdAt,
            Instant updatedAt) {
        this.id = id;
        this.tenantId = tenantId;
        this.customerId = customerId;
        this.sourceLeadId = sourceLeadId;
        this.languageId = languageId;
        this.programId = programId;
        this.courseId = courseId;
        this.branchId = branchId;
        this.ownerId = ownerId;
        this.currentStage = currentStage;
        this.qualificationStatus = qualificationStatus;
        this.leadTemperature = leadTemperature;
        this.slaPolicyId = slaPolicyId;
        this.lostReason = lostReason;
        this.lostNote = lostNote;
        this.firstTouchId = firstTouchId;
        this.lastTouchId = lastTouchId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static AdmissionOpportunity create(
            UUID id,
            UUID tenantId,
            UUID customerId,
            UUID sourceLeadId,
            UUID languageId,
            UUID programId,
            UUID courseId,
            UUID branchId,
            UUID ownerId,
            LeadTemperature leadTemperature,
            Instant now) {
        return new AdmissionOpportunity(
                id,
                tenantId,
                customerId,
                sourceLeadId,
                languageId,
                programId,
                courseId,
                branchId,
                ownerId,
                OpportunityStage.NEW,
                QualificationStatus.UNKNOWN,
                leadTemperature,
                null,
                null,
                null,
                null,
                null,
                now,
                now
        );
    }

    public void updateFields(
            UUID languageId,
            UUID programId,
            UUID courseId,
            UUID branchId,
            QualificationStatus qualificationStatus,
            Instant now) {
        this.languageId = languageId;
        this.programId = programId;
        this.courseId = courseId;
        this.branchId = branchId;
        this.qualificationStatus = qualificationStatus;
        this.updatedAt = now;
    }

    public void changeStage(OpportunityStage nextStage, LostReason nextLostReason, String nextLostNote, Instant now) {
        this.currentStage = nextStage;
        if (nextStage == OpportunityStage.LOST) {
            this.lostReason = nextLostReason;
            this.lostNote = nextLostNote;
        } else {
            this.lostReason = null;
            this.lostNote = null;
        }
        if (nextStage == OpportunityStage.QUALIFIED) {
            this.qualificationStatus = QualificationStatus.QUALIFIED;
        }
        this.updatedAt = now;
    }

    public void linkTouchpoint(UUID touchpointId, Instant now) {
        if (firstTouchId == null) {
            firstTouchId = touchpointId;
        }
        lastTouchId = touchpointId;
        updatedAt = now;
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

    public UUID sourceLeadId() {
        return sourceLeadId;
    }

    public UUID languageId() {
        return languageId;
    }

    public UUID programId() {
        return programId;
    }

    public UUID courseId() {
        return courseId;
    }

    public UUID branchId() {
        return branchId;
    }

    public UUID ownerId() {
        return ownerId;
    }

    public OpportunityStage currentStage() {
        return currentStage;
    }

    public QualificationStatus qualificationStatus() {
        return qualificationStatus;
    }

    public LeadTemperature leadTemperature() {
        return leadTemperature;
    }

    public LostReason lostReason() {
        return lostReason;
    }

    public String lostNote() {
        return lostNote;
    }

    public UUID firstTouchId() {
        return firstTouchId;
    }

    public UUID lastTouchId() {
        return lastTouchId;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }
}
