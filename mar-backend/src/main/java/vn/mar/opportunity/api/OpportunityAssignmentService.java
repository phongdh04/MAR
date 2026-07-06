package vn.mar.opportunity.api;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

public interface OpportunityAssignmentService {

    OpportunityAssignmentSnapshot getAssignmentSnapshot(UUID tenantId, UUID opportunityId);

    OpportunityAssignmentSnapshot assignOwner(AssignOpportunityOwnerCommand command);

    Map<UUID, Long> countActiveOwnedOpportunities(UUID tenantId, Collection<UUID> ownerIds);
}
