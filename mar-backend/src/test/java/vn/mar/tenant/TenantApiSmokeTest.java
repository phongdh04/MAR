package vn.mar.tenant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
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
import vn.mar.opportunity.repository.StageHistoryRepository;
import vn.mar.opportunity.repository.TouchpointRepository;
import vn.mar.role.repository.RoleRepository;
import vn.mar.security.jwt.JwtTokenProvider;
import vn.mar.tenant.entity.Tenant;
import vn.mar.tenant.model.TenantStatus;
import vn.mar.tenant.repository.TenantRepository;
import vn.mar.user.repository.UserRepository;
import vn.mar.userbranch.repository.UserBranchRepository;

@SpringBootTest(classes = MarApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TenantApiSmokeTest {

    private static final UUID ACTOR_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID ACTOR_TENANT_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final UUID TENANT_ID = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
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
    private DuplicateCaseRepository duplicateCaseRepository;

    @MockitoBean
    private MergeHistoryRepository mergeHistoryRepository;

    @MockitoBean
    private LeadRepository leadRepository;

    @MockitoBean
    private AdmissionOpportunityRepository admissionOpportunityRepository;

    @MockitoBean
    private StageHistoryRepository stageHistoryRepository;

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
    void setUp() {
        cacheEvictionService.clearPermissionProfiles();
        when(tenantRepository.save(any(Tenant.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(auditEventRepository.save(any(AuditEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void createTenant_whenValid_shouldReturnCreatedWithDefaults() throws Exception {
        when(permissionProfileRepository.findActivePermissionCodes(ACTOR_TENANT_ID, "ADMIN"))
                .thenReturn(Set.of("tenant.manage"));

        mockMvc.perform(post("/api/v1/tenants")
                        .header(RequestIdFilter.HEADER_NAME, "req_tenant_001")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "tenant_name": "ABC Language Center"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string(RequestIdFilter.HEADER_NAME, "req_tenant_001"))
                .andExpect(jsonPath("$.data.tenant_id").isString())
                .andExpect(jsonPath("$.data.tenant_code").isString())
                .andExpect(jsonPath("$.data.tenant_name").value("ABC Language Center"))
                .andExpect(jsonPath("$.data.timezone").value("Asia/Ho_Chi_Minh"))
                .andExpect(jsonPath("$.data.default_currency").value("VND"))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andExpect(jsonPath("$.meta.request_id").value("req_tenant_001"));

        ArgumentCaptor<AuditEvent> auditCaptor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(auditEventRepository).save(auditCaptor.capture());
        assertThat(auditCaptor.getValue().action()).isEqualTo(AuditActions.TENANT_CREATED);
    }

    @Test
    void createTenant_whenTenantNameMissing_shouldReturnValidationEnvelope() throws Exception {
        when(permissionProfileRepository.findActivePermissionCodes(ACTOR_TENANT_ID, "ADMIN"))
                .thenReturn(Set.of("tenant.manage"));

        mockMvc.perform(post("/api/v1/tenants")
                        .header(RequestIdFilter.HEADER_NAME, "req_tenant_002")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "tenant_name": ""
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(header().string(RequestIdFilter.HEADER_NAME, "req_tenant_002"))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.details[0].field").value("tenant_name"))
                .andExpect(jsonPath("$.meta.request_id").value("req_tenant_002"));
    }

    @Test
    void createTenant_whenPermissionMissing_shouldReturnForbiddenEnvelope() throws Exception {
        when(permissionProfileRepository.findActivePermissionCodes(ACTOR_TENANT_ID, "ADMIN"))
                .thenReturn(Set.of("tenant.view"));

        mockMvc.perform(post("/api/v1/tenants")
                        .header(RequestIdFilter.HEADER_NAME, "req_tenant_003")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "tenant_name": "ABC Language Center"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(header().string(RequestIdFilter.HEADER_NAME, "req_tenant_003"))
                .andExpect(jsonPath("$.error.code").value("PERMISSION_DENIED"))
                .andExpect(jsonPath("$.meta.request_id").value("req_tenant_003"));
    }

    @Test
    void createTenant_whenTimezoneInvalid_shouldReturnValidationEnvelope() throws Exception {
        when(permissionProfileRepository.findActivePermissionCodes(ACTOR_TENANT_ID, "ADMIN"))
                .thenReturn(Set.of("tenant.manage"));

        mockMvc.perform(post("/api/v1/tenants")
                        .header(RequestIdFilter.HEADER_NAME, "req_tenant_004")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "tenant_name": "ABC Language Center",
                                  "timezone": "Mars/Nope"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.details[0].field").value("timezone"));
    }

    @Test
    void createTenant_whenCurrencyInvalid_shouldReturnValidationEnvelope() throws Exception {
        when(permissionProfileRepository.findActivePermissionCodes(ACTOR_TENANT_ID, "ADMIN"))
                .thenReturn(Set.of("tenant.manage"));

        mockMvc.perform(post("/api/v1/tenants")
                        .header(RequestIdFilter.HEADER_NAME, "req_tenant_007")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "tenant_name": "ABC Language Center",
                                  "default_currency": "NOPE"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.details[0].field").value("default_currency"));
    }

    @Test
    void createTenant_whenStatusInvalid_shouldReturnValidationEnvelope() throws Exception {
        when(permissionProfileRepository.findActivePermissionCodes(ACTOR_TENANT_ID, "ADMIN"))
                .thenReturn(Set.of("tenant.manage"));

        mockMvc.perform(post("/api/v1/tenants")
                        .header(RequestIdFilter.HEADER_NAME, "req_tenant_008")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "tenant_name": "ABC Language Center",
                                  "status": "DELETED"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.details[0].field").value("status"));
    }

    @Test
    void updateTenant_whenStatusInactive_shouldReturnUpdatedAndAuditStatusChanged() throws Exception {
        when(permissionProfileRepository.findActivePermissionCodes(ACTOR_TENANT_ID, "ADMIN"))
                .thenReturn(Set.of("tenant.manage"));
        when(tenantRepository.findById(TENANT_ID)).thenReturn(Optional.of(activeTenant()));

        mockMvc.perform(patch("/api/v1/tenants/{tenantId}", TENANT_ID)
                        .header(RequestIdFilter.HEADER_NAME, "req_tenant_005")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "INACTIVE",
                                  "reason": "Pilot ended"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.tenant_id").value(TENANT_ID.toString()))
                .andExpect(jsonPath("$.data.status").value("INACTIVE"))
                .andExpect(jsonPath("$.meta.request_id").value("req_tenant_005"));

        ArgumentCaptor<AuditEvent> auditCaptor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(auditEventRepository).save(auditCaptor.capture());
        assertThat(auditCaptor.getValue().action()).isEqualTo(AuditActions.TENANT_STATUS_CHANGED);
    }

    @Test
    void getTenant_whenTenantViewPermission_shouldReturnTenantDetail() throws Exception {
        when(permissionProfileRepository.findActivePermissionCodes(ACTOR_TENANT_ID, "ADMIN"))
                .thenReturn(Set.of("tenant.view"));
        when(tenantRepository.findById(TENANT_ID)).thenReturn(Optional.of(activeTenant()));

        mockMvc.perform(get("/api/v1/tenants/{tenantId}", TENANT_ID)
                        .header(RequestIdFilter.HEADER_NAME, "req_tenant_006")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.tenant_id").value(TENANT_ID.toString()))
                .andExpect(jsonPath("$.data.tenant_code").value("ABC"))
                .andExpect(jsonPath("$.data.tenant_name").value("ABC Language Center"))
                .andExpect(jsonPath("$.meta.request_id").value("req_tenant_006"));
    }

    private String bearerToken() {
        return "Bearer " + jwtTokenProvider.createAccessToken(ACTOR_ID, ACTOR_TENANT_ID, "ADMIN").token();
    }

    private Tenant activeTenant() {
        return Tenant.restore(
                TENANT_ID,
                "ABC",
                "ABC Language Center",
                "Asia/Ho_Chi_Minh",
                "VND",
                TenantStatus.ACTIVE,
                NOW,
                ACTOR_ID,
                NOW,
                ACTOR_ID
        );
    }
}
