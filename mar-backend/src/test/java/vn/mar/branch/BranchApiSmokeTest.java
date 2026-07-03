package vn.mar.branch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
import vn.mar.leadimport.repository.ImportBatchRepository;
import vn.mar.leadimport.repository.ImportRowRepository;
import vn.mar.role.repository.RoleRepository;
import vn.mar.security.jwt.JwtTokenProvider;
import vn.mar.tenant.repository.TenantRepository;
import vn.mar.user.repository.UserRepository;
import vn.mar.userbranch.repository.UserBranchRepository;

@SpringBootTest(classes = MarApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BranchApiSmokeTest {

    private static final UUID ACTOR_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID TENANT_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final UUID BRANCH_ID = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
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
    private CustomerProfileRepository customerProfileRepository;

    @MockitoBean
    private CustomerIdentityRepository customerIdentityRepository;

    @MockitoBean
    private ImportBatchRepository importBatchRepository;

    @MockitoBean
    private ImportRowRepository importRowRepository;

    @MockitoBean
    private AuditEventRepository auditEventRepository;

    @MockitoBean
    private RoleRepository roleRepository;

    @MockitoBean
    private UserBranchRepository userBranchRepository;

    @BeforeEach
    void setUp() {
        cacheEvictionService.clearPermissionProfiles();
        when(branchRepository.save(any(Branch.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(auditEventRepository.save(any(AuditEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void createBranch_whenValid_shouldReturnCreatedAndAuditCreated() throws Exception {
        when(permissionProfileRepository.findActivePermissionCodes(TENANT_ID, "ADMIN"))
                .thenReturn(Set.of("branch.manage"));

        mockMvc.perform(post("/api/v1/branches")
                        .header(RequestIdFilter.HEADER_NAME, "req_branch_001")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "branch_name": "Ha Noi - Cau Giay",
                                  "city": "Ha Noi",
                                  "address": "Cau Giay"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string(RequestIdFilter.HEADER_NAME, "req_branch_001"))
                .andExpect(jsonPath("$.data.branch_id").isString())
                .andExpect(jsonPath("$.data.tenant_id").value(TENANT_ID.toString()))
                .andExpect(jsonPath("$.data.branch_code").isString())
                .andExpect(jsonPath("$.data.branch_name").value("Ha Noi - Cau Giay"))
                .andExpect(jsonPath("$.data.city").value("Ha Noi"))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andExpect(jsonPath("$.meta.request_id").value("req_branch_001"));

        ArgumentCaptor<AuditEvent> auditCaptor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(auditEventRepository).save(auditCaptor.capture());
        assertThat(auditCaptor.getValue().action()).isEqualTo(AuditActions.BRANCH_CREATED);
    }

    @Test
    void createBranch_whenBranchNameMissing_shouldReturnValidationEnvelope() throws Exception {
        when(permissionProfileRepository.findActivePermissionCodes(TENANT_ID, "ADMIN"))
                .thenReturn(Set.of("branch.manage"));

        mockMvc.perform(post("/api/v1/branches")
                        .header(RequestIdFilter.HEADER_NAME, "req_branch_002")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "branch_name": ""
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(header().string(RequestIdFilter.HEADER_NAME, "req_branch_002"))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.details[0].field").value("branch_name"))
                .andExpect(jsonPath("$.meta.request_id").value("req_branch_002"));
    }

    @Test
    void createBranch_whenActiveNameDuplicated_shouldReturnConflictEnvelope() throws Exception {
        when(permissionProfileRepository.findActivePermissionCodes(TENANT_ID, "ADMIN"))
                .thenReturn(Set.of("branch.manage"));
        when(branchRepository.existsByTenantIdAndBranchNameIgnoreCaseAndStatus(
                TENANT_ID,
                "Ha Noi - Cau Giay",
                BranchStatus.ACTIVE
        )).thenReturn(true);

        mockMvc.perform(post("/api/v1/branches")
                        .header(RequestIdFilter.HEADER_NAME, "req_branch_003")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "branch_name": "Ha Noi - Cau Giay"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("DUPLICATE_ACTIVE_BRANCH"))
                .andExpect(jsonPath("$.meta.request_id").value("req_branch_003"));
    }

    @Test
    void createBranch_whenPermissionMissing_shouldReturnForbiddenEnvelope() throws Exception {
        when(permissionProfileRepository.findActivePermissionCodes(TENANT_ID, "ADMIN"))
                .thenReturn(Set.of("branch.view"));

        mockMvc.perform(post("/api/v1/branches")
                        .header(RequestIdFilter.HEADER_NAME, "req_branch_004")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "branch_name": "Ha Noi - Cau Giay"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("PERMISSION_DENIED"))
                .andExpect(jsonPath("$.meta.request_id").value("req_branch_004"));
    }

    @Test
    void createBranch_whenStatusInvalid_shouldReturnValidationEnvelope() throws Exception {
        when(permissionProfileRepository.findActivePermissionCodes(TENANT_ID, "ADMIN"))
                .thenReturn(Set.of("branch.manage"));

        mockMvc.perform(post("/api/v1/branches")
                        .header(RequestIdFilter.HEADER_NAME, "req_branch_005")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "branch_name": "Ha Noi - Cau Giay",
                                  "status": "DELETED"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.details[0].field").value("status"));
    }

    @Test
    void getBranch_whenBranchViewPermission_shouldReturnBranchDetail() throws Exception {
        when(permissionProfileRepository.findActivePermissionCodes(TENANT_ID, "ADMIN"))
                .thenReturn(Set.of("branch.view"));
        when(branchRepository.findByIdAndTenantId(BRANCH_ID, TENANT_ID)).thenReturn(Optional.of(activeBranch()));

        mockMvc.perform(get("/api/v1/branches/{branchId}", BRANCH_ID)
                        .header(RequestIdFilter.HEADER_NAME, "req_branch_006")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.branch_id").value(BRANCH_ID.toString()))
                .andExpect(jsonPath("$.data.tenant_id").value(TENANT_ID.toString()))
                .andExpect(jsonPath("$.data.branch_code").value("HN01"))
                .andExpect(jsonPath("$.data.branch_name").value("Ha Noi - Cau Giay"))
                .andExpect(jsonPath("$.meta.request_id").value("req_branch_006"));
    }

    @Test
    void getBranch_whenCrossTenant_shouldReturnNotFoundEnvelope() throws Exception {
        when(permissionProfileRepository.findActivePermissionCodes(TENANT_ID, "ADMIN"))
                .thenReturn(Set.of("branch.view"));
        when(branchRepository.findByIdAndTenantId(BRANCH_ID, TENANT_ID)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/branches/{branchId}", BRANCH_ID)
                        .header(RequestIdFilter.HEADER_NAME, "req_branch_007")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("RESOURCE_NOT_FOUND"))
                .andExpect(jsonPath("$.meta.request_id").value("req_branch_007"));
    }

    @Test
    void searchBranches_whenBranchViewPermission_shouldReturnPaginatedList() throws Exception {
        when(permissionProfileRepository.findActivePermissionCodes(TENANT_ID, "ADMIN"))
                .thenReturn(Set.of("branch.view"));
        when(branchRepository.search(
                eq(TENANT_ID),
                eq(BranchStatus.ACTIVE),
                eq("ha"),
                any(Pageable.class)
        )).thenReturn(new PageImpl<>(List.of(activeBranch()), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/v1/branches")
                        .queryParam("keyword", "ha")
                        .queryParam("status", "ACTIVE")
                        .header(RequestIdFilter.HEADER_NAME, "req_branch_008")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].branch_id").value(BRANCH_ID.toString()))
                .andExpect(jsonPath("$.data.items[0].branch_name").value("Ha Noi - Cau Giay"))
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.size").value(20))
                .andExpect(jsonPath("$.data.total_elements").value(1))
                .andExpect(jsonPath("$.data.total_pages").value(1))
                .andExpect(jsonPath("$.meta.request_id").value("req_branch_008"));
    }

    @Test
    void searchBranches_whenSizeTooLarge_shouldReturnValidationEnvelope() throws Exception {
        when(permissionProfileRepository.findActivePermissionCodes(TENANT_ID, "ADMIN"))
                .thenReturn(Set.of("branch.view"));

        mockMvc.perform(get("/api/v1/branches")
                        .queryParam("size", "101")
                        .header(RequestIdFilter.HEADER_NAME, "req_branch_009")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.details[0].field").value("size"))
                .andExpect(jsonPath("$.meta.request_id").value("req_branch_009"));
    }

    @Test
    void updateBranch_whenStatusInactive_shouldReturnUpdatedAndAuditStatusChanged() throws Exception {
        when(permissionProfileRepository.findActivePermissionCodes(TENANT_ID, "ADMIN"))
                .thenReturn(Set.of("branch.manage"));
        when(branchRepository.findByIdAndTenantId(BRANCH_ID, TENANT_ID)).thenReturn(Optional.of(activeBranch()));

        mockMvc.perform(patch("/api/v1/branches/{branchId}", BRANCH_ID)
                        .header(RequestIdFilter.HEADER_NAME, "req_branch_010")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "INACTIVE",
                                  "reason": "Close pilot location"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.branch_id").value(BRANCH_ID.toString()))
                .andExpect(jsonPath("$.data.status").value("INACTIVE"))
                .andExpect(jsonPath("$.meta.request_id").value("req_branch_010"));

        ArgumentCaptor<AuditEvent> auditCaptor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(auditEventRepository).save(auditCaptor.capture());
        assertThat(auditCaptor.getValue().action()).isEqualTo(AuditActions.BRANCH_STATUS_CHANGED);
    }

    private String bearerToken() {
        return "Bearer " + jwtTokenProvider.createAccessToken(ACTOR_ID, TENANT_ID, "ADMIN").token();
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
}
