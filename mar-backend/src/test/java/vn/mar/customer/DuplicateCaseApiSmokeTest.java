package vn.mar.customer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import vn.mar.audit.repository.AuditEventRepository;
import vn.mar.authz.repository.PermissionProfileRepository;
import vn.mar.branch.repository.BranchRepository;
import vn.mar.catalog.repository.CourseRepository;
import vn.mar.catalog.repository.LanguageRepository;
import vn.mar.catalog.repository.ProgramRepository;
import vn.mar.common.cache.CacheEvictionService;
import vn.mar.common.logging.RequestIdFilter;
import vn.mar.customer.entity.CustomerProfile;
import vn.mar.customer.entity.DuplicateCase;
import vn.mar.customer.model.DuplicateCaseStatus;
import vn.mar.customer.model.DuplicateConfidence;
import vn.mar.customer.model.DuplicateMatchType;
import vn.mar.customer.repository.CustomerIdentityRepository;
import vn.mar.customer.repository.CustomerProfileRepository;
import vn.mar.customer.repository.DuplicateCaseRepository;
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
class DuplicateCaseApiSmokeTest {

    private static final UUID ACTOR_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID TENANT_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final UUID SOURCE_CUSTOMER_ID = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
    private static final UUID CANDIDATE_CUSTOMER_ID = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");
    private static final UUID DUPLICATE_CASE_ID = UUID.fromString("eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee");
    private static final Instant NOW = Instant.parse("2026-07-03T01:00:00Z");

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
    private CustomerProfileRepository customerProfileRepository;

    @MockitoBean
    private CustomerIdentityRepository customerIdentityRepository;

    @MockitoBean
    private DuplicateCaseRepository duplicateCaseRepository;

    @MockitoBean
    private ImportBatchRepository importBatchRepository;

    @MockitoBean
    private ImportRowRepository importRowRepository;

    @MockitoBean
    private AuditEventRepository auditEventRepository;

