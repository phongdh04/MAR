package vn.mar.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import vn.mar.MarApplication;
import vn.mar.audit.entity.AuditEvent;
import vn.mar.audit.model.AuditActions;
import vn.mar.audit.repository.AuditEventRepository;
import vn.mar.authz.repository.PermissionProfileRepository;
import vn.mar.branch.entity.Branch;
import vn.mar.branch.model.BranchStatus;
import vn.mar.branch.repository.BranchRepository;
import vn.mar.catalog.repository.CourseRepository;
import vn.mar.catalog.repository.LanguageRepository;
import vn.mar.catalog.repository.ProgramRepository;
import vn.mar.common.cache.CacheEvictionService;
import vn.mar.common.logging.RequestIdFilter;
import vn.mar.customer.repository.CustomerIdentityRepository;
import vn.mar.customer.repository.CustomerProfileRepository;
import vn.mar.customer.repository.DuplicateCaseRepository;
import vn.mar.customer.repository.MergeHistoryRepository;
import vn.mar.lead.repository.LeadRepository;
import vn.mar.leadimport.repository.ImportBatchRepository;
import vn.mar.leadimport.repository.ImportRowRepository;
import vn.mar.opportunity.repository.AdmissionOpportunityRepository;
import vn.mar.opportunity.repository.TouchpointRepository;
import vn.mar.role.model.RoleStatus;
import vn.mar.role.repository.RoleRepository;
import vn.mar.security.jwt.JwtTokenProvider;
import vn.mar.tenant.repository.TenantRepository;
import vn.mar.user.entity.User;
import vn.mar.user.model.UserStatus;
import vn.mar.user.repository.UserRepository;
import vn.mar.userbranch.entity.UserBranch;
import vn.mar.userbranch.model.UserBranchStatus;
import vn.mar.userbranch.repository.UserBranchRepository;

