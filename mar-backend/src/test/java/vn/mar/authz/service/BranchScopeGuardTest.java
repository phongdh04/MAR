package vn.mar.authz.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.mar.common.error.ErrorCode;
import vn.mar.common.exception.BusinessException;
import vn.mar.security.context.CurrentUser;
import vn.mar.userbranch.entity.UserBranch;
import vn.mar.userbranch.model.UserBranchStatus;
import vn.mar.userbranch.repository.UserBranchRepository;

@ExtendWith(MockitoExtension.class)
class BranchScopeGuardTest {

    private static final UUID ACTOR_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID TENANT_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final UUID BRANCH_ID = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
    private static final UUID OTHER_BRANCH_ID = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");
    private static final Instant NOW = Instant.parse("2026-07-06T01:00:00Z");

    @Mock
    private UserBranchRepository userBranchRepository;

    private BranchScopeGuard branchScopeGuard;

    @BeforeEach
    void setUp() {
        branchScopeGuard = new BranchScopeGuard(userBranchRepository);
    }

    @Test
    void requireAssignedBranchForSalesLead_whenActorIsNotSalesLead_shouldAllowWithoutLookup() {
        branchScopeGuard.requireAssignedBranchForSalesLead(
                actor("ADMIN"),
                null,
                "Sales Lead must specify a branch"
        );

        verify(userBranchRepository, never()).findByTenantIdAndUserIdAndStatus(TENANT_ID, ACTOR_ID, UserBranchStatus.ACTIVE);
    }

    @Test
    void requireAssignedBranchForSalesLead_whenBranchIsAssigned_shouldAllow() {
        when(userBranchRepository.findByTenantIdAndUserIdAndStatus(TENANT_ID, ACTOR_ID, UserBranchStatus.ACTIVE))
                .thenReturn(List.of(userBranch(BRANCH_ID)));

        branchScopeGuard.requireAssignedBranchForSalesLead(
                actor("SALES_LEAD"),
                BRANCH_ID,
                "Sales Lead must specify a branch"
        );
    }

    @Test
    void requireAssignedBranchForSalesLead_whenBranchMissing_shouldReject() {
        assertThatThrownBy(() -> branchScopeGuard.requireAssignedBranchForSalesLead(
                actor("SALES_LEAD"),
                null,
                "Sales Lead must specify a branch"
        ))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.PERMISSION_DENIED);
    }

    @Test
    void resolveAssignedBranchesForSalesLead_whenRequestedBranchIsOutsideScope_shouldReject() {
        when(userBranchRepository.findByTenantIdAndUserIdAndStatus(TENANT_ID, ACTOR_ID, UserBranchStatus.ACTIVE))
                .thenReturn(List.of(userBranch(BRANCH_ID)));

        assertThatThrownBy(() -> branchScopeGuard.resolveAssignedBranchesForSalesLead(actor("SALES_LEAD"), OTHER_BRANCH_ID))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.PERMISSION_DENIED);
    }

    @Test
    void resolveAssignedBranchesForSalesLead_whenNoRequestedBranch_shouldReturnAssignedBranchesSorted() {
        when(userBranchRepository.findByTenantIdAndUserIdAndStatus(TENANT_ID, ACTOR_ID, UserBranchStatus.ACTIVE))
                .thenReturn(List.of(userBranch(OTHER_BRANCH_ID), userBranch(BRANCH_ID)));

        List<UUID> branchIds = branchScopeGuard.resolveAssignedBranchesForSalesLead(actor("SALES_LEAD"), null);

        assertThat(branchIds).containsExactly(BRANCH_ID, OTHER_BRANCH_ID);
    }

    private CurrentUser actor(String roleCode) {
        return new CurrentUser(ACTOR_ID, TENANT_ID, roleCode, Set.of(), "req_branch_scope_guard_unit");
    }

    private UserBranch userBranch(UUID branchId) {
        return UserBranch.create(UUID.randomUUID(), TENANT_ID, ACTOR_ID, branchId, ACTOR_ID, NOW);
    }
}
