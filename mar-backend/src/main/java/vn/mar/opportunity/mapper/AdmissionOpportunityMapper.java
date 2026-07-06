package vn.mar.opportunity.mapper;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;
import vn.mar.opportunity.api.AdmissionOpportunitySnapshot;
import vn.mar.opportunity.api.StageChangeSnapshot;
import vn.mar.opportunity.dto.response.OpportunityResponse;
import vn.mar.opportunity.dto.response.StageChangeResponse;
import vn.mar.opportunity.entity.AdmissionOpportunity;

@Component
public class AdmissionOpportunityMapper {

    public AdmissionOpportunitySnapshot toSnapshot(AdmissionOpportunity opportunity) {
        return new AdmissionOpportunitySnapshot(
                opportunity.id(),
                opportunity.tenantId(),
                opportunity.customerId(),
                opportunity.sourceLeadId(),
                opportunity.languageId(),
                opportunity.programId(),
                opportunity.courseId(),
                opportunity.branchId(),
                opportunity.ownerId(),
                opportunity.currentStage().name(),
                opportunity.qualificationStatus() == null ? null : opportunity.qualificationStatus().name(),
                opportunity.leadTemperature() == null ? null : opportunity.leadTemperature().name(),
                opportunity.lostReason() == null ? null : opportunity.lostReason().name(),
                opportunity.lostNote(),
                opportunity.firstTouchId(),
                opportunity.lastTouchId(),
                opportunity.createdAt(),
                opportunity.updatedAt()
        );
    }

    public OpportunityResponse toResponse(AdmissionOpportunitySnapshot snapshot) {
        return new OpportunityResponse(
                snapshot.opportunityId(),
                snapshot.tenantId(),
                snapshot.customerId(),
                snapshot.sourceLeadId(),
                snapshot.languageId(),
                snapshot.programId(),
                snapshot.courseId(),
                snapshot.branchId(),
                snapshot.ownerId(),
                snapshot.currentStage(),
                snapshot.qualificationStatus(),
                snapshot.leadTemperature(),
                snapshot.lostReason(),
                snapshot.lostNote(),
                snapshot.firstTouchId(),
                snapshot.lastTouchId(),
                snapshot.createdAt(),
                snapshot.updatedAt()
        );
    }

    public StageChangeResponse toResponse(StageChangeSnapshot snapshot) {
        return new StageChangeResponse(
                snapshot.opportunityId(),
                snapshot.fromStage(),
                snapshot.toStage(),
                snapshot.stageHistoryId(),
                snapshot.changedAt()
        );
    }

    public Map<String, Object> toAuditData(AdmissionOpportunity opportunity) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("opportunity_id", opportunity.id().toString());
        data.put("tenant_id", opportunity.tenantId().toString());
        data.put("customer_id", opportunity.customerId().toString());
        data.put("source_lead_id", opportunity.sourceLeadId().toString());
        data.put("language_id", opportunity.languageId() == null ? null : opportunity.languageId().toString());
        data.put("program_id", opportunity.programId() == null ? null : opportunity.programId().toString());
        data.put("course_id", opportunity.courseId() == null ? null : opportunity.courseId().toString());
        data.put("branch_id", opportunity.branchId() == null ? null : opportunity.branchId().toString());
        data.put("owner_id", opportunity.ownerId() == null ? null : opportunity.ownerId().toString());
        data.put("current_stage", opportunity.currentStage().name());
        data.put("qualification_status", opportunity.qualificationStatus() == null ? null : opportunity.qualificationStatus().name());
        data.put("lead_temperature", opportunity.leadTemperature() == null ? null : opportunity.leadTemperature().name());
        data.put("lost_reason", opportunity.lostReason() == null ? null : opportunity.lostReason().name());
        data.put("lost_note", opportunity.lostNote());
        data.put("first_touch_id", opportunity.firstTouchId() == null ? null : opportunity.firstTouchId().toString());
        data.put("last_touch_id", opportunity.lastTouchId() == null ? null : opportunity.lastTouchId().toString());
        return data;
    }
}
