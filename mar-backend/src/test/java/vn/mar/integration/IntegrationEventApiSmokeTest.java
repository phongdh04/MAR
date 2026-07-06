package vn.mar.integration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import vn.mar.MarApplication;
import vn.mar.audit.repository.AuditEventRepository;
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
import vn.mar.integration.entity.IntegrationEvent;
import vn.mar.integration.model.IntegrationEventStatus;
import vn.mar.integration.repository.IntegrationEventRepository;
import vn.mar.lead.model.LeadSourceType;
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
class IntegrationEventApiSmokeTest {

    private static final UUID ACTOR_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID TENANT_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final UUID EVENT_ID = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
    private static final String PAYLOAD_HASH =
            "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";
    private static final Instant NOW = Instant.parse("2026-07-06T01:00:00Z");
    private static final Instant FROM = Instant.parse("2026-07-06T00:00:00Z");
    private static final Instant TO = Instant.parse("2026-07-06T02:00:00Z");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private CacheEvictionService cacheEvictionService;

    @Autowired
    private IntegrationEventRepository integrationEventRepository;

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
        reset(integrationEventRepository);
    }

    @Test
    void searchEvents_whenMarketingHasIntegrationLogView_shouldReturnTenantScopedEnvelope() throws Exception {
        allowIntegrationLogView();
        IntegrationEvent event = duplicateEvent();
        when(integrationEventRepository.search(
                eq(TENANT_ID),
                eq(LeadSourceType.WEBSITE_FORM),
                eq(IntegrationEventStatus.DUPLICATE),
                eq("webform_001"),
                eq("webform_001"),
                eq(PAYLOAD_HASH),
                eq("WEBHOOK_DUPLICATE_IGNORED"),
                eq(null),
                eq(null),
                eq(null),
                eq(FROM),
                eq(TO),
                any(Pageable.class)
        )).thenReturn(new PageImpl<>(List.of(event), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/v1/integrations/webhook-events")
                        .queryParam("source_type", "website_form")
                        .queryParam("status", "duplicate")
                        .queryParam("external_id", "webform_001")
                        .queryParam("idempotency_key", "webform_001")
                        .queryParam("payload_hash", PAYLOAD_HASH)
                        .queryParam("error_code", "webhook_duplicate_ignored")
                        .queryParam("from", FROM.toString())
                        .queryParam("to", TO.toString())
                        .header(RequestIdFilter.HEADER_NAME, "req_integration_log_api_001")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken("MARKETING")))
                .andExpect(status().isOk())
                .andExpect(header().string(RequestIdFilter.HEADER_NAME, "req_integration_log_api_001"))
                .andExpect(jsonPath("$.data.items[0].event_id").value(EVENT_ID.toString()))
                .andExpect(jsonPath("$.data.items[0].tenant_id").value(TENANT_ID.toString()))
                .andExpect(jsonPath("$.data.items[0].source_type").value("WEBSITE_FORM"))
                .andExpect(jsonPath("$.data.items[0].external_id").value("webform_001"))
                .andExpect(jsonPath("$.data.items[0].idempotency_key").value("webform_001"))
                .andExpect(jsonPath("$.data.items[0].payload_hash").value(PAYLOAD_HASH))
                .andExpect(jsonPath("$.data.items[0].status").value("DUPLICATE"))
                .andExpect(jsonPath("$.data.items[0].error_code").value("WEBHOOK_DUPLICATE_IGNORED"))
                .andExpect(jsonPath("$.data.total_elements").value(1))
                .andExpect(jsonPath("$.meta.request_id").value("req_integration_log_api_001"));
    }

    @Test
    void getEvent_whenMarketingHasIntegrationLogView_shouldReturnDetail() throws Exception {
        allowIntegrationLogView();
        IntegrationEvent event = duplicateEvent();
        when(integrationEventRepository.findByIdAndTenantId(EVENT_ID, TENANT_ID)).thenReturn(Optional.of(event));

        mockMvc.perform(get("/api/v1/integrations/webhook-events/{eventId}", EVENT_ID)
                        .header(RequestIdFilter.HEADER_NAME, "req_integration_log_api_002")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken("MARKETING")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.event_id").value(EVENT_ID.toString()))
                .andExpect(jsonPath("$.data.status").value("DUPLICATE"))
                .andExpect(jsonPath("$.data.received_at").value(NOW.toString()))
                .andExpect(jsonPath("$.data.processed_at").value(NOW.toString()))
                .andExpect(jsonPath("$.meta.request_id").value("req_integration_log_api_002"));
    }

    @Test
    void getEvent_whenEventIsOutsideCurrentTenant_shouldReturnNotFoundEnvelope() throws Exception {
        allowIntegrationLogView();
        when(integrationEventRepository.findByIdAndTenantId(EVENT_ID, TENANT_ID)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/integrations/webhook-events/{eventId}", EVENT_ID)
                        .header(RequestIdFilter.HEADER_NAME, "req_integration_log_api_003")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken("MARKETING")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("RESOURCE_NOT_FOUND"))
                .andExpect(jsonPath("$.meta.request_id").value("req_integration_log_api_003"));
    }

    @Test
    void searchEvents_whenPermissionMissing_shouldReturnForbiddenEnvelope() throws Exception {
        when(permissionProfileRepository.findActivePermissionCodes(TENANT_ID, "ADVISOR"))
                .thenReturn(Set.of(PermissionCodes.LEAD_VIEW));

        mockMvc.perform(get("/api/v1/integrations/webhook-events")
                        .header(RequestIdFilter.HEADER_NAME, "req_integration_log_api_004")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken("ADVISOR")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("PERMISSION_DENIED"))
                .andExpect(jsonPath("$.meta.request_id").value("req_integration_log_api_004"));

        verify(integrationEventRepository, never()).search(
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any()
        );
    }

    private void allowIntegrationLogView() {
        when(permissionProfileRepository.findActivePermissionCodes(TENANT_ID, "MARKETING"))
                .thenReturn(Set.of(PermissionCodes.INTEGRATION_LOG_VIEW));
    }

    private String bearerToken(String roleCode) {
        return "Bearer " + jwtTokenProvider.createAccessToken(ACTOR_ID, TENANT_ID, roleCode).token();
    }

    private IntegrationEvent duplicateEvent() {
        return IntegrationEvent.duplicate(
                EVENT_ID,
                TENANT_ID,
                LeadSourceType.WEBSITE_FORM,
                "webform_001",
                "webform_001",
                PAYLOAD_HASH,
                NOW
        );
    }
}
