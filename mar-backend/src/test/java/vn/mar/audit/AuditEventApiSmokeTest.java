package vn.mar.audit;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.Map;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import vn.mar.MarApplication;
import vn.mar.audit.entity.AuditEvent;
import vn.mar.audit.model.AuditActions;
import vn.mar.audit.model.AuditResourceTypes;
import vn.mar.audit.repository.AuditEventRepository;
import vn.mar.audit.service.AuditRecordCommand;
import vn.mar.authz.model.PermissionCodes;
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
import vn.mar.opportunity.repository.ActivityRepository;
import vn.mar.opportunity.repository.AdmissionOpportunityRepository;
import vn.mar.opportunity.repository.StageHistoryRepository;
import vn.mar.opportunity.repository.TouchpointRepository;
import vn.mar.role.repository.RoleRepository;
import vn.mar.security.jwt.JwtTokenProvider;
import vn.mar.sla.repository.SlaPolicyRepository;
import vn.mar.sla.repository.WorkingHoursConfigRepository;
import vn.mar.tenant.repository.TenantRepository;
import vn.mar.user.repository.UserRepository;
import vn.mar.userbranch.repository.UserBranchRepository;

@SpringBootTest(classes = MarApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuditEventApiSmokeTest {

    private static final UUID ACTOR_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID TENANT_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final UUID RESOURCE_ID = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
    private static final Instant NOW = Instant.parse("2026-07-06T01:00:00Z");
    private static final Instant FROM = Instant.parse("2026-07-06T00:00:00Z");
    private static final Instant TO = Instant.parse("2026-07-06T02:00:00Z");

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
    private MergeHistoryRepository mergeHistoryRepository;

    @MockitoBean
    private LeadRepository leadRepository;

    @MockitoBean
    private AdmissionOpportunityRepository admissionOpportunityRepository;

    @MockitoBean
    private StageHistoryRepository stageHistoryRepository;

    @MockitoBean
    private ActivityRepository activityRepository;

    @MockitoBean
    private TouchpointRepository touchpointRepository;

    @MockitoBean
    private ImportBatchRepository importBatchRepository;

    @MockitoBean
    private ImportRowRepository importRowRepository;

    @MockitoBean
    private WorkingHoursConfigRepository workingHoursConfigRepository;

    @MockitoBean
    private SlaPolicyRepository slaPolicyRepository;

    @MockitoBean
    private AuditEventRepository auditEventRepository;

    @BeforeEach
    void setUp() {
        cacheEvictionService.clearPermissionProfiles();
    }

    @Test
    void searchEvents_whenAdminHasAuditView_shouldReturnTenantScopedEnvelope() throws Exception {
        allowAuditView();
        AuditEvent event = auditEvent();
        when(auditEventRepository.search(
                eq(TENANT_ID),
                eq(AuditResourceTypes.USER),
                eq(null),
                eq(null),
                eq(ACTOR_ID),
                eq(null),
                eq(AuditActions.USER_STATUS_CHANGED),
                eq("req_user_status_001"),
                eq(FROM),
                eq(TO),
                any(Pageable.class)
        )).thenReturn(new PageImpl<>(List.of(event), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/v1/audit-events")
                        .queryParam("resource_type", "user")
                        .queryParam("actor_id", ACTOR_ID.toString())
                        .queryParam("action", "user_status_changed")
                        .queryParam("request_id", "req_user_status_001")
                        .queryParam("from", FROM.toString())
                        .queryParam("to", TO.toString())
                        .header(RequestIdFilter.HEADER_NAME, "req_audit_api_001")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(header().string(RequestIdFilter.HEADER_NAME, "req_audit_api_001"))
                .andExpect(jsonPath("$.data.items[0].audit_event_id").value(event.id().toString()))
                .andExpect(jsonPath("$.data.items[0].tenant_id").value(TENANT_ID.toString()))
                .andExpect(jsonPath("$.data.items[0].actor_id").value(ACTOR_ID.toString()))
                .andExpect(jsonPath("$.data.items[0].action").value(AuditActions.USER_STATUS_CHANGED))
                .andExpect(jsonPath("$.data.items[0].resource_type").value(AuditResourceTypes.USER))
                .andExpect(jsonPath("$.data.items[0].before_data.status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.items[0].after_data.status").value("INACTIVE"))
                .andExpect(jsonPath("$.data.items[0].reason").value("Deactivate user"))
                .andExpect(jsonPath("$.data.items[0].request_id").value("req_user_status_001"))
                .andExpect(jsonPath("$.data.total_elements").value(1))
                .andExpect(jsonPath("$.meta.request_id").value("req_audit_api_001"));
    }

    @Test
    void getEvent_whenAdminHasAuditView_shouldReturnDetail() throws Exception {
        allowAuditView();
        AuditEvent event = auditEvent();
        when(auditEventRepository.findByIdAndTenantId(event.id(), TENANT_ID)).thenReturn(Optional.of(event));

        mockMvc.perform(get("/api/v1/audit-events/{auditEventId}", event.id())
                        .header(RequestIdFilter.HEADER_NAME, "req_audit_api_002")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.audit_event_id").value(event.id().toString()))
                .andExpect(jsonPath("$.data.actor_type").value("USER"))
                .andExpect(jsonPath("$.data.actor_role").value("ADMIN"))
                .andExpect(jsonPath("$.meta.request_id").value("req_audit_api_002"));
    }

    @Test
    void searchEvents_whenPermissionMissing_shouldReturnForbiddenEnvelope() throws Exception {
        when(permissionProfileRepository.findActivePermissionCodes(TENANT_ID, "ADVISOR"))
                .thenReturn(Set.of(PermissionCodes.USER_VIEW));

        mockMvc.perform(get("/api/v1/audit-events")
                        .header(RequestIdFilter.HEADER_NAME, "req_audit_api_003")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken("ADVISOR")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("PERMISSION_DENIED"))
                .andExpect(jsonPath("$.meta.request_id").value("req_audit_api_003"));
    }

    private void allowAuditView() {
        when(permissionProfileRepository.findActivePermissionCodes(TENANT_ID, "ADMIN"))
                .thenReturn(Set.of(PermissionCodes.AUDIT_VIEW));
    }

    private String bearerToken(String roleCode) {
        return "Bearer " + jwtTokenProvider.createAccessToken(ACTOR_ID, TENANT_ID, roleCode).token();
    }

    private AuditEvent auditEvent() {
        return AuditEvent.create(new AuditRecordCommand(
                TENANT_ID,
                ACTOR_ID,
                "USER",
                "ADMIN",
                AuditActions.USER_STATUS_CHANGED,
                AuditResourceTypes.USER,
                RESOURCE_ID,
                "user@example.com",
                Map.of("status", "ACTIVE"),
                Map.of("status", "INACTIVE"),
                Map.of("source", "api-smoke-test"),
                "Deactivate user",
                "req_user_status_001"
        ), NOW);
    }
}
