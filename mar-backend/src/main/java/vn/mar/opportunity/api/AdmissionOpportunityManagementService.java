package vn.mar.opportunity.api;

import java.util.List;
import java.util.UUID;
import vn.mar.common.pagination.PageResponse;

public interface AdmissionOpportunityManagementService {

    AdmissionOpportunitySnapshot createOrLinkFromLead(CreateAdmissionOpportunityCommand command);

    PageResponse<AdmissionOpportunitySnapshot> searchOpportunities(AdmissionOpportunitySearchCommand command);

    AdmissionOpportunitySnapshot getOpportunity(UUID opportunityId);

    AdmissionOpportunitySnapshot updateOpportunity(UpdateAdmissionOpportunityCommand command);

    StageChangeSnapshot changeStage(ChangeOpportunityStageCommand command);

    List<StageHistorySnapshot> getStageHistory(UUID opportunityId);
}