    @BeforeEach
    void setUp() {
        cacheEvictionService.clearPermissionProfiles();
        when(auditEventRepository.save(any(AuditEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void searchDuplicateCases_whenDuplicateManagePermission_shouldReturnPaginatedCases() throws Exception {
        when(permissionProfileRepository.findActivePermissionCodes(TENANT_ID, "ADMIN"))
                .thenReturn(Set.of("duplicate.manage"));
        when(duplicateCaseRepository.search(
                eq(TENANT_ID),
                eq(DuplicateCaseStatus.NEEDS_REVIEW),
                eq(DuplicateMatchType.NEAR_MATCH),
                any(Pageable.class)
        )).thenReturn(new PageImpl<>(List.of(openDuplicateCase()), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/v1/duplicates")
                        .queryParam("status", "NEEDS_REVIEW")
                        .queryParam("match_type", "NEAR_MATCH")
                        .header(RequestIdFilter.HEADER_NAME, "req_dup_001")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken()))
                .andExpect(status().isOk())
                .andExpect(header().string(RequestIdFilter.HEADER_NAME, "req_dup_001"))
                .andExpect(jsonPath("$.data.items[0].duplicate_case_id").value(DUPLICATE_CASE_ID.toString()))
                .andExpect(jsonPath("$.data.items[0].candidate_customer_id").value(CANDIDATE_CUSTOMER_ID.toString()))
                .andExpect(jsonPath("$.data.items[0].match_type").value("NEAR_MATCH"))
                .andExpect(jsonPath("$.data.items[0].status").value("NEEDS_REVIEW"))
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.size").value(20))
                .andExpect(jsonPath("$.data.total_elements").value(1))
                .andExpect(jsonPath("$.meta.request_id").value("req_dup_001"));
    }

    @Test
    void resolveDuplicateCase_whenMergeWithPermissions_shouldResolveAndAudit() throws Exception {
        when(permissionProfileRepository.findActivePermissionCodes(TENANT_ID, "ADMIN"))
                .thenReturn(Set.of("duplicate.manage", "customer.merge"));
        when(duplicateCaseRepository.findByIdAndTenantId(DUPLICATE_CASE_ID, TENANT_ID))
                .thenReturn(Optional.of(openDuplicateCase()));
        when(customerProfileRepository.findByIdAndTenantId(SOURCE_CUSTOMER_ID, TENANT_ID))
                .thenReturn(Optional.of(customer(SOURCE_CUSTOMER_ID)));
        when(customerProfileRepository.findByIdAndTenantId(CANDIDATE_CUSTOMER_ID, TENANT_ID))
                .thenReturn(Optional.of(customer(CANDIDATE_CUSTOMER_ID)));
        when(duplicateCaseRepository.save(any(DuplicateCase.class))).thenAnswer(invocation -> invocation.getArgument(0));

        mockMvc.perform(post("/api/v1/duplicates/{caseId}/resolve", DUPLICATE_CASE_ID)
                        .header(RequestIdFilter.HEADER_NAME, "req_dup_002")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "action": "MERGE",
                                  "reason": "Confirmed same learner by parent"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.duplicate_case_id").value(DUPLICATE_CASE_ID.toString()))
                .andExpect(jsonPath("$.data.status").value("MERGED"))
                .andExpect(jsonPath("$.data.resolution_action").value("MERGE"))
                .andExpect(jsonPath("$.data.resolved_by").value(ACTOR_ID.toString()))
                .andExpect(jsonPath("$.meta.request_id").value("req_dup_002"));
    }

    @Test
    void resolveDuplicateCase_whenDuplicateManageMissing_shouldReturnForbidden() throws Exception {
        when(permissionProfileRepository.findActivePermissionCodes(TENANT_ID, "ADMIN"))
                .thenReturn(Set.of("customer.merge"));

        mockMvc.perform(post("/api/v1/duplicates/{caseId}/resolve", DUPLICATE_CASE_ID)
                        .header(RequestIdFilter.HEADER_NAME, "req_dup_003")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "action": "MERGE",
                                  "reason": "Confirmed same learner"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("PERMISSION_DENIED"))
                .andExpect(jsonPath("$.meta.request_id").value("req_dup_003"));
    }

    @Test
    void resolveDuplicateCase_whenMergePermissionMissing_shouldReturnForbidden() throws Exception {
        when(permissionProfileRepository.findActivePermissionCodes(TENANT_ID, "ADMIN"))
                .thenReturn(Set.of("duplicate.manage"));

        mockMvc.perform(post("/api/v1/duplicates/{caseId}/resolve", DUPLICATE_CASE_ID)
                        .header(RequestIdFilter.HEADER_NAME, "req_dup_004")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "action": "MERGE",
                                  "reason": "Confirmed same learner"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("PERMISSION_DENIED"))
                .andExpect(jsonPath("$.meta.request_id").value("req_dup_004"));
    }

    @Test
    void getDuplicateCase_whenCrossTenant_shouldReturnNotFound() throws Exception {
        when(permissionProfileRepository.findActivePermissionCodes(TENANT_ID, "ADMIN"))
                .thenReturn(Set.of("duplicate.manage"));
        when(duplicateCaseRepository.findByIdAndTenantId(DUPLICATE_CASE_ID, TENANT_ID))
                .thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/duplicates/{caseId}", DUPLICATE_CASE_ID)
                        .header(RequestIdFilter.HEADER_NAME, "req_dup_005")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("RESOURCE_NOT_FOUND"))
                .andExpect(jsonPath("$.meta.request_id").value("req_dup_005"));
    }

    @Test
    void searchDuplicateCases_whenSizeTooLarge_shouldReturnValidationEnvelope() throws Exception {
        when(permissionProfileRepository.findActivePermissionCodes(TENANT_ID, "ADMIN"))
                .thenReturn(Set.of("duplicate.manage"));

        mockMvc.perform(get("/api/v1/duplicates")
                        .queryParam("size", "101")
                        .header(RequestIdFilter.HEADER_NAME, "req_dup_006")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.details[0].field").value("size"))
                .andExpect(jsonPath("$.meta.request_id").value("req_dup_006"));
    }

    private String bearerToken() {
        return "Bearer " + jwtTokenProvider.createAccessToken(ACTOR_ID, TENANT_ID, "ADMIN").token();
    }

    private DuplicateCase openDuplicateCase() {
        return DuplicateCase.restore(
                DUPLICATE_CASE_ID,
                TENANT_ID,
                SOURCE_CUSTOMER_ID,
                CANDIDATE_CUSTOMER_ID,
                DuplicateMatchType.NEAR_MATCH,
                DuplicateConfidence.LOW,
                DuplicateCaseStatus.NEEDS_REVIEW,
                "Similar learner name",
                null,
                null,
                null,
                null,
                NOW,
                NOW
        );
    }

    private CustomerProfile customer(UUID customerId) {
        return CustomerProfile.restore(
                customerId,
                TENANT_ID,
                "Student",
                "0900000001",
                "student@example.com",
                null,
                null,
                null,
                null,
                NOW,
                NOW
        );
    }
}
