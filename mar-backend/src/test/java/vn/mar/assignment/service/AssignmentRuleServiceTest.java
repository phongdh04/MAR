package vn.mar.assignment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
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
import vn.mar.assignment.api.AssignmentRuleSearchCommand;
import vn.mar.assignment.dto.request.CreateAssignmentRuleRequest;
import vn.mar.assignment.mapper.AssignmentMapper;
import vn.mar.assignment.repository.AssignmentRuleAdvisorRepository;
import vn.mar.assignment.repository.AssignmentRuleRepository;
import vn.mar.assignment.repository.UnassignedAssignmentItemRepository;
import vn.mar.audit.service.AuditService;
import vn.mar.authz.model.PermissionCodes;
import vn.mar.authz.service.BranchScopeGuard;
import vn.mar.authz.service.PermissionGuard;
import vn.mar.branch.repository.BranchRepository;
import vn.mar.catalog.repository.LanguageRepository;
import vn.mar.catalog.repository.ProgramRepository;
import vn.mar.common.exception.ValidationException;
import vn.mar.common.time.TimeProvider;
import vn.mar.security.context.CurrentUser;
import vn.mar.security.context.CurrentUserContext;
import vn.mar.user.repository.UserRepository;
import vn.mar.userbranch.repository.UserBranchRepository;

@ExtendWith(MockitoExtension.class)
class AssignmentRuleServiceTest {

    private static final UUID ACTOR_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID TENANT_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final UUID ADVISOR_ID = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
    private static final Instant NOW = Instant.parse("2026-07-06T01:00:00Z");

    @Mock
    private AssignmentRuleRepository assignmentRuleRepository;

    @Mock
    private AssignmentRuleAdvisorRepository assignmentRuleAdvisorRepository;

    @Mock
    private UnassignedAssignmentItemRepository unassignedAssignmentItemRepository;

    @Mock
    private LanguageRepository languageRepository;

    @Mock
    private ProgramRepository programRepository;

    @Mock
    private BranchRepository branchRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserBranchRepository userBranchRepository;

    @Mock
    private CurrentUserContext currentUserContext;

    @Mock
    private AuditService auditService;

    private AssignmentRuleService assignmentRuleService;

    @BeforeEach
    void setUp() {
        TimeProvider timeProvider = () -> NOW;
        assignmentRuleService = new AssignmentRuleService(
                assignmentRuleRepository,
                assignmentRuleAdvisorRepository,
                unassignedAssignmentItemRepository,
                languageRepository,
                programRepository,
                branchRepository,
                userRepository,
                userBranchRepository,
                new AssignmentMapper(),
                currentUserContext,
                auditService,
                timeProvider,
                new PermissionGuard(),
                new BranchScopeGuard(userBranchRepository)
        );
    }

    @Test
    void createRule_whenAssignmentStrategyInvalid_shouldRejectWithFieldDetail() {
        allowAdmin(PermissionCodes.ASSIGNMENT_MANAGE);

        assertThatThrownBy(() -> assignmentRuleService.createRule(new CreateAssignmentRuleRequest(
                "HN English assignment",
                10,
                null,
                null,
                null,
                "FASTEST",
                "ACTIVE",
                List.of(ADVISOR_ID)
        )))
                .isInstanceOf(ValidationException.class)
                .satisfies(error -> assertThat(((ValidationException) error).getDetails())
                        .singleElement()
                        .satisfies(detail -> {
                            assertThat(detail.field()).isEqualTo("assignment_strategy");
                            assertThat(detail.code()).isEqualTo("INVALID");
                            assertThat(detail.message()).isEqualTo("Assignment strategy is invalid");
                        }));

        verifyNoInteractions(
                assignmentRuleRepository,
                assignmentRuleAdvisorRepository,
                unassignedAssignmentItemRepository,
                languageRepository,
                programRepository,
                branchRepository,
                userRepository,
                auditService
        );
    }

    @Test
    void searchRules_whenStatusInvalid_shouldRejectWithFieldDetail() {
        allowAdmin(PermissionCodes.ASSIGNMENT_VIEW);

        assertThatThrownBy(() -> assignmentRuleService.searchRules(new AssignmentRuleSearchCommand(
                "UNKNOWN",
                null,
                0,
                20
        )))
                .isInstanceOf(ValidationException.class)
                .satisfies(error -> assertThat(((ValidationException) error).getDetails())
                        .singleElement()
                        .satisfies(detail -> {
                            assertThat(detail.field()).isEqualTo("status");
                            assertThat(detail.code()).isEqualTo("INVALID");
                            assertThat(detail.message()).isEqualTo("Assignment rule status is invalid");
                        }));

        verifyNoInteractions(assignmentRuleRepository, assignmentRuleAdvisorRepository, unassignedAssignmentItemRepository);
    }

    private void allowAdmin(String... permissions) {
        when(currentUserContext.currentUser()).thenReturn(new CurrentUser(
                ACTOR_ID,
                TENANT_ID,
                "ADMIN",
                Set.of(permissions),
                "req_assignment_rule_unit_001"
        ));
    }
}
