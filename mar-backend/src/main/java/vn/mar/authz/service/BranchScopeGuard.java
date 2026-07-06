package vn.mar.authz.service;

import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;
import vn.mar.common.exception.BusinessException;
import vn.mar.security.context.CurrentUser;
import vn.mar.userbranch.model.UserBranchStatus;
import vn.mar.userbranch.repository.UserBranchRepository;

@Component
public class BranchScopeGuard {

    private static final String ROLE_SALES_LEAD = "SALES_LEAD";

    private final UserBranchRepository userBranchRepository;

    public BranchScopeGuard(UserBranchRepository userBranchRepository) {
        this.userBranchRepository = userBranchRepository;
    }

    public void requireAssignedBranchForSalesLead(CurrentUser actor, UUID branchId, String missingBranchMessage) {
        if (!isSalesLead(actor)) {
            return;
        }
        if (branchId == null) {
            throw BusinessException.forbidden("branch_id", "BRANCH_SCOPE_REQUIRED", missingBranchMessage);
        }
        if (!assignedBranchIds(actor).contains(branchId)) {
            throw BusinessException.forbidden("branch_id", "OUT_OF_SCOPE", "Branch is outside the actor scope");
        }
    }

    public List<UUID> resolveAssignedBranchesForSalesLead(CurrentUser actor, UUID requestedBranchId) {
        List<UUID> branchIds = assignedBranchIds(actor);
        if (requestedBranchId == null) {
            return branchIds;
        }
        if (!branchIds.contains(requestedBranchId)) {
            throw BusinessException.forbidden("branch_id", "OUT_OF_SCOPE", "Branch is outside the actor scope");
        }
        return List.of(requestedBranchId);
    }

    private boolean isSalesLead(CurrentUser actor) {
        return actor != null && ROLE_SALES_LEAD.equals(actor.roleCode());
    }

    private List<UUID> assignedBranchIds(CurrentUser actor) {
        return userBranchRepository
                .findByTenantIdAndUserIdAndStatus(actor.tenantId(), actor.actorId(), UserBranchStatus.ACTIVE)
                .stream()
                .map(userBranch -> userBranch.branchId())
                .sorted()
                .toList();
    }
}
