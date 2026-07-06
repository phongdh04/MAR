package vn.mar.security;

import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.mar.MarApplication;
import vn.mar.audit.repository.AuditEventRepository;
import vn.mar.authz.repository.PermissionProfileRepository;
import vn.mar.branch.repository.BranchRepository;
import vn.mar.catalog.repository.CourseRepository;
import vn.mar.catalog.repository.LanguageRepository;
import vn.mar.catalog.repository.ProgramRepository;
import vn.mar.common.cache.CacheEvictionService;
import vn.mar.common.dto.ApiResponse;
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
import vn.mar.role.repository.RoleRepository;
import vn.mar.security.context.CurrentUserPrincipal;
import vn.mar.security.jwt.JwtToken;
import vn.mar.security.jwt.JwtTokenProvider;
import vn.mar.tenant.repository.TenantRepository;
import vn.mar.user.entity.User;
import vn.mar.user.model.UserStatus;
import vn.mar.user.repository.UserRepository;
import vn.mar.userbranch.repository.UserBranchRepository;

@SpringBootTest(classes = MarApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(AuthSecuritySmokeTest.ProtectedTestController.class)
class AuthSecuritySmokeTest {

    private static final UUID ACTOR_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID TENANT_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PasswordEncoder passwordEncoder;

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
    private DuplicateCaseRepository duplicateCaseRepository;

    @MockitoBean
    private MergeHistoryRepository mergeHistoryRepository;

    @MockitoBean
    private LeadRepository leadRepository;

    @MockitoBean
    private AdmissionOpportunityRepository admissionOpportunityRepository;

    @MockitoBean
    private TouchpointRepository touchpointRepository;

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
    void clearPermissionCache() {
        cacheEvictionService.clearPermissionProfiles();
    }

    @Test
    void login_whenCredentialsValid_shouldReturnAccessTokenAndPermissions() throws Exception {
        when(userRepository.findByTenantIdAndEmailIgnoreCase(TENANT_ID, "admin@mar.vn"))
                .thenReturn(Optional.of(activeUser("admin@mar.vn", "StrongPass123!")));
        when(permissionProfileRepository.findActivePermissionCodes(TENANT_ID, "ADMIN"))
                .thenReturn(Set.of("tenant.view", "tenant.manage"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .header(RequestIdFilter.HEADER_NAME, "req_login_001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "tenant_id": "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb",
                                  "email": "admin@mar.vn",
                                  "password": "StrongPass123!"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(header().string(RequestIdFilter.HEADER_NAME, "req_login_001"))
                .andExpect(jsonPath("$.data.access_token").isString())
                .andExpect(jsonPath("$.data.token_type").value("Bearer"))
                .andExpect(jsonPath("$.data.expires_in_seconds").value(not(0)))
                .andExpect(jsonPath("$.data.actor_id").value(ACTOR_ID.toString()))
                .andExpect(jsonPath("$.data.tenant_id").value(TENANT_ID.toString()))
                .andExpect(jsonPath("$.data.role_code").value("ADMIN"))
                .andExpect(jsonPath("$.data.permission_codes").isArray())
                .andExpect(jsonPath("$.meta.request_id").value("req_login_001"));
    }

    @Test
    void login_whenPasswordInvalid_shouldReturnUnauthenticatedEnvelope() throws Exception {
        when(userRepository.findByTenantIdAndEmailIgnoreCase(TENANT_ID, "admin@mar.vn"))
                .thenReturn(Optional.of(activeUser("admin@mar.vn", "StrongPass123!")));

        mockMvc.perform(post("/api/v1/auth/login")
                        .header(RequestIdFilter.HEADER_NAME, "req_login_002")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "tenant_id": "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb",
                                  "email": "admin@mar.vn",
                                  "password": "wrong-password"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(RequestIdFilter.HEADER_NAME, "req_login_002"))
                .andExpect(jsonPath("$.error.code").value("UNAUTHENTICATED"))
                .andExpect(jsonPath("$.error.message").value("Invalid credentials"))
                .andExpect(jsonPath("$.meta.request_id").value("req_login_002"));
    }

    @Test
    void protectedEndpoint_whenBearerTokenValid_shouldExposeCurrentUser() throws Exception {
        JwtToken token = jwtTokenProvider.createAccessToken(ACTOR_ID, TENANT_ID, "ADMIN");
        when(permissionProfileRepository.findActivePermissionCodes(TENANT_ID, "ADMIN"))
                .thenReturn(Set.of("tenant.view"));

        mockMvc.perform(get("/api/v1/test-auth/current-user")
                        .header(RequestIdFilter.HEADER_NAME, "req_token_001")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token.token()))
                .andExpect(status().isOk())
                .andExpect(header().string(RequestIdFilter.HEADER_NAME, "req_token_001"))
                .andExpect(jsonPath("$.data").value(ACTOR_ID.toString()))
                .andExpect(jsonPath("$.meta.request_id").value("req_token_001"));
    }

    @Test
    void protectedEndpoint_whenBearerTokenInvalid_shouldReturnUnauthenticatedEnvelope() throws Exception {
        mockMvc.perform(get("/api/v1/test-auth/current-user")
                        .header(RequestIdFilter.HEADER_NAME, "req_token_002")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer invalid-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(RequestIdFilter.HEADER_NAME, "req_token_002"))
                .andExpect(jsonPath("$.error.code").value("UNAUTHENTICATED"))
                .andExpect(jsonPath("$.meta.request_id").value("req_token_002"));
    }

    private User activeUser(String email, String rawPassword) {
        return User.restore(
                ACTOR_ID,
                TENANT_ID,
                email,
                passwordEncoder.encode(rawPassword),
                "ADMIN",
                UserStatus.ACTIVE
        );
    }

    @RestController
    static class ProtectedTestController {

        @GetMapping("/api/v1/test-auth/current-user")
        ApiResponse<String> currentUser(Authentication authentication) {
            CurrentUserPrincipal principal = (CurrentUserPrincipal) authentication.getPrincipal();
            return ApiResponse.success(principal.currentUser().actorId().toString());
        }
    }
}