@SpringBootTest(classes = MarApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserApiSmokeTest {

    private static final UUID ACTOR_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID TENANT_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final UUID USER_ID = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
    private static final UUID BRANCH_ID = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");
    private static final UUID USER_BRANCH_ID = UUID.fromString("eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee");
    private static final Instant NOW = Instant.parse("2026-07-01T08:00:00Z");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private CacheEvictionService cacheEvictionService;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private UserBranchRepository userBranchRepository;

    @MockitoBean
    private RoleRepository roleRepository;

    @MockitoBean
    private PermissionProfileRepository permissionProfileRepository;

    @MockitoBean
    private TenantRepository tenantRepository;

    @MockitoBean
    private BranchRepository branchRepository;

    @MockitoBean
    private LanguageRepository languageRepository;

    @MockitoBean
    private ProgramRepository programRepository;

    @MockitoBean
    private CourseRepository courseRepository;

    @MockitoBean
    private ImportBatchRepository importBatchRepository;

    @MockitoBean
    private ImportRowRepository importRowRepository;

    @MockitoBean
    private AuditEventRepository auditEventRepository;

    @MockitoBean
    private CustomerProfileRepository customerProfileRepository;

    @MockitoBean
    private CustomerIdentityRepository customerIdentityRepository;

    @MockitoBean
    private DuplicateCaseRepository duplicateCaseRepository;

    @MockitoBean
    private MergeHistoryRepository mergeHistoryRepository;

    @MockitoBean
    private LeadRepository leadRepository;

    @MockitoBean
    private AdmissionOpportunityRepository admissionOpportunityRepository;

    @MockitoBean
    private TouchpointRepository touchpointRepository;

    @BeforeEach
    void setUp() {
        cacheEvictionService.clearPermissionProfiles();
        when(roleRepository.existsByRoleCodeAndStatus(anyString(), eq(RoleStatus.ACTIVE))).thenReturn(true);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userBranchRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));
        when(auditEventRepository.save(any(AuditEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void createUser_whenValidWithBranch_shouldReturnCreatedAndAuditEvents() throws Exception {
        when(permissionProfileRepository.findActivePermissionCodes(TENANT_ID, "ADMIN"))
                .thenReturn(Set.of("user.manage"));
        when(branchRepository.findByTenantIdAndIdIn(eq(TENANT_ID), anyCollection()))
                .thenReturn(List.of(activeBranch()));

        mockMvc.perform(post("/api/v1/users")
                        .header(RequestIdFilter.HEADER_NAME, "req_user_001")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "full_name": "An Advisor",
                                  "email": "Advisor@MAR.vn",
                                  "phone": "0900000001",
                                  "role": "ADVISOR",
                                  "branch_ids": ["dddddddd-dddd-dddd-dddd-dddddddddddd"]
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string(RequestIdFilter.HEADER_NAME, "req_user_001"))
                .andExpect(jsonPath("$.data.user_id").isString())
                .andExpect(jsonPath("$.data.tenant_id").value(TENANT_ID.toString()))
                .andExpect(jsonPath("$.data.full_name").value("An Advisor"))
                .andExpect(jsonPath("$.data.email").value("advisor@mar.vn"))
                .andExpect(jsonPath("$.data.role").value("ADVISOR"))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.branch_ids[0]").value(BRANCH_ID.toString()))
                .andExpect(jsonPath("$.meta.request_id").value("req_user_001"));

        ArgumentCaptor<AuditEvent> auditCaptor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(auditEventRepository, times(2)).save(auditCaptor.capture());
        assertThat(auditCaptor.getAllValues())
                .extracting(AuditEvent::action)
                .containsExactly(AuditActions.USER_CREATED, AuditActions.USER_BRANCH_ASSIGNED);
    }

    @Test
    void createUser_whenFullNameMissing_shouldReturnValidationEnvelope() throws Exception {
        when(permissionProfileRepository.findActivePermissionCodes(TENANT_ID, "ADMIN"))
                .thenReturn(Set.of("user.manage"));

        mockMvc.perform(post("/api/v1/users")
                        .header(RequestIdFilter.HEADER_NAME, "req_user_002")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "full_name": "",
                                  "email": "advisor@mar.vn",
                                  "role": "ADVISOR"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(header().string(RequestIdFilter.HEADER_NAME, "req_user_002"))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.details[0].field").value("full_name"))
                .andExpect(jsonPath("$.meta.request_id").value("req_user_002"));
    }

    @Test
    void createUser_whenEmailDuplicated_shouldReturnConflictEnvelope() throws Exception {
        when(permissionProfileRepository.findActivePermissionCodes(TENANT_ID, "ADMIN"))
                .thenReturn(Set.of("user.manage"));
        when(userRepository.existsByTenantIdAndEmailIgnoreCase(TENANT_ID, "advisor@mar.vn")).thenReturn(true);

        mockMvc.perform(post("/api/v1/users")
                        .header(RequestIdFilter.HEADER_NAME, "req_user_003")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "full_name": "An Advisor",
                                  "email": "advisor@mar.vn",
                                  "role": "ADVISOR"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("DUPLICATE_USER_EMAIL"))
                .andExpect(jsonPath("$.meta.request_id").value("req_user_003"));
    }

    @Test
    void createUser_whenRoleInvalid_shouldReturnValidationEnvelope() throws Exception {
        when(permissionProfileRepository.findActivePermissionCodes(TENANT_ID, "ADMIN"))
                .thenReturn(Set.of("user.manage"));

        mockMvc.perform(post("/api/v1/users")
                        .header(RequestIdFilter.HEADER_NAME, "req_user_004")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "full_name": "An Advisor",
                                  "email": "advisor@mar.vn",
                                  "role": "NOPE"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.details[0].field").value("role"))
                .andExpect(jsonPath("$.meta.request_id").value("req_user_004"));
    }

    @Test
    void createUser_whenBranchNotInTenant_shouldReturnNotFoundEnvelope() throws Exception {
        when(permissionProfileRepository.findActivePermissionCodes(TENANT_ID, "ADMIN"))
                .thenReturn(Set.of("user.manage"));
        when(branchRepository.findByTenantIdAndIdIn(eq(TENANT_ID), anyCollection())).thenReturn(List.of());

        mockMvc.perform(post("/api/v1/users")
                        .header(RequestIdFilter.HEADER_NAME, "req_user_005")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "full_name": "An Advisor",
                                  "email": "advisor@mar.vn",
                                  "role": "ADVISOR",
                                  "branch_ids": ["dddddddd-dddd-dddd-dddd-dddddddddddd"]
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("RESOURCE_NOT_FOUND"))
                .andExpect(jsonPath("$.meta.request_id").value("req_user_005"));
    }

    @Test
    void createUser_whenInactiveWithBranchAssignment_shouldReturnBusinessRuleEnvelope() throws Exception {
        when(permissionProfileRepository.findActivePermissionCodes(TENANT_ID, "ADMIN"))
                .thenReturn(Set.of("user.manage"));

        mockMvc.perform(post("/api/v1/users")
                        .header(RequestIdFilter.HEADER_NAME, "req_user_006")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "full_name": "An Advisor",
                                  "email": "advisor@mar.vn",
                                  "role": "ADVISOR",
                                  "status": "INACTIVE",
                                  "branch_ids": ["dddddddd-dddd-dddd-dddd-dddddddddddd"]
                                }
                                """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.code").value("BUSINESS_RULE_VIOLATION"))
                .andExpect(jsonPath("$.meta.request_id").value("req_user_006"));
    }

    @Test
    void createUser_whenPermissionMissing_shouldReturnForbiddenEnvelope() throws Exception {
        when(permissionProfileRepository.findActivePermissionCodes(TENANT_ID, "ADMIN"))
                .thenReturn(Set.of("user.view"));

        mockMvc.perform(post("/api/v1/users")
                        .header(RequestIdFilter.HEADER_NAME, "req_user_007")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "full_name": "An Advisor",
                                  "email": "advisor@mar.vn",
                                  "role": "ADVISOR"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("PERMISSION_DENIED"))
                .andExpect(jsonPath("$.meta.request_id").value("req_user_007"));
    }

    @Test
    void getUser_whenUserViewPermission_shouldReturnUserDetail() throws Exception {
        when(permissionProfileRepository.findActivePermissionCodes(TENANT_ID, "ADMIN"))
                .thenReturn(Set.of("user.view"));
        when(userRepository.findByIdAndTenantId(USER_ID, TENANT_ID)).thenReturn(Optional.of(activeUser()));
        when(userBranchRepository.findByTenantIdAndUserIdAndStatus(TENANT_ID, USER_ID, UserBranchStatus.ACTIVE))
                .thenReturn(List.of(activeAssignment()));

        mockMvc.perform(get("/api/v1/users/{userId}", USER_ID)
                        .header(RequestIdFilter.HEADER_NAME, "req_user_008")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.user_id").value(USER_ID.toString()))
                .andExpect(jsonPath("$.data.role").value("ADVISOR"))
                .andExpect(jsonPath("$.data.branch_ids[0]").value(BRANCH_ID.toString()))
                .andExpect(jsonPath("$.meta.request_id").value("req_user_008"));
    }

    @Test
    void searchUsers_whenUserViewPermission_shouldReturnPaginatedList() throws Exception {
        when(permissionProfileRepository.findActivePermissionCodes(TENANT_ID, "ADMIN"))
                .thenReturn(Set.of("user.view"));
        when(userRepository.search(
                eq(TENANT_ID),
                eq(UserStatus.ACTIVE),
                eq("ADVISOR"),
                eq("an"),
                any(Pageable.class)
        )).thenReturn(new PageImpl<>(List.of(activeUser()), PageRequest.of(0, 20), 1));
        when(userBranchRepository.findByTenantIdAndUserIdInAndStatus(
                eq(TENANT_ID),
                anyCollection(),
                eq(UserBranchStatus.ACTIVE)
        )).thenReturn(List.of(activeAssignment()));

        mockMvc.perform(get("/api/v1/users")
                        .queryParam("keyword", "An")
                        .queryParam("role", "ADVISOR")
                        .queryParam("status", "ACTIVE")
                        .header(RequestIdFilter.HEADER_NAME, "req_user_009")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].user_id").value(USER_ID.toString()))
                .andExpect(jsonPath("$.data.items[0].full_name").value("An Advisor"))
                .andExpect(jsonPath("$.data.items[0].branch_ids[0]").value(BRANCH_ID.toString()))
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.size").value(20))
                .andExpect(jsonPath("$.data.total_elements").value(1))
                .andExpect(jsonPath("$.meta.request_id").value("req_user_009"));
    }

    @Test
    void updateUser_whenStatusInactive_shouldReturnUpdatedAndAuditStatusChanged() throws Exception {
        when(permissionProfileRepository.findActivePermissionCodes(TENANT_ID, "ADMIN"))
                .thenReturn(Set.of("user.manage"));
        when(userRepository.findByIdAndTenantId(USER_ID, TENANT_ID)).thenReturn(Optional.of(activeUser()));
        when(userBranchRepository.findByTenantIdAndUserIdAndStatus(TENANT_ID, USER_ID, UserBranchStatus.ACTIVE))
                .thenReturn(List.of(activeAssignment()));

        mockMvc.perform(patch("/api/v1/users/{userId}", USER_ID)
                        .header(RequestIdFilter.HEADER_NAME, "req_user_010")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "INACTIVE",
                                  "reason": "Advisor left"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.user_id").value(USER_ID.toString()))
                .andExpect(jsonPath("$.data.status").value("INACTIVE"))
                .andExpect(jsonPath("$.data.branch_ids").isEmpty())
                .andExpect(jsonPath("$.meta.request_id").value("req_user_010"));

        ArgumentCaptor<AuditEvent> auditCaptor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(auditEventRepository, times(2)).save(auditCaptor.capture());
        assertThat(auditCaptor.getAllValues())
                .extracting(AuditEvent::action)
                .containsExactly(AuditActions.USER_STATUS_CHANGED, AuditActions.USER_BRANCH_ASSIGNED);
    }

    private String bearerToken() {
        return "Bearer " + jwtTokenProvider.createAccessToken(ACTOR_ID, TENANT_ID, "ADMIN").token();
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
