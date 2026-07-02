package vn.mar.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.mar.audit.model.AuditActions;
import vn.mar.audit.service.AuditRecordCommand;
import vn.mar.audit.service.AuditService;
import vn.mar.branch.entity.Branch;
import vn.mar.branch.model.BranchStatus;
import vn.mar.branch.repository.BranchRepository;
import vn.mar.common.error.ErrorCode;
import vn.mar.common.exception.BusinessException;
import vn.mar.common.exception.ResourceNotFoundException;
import vn.mar.common.time.TimeProvider;
import vn.mar.role.model.RoleStatus;
import vn.mar.role.repository.RoleRepository;
import vn.mar.security.context.CurrentUser;
import vn.mar.security.context.CurrentUserContext;
import vn.mar.user.dto.request.CreateUserRequest;
import vn.mar.user.dto.request.UpdateUserRequest;
import vn.mar.user.dto.response.UserDetailResponse;
import vn.mar.user.entity.User;
import vn.mar.user.mapper.UserMapper;
import vn.mar.user.model.UserStatus;
import vn.mar.user.repository.UserRepository;
import vn.mar.userbranch.entity.UserBranch;
import vn.mar.userbranch.model.UserBranchStatus;
import vn.mar.userbranch.repository.UserBranchRepository;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    private static final UUID ACTOR_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID TENANT_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final UUID USER_ID = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
    private static final UUID BRANCH_ID = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");
    private static final UUID USER_BRANCH_ID = UUID.fromString("eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee");
    private static final Instant NOW = Instant.parse("2026-07-01T08:00:00Z");

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserBranchRepository userBranchRepository;

    @Mock
    private BranchRepository branchRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private CurrentUserContext currentUserContext;

    @Mock
    private AuditService auditService;

    private UserService userService;

    @BeforeEach
    void setUp() {
        TimeProvider timeProvider = () -> NOW;
        userService = new UserService(
                userRepository,
                userBranchRepository,
                branchRepository,
                roleRepository,
                new UserMapper(),
                timeProvider,
                currentUserContext,
                auditService
        );
        when(currentUserContext.currentUser()).thenReturn(new CurrentUser(
                ACTOR_ID,
                TENANT_ID,
                "ADMIN",
                Set.of("user.manage"),
                "req_user_unit_001"
        ));
    }

    @Test
    void createUser_whenValidWithBranch_shouldSaveAssignmentAndAuditEvents() {
        when(roleRepository.existsByRoleCodeAndStatus("ADVISOR", RoleStatus.ACTIVE)).thenReturn(true);
        when(branchRepository.findByTenantIdAndIdIn(eq(TENANT_ID), anyCollection()))
                .thenReturn(List.of(activeBranch()));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userBranchRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        UserDetailResponse response = userService.createUser(new CreateUserRequest(
                "An Advisor",
                "Advisor@MAR.vn",
                "0900000001",
                "advisor",
                Set.of(BRANCH_ID),
                null
        ));

        assertThat(response.fullName()).isEqualTo("An Advisor");
        assertThat(response.email()).isEqualTo("advisor@mar.vn");
        assertThat(response.role()).isEqualTo("ADVISOR");
        assertThat(response.status()).isEqualTo("ACTIVE");
        assertThat(response.branchIds()).containsExactly(BRANCH_ID);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().tenantId()).isEqualTo(TENANT_ID);
        assertThat(userCaptor.getValue().passwordHash()).isNull();

        ArgumentCaptor<AuditRecordCommand> auditCaptor = ArgumentCaptor.forClass(AuditRecordCommand.class);
        verify(auditService, times(2)).record(auditCaptor.capture());
        assertThat(auditCaptor.getAllValues())
                .extracting(AuditRecordCommand::action)
                .containsExactly(AuditActions.USER_CREATED, AuditActions.USER_BRANCH_ASSIGNED);
    }

    @Test
    void createUser_whenEmailDuplicated_shouldRejectWithConflict() {
        when(userRepository.existsByTenantIdAndEmailIgnoreCase(TENANT_ID, "advisor@mar.vn")).thenReturn(true);

        assertThatThrownBy(() -> userService.createUser(new CreateUserRequest(
                "An Advisor",
                "advisor@mar.vn",
                null,
                "ADVISOR",
                Set.of(),
                "ACTIVE"
        )))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.DUPLICATE_USER_EMAIL);

        verify(roleRepository, never()).existsByRoleCodeAndStatus(any(), any());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void createUser_whenInactiveWithBranchAssignment_shouldRejectBusinessRule() {
        when(roleRepository.existsByRoleCodeAndStatus("ADVISOR", RoleStatus.ACTIVE)).thenReturn(true);

        assertThatThrownBy(() -> userService.createUser(new CreateUserRequest(
                "An Advisor",
                "advisor@mar.vn",
                null,
                "ADVISOR",
                Set.of(BRANCH_ID),
                "INACTIVE"
        )))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.BUSINESS_RULE_VIOLATION);

        verify(branchRepository, never()).findByTenantIdAndIdIn(eq(TENANT_ID), anyCollection());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void createUser_whenBranchNotInTenant_shouldUseTenantScopedLookupAndReturnNotFound() {
        when(roleRepository.existsByRoleCodeAndStatus("ADVISOR", RoleStatus.ACTIVE)).thenReturn(true);
        when(branchRepository.findByTenantIdAndIdIn(eq(TENANT_ID), anyCollection())).thenReturn(List.of());

        assertThatThrownBy(() -> userService.createUser(new CreateUserRequest(
                "An Advisor",
                "advisor@mar.vn",
                null,
                "ADVISOR",
                Set.of(BRANCH_ID),
                "ACTIVE"
        )))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(branchRepository).findByTenantIdAndIdIn(eq(TENANT_ID), anyCollection());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void updateUser_whenStatusInactive_shouldInactivateAssignmentsAndAuditChanges() {
        User activeUser = activeUser();
        UserBranch activeAssignment = activeAssignment();
        when(userRepository.findByIdAndTenantId(USER_ID, TENANT_ID)).thenReturn(Optional.of(activeUser));
        when(userBranchRepository.findByTenantIdAndUserIdAndStatus(TENANT_ID, USER_ID, UserBranchStatus.ACTIVE))
                .thenReturn(List.of(activeAssignment));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userBranchRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        UserDetailResponse response = userService.updateUser(USER_ID, new UpdateUserRequest(
                null,
                null,
                null,
                null,
                null,
                "INACTIVE",
                "Advisor left"
        ));

        assertThat(response.status()).isEqualTo("INACTIVE");
        assertThat(response.branchIds()).isEmpty();
        assertThat(activeAssignment.status()).isEqualTo(UserBranchStatus.INACTIVE);

        ArgumentCaptor<AuditRecordCommand> auditCaptor = ArgumentCaptor.forClass(AuditRecordCommand.class);
        verify(auditService, times(2)).record(auditCaptor.capture());
        assertThat(auditCaptor.getAllValues())
                .extracting(AuditRecordCommand::action)
                .containsExactly(AuditActions.USER_STATUS_CHANGED, AuditActions.USER_BRANCH_ASSIGNED);
        assertThat(auditCaptor.getAllValues())
                .extracting(AuditRecordCommand::reason)
                .containsOnly("Advisor left");
    }

    @Test
    void getUser_whenUserBelongsToAnotherTenant_shouldThrowNotFound() {
        when(userRepository.findByIdAndTenantId(USER_ID, TENANT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUser(USER_ID))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(userRepository).findByIdAndTenantId(USER_ID, TENANT_ID);
        verify(userRepository, never()).findById(USER_ID);
    }

    private User activeUser() {
        return User.restore(
                USER_ID,
                TENANT_ID,
                "advisor@mar.vn",
                "An Advisor",
                "0900000001",
                null,
                "ADVISOR",
                UserStatus.ACTIVE,
                null,
                NOW,
                ACTOR_ID,
                NOW,
                ACTOR_ID
        );
    }

    private Branch activeBranch() {
        return Branch.restore(
                BRANCH_ID,
                TENANT_ID,
                "HN01",
                "Ha Noi - Cau Giay",
                "Ha Noi",
                null,
                "Cau Giay",
                BranchStatus.ACTIVE,
                NOW,
                ACTOR_ID,
                NOW,
                ACTOR_ID
        );
    }

    private UserBranch activeAssignment() {
        return UserBranch.restore(
                USER_BRANCH_ID,
                TENANT_ID,
                USER_ID,
                BRANCH_ID,
                UserBranchStatus.ACTIVE,
                NOW,
                ACTOR_ID,
                NOW,
                ACTOR_ID
        );
    }
}
