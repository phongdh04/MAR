package vn.mar.integration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import vn.mar.MarApplication;
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
import vn.mar.integration.entity.IntegrationEvent;
import vn.mar.integration.repository.IntegrationEventRepository;
import vn.mar.integration.service.WebhookPayloadSecurityService;
import vn.mar.lead.model.LeadSourceType;
import vn.mar.lead.repository.LeadRepository;
import vn.mar.leadimport.repository.ImportBatchRepository;
import vn.mar.leadimport.repository.ImportRowRepository;
import vn.mar.opportunity.repository.ActivityRepository;
import vn.mar.opportunity.repository.AdmissionOpportunityRepository;
import vn.mar.opportunity.repository.StageHistoryRepository;
import vn.mar.opportunity.repository.TouchpointRepository;
import vn.mar.role.repository.RoleRepository;
import vn.mar.sla.repository.SlaPolicyRepository;
import vn.mar.sla.repository.WorkingHoursConfigRepository;
import vn.mar.tenant.entity.Tenant;
import vn.mar.tenant.model.TenantStatus;
import vn.mar.tenant.repository.TenantRepository;
import vn.mar.user.repository.UserRepository;
import vn.mar.userbranch.repository.UserBranchRepository;

@SpringBootTest(classes = MarApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class WebsiteLeadWebhookApiSmokeTest {

    private static final UUID TENANT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final Instant NOW = Instant.parse("2026-07-06T01:00:00Z");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private WebhookPayloadSecurityService webhookPayloadSecurityService;

    @Autowired
    private IntegrationEventRepository integrationEventRepository;

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
        reset(integrationEventRepository);
    }

    @Test
    void receiveWebsiteLead_whenValidSignedPayload_shouldStoreReceivedEvent() throws Exception {
        String body = """
                {
                  "external_id": "webform_001",
                  "full_name": "Nguyen Minh A",
                  "phone": "0912345678"
                }
                """;
        JsonNode payload = objectMapper.readTree(body);
        when(tenantRepository.findByTenantCodeIgnoreCase("MAR")).thenReturn(Optional.of(activeTenant()));
        when(integrationEventRepository
                .findFirstByTenantIdAndSourceTypeAndExternalIdAndStatusNotOrderByReceivedAtAsc(any(), any(), any(), any()))
                .thenReturn(Optional.empty());
        when(integrationEventRepository
                .findFirstByTenantIdAndSourceTypeAndIdempotencyKeyAndStatusNotOrderByReceivedAtAsc(any(), any(), any(), any()))
                .thenReturn(Optional.empty());
        when(integrationEventRepository
                .findFirstByTenantIdAndSourceTypeAndPayloadHashAndStatusNotOrderByReceivedAtAsc(any(), any(), any(), any()))
                .thenReturn(Optional.empty());
        when(integrationEventRepository.save(any(IntegrationEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));

        mockMvc.perform(post("/api/v1/webhooks/leads/website")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .header(RequestIdFilter.HEADER_NAME, "req_webhook_001")
                        .header("X-Mar-Tenant-Key", "MAR")
                        .header("X-Mar-Signature", webhookPayloadSecurityService.signature(payload)))
                .andExpect(status().isAccepted())
                .andExpect(header().string(RequestIdFilter.HEADER_NAME, "req_webhook_001"))
                .andExpect(jsonPath("$.data.tenant_id").value(TENANT_ID.toString()))
                .andExpect(jsonPath("$.data.source_type").value("WEBSITE_FORM"))
                .andExpect(jsonPath("$.data.external_id").value("webform_001"))
                .andExpect(jsonPath("$.data.idempotency_key").value("webform_001"))
                .andExpect(jsonPath("$.data.payload_hash").isNotEmpty())
                .andExpect(jsonPath("$.data.status").value("RECEIVED"))
                .andExpect(jsonPath("$.data.duplicate").value(false))
                .andExpect(jsonPath("$.meta.request_id").value("req_webhook_001"));
    }

    @Test
    void receiveWebsiteLead_whenDuplicateExternalId_shouldReturnDuplicateEvent() throws Exception {
        String body = """
                {
                  "external_id": "webform_002",
                  "full_name": "Nguyen Minh B",
                  "phone": "0912345679"
                }
                """;
        JsonNode payload = objectMapper.readTree(body);
        IntegrationEvent existing = IntegrationEvent.received(
                UUID.randomUUID(),
                TENANT_ID,
                LeadSourceType.WEBSITE_FORM,
                "webform_002",
                "webform_002",
                webhookPayloadSecurityService.payloadHash(payload),
                NOW
        );
        when(tenantRepository.findByTenantCodeIgnoreCase("MAR")).thenReturn(Optional.of(activeTenant()));
        when(integrationEventRepository
                .findFirstByTenantIdAndSourceTypeAndExternalIdAndStatusNotOrderByReceivedAtAsc(any(), any(), any(), any()))
                .thenReturn(Optional.of(existing));
        when(integrationEventRepository.save(any(IntegrationEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));

        mockMvc.perform(post("/api/v1/webhooks/leads/website")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .header(RequestIdFilter.HEADER_NAME, "req_webhook_002")
                        .header("X-Mar-Tenant-Key", "MAR")
                        .header("X-Mar-Signature", webhookPayloadSecurityService.signature(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DUPLICATE"))
                .andExpect(jsonPath("$.data.error_code").value("WEBHOOK_DUPLICATE_IGNORED"))
                .andExpect(jsonPath("$.data.duplicate").value(true))
                .andExpect(jsonPath("$.meta.request_id").value("req_webhook_002"));
    }

    @Test
    void receiveWebsiteLead_whenTenantKeyInvalid_shouldReturnUnauthorizedEnvelope() throws Exception {
        String body = """
                {
                  "external_id": "webform_003",
                  "full_name": "Nguyen Minh C"
                }
                """;
        JsonNode payload = objectMapper.readTree(body);
        when(tenantRepository.findByTenantCodeIgnoreCase("UNKNOWN")).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/v1/webhooks/leads/website")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .header(RequestIdFilter.HEADER_NAME, "req_webhook_003")
                        .header("X-Mar-Tenant-Key", "UNKNOWN")
                        .header("X-Mar-Signature", webhookPayloadSecurityService.signature(payload)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("WEBHOOK_TENANT_KEY_INVALID"))
                .andExpect(jsonPath("$.meta.request_id").value("req_webhook_003"));

        verify(integrationEventRepository, org.mockito.Mockito.never()).save(any());
    }

    @Test
    void receiveWebsiteLead_whenSignatureInvalid_shouldReturnUnauthorizedEnvelope() throws Exception {
        String body = """
                {
                  "external_id": "webform_004",
                  "full_name": "Nguyen Minh D"
                }
                """;

        mockMvc.perform(post("/api/v1/webhooks/leads/website")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .header(RequestIdFilter.HEADER_NAME, "req_webhook_004")
                        .header("X-Mar-Tenant-Key", "MAR")
                        .header("X-Mar-Signature", "sha256=invalid"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("WEBHOOK_SIGNATURE_INVALID"))
                .andExpect(jsonPath("$.meta.request_id").value("req_webhook_004"));

        verifyNoInteractions(tenantRepository);
        verify(integrationEventRepository, org.mockito.Mockito.never()).save(any());
    }

    private Tenant activeTenant() {
        return Tenant.restore(
                TENANT_ID,
                "MAR",
                "MAR Tenant",
                "Asia/Ho_Chi_Minh",
                "VND",
                TenantStatus.ACTIVE,
                NOW,
                null,
                NOW,
                null
        );
    }
}
